package com.mrlu.table.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mrlu.table.dto.DataDTO;
import com.mrlu.table.dto.DataDTOListWrapper;
import com.mrlu.table.dto.DataSearchDTO;
import com.mrlu.table.dto.UpdateDataDTO;
import com.mrlu.table.entity.SideMonitorDataBackup;

import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-08-16 11:55
 */
public interface SideMonitorDataBackupService extends IService<SideMonitorDataBackup> {

    List<SideMonitorDataBackup> saveAndGetBackups(DataDTOListWrapper dtoListWrapper);

    void doubleSave(DataDTOListWrapper dtoListWrapper);

    void deleteData(DataSearchDTO searchDTO);

    void updateData(UpdateDataDTO updateDataDTO);

    void saveData(DataDTO data);
}
