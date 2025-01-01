package com.mrlu.limit.anno;

import com.mrlu.limit.constant.LimitType;

import java.lang.annotation.*;

/**
 * @author 简单de快乐
 * @create 2023-05-26 15:37
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Limit {

    /**
     * 名字
     */
    String name() default "";

    /**
     * key
     */
    String key() default "";

    /**
     * key的前缀
     * @return
     */
    String prefix() default "";

    /**
     * 给定的时间范围 单位(秒)
     * @return
     */
    int period();

    /**
     * 一定时间内最多访问次数
    */
    int count();

    /**
     * 限流的类型（用户自定义key或者ip）
     * @return
     */
    LimitType limitType() default LimitType.CUSTOMER;

}
