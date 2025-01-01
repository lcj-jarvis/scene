package com.mrlu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author 简单de快乐
 * @create 2024-01-09 22:39
 */
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.mrlu.**.mapper")
public class LockServer {

    public static void main(String[] args) {
        SpringApplication.run(LockServer.class, args);
    }


}
