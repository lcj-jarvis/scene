package com.mrlu.core.anno;

import java.lang.annotation.*;

/**
 * @author 简单de快乐
 * @create 2024-07-03 15:05
 * 日志注解
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Log {

}
