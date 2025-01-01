package com.mrlu.table.dto;

import lombok.Data;

/**
 * @author 简单de快乐
 * @create 2024-08-19 15:45
 */
@Data
public class UpdateDataDTO {

    /**
     * 修改的内容
     */
    private DataDTO dataDTO;

    /**
     * 修改的条件
     */
    private DataSearchDTO dataSearchDTO;

}
