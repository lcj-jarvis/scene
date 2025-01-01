package com.mrlu.entity;

import lombok.Data;

import javax.annotation.PreDestroy;

/**
 * @author 简单de快乐
 * @create 2023-10-18 18:16
 */
@Data
public class Grape implements AutoCloseable{

    private String name;

    @PreDestroy
    public void preDestroy() {
        System.out.println("=========Grape=preDestroy====1===================");
    }

    @Override
    public void close() throws Exception {
        System.out.println("==========Grape====close来自AutoCloseable===2===============");
    }

    private void grapeCustomDestroy() {
        System.out.println("==========grape1====grapeCustomDestroy===3===============");
    }
}
