package com.mrlu;

import com.linkcm.demo.Demo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2023-08-25 16:04
 */
@Configuration
public class DeferredDemoConfig {

    @Bean
    public Demo deferredDemo() {
        Demo demo = new Demo();
        return demo;
    }
}
