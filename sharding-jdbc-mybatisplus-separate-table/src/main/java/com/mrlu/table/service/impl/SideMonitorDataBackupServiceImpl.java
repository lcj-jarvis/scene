package com.mrlu.table.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.table.dto.DataDTO;
import com.mrlu.table.dto.DataDTOListWrapper;
import com.mrlu.table.dto.DataSearchDTO;
import com.mrlu.table.dto.UpdateDataDTO;
import com.mrlu.table.entity.SideMonitorData;
import com.mrlu.table.entity.SideMonitorDataBackup;
import com.mrlu.table.mapper.SideMonitorDataBackupMapper;
import com.mrlu.table.service.SideMonitorDataBackupService;
import com.mrlu.table.service.SideMonitorDataService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-08-16 11:56
 */
@Service
public class SideMonitorDataBackupServiceImpl  extends ServiceImpl<SideMonitorDataBackupMapper, SideMonitorDataBackup>  implements SideMonitorDataBackupService {

    @Autowired
    private SideMonitorDataService sideMonitorDataService;

    // ShardingSphereConnection
    @Override
    @DS("mysql")
    public List<SideMonitorDataBackup> saveAndGetBackups(DataDTOListWrapper dtoListWrapper) {
        List<SideMonitorDataBackup> backups = new ArrayList<>();
        for (DataDTO dataDTO : dtoListWrapper.getDataDTOList()) {
            SideMonitorDataBackup dataBackup = SideMonitorDataBackup.from(dataDTO);
            backups.add(dataBackup);
        }
        saveBatch(backups);
        return backups;
    }

    // ShardingSphereConnection
    @Override
    @DS("mysql")
    @DSTransactional
    public void doubleSave(DataDTOListWrapper dtoListWrapper) {
        List<SideMonitorDataBackup> backups = saveAndGetBackups(dtoListWrapper);
        sideMonitorDataService.saveBatchData(backups);
    }

    /**
     * 双删除，先删除原始库，再删除分库
     * @param searchDTO
     */
    @Override
    @DS("mysql")
    @DSTransactional
    public void deleteData(DataSearchDTO searchDTO) {
        LambdaQueryWrapper<SideMonitorDataBackup> wrapper = getWrapper(searchDTO);
        remove(wrapper);
        sideMonitorDataService.deleteData(searchDTO);
    }

    private LambdaQueryWrapper<SideMonitorDataBackup> getWrapper(DataSearchDTO searchDTO) {
        LambdaQueryWrapper<SideMonitorDataBackup> wrapper = new LambdaQueryWrapper<SideMonitorDataBackup>()
                .eq(StringUtils.isNotEmpty(searchDTO.getType()), SideMonitorDataBackup::getType, searchDTO.getType())
                .eq(StringUtils.isNotEmpty(searchDTO.getName()), SideMonitorDataBackup::getName, searchDTO.getName())
                .ge(searchDTO.getBegin() != null, SideMonitorDataBackup::getCollectTime, searchDTO.getBegin())
                .le(searchDTO.getEnd() != null, SideMonitorDataBackup::getCollectTime, searchDTO.getEnd());
        return wrapper;
    }

    /**
    * 双修改，先修改原始库，再修改分库
    */
    @Override
    @DS("mysql")
    @DSTransactional
    public void updateData(UpdateDataDTO updateDataDTO) {
        DataDTO dataDTO = updateDataDTO.getDataDTO();
        // 不允许修改分库分表的字段，否则会报错
        dataDTO.setType(null);
        dataDTO.setCollectTime(null);

        SideMonitorDataBackup dataBackup = SideMonitorDataBackup.from(dataDTO);
        DataSearchDTO searchDTO = updateDataDTO.getDataSearchDTO();
        LambdaQueryWrapper<SideMonitorDataBackup> wrapper = getWrapper(searchDTO);
        update(dataBackup, wrapper);
        sideMonitorDataService.updateData(updateDataDTO);
    }

    @Override
    @DS("mysql")
    @DSTransactional
    public void saveData(DataDTO data) {
        // 双写。先记录到原始表，再记录到分表
        SideMonitorDataBackup dataBackup = SideMonitorDataBackup.from(data);
        save(dataBackup);
        sideMonitorDataService.saveData(dataBackup);
    }



}
