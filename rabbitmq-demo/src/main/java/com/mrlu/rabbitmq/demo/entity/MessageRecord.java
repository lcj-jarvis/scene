package com.mrlu.rabbitmq.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * (NsMessageRecord)实体类
 *
 * @author 简单de快乐
 * @since 2024-03-06 16:50:08
 */
@Data
@Accessors(chain = true)
@TableName("t_message_record")
public class MessageRecord implements Serializable {
    private static final long serialVersionUID = -46719074231419663L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private Integer sysStatus;

    private Date sysCreateTime;

    private Date sysUpdateTime;

}

