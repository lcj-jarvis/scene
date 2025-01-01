package com.mrlu.entity;

import lombok.Data;

import javax.annotation.PreDestroy;

/**
 * @author 简单de快乐
 * @create 2023-10-13 16:34
 */
@Data
public class Person {

    private String name;

    @PreDestroy
    public void preDestroy() {
        System.out.println("=========Person=preDestroy====1===================");
    }

    private void captainDestroy() {
        System.out.println("=========captainDestroy====2===================");
    }
}
