package com.mrlu.limit.aspect;

import com.mrlu.limit.anno.Limit;
import com.mrlu.limit.constant.LimitType;
import com.mrlu.response.CommonResults;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collections;

/**
 * @author 简单de快乐
 * @create 2023-05-26 15:41
 */
@Aspect
@Component
public class LimitAspect {
    private static final Logger logger = LoggerFactory.getLogger(LimitAspect.class);

    private static final String UNKNOWN = "unknown";

    @Autowired
    private RedisTemplate redisTemplate;

    @Around("execution(public * *(..)) && @annotation(com.mrlu.limit.anno.Limit)")
    public Object doAround(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 获取注解相应参数
        Limit annotation = method.getAnnotation(Limit.class);

        String name = annotation.name();
        int limitCount = annotation.count();
        int period = annotation.period();
        LimitType limitType = annotation.limitType();
        String prefix = annotation.prefix();

        String key;
        switch (limitType) {
            case IP:
                key = getIpAddress();
                break;
            case CUSTOMER:
                key = annotation.key();
                break;
            default:
                key = annotation.key();
                if (StringUtils.isEmpty(key)) {
                    // 获取用方法名做限流的key
                    key = StringUtils.upperCase(method.getName());
                }
        }

        String luaScript = getLimitLuaScript();
        DefaultRedisScript<Long> script = new DefaultRedisScript<Long>(luaScript, Long.class);
        script.setResultType(Long.class);
        String wholeKey = StringUtils.isEmpty(prefix) ? key : prefix + "-" + key;
        Long count = (Long) redisTemplate.execute(script, Collections.singletonList(wholeKey), limitCount, period);
        logger.info("Access try count is {} for name={} and key = {}", count, name, key);
        if (count != null && count.intValue() <= limitCount) {
            return point.proceed();
        }
        return CommonResults.failed("系统繁忙，请稍后重试。。。");
    }

    public String getLimitLuaScript() {
        String lua = "local key = KEYS[1]\n" +
                "local limit = tonumber(ARGV[1])\n" +
                "local currentLimit = tonumber(redis.call('get', key) or '0')\n" +
                "if currentLimit > limit then\n" +
                "return currentLimit\n" +
                "end\n" +
                "currentLimit = redis.call('INCRBY', key, 1)\n" +
                "if currentLimit == 1 then\n" +
                "redis.call('EXPIRE', key, ARGV[2])\n" +
                "end\n" +
                "return currentLimit\n";
        return lua;
    }


    /**
     * 获取ip地址
     */
    public String getIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
