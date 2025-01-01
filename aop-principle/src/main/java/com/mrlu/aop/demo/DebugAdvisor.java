package com.mrlu.aop.demo;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

/**
 * @author 简单de快乐
 * @create 2024-07-19 14:47
 */
@Component
@Slf4j
public class DebugAdvisor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        log.info("=============DebugAdvisor invoke==============");
        return invocation.proceed();
    }
}
