package com.mrlu.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler;

import com.mrlu.server.val.StudentDemoValObject;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * (StudentDemo)实体类
 *
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
@Data
@Accessors(chain = true)
@TableName(value = "t_student_demo", autoResultMap = true)
public class StudentDemo implements Serializable {
    private static final long serialVersionUID = -75941339471069023L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    @TableField(typeHandler = MybatisEnumTypeHandler.class)
    private StudentDemoValObject.FinishEnum finish;

    /**
     * 逻辑删除字段
     */
    @TableLogic
    private Integer deleteFlag;

}

