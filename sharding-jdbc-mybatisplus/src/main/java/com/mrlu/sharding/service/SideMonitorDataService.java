package com.mrlu.sharding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mrlu.sharding.dto.DataDTO;
import com.mrlu.sharding.dto.DataSearchDTO;
import com.mrlu.sharding.entity.SideMonitorData;
import com.mrlu.sharding.entity.SideMonitorDataBackup;

import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-08-14 23:21
 */
public interface SideMonitorDataService extends IService<SideMonitorData> {

    void saveData(DataDTO data);

    List<SideMonitorData> getMonitorDataList(DataSearchDTO searchDTO);

    void deleteData(DataSearchDTO searchDTO);

    void saveBackupData(List<SideMonitorDataBackup> backups);

}
