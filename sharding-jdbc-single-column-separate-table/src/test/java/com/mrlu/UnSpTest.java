package com.mrlu;

import com.mrlu.entity.UnSeparateData;
import com.mrlu.service.UnSeparateDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

/**
 * @author 简单de快乐
 * @create 2024-11-14 10:30
 *
 * 不分表的表测试
 */
@SpringBootTest
public class UnSpTest {

    @Autowired
    UnSeparateDataService unSeparateDataService;

    @Test
    void saveTest() {
        UnSeparateData unSeparateData = new UnSeparateData();
        unSeparateData.setName("测试");
        unSeparateData.setValue(888.88);
        unSeparateData.setCollectTime(new Date());
        unSeparateDataService.save(unSeparateData);
    }
}
