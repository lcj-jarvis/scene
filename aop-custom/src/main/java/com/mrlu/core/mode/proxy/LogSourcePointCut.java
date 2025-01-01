package com.mrlu.core.mode.proxy;

import com.mrlu.core.config.ProxyLogManagementConfiguration;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;
import org.springframework.core.MethodClassKey;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 简单de快乐
 * @create 2024-07-03 15:28
 * 日志切入点
 */
public class LogSourcePointCut implements Pointcut {

    // 类过滤器
    private ClassFilter classFilter;

    // 方法匹配器
    private MethodMatcher methodMatcher;

    public LogSourcePointCut(Class<? extends Annotation> annotationType, boolean checkInherited) {
        this.classFilter = new LogSourceClassFilter();
        this.methodMatcher = new LogMethodMatcher(annotationType, checkInherited);
    }

    @Override
    public ClassFilter getClassFilter() {
        return classFilter;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    private class LogSourceClassFilter implements ClassFilter {

        @Override
        public boolean matches(Class<?> clazz) {
            // 过滤ProxyLogManagementConfiguration类
            if (ProxyLogManagementConfiguration.class.isAssignableFrom(clazz)) {
                return false;
            }
            // 返回true。让后面的方法匹配器校验是否为符合条件的增强器
            return true;
        }
    }

    private class LogMethodMatcher extends StaticMethodMatcher {
        private MethodMatcher methodMatcher;

        private boolean checkInherited;

        private Class<? extends Annotation> annotationType;

        public LogMethodMatcher(Class<? extends Annotation> annotationType, boolean checkInherited) {
            // 是否考虑继承
            this.checkInherited = checkInherited;
            this.annotationType = annotationType;
            this.methodMatcher = new AnnotationMethodMatcher(annotationType, checkInherited);
        }

        private Map<MethodClassKey, Boolean> attributeCache = new ConcurrentHashMap<>();

        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            // First, see if we have a cached value.
            MethodClassKey cacheKey = getCacheKey(method, targetClass);
            Boolean cached = attributeCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            boolean matches = methodMatcher.matches(method, targetClass);
            if (matches) {
                // 方法或者继承上的方法有注解存在
                attributeCache.put(cacheKey, true);
                return true;
            }
            // 方法上没有注解，则从类上获取
            matches = checkInClass(targetClass);
            attributeCache.put(cacheKey, matches);
            return matches;
        }

        public boolean checkInClass(Class<?> clazz) {
            return (this.checkInherited ? AnnotatedElementUtils.hasAnnotation(clazz, this.annotationType) :
                    clazz.isAnnotationPresent(this.annotationType));
        }

        private MethodClassKey getCacheKey(Method method, @Nullable Class<?> targetClass) {
            return new MethodClassKey(method, targetClass);
        }
    }

}
