package com.mrlu.core.mode.proxy;




import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;

import java.lang.reflect.Method;
import java.util.Arrays;


/**
 * @author 简单de快乐
 * @create 2024-07-03 16:38
 *
 * 日志通知
 */
public class LogInterceptor implements MethodInterceptor {

    public static final Logger LOGGER = LoggerFactory.getLogger(LogInterceptor.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        ReflectiveMethodInvocation reflectiveMethodInvocation = (ReflectiveMethodInvocation)invocation;
        Method method = reflectiveMethodInvocation.getMethod();
        Object[] arguments = reflectiveMethodInvocation.getArguments();
        LOGGER.info("开始调用：{}类的{}方法，调用参数：{}", method.getDeclaringClass(), method.getName(), Arrays.toString(arguments));
        Object proceed = invocation.proceed();
        LOGGER.info("完成调用：{}类的{}方法，方法返回值：{}", method.getDeclaringClass(), method.getName(),  proceed);
        return proceed;
    }
}
