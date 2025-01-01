package com.mrlu.rabbitmq.test;

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
@RequestMapping("/demo/v2/delay")
public class DelayDemoV2Controller {

    @Autowired
    private RabbitTemplate rabbitTemplate;



    @GetMapping("/one/{message}/{delayTime}")
    public void sendDelayedMessage(@PathVariable("message") String message,
                                   @PathVariable("delayTime") Integer delayTime) {
        log.info("当前时间：{}，发送一条消息给单条延时队列：{}", LocalDateTime.now(), message);

        MessagePostProcessor messagePostProcessor = msg -> {
            //设置消息的延时时长,单位是ms
            msg.getMessageProperties().setDelay(delayTime);
            return msg;
        };
        rabbitTemplate.convertAndSend(DelayQueueDemoV2Config.DELAY_EXCHANGE_NAME_V2, DelayQueueDemoV2Config.ROUTING_KEY_V2, message, messagePostProcessor);
    }
}
