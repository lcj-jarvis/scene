package com.mrlu.rabbitmq.demo.mq;

import com.mrlu.rabbitmq.demo.entity.MessageRecord;
import com.mrlu.rabbitmq.demo.service.MessageRecordService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author 简单de快乐
 * @date 2021-07-08 22:37
 *
 * 消息幂等性保证
 * https://blog.csdn.net/bookssea/article/details/123119980
 */
@Component
@Slf4j
public class CustomDelayQueueConsumer {

    @Value("${spring.rabbitmq.listener.simple.retry.max-attempts}")
    private Integer maxRetryTime;

    private final ThreadLocal<Integer> retryCount = ThreadLocal.withInitial(() -> 0);

    public static final Set<String> set = new HashSet<String>();

    @Autowired
    protected PlatformTransactionManager platformTransactionManager;

    @Autowired
    protected TransactionDefinition transactionDefinition;

    @Autowired
    private MessageRecordService messageRecordService;

    /**
        一般情况：
        1、情况一：保存messageId失败（非DuplicateKeyException）--> 事务回滚 --> 触发重试
        2、情况二：保存messageId成功 --> 执行业务逻辑失败 --> 事务回滚 --> 触发重试

        比较极端的情况分析：
        1、情况一
        事务提交，消息被消费，最后的ack失败，抛出异常，触发重试 （第一次重试）
        --> 保存messageId失败 -> DuplicateKeyException
        --> 再次ack失败，抛出异常 （第二次），继续触发重试 -....-> 第4次结束
        --> 再次ack成功，消息不入队。执行完成

        2、情况二
        事务准备提交的时候，服务挂了，消息还在mq，然后重启服务。这时候重试，会一直穿透完所有逻辑。
        对于发送通知的，这时候一键通点通知查看详情页，要求页面通过业务侧的接口查看到所有的最新内容。

        3、情况三
        事务提交了，准备最后ack的时候，服务挂了，消息还在mq，然后重启服务，走下面的（1）处逻辑。
        （1） 保存messageId -> DuplicateKeyException --> ack
        如果ack失败，再次重试
       */
    @RabbitListener(queues = CustomDelayQueueConfig.TEST_DELAY_QUEUE_NAME)
    public void receive(Channel channel, Message message) throws IOException, InterruptedException {
        String msg = new String(message.getBody());
        String messageId = message.getMessageProperties().getMessageId();
        log.info("消费线程={}，当前时间：{}，收到延迟队列的消息：{}，msgId={}", Thread.currentThread().getName(), LocalDateTime.now(), msg, messageId);

        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        boolean processed = false;
        try {
            try {
                // 全局msgId入库实现幂等性
                saveMsgId(messageId);
                // 执行业务逻辑
                doSomeThing();
                log.info("~~~~~~~~~~~执行业务逻辑完成~~~~~~~~");
                // 提交事务
                //TimeUnit.SECONDS.sleep(6666666);
                platformTransactionManager.commit(transactionStatus);
            } catch (DuplicateKeyException e) {
                log.error("duplicate consume message;messageId={}, msg={}", messageId, msg);
                processed = true;
                throw e;
            }
        } catch (Exception e) {
            platformTransactionManager.rollback(transactionStatus);
            if (!processed) {
                // 手动回滚事务
                Integer retryTime = retryCount.get();
                if ((++retryTime) >= maxRetryTime) {
                    log.error("超过最大重试次数;retryTime={}, messageId={}, msg={}", retryTime, messageId, msg);
                    // 达到最大重试次数，执行某些业务逻辑。这是一个新的事务。
                    doOther();
                    //拒绝确认消息，并且不重新入队
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                    retryCount.remove();
                } else {
                    // 更新重试次数
                    retryCount.set(retryTime);
                }
                // 抛出异常spring才能触发重试
                throw e;
            }
        }
        // 事务提交后，再确认消息消费
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    private void saveMsgId(String messageId) {
        messageRecordService.save(new MessageRecord().setMessageId(messageId));
        //int a = 1/0;
    }

    private void doSomeThing() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
        log.info("~~~~~~~~~~~执行业务逻辑中~~~~~~~~");
        //int a = 1/0;
    }


    private void doOther() throws InterruptedException {
        log.info("~~~~~~~~~~~达到最大重试次数后，需要执行逻辑~~~~~~~~");
    }
}
