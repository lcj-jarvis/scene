package com.mrlu.server;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
/**
 * @author 简单de快乐
 * @create 2024-01-08 14:48
 *
 *
 */
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.mrlu.**.mapper")
//@MapperScan("com.mrlu.server.mapper")
@ImportResource(locations = {"classpath:/config/*.xml"})
public class TcAppServer {

    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(TcAppServer.class, args);
        //System.out.println(application.getBean(MybatisConfiguration.class));
    }

}
