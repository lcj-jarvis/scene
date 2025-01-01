package com.mrlu.sharding.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author 简单de快乐
 * @create 2024-08-19 10:32
 */
@Data
public class DataSearchDTO {

    private String type;

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date begin;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date end;

}
