package com.mrlu.rabbitmq.test;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 简单de快乐
 * @date 2021-07-08 22:41
 *
 * 延迟队列的配置
 *
 * 参考延时队列实战图进行配置
 */
@Configuration
public class DelayQueueDemoV2Config {

    /**
     * 队列
     */
    public static final String QUEUE_V2 = "queue-v3";

    /**
     * 交换机
     */
    public static final String DELAY_EXCHANGE_NAME_V2 = "delayed-exchange-v3";

    public static final String ROUTING_KEY_V2 = "routingKey_v3";

    /**
     * 声明交换机，因为这个交换机类型是插件自定义的，所以使用自定义的交换机
     */
    @Bean
    public CustomExchange delayedExchangeDemoV2() {
        Map<String, Object> arguments = new HashMap<>(16);
        arguments.put("x-delayed-type", "direct");
        return new CustomExchange(DELAY_EXCHANGE_NAME_V2, "x-delayed-message", true, false, arguments);
    }


    @Bean
    public Queue delayedQueueV2() {
        return new Queue(QUEUE_V2);
    }



    /**
     * 队列绑定交换机
     */
    @Bean
    public Binding bindingV2(@Qualifier("delayedQueueV2") Queue oneDelayedQueueV1,
                             @Qualifier("delayedExchangeDemoV2")CustomExchange delayedExchangeDemoV1) {
        return BindingBuilder.bind(oneDelayedQueueV1)
                .to(delayedExchangeDemoV1)
                .with(ROUTING_KEY_V2).noargs();
    }
}
