package com.mrlu.tx.exception;

/**
 * @author 简单de快乐
 * @create 2024-07-19 18:21
 */
public class NotRunTimeException extends Exception {

    private String message;

    public NotRunTimeException(String message) {
        super(message);
    }

}
