package com.mrlu.server;

import java.lang.annotation.*;

/**
 * @author 简单de快乐
 * @create 2024-04-20 1:50
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface A {

    String value();

}
