package com.mrlu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.ConfigurableEnvironment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author 简单de快乐
 * @create 2023-10-13 16:31
 */
@SpringBootApplication
@Slf4j
@ImportResource(locations = {"classpath:spring.xml"})
public class BeanCreationApp {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(BeanCreationApp.class, args);
        String ip = InetAddress.getLocalHost().getHostAddress();
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String port = environment.getProperty("server.port");
        String path = environment.getProperty("server.servlet.context-path");
        log.info("访问地址：http://{}:{}{}doc.html",ip,port,path);

        // 测试bean实例的销毁
        applicationContext.close();
    }

}
