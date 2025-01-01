package com.mrlu.server;

/**
 * @author 简单de快乐
 * @create 2024-04-20 1:54
 */
public abstract class Parent<T> {

    @A(value = "from-parent")
    public abstract T test();
}
