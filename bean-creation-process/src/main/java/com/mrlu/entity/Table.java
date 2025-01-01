package com.mrlu.entity;

import lombok.Data;

/**
 * @author 简单de快乐
 * @create 2024-12-26 18:46
 */
@Data
public class Table {

    public Table() {
    }

    public Table(Desk desk) {
        this.desk = desk;
    }

    private Desk desk;

}
