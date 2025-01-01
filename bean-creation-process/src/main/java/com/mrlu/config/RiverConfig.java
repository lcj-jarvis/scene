package com.mrlu.config;

import com.mrlu.entity.River;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2024-12-29 23:01
 */
@Configuration
public class RiverConfig {
    @Bean
    @Qualifier("r1")
    public River river1() {
        River river = new River();
        river.setName("长江");
        return river;
    }

    @Bean
    @Qualifier("r2")
    public River river2() {
        River river = new River();
        river.setName("黄河");
        return river;
    }
}
