package com.mrlu.rabbitmq.topic;

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
public class DelayQueueDemoConfig {

    /**
     * 队列
     */
    public static final String BATCH_DELAY_QUEUE_NAME = "delayed-queue-batch";


    /**
     * 队列
     */
    public static final String ONE_DELAY_QUEUE_NAME = "delayed-queue-one";


    /**
     * 交换机
     */
    public static final String TEST_DELAY_EXCHANGE_NAME = "delayed-exchange-demo";

    /**
     * routingKey
     */
    public static final String BATCH_DELAY_ROUTING_KEY = "batch-delay-routingKey";

    public static final String ONE_DELAY_ROUTING_KEY = "one-delay-routingKey";

    /**
     * 声明交换机，因为这个交换机类型是插件自定义的，所以使用自定义的交换机
     */
    @Bean
    public CustomExchange delayedExchangeDemo() {
        Map<String, Object> arguments = new HashMap<>(16);
        arguments.put("x-delayed-type", "direct");
        return new CustomExchange(TEST_DELAY_EXCHANGE_NAME, "x-delayed-message", true, false, arguments);
    }

    /**
     * 声明队列
     * @return
     */
    @Bean
    public Queue batchDelayedQueue() {
        return new Queue(BATCH_DELAY_QUEUE_NAME);
    }

    @Bean
    public Queue oneDelayedQueue() {
        return new Queue(ONE_DELAY_QUEUE_NAME);
    }

    /**
     * 队列绑定交换机
     */
    @Bean
    public Binding bindingBatch(@Qualifier("batchDelayedQueue") Queue batchDelayedQueue,
                                @Qualifier("delayedExchangeDemo")CustomExchange delayedExchangeDemo) {
        return BindingBuilder.bind(batchDelayedQueue)
                .to(delayedExchangeDemo)
                .with(BATCH_DELAY_ROUTING_KEY).noargs();
    }

    /**
     * 队列绑定交换机
     */
    @Bean
    public Binding bindingOne(@Qualifier("oneDelayedQueue") Queue oneDelayedQueue,
                               @Qualifier("delayedExchangeDemo")CustomExchange delayedExchangeDemo) {
        return BindingBuilder.bind(oneDelayedQueue)
                .to(delayedExchangeDemo)
                .with(ONE_DELAY_ROUTING_KEY).noargs();
    }
}
