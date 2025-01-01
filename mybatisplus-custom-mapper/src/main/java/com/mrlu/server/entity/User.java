package com.mrlu.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mrlu.server.config.CustomTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 用户表
 * 设置逻辑删除字段,并且逻辑删除字段不 select 出来
 *
 * @author miemie
 * @since 2018-08-12
 */
@Data
@Accessors(chain = true)
@TableName("t_sys_user")
public class User {
    private Long id;
    private String name;
    private Integer age;

    @TableField(typeHandler = CustomTypeHandler.class)
    private String email;

    @Version
    private Integer version;

    @TableLogic(value = "0", delval = "1")
    @TableField(select = false)
    private Integer deleted;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Timestamp createTime;
}
