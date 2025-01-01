package com.mrlu.entity;

import lombok.Data;
import lombok.ToString;

import javax.annotation.PreDestroy;

/**
 * @author 简单de快乐
 * @create 2023-12-07 15:37
 */
@Data
@ToString
public class XMLBean {

    private String name;

    @PreDestroy
    public void preDestroy() {
        System.out.println(this + "=========XMLBean=preDestroy====1===================");
    }

    public void close()  {
        System.out.println(this + "==========close方法不是来自AutoCloseable======2============");
    }

    public void shutdown()  {
        System.out.println(this + "==========shutdown======2============");
    }

    public void xmLBeanCustomDestroy() {
        System.out.println(this + "==========xmLBeanCustomDestroy===2===========");
    }
}
