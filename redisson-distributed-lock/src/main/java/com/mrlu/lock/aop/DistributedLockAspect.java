package com.mrlu.lock.aop;

import com.mrlu.exception.ServiceException;
import com.mrlu.lock.anno.DistributedLock;
import com.mrlu.lock.core.IDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author 简单de快乐
 * @create 2024-01-10 10:21
 *
 * 优先级比事务切面高，保证事务提交之后才释放锁
 */
@Component
@Aspect
@Order(1)
@Slf4j
public class DistributedLockAspect {

    @Autowired
    private IDistributedLock idistributedLock;

    private static final String SPEL_PREFIX = "#";

    /**
     * SpEL表达式解析
     */
    private SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

    /**
     * 用于获取方法参数名字
     */
    private DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Pointcut("@annotation(com.mrlu.lock.anno.DistributedLock)")
    public void distributorLock() {
    }


    private String getLockKey(ProceedingJoinPoint point, DistributedLock distributedLock) {
        String lockKey = distributedLock.key();
        String keyPrefix = distributedLock.keyPrefix();
        if (StringUtils.isBlank(lockKey)) {
            throw new ServiceException("Lock key cannot be empty");
        }
        if (lockKey.contains(SPEL_PREFIX)) {
            checkSpEL(lockKey);
            MethodSignature methodSignature = (MethodSignature) point.getSignature();
            // 获取方法参数值
            Object[] args = point.getArgs();
            lockKey = getValBySpEL(lockKey, methodSignature, args);
        }
        lockKey = StringUtils.isBlank(keyPrefix) ? lockKey : keyPrefix + lockKey;
        return lockKey;
    }

    /**
     * 解析spEL表达式
     *
     * @param spEL
     * @param methodSignature
     * @param args
     * @return
     */
    private String getValBySpEL(String spEL, MethodSignature methodSignature, Object[] args) {
        // 获取方法形参名数组
        String[] paramNames = nameDiscoverer.getParameterNames(methodSignature.getMethod());
        if (paramNames == null || paramNames.length < 1) {
            throw new ServiceException("arg for Lock key cannot be empty");
        }
        Expression expression = spelExpressionParser.parseExpression(spEL);
        // spring的表达式上下文对象
        EvaluationContext context = new StandardEvaluationContext();
        // 给上下文赋值
        for (int i = 0; i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        return Objects.requireNonNull(expression.getValue(context)).toString();
    }

    /**
     * SpEL 表达式校验
     *
     * @param spEL
     * @return
     */
    private void checkSpEL(String spEL) {
        try {
            ExpressionParser parser = new SpelExpressionParser();
            parser.parseExpression(spEL, new TemplateParserContext());
        } catch (Exception e) {
            throw new ServiceException("Invalid SpEL expression [" + spEL + "]");
        }
    }

    @Around("distributorLock()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);
        TimeUnit timeUnit = annotation.unit();
        boolean tryLock = annotation.tryLock();
        boolean fair = annotation.fair();
        long lockTime = annotation.lockTime();
        long tryTime = annotation.tryTime();
        String lockKey = getLockKey(point, annotation);
        RLock lock = null;
        try {
            if (tryLock) {
                lock = idistributedLock.tryLock(lockKey, tryTime, lockTime, timeUnit, fair);
            } else {
                lock = idistributedLock.lock(lockKey, lockTime, timeUnit, fair);
            }
            log.info("lock entry={}", lock.getName());
            return point.proceed();
        } catch (Exception e) {
            throw new ServiceException(e);
        } finally {
            idistributedLock.unlock(lock);
        }
    }

}
