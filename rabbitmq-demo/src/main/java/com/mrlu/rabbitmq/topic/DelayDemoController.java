package com.mrlu.rabbitmq.topic;

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
public class DelayDemoController {

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
    @GetMapping("/batch/{message}/{delayTime}")
    public void sendDelayedMessageToBatchQueue(@PathVariable("message") String message,
                                               @PathVariable("delayTime") Integer delayTime) {
        log.info("当前时间：{}，发送一条消息给批量延时队列：{}", LocalDateTime.now().toString(), message);

        MessagePostProcessor messagePostProcessor = msg -> {
            //设置消息的延时时长,单位是ms
            msg.getMessageProperties().setDelay(delayTime);
            return msg;
        };

        rabbitTemplate.convertAndSend(DelayQueueDemoConfig.TEST_DELAY_EXCHANGE_NAME, DelayQueueDemoConfig.BATCH_DELAY_ROUTING_KEY, message, messagePostProcessor);
    }

    @GetMapping("/one/{message}/{delayTime}")
    public void sendDelayedMessageToOneQueue(@PathVariable("message") String message,
                                             @PathVariable("delayTime") Integer delayTime) {
        log.info("当前时间：{}，发送一条消息给单条延时队列：{}", LocalDateTime.now(), message);

        MessagePostProcessor messagePostProcessor = msg -> {
            //设置消息的延时时长,单位是ms
            msg.getMessageProperties().setDelay(delayTime);
            return msg;
        };

        rabbitTemplate.convertAndSend(DelayQueueDemoConfig.TEST_DELAY_EXCHANGE_NAME, DelayQueueDemoConfig.ONE_DELAY_ROUTING_KEY, message, messagePostProcessor);
    }
}
