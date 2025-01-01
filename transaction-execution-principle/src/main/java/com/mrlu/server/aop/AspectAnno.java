package com.mrlu.server.aop;

import java.lang.annotation.*;

/**
 * @author 简单de快乐
 * @create 2024-01-09 17:37
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AspectAnno {
}
