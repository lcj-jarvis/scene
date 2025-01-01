package com.mrlu.rabbitmq.demo.controller;

import com.mrlu.rabbitmq.demo.mq.CustomDelayQueueConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;


@RestController
@Slf4j
@RequestMapping("/demo/delay")
public class DelayController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 先发30s的
     * http://localhost:8080/delay/hello/30000
     * 再发3s的
     * http://localhost:8080/delay/hello1/3000
     * 最后就会发现3s的先被收到，30s的后被收到
     * @param message
     * @param delayTime
     */
    @GetMapping("/{message}/{delayTime}/{messageId}")
    public void sendDelayedMessage(@PathVariable("message") String message,
                                   @PathVariable("delayTime") Integer delayTime,
                                   @PathVariable("message") String messageId) {
        log.info("当前时间：{}，发送一条消息给延时队列：{}, messageId={}", LocalDateTime.now(), message, messageId);

        MessagePostProcessor messagePostProcessor = msg -> {
            //设置消息的延时时长,单位是ms
            msg.getMessageProperties().setDelay(delayTime);
            msg.getMessageProperties().setMessageId(messageId);
            return msg;
        };

        rabbitTemplate.convertAndSend(CustomDelayQueueConfig.TEST_DELAY_EXCHANGE_NAME,
                CustomDelayQueueConfig.TEST_DELAY_ROUTING_KEY,
                "消息来自延迟队列：" + message, messagePostProcessor);
    }
}
