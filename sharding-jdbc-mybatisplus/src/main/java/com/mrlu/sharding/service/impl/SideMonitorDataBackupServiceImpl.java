package com.mrlu.sharding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.sharding.dto.DataDTOListWrapper;
import com.mrlu.sharding.mapper.SideMonitorDataBackupMapper;
import com.mrlu.sharding.dto.DataDTO;
import com.mrlu.sharding.dto.DataSearchDTO;
import com.mrlu.sharding.entity.SideMonitorDataBackup;
import com.mrlu.sharding.service.SideMonitorDataBackupService;
import com.mrlu.sharding.service.SideMonitorDataService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-08-19 16:25
 */
@Service
public class SideMonitorDataBackupServiceImpl extends ServiceImpl<SideMonitorDataBackupMapper, SideMonitorDataBackup>  implements SideMonitorDataBackupService {

    @Autowired
    private SideMonitorDataService sideMonitorDataService;

    @Autowired
    private SideMonitorDataBackupMapper sideMonitorDataBackupMapper;

    /**
     * 双删除，先删除原始库，再删除分库
     * @param searchDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteData(DataSearchDTO searchDTO) {
        LambdaQueryWrapper<SideMonitorDataBackup> wrapper = getWrapper(searchDTO);
        remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doubleSave(DataDTOListWrapper dtoListWrapper) {
        List<SideMonitorDataBackup> backups = new ArrayList<>();
        for (DataDTO dataDTO : dtoListWrapper.getDataDTOList()) {
            SideMonitorDataBackup dataBackup = SideMonitorDataBackup.from(dataDTO);
            backups.add(dataBackup);
        }
        // https://blog.csdn.net/HHCS231/article/details/137481669
        // 这里的saveBatch方法不会生成自增id
        saveBatch(backups);
        //for (SideMonitorDataBackup backup : backups) {
        //    sideMonitorDataBackupMapper.insert(backup);
        //}

        sideMonitorDataService.saveBackupData(backups);
    }

    private LambdaQueryWrapper<SideMonitorDataBackup> getWrapper(DataSearchDTO searchDTO) {
        LambdaQueryWrapper<SideMonitorDataBackup> wrapper = new LambdaQueryWrapper<SideMonitorDataBackup>()
                .eq(StringUtils.isNotEmpty(searchDTO.getType()), SideMonitorDataBackup::getType, searchDTO.getType())
                .eq(StringUtils.isNotEmpty(searchDTO.getName()), SideMonitorDataBackup::getName, searchDTO.getName())
                .ge(searchDTO.getBegin() != null, SideMonitorDataBackup::getCollectTime, searchDTO.getBegin())
                .le(searchDTO.getEnd() != null, SideMonitorDataBackup::getCollectTime, searchDTO.getEnd());
        return wrapper;
    }
}
