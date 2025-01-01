package com.mrlu.sharding;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author 简单de快乐
 * @create 2024-08-14 23:11
 *
 * 分库分表，结合mybatis-plus动态数据源
 *
 * 参考文档：
 * https://blog.csdn.net/mrqiang9001/article/details/124175654
 * https://way2j.com/a/2090
 * https://www.bookstack.cn/read/shardingsphere-5.4.1-zh/47cfc7b03b18b09a.md
 *
 *  原始数据表和分表不在同一个数据库，将数据进行分库分表
 */
@EnableTransactionManagement
@MapperScan("com.mrlu.**.mapper")
@SpringBootApplication
//@SpringBootApplication(exclude = {DruidDataSourceAutoConfigure.class})
public class SparateDBAndTableAppServer {
    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(SparateDBAndTableAppServer.class, args);
    }
}
