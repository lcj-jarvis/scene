package com.mrlu.rabbitmq;


import com.mrlu.rabbitmq.demo.entity.MessageRecord;
import com.mrlu.rabbitmq.demo.service.MessageRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 简单de快乐
 * @create 2024-03-06 16:54
 */
@SpringBootTest
public class MessageRecordTest {


    @Autowired
    private MessageRecordService messageRecordService;


    @Test
    public void test() {
        //org.springframework.dao.DuplicateKeyException:
        //### Error updating database.Cause:
        // com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException:
        // Duplicate entry 'aaa' for key 'message_id'
        MessageRecord messageRecord = new MessageRecord().setMessageId("aaa");
        messageRecordService.save(messageRecord);
    }

}
