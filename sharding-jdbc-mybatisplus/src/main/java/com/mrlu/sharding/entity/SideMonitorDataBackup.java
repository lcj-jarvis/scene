package com.mrlu.sharding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.mrlu.sharding.dto.DataDTO;
import lombok.Data;

import java.util.Date;

/**
 * @author 简单de快乐
 * @create 2024-08-15 1:29
 *
 * 分表前的数据实体
 */
@Data
@TableName("t_side_monitor_data_backup")
public class SideMonitorDataBackup {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String name;
    private String code;
    private Double value;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectTime;

    public static SideMonitorDataBackup from(DataDTO sideMonitorData) {
        SideMonitorDataBackup sideMonitorDataBackup1 = new SideMonitorDataBackup();
        sideMonitorDataBackup1.setType(sideMonitorData.getType());
        sideMonitorDataBackup1.setName(sideMonitorData.getName());
        sideMonitorDataBackup1.setCode(sideMonitorData.getCode());
        sideMonitorDataBackup1.setValue(sideMonitorData.getValue());
        sideMonitorDataBackup1.setCollectTime(sideMonitorData.getCollectTime());
        return sideMonitorDataBackup1;
    }


}
