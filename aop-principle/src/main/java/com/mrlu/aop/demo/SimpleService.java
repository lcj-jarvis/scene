package com.mrlu.aop.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SimpleService {

    // 指定使用代理对象
    @Autowired
    @Qualifier("manServiceProxy")
    private ManService manService;

    public void doSomething() {
        manService.save();
        log.info("===========doSomething==============");
    }
}
