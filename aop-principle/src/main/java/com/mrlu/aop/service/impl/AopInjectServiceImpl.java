package com.mrlu.aop.service.impl;

import com.mrlu.aop.service.AopInjectService;
import com.mrlu.aop.service.WebLogAnno;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author 简单de快乐
 * @create 2024-07-03 14:34
 */
@Service
public class AopInjectServiceImpl implements AopInjectService, InitializingBean {

    @WebLogAnno
    @Transactional
    @Override
    public void proxyedMethod() {
        System.out.println("---被代理方法----");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("--AopInjectServiceImpl afterPropertiesSet--");
    }
}
