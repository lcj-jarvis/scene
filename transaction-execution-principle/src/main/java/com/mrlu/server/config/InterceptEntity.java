package com.mrlu.server.config;

import java.lang.annotation.*;

/**
 * @author 简单de快乐
 * @create 2024-04-22 21:59
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InterceptEntity {

    boolean intercept() default false;

}
