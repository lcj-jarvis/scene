package com.mrlu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mrlu.entity.MonitoringData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MonitoringDataMapper extends BaseMapper<MonitoringData> {
    // 自定义查询方法，如果需要
}
