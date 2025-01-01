package com.mrlu.controller;

import com.mrlu.dto.DataDTO;
import com.mrlu.entity.MonitoringData;
import com.mrlu.response.CommonResults;
import com.mrlu.service.MonitoringDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/monitoring")
public class MonitoringDataController {

    @Autowired
    private MonitoringDataService monitoringDataService;

    @GetMapping("/query")
    public CommonResults<List<MonitoringData>> query(@RequestBody DataDTO dataDTO) {
        return CommonResults.ok(monitoringDataService.getMonitoringData(dataDTO));
    }

    @PostMapping("/add")
    public CommonResults addSingleData(@RequestBody MonitoringData data) {
        return CommonResults.ok(monitoringDataService.addSingleData(data));
    }

    @PostMapping("/batchAdd")
    public CommonResults batchAdd(@RequestBody List<MonitoringData> dataList) {
        return CommonResults.ok(monitoringDataService.batchAdd(dataList));
    }

}
