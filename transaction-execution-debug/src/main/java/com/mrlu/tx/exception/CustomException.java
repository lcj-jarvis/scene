package com.mrlu.tx.exception;

/**
 * @author 简单de快乐
 * @create 2024-07-19 17:13
 */
public class CustomException extends RuntimeException {

    private String message;

    public CustomException(String message) {
        super(message);
    }

    public static class AnotherException extends RuntimeException{
        private String message;
        public AnotherException(String message) {
            super(message);
        }
    }

}
