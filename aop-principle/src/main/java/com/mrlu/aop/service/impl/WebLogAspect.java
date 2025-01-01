package com.mrlu.aop.service.impl;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 简单de快乐
 * @create 2024-05-20 14:53
 */
@Component
@Aspect
@Order(1)
public class WebLogAspect implements InitializingBean {

    private final static Logger LOGGER = LoggerFactory.getLogger(WebLogAspect.class);

    /** 以 controller 包下定义的所有请求为切入点 */
    // @Pointcut("execution(public * com..*..*.controller..*.*(..))")
    @Pointcut("@annotation(com.mrlu.aop.service.WebLogAnno)")
    public void webLog() {}

    /**
     * 在切点之前织入
     * @param joinPoint
     * @throws Throwable
     */
    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        //  开始打印请求日志
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 初始化traceId
        // initTraceId(request);
        // 打印请求相关参数
        //LOGGER.info("========================================== Start ==========================================");
        //// 打印请求 url
        //LOGGER.info("URL            : {}", request.getRequestURL().toString());
        //// 打印 Http method
        //LOGGER.info("HTTP Method    : {}", request.getMethod());
        //// 打印调用 controller 的全路径以及执行方法
        //LOGGER.info("Class Method   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        //// 打印请求的 IP
        //LOGGER.info("IP             : {}", IPAddressUtil.getIpAdrress(request));
        //// 打印请求入参
        //LOGGER.info("Request Args   : {}", joinPoint.getArgs());



        LOGGER.info("-------------doBefore-------------");
    }

    @AfterReturning("webLog()")
    public void afterReturning() {
        LOGGER.info("-------------afterReturning-------------");
    }

    @AfterThrowing("webLog()")
    public void afterThrowing() {
        LOGGER.info("-------------afterThrowing-------------");
    }

    /**
     * 在切点之后织入
     * @throws Throwable
     */
    @After("webLog()")
    public void doAfter() throws Throwable {
        LOGGER.info("-------------doAfter-------------");
    }

    /**
     * 环绕
     * @param proceedingJoinPoint
     * @return
     * @throws Throwable
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        LOGGER.info("-------------doAround before proceed-------------");
        Object result = proceedingJoinPoint.proceed();
        // 打印出参
        LOGGER.info("Response Args  : {}", result);
        // 执行耗时
        LOGGER.info("Time-Consuming : {} ms", System.currentTimeMillis() - startTime);
        LOGGER.info("-------------doAround after proceed-------------");
        return result;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("=========WebLogAspect=========");
    }
}
