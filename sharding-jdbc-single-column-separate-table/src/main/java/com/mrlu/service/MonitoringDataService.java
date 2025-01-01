package com.mrlu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mrlu.dto.DataDTO;
import com.mrlu.entity.MonitoringData;

import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-11-13 22:04
 */
public interface MonitoringDataService  extends IService<MonitoringData> {
    List<MonitoringData> getMonitoringData(DataDTO dataDTO);

    boolean addSingleData(MonitoringData data);

    boolean batchAdd(List<MonitoringData> dataList);

    boolean delete(DataDTO dataDTO);

}
