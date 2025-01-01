package com.mrlu.sharding.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author 简单de快乐
 * @create 2024-08-15 16:20
 */
@Data
public class DataDTO {
    private String type;
    private String name;
    private String code;
    private Double value;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectTime;
}
