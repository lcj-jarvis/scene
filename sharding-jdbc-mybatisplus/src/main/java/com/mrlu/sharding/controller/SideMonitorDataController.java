package com.mrlu.sharding.controller;

import com.mrlu.sharding.dto.DataDTO;
import com.mrlu.sharding.dto.DataDTOListWrapper;
import com.mrlu.sharding.dto.DataSearchDTO;
import com.mrlu.sharding.entity.SideMonitorData;
import com.mrlu.sharding.service.SideMonitorDataBackupService;
import com.mrlu.sharding.service.SideMonitorDataService;
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
        sideMonitorDataService.saveData(data);
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
        sideMonitorDataService.deleteData(searchDTO);
        return ResponseEntity.ok("Data deleteData successfully!");
    }




}
