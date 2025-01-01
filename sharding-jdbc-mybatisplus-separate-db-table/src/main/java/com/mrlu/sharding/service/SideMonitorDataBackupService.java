package com.mrlu.sharding.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mrlu.sharding.dto.DataDTOListWrapper;
import com.mrlu.sharding.dto.UpdateDataDTO;
import com.mrlu.sharding.dto.DataSearchDTO;
import com.mrlu.sharding.entity.SideMonitorDataBackup;

import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-08-16 11:55
 */
public interface SideMonitorDataBackupService extends IService<SideMonitorDataBackup> {

    @DS("mysql-test")
    List<SideMonitorDataBackup> saveAndGetBackups(DataDTOListWrapper dtoListWrapper);

    void doubleSaveSecond(DataDTOListWrapper dtoListWrapper);

    void deleteData(DataSearchDTO searchDTO);

    void updateData(UpdateDataDTO updateDataDTO);
}
