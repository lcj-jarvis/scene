package com.mrlu.table.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mrlu.table.entity.SideMonitorData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface SideMonitorDataMapper extends BaseMapper<SideMonitorData> {

    List<SideMonitorData> selectByCondition(
            @Param("type") String type,
            @Param("name") String name,
            @Param("begin") Date begin,
            @Param("end") Date end);
}