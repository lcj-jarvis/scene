package com.mrlu.entity;

import lombok.Data;

import javax.annotation.PreDestroy;

/**
 * @author 简单de快乐
 * @create 2023-10-13 16:34
 */
@Data
public class Cat {
    @PreDestroy
    public void preDestroy() {
        System.out.println("=========Cat=preDestroy====1===================");
    }

    public void shutdown()  {
        System.out.println("======Cat====shutdown======2============");
    }


}
