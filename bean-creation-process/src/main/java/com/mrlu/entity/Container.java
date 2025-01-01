package com.mrlu.entity;

import lombok.Data;

/**
 * @author 简单de快乐
 * @create 2024-12-26 23:58
 */
@Data
public class Container {
    private Parent parent;

    public Container() {
        System.out.println("Container 无参构造");
    }

    public Container(Parent parent) {
        this.parent = parent;
        System.out.println("Container(Parent parent) 构造方法");
    }
}
