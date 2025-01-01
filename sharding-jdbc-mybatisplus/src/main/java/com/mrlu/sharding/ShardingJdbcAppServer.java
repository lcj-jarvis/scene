package com.mrlu.sharding;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author 简单de快乐
 * @create 2024-08-14 23:11
 *
 * 只分表不分库
 * https://github.com/apache/shardingsphere/issues/5317
 *
 * https://blog.csdn.net/Alian_1223/article/details/134892390
 * https://blog.csdn.net/mrqiang9001/article/details/124175654
 * https://way2j.com/a/2090
 *
 * 原始数据表和分表在同一个数据库，不分库
 *
 */
//@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.mrlu.**.mapper")
@SpringBootApplication(exclude = {DruidDataSourceAutoConfigure.class})
public class ShardingJdbcAppServer {
    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(ShardingJdbcAppServer.class, args);
    }

}
