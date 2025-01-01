package com.mrlu.rabbitmq.demo.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.rabbitmq.demo.entity.MessageRecord;
import com.mrlu.rabbitmq.demo.mapper.MessageRecordMapper;
import com.mrlu.rabbitmq.demo.service.MessageRecordService;
import org.springframework.stereotype.Service;

/**
 * (NsMessageRecord)表服务实现类
 *
 * @author 简单de快乐
 * @since 2024-03-06 16:50:08
 */
@Service
public class MessageRecordServiceImpl extends ServiceImpl<MessageRecordMapper, MessageRecord> implements MessageRecordService {

}
