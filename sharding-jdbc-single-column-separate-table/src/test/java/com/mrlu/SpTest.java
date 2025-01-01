package com.mrlu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mrlu.dto.DataDTO;
import com.mrlu.entity.MonitoringData;
import com.mrlu.service.MonitoringDataService;
import com.mrlu.utils.DateUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.mrlu.utils.DateUtil.DATETIME_PATTERN_DATE_TIME;

/**
 * @author 简单de快乐
 * @create 2024-11-13 22:28
 *
 * 分表的表测试
 */
@SpringBootTest
public class SpTest {

    @Autowired
    private List<DataSource> dataSources;

    @Autowired
    private MonitoringDataService monitoringDataService;

    @Test
    void  updateTest() {
        Date begin = DateUtil.str2Date("2024-11-17 00:30:00", DATETIME_PATTERN_DATE_TIME);
        Date end = DateUtil.str2Date("2024-11-19 00:32:00", DATETIME_PATTERN_DATE_TIME);
        MonitoringData data = new MonitoringData();
        data.setValue(66.666);
        LambdaQueryWrapper<MonitoringData> wrapper = new LambdaQueryWrapper<MonitoringData>()
                .eq(MonitoringData::getEquipmentNo, "equip03")
                .ge(MonitoringData::getLastCollectTime, begin)
                .le(MonitoringData::getLastCollectTime, end);
        monitoringDataService.update(data, wrapper);
    }

    @Test
    void  deleteTest() {
        DataDTO dataDTO = new DataDTO();
        ArrayList<String> list = new ArrayList<>();
        list.add("equip02");
        list.add("equip01");
        list.add("equip03");
        dataDTO.setEquipmentNos(list);
        Date begin = DateUtil.str2Date("2024-11-17 9:00:00", DATETIME_PATTERN_DATE_TIME);
        Date end = DateUtil.str2Date("2024-11-19 00:30:00", DATETIME_PATTERN_DATE_TIME);
        dataDTO.setEquipmentNos(list);
        dataDTO.setBegin(begin);
        dataDTO.setEnd(end);
        //dataDTO.setBusinessType("js");
        dataDTO.setBusinessType("dz");
        monitoringDataService.delete(dataDTO);
        List<MonitoringData> monitoringData = monitoringDataService.getMonitoringData(dataDTO);
        monitoringData.forEach(System.out::println);
    }

    @Test
    void  getMonitoringDataTest() {
        DataDTO dataDTO = new DataDTO();
        // 不传分片的字段的话，会查询所有的表【强烈不推荐】
        ArrayList<String> list = new ArrayList<>();
        list.add("equip02");
        list.add("equip01");
        list.add("equip03");
        dataDTO.setEquipmentNos(list);
        Date begin = DateUtil.str2Date("2024-11-17 21:38:00", DATETIME_PATTERN_DATE_TIME);
        Date end = DateUtil.str2Date("2024-11-19 21:40:00", DATETIME_PATTERN_DATE_TIME);
        dataDTO.setBegin(begin);
        dataDTO.setEnd(end);
        //dataDTO.setBusinessType("js");
        List<MonitoringData> monitoringData = monitoringDataService.getMonitoringData(dataDTO);
        monitoringData.forEach(System.out::println);
    }

    @Test
    void  addSingleDataTest() {
        MonitoringData monitoringData = new MonitoringData();
        monitoringData.setEquipmentNo("equip888888");
        Date date = new Date();
        monitoringData.setLastCollectTime(date);
        monitoringData.setValue(88888.0);
        monitoringData.setBuinessType("js");
        monitoringDataService.addSingleData(monitoringData);
        System.out.println("Hash ': " + ((Math.abs(monitoringData.getEquipmentNo().hashCode()) % 10) + 1));
    }

    @Test
    void  batchAddTest() {
        MonitoringData monitoringData01 = new MonitoringData();
        monitoringData01.setEquipmentNo("equip01");
        Date date = new Date();
        monitoringData01.setLastCollectTime(date);
        monitoringData01.setValue(0.3);
        monitoringData01.setBuinessType("js");


        MonitoringData monitoringData02 = new MonitoringData();
        monitoringData02.setEquipmentNo("equip02");
        date = new Date();
        monitoringData02.setLastCollectTime(date);
        monitoringData02.setValue(0.2);
        monitoringData02.setBuinessType("dz");


        MonitoringData monitoringData03 = new MonitoringData();
        monitoringData03.setEquipmentNo("equip03");
        monitoringData03.setLastCollectTime(date);
        monitoringData03.setValue(0.6);
        monitoringData03.setBuinessType("js");

        MonitoringData monitoringData04 = new MonitoringData();
        monitoringData04.setEquipmentNo("equip03");
        date = new Date();
        monitoringData04.setLastCollectTime(date);
        monitoringData04.setValue(0.6);
        monitoringData04.setBuinessType("dz");

        List<MonitoringData> monitoringDataList = Arrays.asList(monitoringData01, monitoringData02,
                monitoringData03, monitoringData04);
        monitoringDataService.batchAdd(monitoringDataList);
    }

}
