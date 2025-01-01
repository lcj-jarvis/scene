package com.mrlu.server.config;

import com.mrlu.server.entity.Animal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2024-04-16 22:12
 */
@Configuration
public class BeanConfig {

    @Bean(initMethod = "customizeInit")
    public Animal animal() {
        return new Animal();
    }

}
