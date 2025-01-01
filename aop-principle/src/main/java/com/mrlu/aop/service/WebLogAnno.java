package com.mrlu.aop.service;

import java.lang.annotation.*;

/**
 * @author 简单de快乐
 * @create 2024-05-20 14:53
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface WebLogAnno {

}
