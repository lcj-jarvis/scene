package com.mrlu.server.aop;


import com.mrlu.exception.ServiceException;
import com.mrlu.server.service.AnimalService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * @author 简单de快乐
 * @create 2024-01-09 17:24
 *
 * 默认其他情况下，其他切面和事务切面没有指定order的话(都是Ordered.LOWEST_PRECEDENCE)，
 * 事务的切面会排在其他切面之前（优先级比其他切面高），
 * 因为spring的先获取事务的切面，有问题可以直接回滚。可以理解成事务切面里的try-catch包着其他切面的逻辑。
 */
@Aspect
@Component
@Order(1)
public class OtherAspect {

    private final static Logger LOGGER = LoggerFactory.getLogger(OtherAspect.class);

    @Autowired
    private AnimalService animalService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    // 默认传播行为: PROPAGATION_REQUIRED
    @Autowired
    private TransactionDefinition transactionDefinition;


    /** 以 controller 包下定义的所有请求为切入点 */
    @Pointcut("@annotation(com.mrlu.server.aop.AspectAnno)")
    public void webLog() {}

    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        LOGGER.info("====================begin doAround=====================");
        Object result;
        // 手动开启事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        try {
            // 执行目标方法
            result = proceedingJoinPoint.proceed();

            //int i = 1 / 0;

            animalService.saveAnimal();

            // 手动提交事务
            platformTransactionManager.commit(transactionStatus);
            LOGGER.info("====================commit=====================");
        } catch (Exception e) {
            // 手动回滚事务并抛出异常
            platformTransactionManager.rollback(transactionStatus);
            LOGGER.info("====================rollback=====================");
            throw new ServiceException(e);
        }
        LOGGER.info("====================finish doAround=====================");
        return result;
    }





}
