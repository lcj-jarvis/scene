package com.mrlu.sharding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.sharding.dto.DataDTO;
import com.mrlu.sharding.dto.DataSearchDTO;
import com.mrlu.sharding.entity.SideMonitorData;
import com.mrlu.sharding.entity.SideMonitorDataBackup;
import com.mrlu.sharding.mapper.SideMonitorDataMapper;
import com.mrlu.sharding.service.SideMonitorDataBackupService;
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
    private SideMonitorDataBackupService sideMonitorDataBackupService;

    @Override
    @Transactional
    public void saveData(DataDTO data) {
        // 双写。先记录到原始表，再记录到分表
        SideMonitorDataBackup dataBackup = SideMonitorDataBackup.from(data);
        sideMonitorDataBackupService.save(dataBackup);
        // 设置backupId，用于数据双写校验
        SideMonitorData monitorData = SideMonitorData.from(data);
        monitorData.setBackupId(dataBackup.getId());
        save(monitorData);
    }

    @Override
    public List<SideMonitorData> getMonitorDataList(DataSearchDTO searchDTO) {
        LambdaQueryWrapper<SideMonitorData> wrapper = getWrapper(searchDTO);
        return list(wrapper);
    }

    private LambdaQueryWrapper<SideMonitorData> getWrapper(DataSearchDTO searchDTO) {
        LambdaQueryWrapper<SideMonitorData> wrapper = new LambdaQueryWrapper<SideMonitorData>()
                .eq(StringUtils.isNotEmpty(searchDTO.getType()), SideMonitorData::getType, searchDTO.getType())
                .eq(StringUtils.isNotEmpty(searchDTO.getName()), SideMonitorData::getName, searchDTO.getName())
                //.like(StringUtils.isNotEmpty(searchDTO.getName()), SideMonitorData::getName, searchDTO.getName())
                .ge(searchDTO.getBegin() != null, SideMonitorData::getCollectTime, searchDTO.getBegin())
                .le(searchDTO.getEnd() != null, SideMonitorData::getCollectTime, searchDTO.getEnd());
                // 排序有bug
                //https://maimai.cn/article/detail?fid=1768295449&efid=MY7fxdOESSt6KKi4XN7FQA
                //.orderByDesc(SideMonitorData::getCollectTime);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteData(DataSearchDTO searchDTO) {
        // 双删除
        sideMonitorDataBackupService.deleteData(searchDTO);
        LambdaQueryWrapper<SideMonitorData> wrapper = getWrapper(searchDTO);
        remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBackupData(List<SideMonitorDataBackup> backups) {
        List<SideMonitorData> dataList = new ArrayList<>();
        for (SideMonitorDataBackup data : backups) {
            SideMonitorData monitorData = SideMonitorData.from(data);
            dataList.add(monitorData);
        }
        saveBatch(dataList);
    }


}
