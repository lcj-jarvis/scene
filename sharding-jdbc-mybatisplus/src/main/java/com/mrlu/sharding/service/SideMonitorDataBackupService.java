package com.mrlu.sharding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mrlu.sharding.dto.DataDTOListWrapper;
import com.mrlu.sharding.dto.DataSearchDTO;
import com.mrlu.sharding.entity.SideMonitorDataBackup;

/**
 * @author 简单de快乐
 * @create 2024-08-16 11:55
 */
public interface SideMonitorDataBackupService extends IService<SideMonitorDataBackup> {

    void deleteData(DataSearchDTO searchDTO);

    void doubleSave(DataDTOListWrapper dtoListWrapper);

}
