package com.mrlu.sharding.route;

import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.*;

/**
 * @author 简单de快乐
 * @create 2024-08-19 14:25
 *
 * 自定义分表算法
 */
@Slf4j
public class CustomTableShardingAlgorithm implements StandardShardingAlgorithm<Date> {

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Date> shardingValue) {
        // Check type and convert to Date
        Date collectTime = shardingValue.getValue();
        // 使用 Calendar 获取月份
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(collectTime);
        // Calendar 的月份从 0 开始，所以要 +1
        int month = calendar.get(Calendar.MONTH) + 1;
        //String tableName = "t_side_monitor_data_" + String.format("%02d", month);

        String tableName = "t_side_monitor_data_" + month;
        log.info("Precise sharding, table={}", tableName);
        return tableName;
    }

    /*
    * 根据日期范围查询的表
    */
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Date> shardingValue) {
        // 范围查询时候要查询哪些表
        Collection<String> result = new LinkedHashSet<>();
        Range<Date> valueRange = shardingValue.getValueRange();

        Calendar calendar = Calendar.getInstance();

        // 获取起始日期的年份和月份
        calendar.setTime(valueRange.lowerEndpoint());
        int startYear = calendar.get(Calendar.YEAR);
        int startMonth = calendar.get(Calendar.MONTH) + 1;

        // 获取结束日期的年份和月份
        calendar.setTime(valueRange.upperEndpoint());
        int endYear = calendar.get(Calendar.YEAR);
        int endMonth = calendar.get(Calendar.MONTH) + 1;
        // 处理跨年情况
        for (int year = startYear; year <= endYear; year++) {
            int start = (year == startYear) ? startMonth : 1;
            int end = (year == endYear) ? endMonth : 12;

            for (int month = start; month <= end; month++) {
                String tableName = "t_side_monitor_data_" + month;
                if (availableTargetNames.contains(tableName)) {
                    result.add(tableName);
                }
            }
        }
        log.info("Range sharding, tables={}", result);
        return result;
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public String getType() {
        return "TABLE-CUSTOM";
    }

    @Override
    public void init(Properties properties) {

    }
}
