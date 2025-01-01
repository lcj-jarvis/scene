package com.mrlu.table.service.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.table.config.DataSourceConfiguration;
import com.mrlu.table.dto.DataDTO;
import com.mrlu.table.dto.DataDTOListWrapper;
import com.mrlu.table.dto.DataSearchDTO;
import com.mrlu.table.dto.UpdateDataDTO;
import com.mrlu.table.entity.SideMonitorData;
import com.mrlu.table.entity.SideMonitorDataBackup;
import com.mrlu.table.mapper.SideMonitorDataBackupMapper;
import com.mrlu.table.mapper.SideMonitorDataMapper;
import com.mrlu.table.service.SideMonitorDataBackupService;
import com.mrlu.table.service.SideMonitorDataService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SideMonitorDataServiceImpl extends ServiceImpl<SideMonitorDataMapper, SideMonitorData> implements SideMonitorDataService {

    @Autowired
    private SideMonitorDataMapper sideMonitorDataMapper;

    @Override
    @DS("sharding")
    @DSTransactional
    public void saveData(SideMonitorDataBackup dataBackup) {
        // 设置backupId，用于数据双写校验
        SideMonitorData monitorData = SideMonitorData.from(dataBackup);
        save(monitorData);
    }

    /**
     * 批量双写方式二的写法二
     * 通过Mybatis-Plus多数据源实现，通过@DSTransactional.
     * 可以不在
     * @see DataSourceConfiguration#dataSource()
     * 指定sharding-jdbc为primary。
     * */
    @Override
    @DS("sharding")
    @DSTransactional
    public void saveBatchData(List<SideMonitorDataBackup> originDataList) {
        doSaveBatch(originDataList);
    }

    private void doSaveBatch(List<SideMonitorDataBackup> originDataList) {
        List<SideMonitorData> dataList = new ArrayList<>();
        for (SideMonitorDataBackup data : originDataList) {
            SideMonitorData monitorData = SideMonitorData.from(data);
            dataList.add(monitorData);
        }
        saveBatch(dataList);
    }


    @Override
    @DS("sharding")
    public List<SideMonitorData> getMonitorDataList(DataSearchDTO searchDTO) {
        // 这样加上排序也会报错
        //return sideMonitorDataMapper.selectByCondition(searchDTO.getType(), searchDTO.getName(), searchDTO.getBegin(), searchDTO.getEnd());

        LambdaQueryWrapper<SideMonitorData> wrapper = new LambdaQueryWrapper<SideMonitorData>()
                .eq(StringUtils.isNotEmpty(searchDTO.getType()), SideMonitorData::getType, searchDTO.getType())
                .eq(StringUtils.isNotEmpty(searchDTO.getName()), SideMonitorData::getName, searchDTO.getName())
                //.like(StringUtils.isNotEmpty(searchDTO.getName()), SideMonitorData::getName, searchDTO.getName())
                .ge(searchDTO.getBegin() != null, SideMonitorData::getCollectTime, searchDTO.getBegin())
                .le(searchDTO.getEnd() != null, SideMonitorData::getCollectTime, searchDTO.getEnd());
                // 排序有bug
                //https://maimai.cn/article/detail?fid=1768295449&efid=MY7fxdOESSt6KKi4XN7FQA
                //.orderByDesc(SideMonitorData::getCollectTime);
        return list(wrapper);
    }

    @Override
    @DS("sharding")
    @DSTransactional
    public void deleteData(DataSearchDTO searchDTO) {
        LambdaQueryWrapper<SideMonitorData> wrapper = getWrapper(searchDTO);
        remove(wrapper);
    }

    private LambdaQueryWrapper<SideMonitorData> getWrapper(DataSearchDTO searchDTO) {
        LambdaQueryWrapper<SideMonitorData> wrapper = new LambdaQueryWrapper<SideMonitorData>()
                .eq(StringUtils.isNotEmpty(searchDTO.getType()), SideMonitorData::getType, searchDTO.getType())
                .eq(StringUtils.isNotEmpty(searchDTO.getName()), SideMonitorData::getName, searchDTO.getName())
                .ge(searchDTO.getBegin() != null, SideMonitorData::getCollectTime, searchDTO.getBegin())
                .le(searchDTO.getEnd() != null, SideMonitorData::getCollectTime, searchDTO.getEnd());
        return wrapper;
    }

    @Override
    @DS("sharding")
    @DSTransactional
    public void updateData(UpdateDataDTO updateDataDTO) {
        DataDTO dataDTO = updateDataDTO.getDataDTO();
        // 不允许修改分库分表的字段，否则会报错
        dataDTO.setType(null);
        dataDTO.setCollectTime(null);

        SideMonitorData monitorData = SideMonitorData.from(dataDTO);
        LambdaQueryWrapper<SideMonitorData> wrapper = getWrapper(updateDataDTO.getDataSearchDTO());
        update(monitorData, wrapper);
    }

}
