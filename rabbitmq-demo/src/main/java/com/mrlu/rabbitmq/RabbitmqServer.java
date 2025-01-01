package com.mrlu.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author 简单de快乐
 * @create 2024-03-04 22:08
 */
@SpringBootApplication
@Slf4j
@MapperScan("com.mrlu.**.mapper")
public class RabbitmqServer {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(RabbitmqServer.class, args);
        String ip = InetAddress.getLocalHost().getHostAddress();
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String port = environment.getProperty("server.port");
        String path = environment.getProperty("server.servlet.context-path");
        log.info("访问地址：http://{}:{}{}doc.html",ip,port,path);
    }

}
