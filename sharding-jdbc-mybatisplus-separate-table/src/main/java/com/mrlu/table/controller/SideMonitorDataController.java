package com.mrlu.table.controller;


import com.mrlu.table.dto.DataDTO;
import com.mrlu.table.dto.DataDTOListWrapper;
import com.mrlu.table.dto.DataSearchDTO;
import com.mrlu.table.dto.UpdateDataDTO;
import com.mrlu.table.entity.SideMonitorData;
import com.mrlu.table.service.SideMonitorDataBackupService;
import com.mrlu.table.service.SideMonitorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitor-data")
public class SideMonitorDataController {

    @Autowired
    private SideMonitorDataService sideMonitorDataService;

    @Autowired
    private SideMonitorDataBackupService sideMonitorDataBackupService;

    @PostMapping("/save")
    public ResponseEntity<String> saveMonitorData(@RequestBody DataDTO data) {
        sideMonitorDataBackupService.saveData(data);
        return ResponseEntity.ok("Data saved successfully!");
    }

    @PostMapping("/doubleSave")
    public ResponseEntity<String> doubleSave(@RequestBody DataDTOListWrapper dtoListWrapper) {
        sideMonitorDataBackupService.doubleSave(dtoListWrapper);
        return ResponseEntity.ok("Data saved successfully!");
    }

    /**
    * 测试精确、模糊、范围查询
    */
    @PostMapping("/getMonitorDataList")
    public ResponseEntity<List<SideMonitorData>> getMonitorDataList(@RequestBody DataSearchDTO searchDTO) {
        return ResponseEntity.ok(sideMonitorDataService.getMonitorDataList(searchDTO));
    }

    /**
     * 测试删除
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteData(@RequestBody DataSearchDTO searchDTO) {
        sideMonitorDataBackupService.deleteData(searchDTO);
        return ResponseEntity.ok("Data deleteData successfully!");
    }

    /**
     * 测试修改
     */
    @PostMapping("/update")
    public ResponseEntity<String> updateData(@RequestBody UpdateDataDTO updateDataDTO) {
        sideMonitorDataBackupService.updateData(updateDataDTO);
        return ResponseEntity.ok("Data updateData successfully!");
    }


}
