package com.mrlu;

import com.linkcm.demo.Demo;
import org.springframework.context.annotation.Bean;

/**
 * @author 简单de快乐
 * @create 2023-08-18 23:14
 */
public class HelloWorld {

    public static void main(String[] args) {
        System.out.println("hello world");
    }

    @Bean
    public Demo helloDemo() {
        return new Demo();
    }
}
