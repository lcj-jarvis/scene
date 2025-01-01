package com.mrlu.rabbitmq.topic;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author 简单de快乐
 * @create 2024-03-05 11:55
 */
@Component
@Slf4j
public class DelayDemoConsumer {


    @RabbitListener(queues = DelayQueueDemoConfig.BATCH_DELAY_QUEUE_NAME)
    public void receiveFromBacthQueue(Channel channel, Message message) throws IOException, InterruptedException {
        String msg = new String(message.getBody());
        int second = new Random().nextInt(5);
        log.info("bacthQueue当前消费线程={}，当前时间：{}，收到延迟队列的消息：{}，业务执行时间={}", Thread.currentThread().getName(), LocalDateTime.now(), msg, second);
        TimeUnit.SECONDS.sleep(second);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


    @RabbitListener(queues = DelayQueueDemoConfig.ONE_DELAY_QUEUE_NAME)
    public void receiveFromOneQueue(Channel channel, Message message) throws IOException, InterruptedException {
        String msg = new String(message.getBody());
        int second = new Random().nextInt(5);
        log.info("oneQueue当前消费线程={}，当前时间：{}，收到延迟队列的消息：{}，业务执行时间={}", Thread.currentThread().getName(), LocalDateTime.now(), msg, second);
        //TimeUnit.SECONDS.sleep(second);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
