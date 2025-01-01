package com.mrlu.aop;

import com.mrlu.aop.service.WhInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 简单de快乐
 * @create 2024-07-03 14:55
 */
@SpringBootTest
public class WhServiceTest {
    //@Autowired
    //private WhService whService;
    //
    //@Test
    //public void test() {
    //    whService.getWhList("aaaa");
    //}

    // 修改spring.aop.proxy-target-class=false，使用jdk动态代理
    // 测试jdk使用下面的。因为jdk动态代理的类已经继承了Proxy，上面注入具体的类为WhService，类型不匹配，会报错。
    @Autowired
    private WhInterface whInterface;

    @Test
    public void tt() {
        whInterface.tt("ttt");
    }

    @Test
    public void testLog() {
        whInterface.testLog("自定义aop操作");
    }



}
