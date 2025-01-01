package com.mrlu.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author 简单de快乐
 * @create 2024-04-17 15:39
 *
 * mybatis-plus自定义通用mapper方法
 */
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.mrlu.**.mapper")
public class CustomMapperMethodApplication {


    public static void main(String[] args) {
        SpringApplication.run(CustomMapperMethodApplication.class, args);
    }

}
