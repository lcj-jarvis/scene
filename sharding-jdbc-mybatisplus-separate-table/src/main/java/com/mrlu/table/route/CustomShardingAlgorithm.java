package com.mrlu.table.route;

import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.*;

/**
 * 自定义分片策略
 * 先根据类型hash范围，再根据日期确定月份，最终确定所属的表
 */
@Slf4j
public class CustomShardingAlgorithm implements ComplexKeysShardingAlgorithm<String> {

    private static final String COLLECT_TIME = "collect_time";

    private static final String TYPE = "type";

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         ComplexKeysShardingValue<String> shardingValue) {
        Collection<String> tables = new HashSet<>();
        // 根据类型的hashCode取模
        int typeHashMod = getTypeHashMod(shardingValue);
        // 计算月份
        List<Integer> months = getMonths(shardingValue);
        for (Integer month : months) {
            // 保留两位数
            tables.add("t_side_monitor_data_" + String.format("%02d", typeHashMod * 12 + month));
        }
        log.info("tables={}", tables);
        return tables;
    }

    private int getTypeHashMod(ComplexKeysShardingValue shardingValue) {
        // 精确查询的字段
        Map<String, Collection<String>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        // 获取分片列的值
        int typeHashMod = Math.abs(columnNameAndShardingValuesMap.get(TYPE).iterator().next().hashCode() % 2);
        return typeHashMod;
    }

    private List<Integer> getMonths(ComplexKeysShardingValue shardingValue) {
        // 精确查询的字段
        Map<String, Collection<Date>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        if (columnNameAndShardingValuesMap.containsKey(COLLECT_TIME)) {
            // Check type and convert to Date
            Date collectTime = columnNameAndShardingValuesMap.get(COLLECT_TIME).iterator().next();
            // 使用 Calendar 获取月份
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(collectTime);
            // Calendar 的月份从 0 开始，所以要 +1
            int month = calendar.get(Calendar.MONTH) + 1;
            return Collections.singletonList(month);
        }

        // 范围查询的月份
        return computeMonths(shardingValue);
    }

    private List<Integer> computeMonths(ComplexKeysShardingValue shardingValue) {
        // 范围查询的字段
        Map<String, Range> columnNameAndRangeValuesMap = shardingValue.getColumnNameAndRangeValuesMap();
        Range<Date> valueRange = columnNameAndRangeValuesMap.get(COLLECT_TIME);
        // 范围查询时候要查询哪些表
        Collection<Integer> result = new LinkedHashSet<>();

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
                result.add(month);
            }
        }
        return new ArrayList<>(result);
    }


    @Override
    public String getType() {
        return "COMPLEX";
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}
