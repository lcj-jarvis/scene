package com.mrlu.rabbitmq.test;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * @author 简单de快乐
 * @create 2024-03-05 11:55
 */
@Component
@Slf4j
public class DelayDemoV2Consumer {


    @RabbitListener(queues = DelayQueueDemoV2Config.QUEUE_V2)
    public void receive(Channel channel, Message message) throws IOException, InterruptedException {
        String msg = new String(message.getBody());
        int second = new Random().nextInt(5);
        log.info("v2当前消费线程={}，当前时间：{}，收到延迟队列的消息：{}，业务执行时间={}", Thread.currentThread().getName(), LocalDateTime.now(), msg, second);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
