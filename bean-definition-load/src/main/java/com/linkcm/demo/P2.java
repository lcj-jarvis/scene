package com.linkcm.demo;

import java.lang.annotation.*;

/**
 * @author 简单de快乐
 * @create 2023-10-09 16:46
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Repeatable(P1.class)
public @interface P2 {

    String value();
}
