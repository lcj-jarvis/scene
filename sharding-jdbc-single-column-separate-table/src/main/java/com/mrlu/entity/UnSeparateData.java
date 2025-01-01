package com.mrlu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author 简单de快乐
 * @create 2024-08-15 1:29
 *
 * 分表前的数据实体
 */
@Data
@TableName("t_un_separate_data")
public class UnSeparateData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String name;
    private String code;
    private Double value;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectTime;
}
