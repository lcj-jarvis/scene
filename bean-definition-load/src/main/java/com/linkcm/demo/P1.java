package com.linkcm.demo;

import java.lang.annotation.*;

/**
 * @author 简单de快乐
 * @create 2023-10-09 16:45
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface P1 {
    P2[] value();
}
