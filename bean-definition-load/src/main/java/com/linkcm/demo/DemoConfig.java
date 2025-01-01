package com.linkcm.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2023-08-17 18:01
 */
@Configuration
public class DemoConfig {

    @Bean
    public Demo demo10() {
        return new Demo();
    }
}
