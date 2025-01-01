package com.mrlu.protect.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 简单de快乐
 * @create 2023-06-05 18:09
 */
@Data
@EqualsAndHashCode
public class Product {
    private Double price;
    private String name;
    private Integer id;
}
