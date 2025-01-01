package com.linkcm.demo;

import org.springframework.context.annotation.Bean;

/**
 * @author 简单de快乐
 * @create 2023-08-18 16:02
 */
public interface Asseme {

    @Bean
    default Demo aDemo() {
        return new Demo();
    }
}
