package com.mrlu.table.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.mrlu.table.dto.DataSearchDTO;
import com.mrlu.table.dto.UpdateDataDTO;
import com.mrlu.table.entity.SideMonitorData;
import com.mrlu.table.entity.SideMonitorDataBackup;

import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-08-14 23:21
 */
public interface SideMonitorDataService extends IService<SideMonitorData> {

    void saveBatchData(List<SideMonitorDataBackup> originDataList);

    List<SideMonitorData> getMonitorDataList(DataSearchDTO searchDTO);

    void deleteData(DataSearchDTO searchDTO);

    void updateData(UpdateDataDTO updateDataDTO);

    void saveData(SideMonitorDataBackup dataBackup);
}
