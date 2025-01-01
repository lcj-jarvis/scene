package com.mrlu.aop.demo;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

/**
 * @author 简单de快乐
 * @create 2024-07-19 14:43
 */
@Component
@Slf4j
public class MyAdvisor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        log.info("=============MyAdvisor invoke==============");
        return invocation.proceed();
    }
}
