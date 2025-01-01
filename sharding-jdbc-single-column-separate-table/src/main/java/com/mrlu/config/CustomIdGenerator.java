package com.mrlu.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 自定义雪花算法生成分布式id。
 * 这里我们借助hutool工具
 */
@Configuration
public class CustomIdGenerator implements IdentifierGenerator {

    @Autowired
    private  Snowflake snowflake;

    @Bean
    public Snowflake snowflake() {
        // 指定终端ID (workerId) 和 数据中心ID (datacenterId)。
        // 这里做得好点，可以改成配置化。
        // 机器ID
        long workerId = 1;
        // 数据中心ID
        long datacenterId = 1;
        // 创建Snowflake生成器
        Snowflake snowflake = IdUtil.getSnowflake(workerId, datacenterId);
        return snowflake;
    }

    @Override
    public Long nextId(Object entity) {
        return snowflake.nextId();
    }
}