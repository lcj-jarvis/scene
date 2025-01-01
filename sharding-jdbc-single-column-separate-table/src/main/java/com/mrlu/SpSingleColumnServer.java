package com.mrlu;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author 简单de快乐
 * @create 2024-11-13 22:01
 *
 *  官网地址：
 *  https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/yaml-config/
 *
 *  application-separate_db_table.yml：
 *  （1）MonitoringData 根据 equipmentNo的hashcode进行取模分表
 *  application-separate_db_table.yml：
 *  （2）MonitoringData 先根据equipmentNo的hashcode进行取模分库，进行取模分表
 */
@MapperScan("com.mrlu.**.mapper")
// 移除默认的DruidDataSourceWrapper，使用ShardingSphereDataSource
@SpringBootApplication(exclude = {DruidDataSourceAutoConfigure.class})
public class SpSingleColumnServer {

    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(SpSingleColumnServer.class, args);
    }

}
