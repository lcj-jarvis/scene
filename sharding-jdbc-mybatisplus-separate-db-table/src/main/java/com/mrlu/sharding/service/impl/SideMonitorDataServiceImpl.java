package com.mrlu.sharding.service.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.sharding.config.DataSourceConfiguration;
import com.mrlu.sharding.dto.DataDTOListWrapper;
import com.mrlu.sharding.dto.UpdateDataDTO;
import com.mrlu.sharding.entity.SideMonitorData;
import com.mrlu.sharding.mapper.SideMonitorDataBackupMapper;
import com.mrlu.sharding.service.SideMonitorDataBackupService;
import com.mrlu.sharding.dto.DataDTO;
import com.mrlu.sharding.dto.DataSearchDTO;
import com.mrlu.sharding.entity.SideMonitorDataBackup;
import com.mrlu.sharding.mapper.SideMonitorDataMapper;
import com.mrlu.sharding.service.SideMonitorDataService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SideMonitorDataServiceImpl extends ServiceImpl<SideMonitorDataMapper, SideMonitorData> implements SideMonitorDataService {

    @Autowired
    private SideMonitorDataBackupMapper sideMonitorDataBackupMapper;

    @Autowired
    private SideMonitorDataBackupService sideMonitorDataBackupService;

    @Override
    @Transactional
    public void saveData(DataDTO data) {
        //双写。先记录到原始表，再记录到分表
        SideMonitorDataBackup dataBackup = SideMonitorDataBackup.from(data);
        sideMonitorDataBackupMapper.insert(dataBackup);
        // 设置backupId，用于数据双写校验
        SideMonitorData monitorData = SideMonitorData.from(data);
        monitorData.setBackupId(dataBackup.getId());
        save(monitorData);
    }

    /**
    * 批量双写方式一
     * 如果不使用Mybatis-Plus多数据源，要求在
     * @see DataSourceConfiguration#dataSource()，指定sharding-jdbc为primary。
     * 同时application-sharding_jdbc.yml配置一个默认的数据库，用于不参与分库分表的表使用。
     *
     * saveBatch没返回自增id
     *     //sideMonitorDataBackupService.saveBatch(backups);
     *     https://blog.csdn.net/weixin_39893621/article/details/125657828
     *
     * 这样总的来说，事务做不到很好的控制。
    */
    // 写到default库。
    //for (SideMonitorDataBackup backup : backups) {
    //    sideMonitorDataBackupMapper.insert(backup);
    //}
    //int a = 1/0;
    @Override
    @Transactional
    public void saveBatchData(DataDTOListWrapper dtoListWrapper) {
        //双写。先记录到原始表，再记录到分表
        List<SideMonitorDataBackup> backups = new ArrayList<>();
        for (DataDTO dataDTO : dtoListWrapper.getDataDTOList()) {
            SideMonitorDataBackup dataBackup = SideMonitorDataBackup.from(dataDTO);
            backups.add(dataBackup);
        }
        // https://blog.csdn.net/weixin_39893621/article/details/125657828
        // saveBatch没返回自增id
        //sideMonitorDataBackupService.saveBatch(backups);

        // 这样才可以
        for (SideMonitorDataBackup backup : backups) {
            sideMonitorDataBackupMapper.insert(backup);
        }

        // 写到分库
        List<SideMonitorData> dataList = new ArrayList<>();
        for (SideMonitorDataBackup backup : backups) {
            SideMonitorData monitorData = SideMonitorData.from(backup);
            dataList.add(monitorData);
        }
        saveBatch(dataList);

        int a = 1/0;
    }

    /**
     * 批量双写方式二的写法一
     * 通过Mybatis-Plus多数据源实现，通过@DSTransactional.
     * 可以不在
     * @see DataSourceConfiguration#dataSource()
     * 指定sharding-jdbc为primary。
     * */
    @Override
    @DS("sharding")
    @DSTransactional
    public void saveBatchDataFirst(DataDTOListWrapper dtoListWrapper) {
        // 先写原始库
        List<SideMonitorDataBackup> backups = sideMonitorDataBackupService.saveAndGetBackups(dtoListWrapper);
        // 写到分库分表
        doSaveBatch(backups);
        //int a = 1/0;
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
    public void saveBatchDataSecond(List<SideMonitorDataBackup> originDataList) {
        doSaveBatch(originDataList);
        // int a = 1/0;
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
    public List<SideMonitorData> getMonitorDataList(DataSearchDTO searchDTO) {
        LambdaQueryWrapper<SideMonitorData> wrapper = new LambdaQueryWrapper<SideMonitorData>()
                .eq(StringUtils.isNotEmpty(searchDTO.getType()), SideMonitorData::getType, searchDTO.getType())
                .eq(StringUtils.isNotEmpty(searchDTO.getName()), SideMonitorData::getName, searchDTO.getName())
                //.like(StringUtils.isNotEmpty(searchDTO.getName()), SideMonitorData::getName, searchDTO.getName())
                .ge(searchDTO.getBegin() != null, SideMonitorData::getCollectTime, searchDTO.getBegin())
                .le(searchDTO.getEnd() != null, SideMonitorData::getCollectTime, searchDTO.getEnd())
                .orderByDesc(SideMonitorData::getCollectTime);
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
