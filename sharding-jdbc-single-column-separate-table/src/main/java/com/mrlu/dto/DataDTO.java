package com.mrlu.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-11-13 22:07
 */
@Data
public class DataDTO {
    private List<String> equipmentNos;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date begin;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date end;
    String monitorType;
    String businessType;
}
