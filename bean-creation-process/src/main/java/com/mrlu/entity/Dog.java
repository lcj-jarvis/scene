package com.mrlu.entity;

import lombok.Data;

import javax.annotation.PreDestroy;

/**
 * @author 简单de快乐
 * @create 2023-10-13 16:33
 */
@Data
public class Dog {

    private String name;

    @PreDestroy
    public void preDestroy() {
        System.out.println("=========Dog=preDestroy====1===================");
    }

    public void close()  {
        System.out.println("======Dog====close方法不是来自AutoCloseable======2============");
    }
}
