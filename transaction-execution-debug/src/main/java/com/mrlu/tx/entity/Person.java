package com.mrlu.tx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;


@Data
@Accessors(chain = true)
@TableName(value = "t_person", autoResultMap = true)
@ToString
public class Person implements Serializable {
    private static final long serialVersionUID = -75941339471069023L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private Integer age;


}

