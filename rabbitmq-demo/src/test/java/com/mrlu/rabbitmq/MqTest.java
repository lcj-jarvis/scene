package com.mrlu.rabbitmq;

import com.mrlu.rabbitmq.demo.controller.DelayController;
import com.mrlu.rabbitmq.test.DelayDemoV2Controller;
import com.mrlu.rabbitmq.topic.DelayDemoController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 简单de快乐
 * @create 2024-03-05 11:59
 */
@SpringBootTest
public class MqTest {

    @Autowired
    private DelayDemoController delayDemoController;

    @Test
    public void send() {
        for (int i = 1; i <=3; i++) {
            //delayDemoController.sendDelayedMessageToBatchQueue("batchQueue延时消息" + i, i * 1000);
            delayDemoController.sendDelayedMessageToOneQueue("oneQue延时消息" + i, -1);
        }

    }


    @Autowired
    private DelayController delayController;

    @Test
    public void testRepeat() {
        long currentTimeMillis = System.currentTimeMillis();
        delayController.sendDelayedMessage("测试消息幂等性" + currentTimeMillis, 1000, String.valueOf(currentTimeMillis));
    }

    @Autowired
    private DelayDemoV2Controller v1Controller;

    @Test
    public void sendv1() {
        for (int i = 1; i <=5; i++) {
            v1Controller.sendDelayedMessage("延时消息" + i, -1);
        }

    }

}
