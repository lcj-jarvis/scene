package com.mrlu.config;

import com.mrlu.entity.Desk;
import com.mrlu.entity.Son;
import com.mrlu.entity.Tv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2024-12-22 22:57
 */
@Configuration
public class HouseConfig {

    @Bean
    public Tv tv() {
        Tv tv = new Tv();
        tv.setName("电视");
        return tv;
    }


    @Bean
    public Desk desk(){
        Desk desk = new Desk();
        desk.setBrand("桌子");
        return desk;
    }

    @Bean
    public Son son() {
        Son son = new Son();
        son.setName("Son");
        return son;
    }
}
