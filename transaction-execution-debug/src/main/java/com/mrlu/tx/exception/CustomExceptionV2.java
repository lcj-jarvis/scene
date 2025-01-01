package com.mrlu.tx.exception;

/**
 * @author 简单de快乐
 * @create 2024-07-19 17:13
 */
public class CustomExceptionV2 extends RuntimeException {

    private String message;

    public CustomExceptionV2(String message) {
        super(message);
    }
}
