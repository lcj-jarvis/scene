package com.mrlu.sharding.route;



import lombok.extern.slf4j.Slf4j;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;


import java.util.Collection;
import java.util.Properties;

/**
 * 自定义分库算法
 */
@Slf4j
public class CustomDatabaseShardingAlgorithm implements StandardShardingAlgorithm<String> {

    public static void main(String[] args) {
        int hash = "aaa".hashCode();
        int index = Math.abs(hash) % 3;
        System.out.println(index);
    }

    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<String> preciseShardingValue) {
        // 获取 type 字段的值
        String type = preciseShardingValue.getValue();
        // 基于字符串的哈希值决定数据库
        int hash = type.hashCode();
        int index = Math.abs(hash) % 2;
        // 根据哈希值选择数据库
        String database = null;
        switch (index) {
            case 0:
                database = "mysql01";
                break;
            case 1:
                database = "mysql02";
                break;
        }
        log.info("type={};database={}", type, database);
        return database;
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<String> rangeShardingValue) {
        log.info("tables={}", collection);
        return collection;
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public String getType() {
        return "DATABASE-CUSTOM";
    }

    @Override
    public void init(Properties properties) {

    }
}
