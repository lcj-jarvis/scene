package com.mrlu.core.mode.aspectj;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    // 定义切入点，匹配被 @Log 注解标记的类和方法
    @Pointcut("@within(com.mrlu.core.anno.Log) || @annotation(com.mrlu.core.anno.Log)")
    public void logAnnotated() {}

    // 在方法执行前执行日志记录
    @Before("logAnnotated()")
    public void logBefore(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getName();
        Object[] args = joinPoint.getArgs();
        logger.info("开始调用方法 {}#{}，参数为: {}", className, methodName, args);
    }

    // 在方法执行后执行日志记录
    @AfterReturning(pointcut = "logAnnotated()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getName();

        logger.info("方法 {}#{} 执行完成，返回值为: {}", className, methodName, result);
    }
}
