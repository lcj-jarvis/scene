package com.mrlu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.dto.DataDTO;
import com.mrlu.entity.MonitoringData;
import com.mrlu.mapper.MonitoringDataMapper;
import com.mrlu.service.MonitoringDataService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-11-13 22:04
 */
@Service
@Slf4j
public class MonitoringDataServiceImpl extends ServiceImpl<MonitoringDataMapper, MonitoringData> implements MonitoringDataService {

    @Override
    public List<MonitoringData> getMonitoringData(DataDTO dataDTO) {
        return queryByConditions(dataDTO.getEquipmentNos(), dataDTO.getBegin(),
                dataDTO.getEnd(), dataDTO.getMonitorType(), dataDTO.getBusinessType());
    }

    @Override
    @Transactional
    public boolean addSingleData(MonitoringData data) {
        save(data);
        //int a = 1/0;
        return true;
    }

    @Override
    @Transactional
    public boolean batchAdd(List<MonitoringData> dataList) {
        dataList.forEach(t -> {
            log.info(t.getEquipmentNo() + " Hash ': " + ((Math.abs(t.getEquipmentNo().hashCode()) % 10) + 1));
        });
        saveBatch(dataList);
        //int a = 1/0;
        return true;
    }

    @Override
    @Transactional
    public boolean delete(DataDTO dataDTO) {
        LambdaQueryWrapper<MonitoringData> wrapper = getLambdaQueryWrapper(dataDTO.getEquipmentNos(), dataDTO.getBegin(),
                dataDTO.getEnd(), dataDTO.getMonitorType(), dataDTO.getBusinessType());
        remove(wrapper);
        return true;
    }

    // 组合查询：支持多个条件
    public List<MonitoringData> queryByConditions(List<String> equipmentNos,
                                                  Date startTime,
                                                  Date endTime,
                                                  String monitorType,
                                                  String businessType) {
        LambdaQueryWrapper<MonitoringData> queryWrapper = getLambdaQueryWrapper(equipmentNos, startTime, endTime, monitorType, businessType);
        return list(queryWrapper);
    }

    private LambdaQueryWrapper<MonitoringData> getLambdaQueryWrapper(List<String> equipmentNos, Date startTime, Date endTime, String monitorType, String businessType) {
        LambdaQueryWrapper<MonitoringData> queryWrapper = new LambdaQueryWrapper<>();
        if (CollectionUtils.isNotEmpty(equipmentNos) && equipmentNos.size() == 1) {
            queryWrapper.eq(MonitoringData::getEquipmentNo, equipmentNos.get(0));
        } else if (CollectionUtils.isNotEmpty(equipmentNos) && equipmentNos.size() > 1) {
            queryWrapper.in(MonitoringData::getEquipmentNo, equipmentNos);
        }
        if (startTime != null && endTime != null) {
            queryWrapper.between(MonitoringData::getLastCollectTime, startTime, endTime);
        }
        if (monitorType != null) {
            queryWrapper.eq(MonitoringData::getMonitorType, monitorType);
        }
        if (businessType != null) {
            queryWrapper.eq(MonitoringData::getBuinessType, businessType);
        }
        return queryWrapper;
    }

}
