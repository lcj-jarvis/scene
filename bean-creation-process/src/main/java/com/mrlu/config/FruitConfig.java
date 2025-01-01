package com.mrlu.config;

import com.mrlu.entity.Apple;
import com.mrlu.entity.Grape;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2023-10-18 18:09
 */
@Configuration
public class FruitConfig {


    /**
     * 给apple1实例指定custom销毁方法
     * @return
     */
    @Bean(destroyMethod = "custom")
    @Qualifier("a1")
    public Apple apple1() {
        return new Apple();
    }

    @Bean
    @Qualifier("a2")
    public Apple apple2() {
        return new Apple();
    }

    // 指定bean名称为g1
    @Bean(value = {"g1"}, destroyMethod = "grapeCustomDestroy")
    public Grape grape1() {
        Grape g1 = new Grape();
        g1.setName("g1");
        return g1;
    }

    // 指定bean名称为g2
    @Bean("g2")
    public Grape grape2() {
        Grape g2 = new Grape();
        g2.setName("g2");
        return g2;
    }

}
