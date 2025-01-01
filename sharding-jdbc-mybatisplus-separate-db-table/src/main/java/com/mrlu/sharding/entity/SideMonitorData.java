package com.mrlu.sharding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import com.mrlu.sharding.dto.DataDTO;

import java.util.Date;

/**
 * 对应分表后的数据
 */
@Data
@TableName("t_side_monitor_data")
public class SideMonitorData {
    private Long id;
    private String type;
    private String name;
    private String code;
    private Double value;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectTime;
    // 原始表id
    private Long backupId;


    public static SideMonitorData from(DataDTO dataDTO) {
        SideMonitorData sideMonitorData = new SideMonitorData();
        sideMonitorData.setType(dataDTO.getType());
        sideMonitorData.setName(dataDTO.getName());
        sideMonitorData.setCode(dataDTO.getCode());
        sideMonitorData.setValue(dataDTO.getValue());
        sideMonitorData.setCollectTime(dataDTO.getCollectTime());
        return sideMonitorData;
    }


    public static SideMonitorData from(SideMonitorDataBackup sideMonitorDataBackup) {
        SideMonitorData sideMonitorData = new SideMonitorData();
        sideMonitorData.setBackupId(sideMonitorDataBackup.getId());
        sideMonitorData.setType(sideMonitorDataBackup.getType());
        sideMonitorData.setName(sideMonitorDataBackup.getName());
        sideMonitorData.setCode(sideMonitorDataBackup.getCode());
        sideMonitorData.setValue(sideMonitorDataBackup.getValue());
        sideMonitorData.setCollectTime(sideMonitorDataBackup.getCollectTime());
        return sideMonitorData;
    }
}