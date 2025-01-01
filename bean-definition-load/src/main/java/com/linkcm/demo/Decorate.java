package com.linkcm.demo;

import org.springframework.context.annotation.Bean;

/**
 * @author 简单de快乐
 * @create 2023-08-18 16:04
 */
public interface Decorate extends Asseme {

    @Bean
    default Demo decorate() {
        return new Demo();
    }
}
