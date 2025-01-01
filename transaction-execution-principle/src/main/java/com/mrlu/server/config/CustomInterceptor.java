package com.mrlu.server.config;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

/**
 * @author 简单de快乐
 * @create 2024-04-11 22:12
 * 自定义拦截器，直接注入即可
 */
//@Component
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class CustomInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 在执行SQL语句前的逻辑
        System.out.println("Before executing SQL statement...");
        // 执行原始方法
        Object result = invocation.proceed();
        // 在执行SQL语句后的逻辑
        System.out.println("After executing SQL statement...");
        return result;
    }

}
