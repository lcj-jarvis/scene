package com.mrlu.exception;


import com.mrlu.response.ApiErrorCode;

public class ServiceException extends RuntimeException {

    private long code = ApiErrorCode.FAILED.getCode();

    private String msg;

    public ServiceException(String message) {
        super(message);
        msg = message;
    }

    public ServiceException(long code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ServiceException(Exception exception) {
        super(exception);
        msg = getMessage();
    }

    public ServiceException(String fmt, Object... args) {
        init(fmt, args);
    }

    // 示例：throw new ServiceException(ApiErrorCode.ARGS_ERROR.getCode(), "args error;arg1=%s,arg2=%s", 1, 2);
    public ServiceException(long code, String fmt, Object... args) {
        this.code = code;
        init(fmt, args);
    }

    private void init(String fmt, Object... args) {
        if (fmt != null) {
            try {
                msg = format(getThrowableStack() + fmt, args);
            } catch (Exception e) {
                msg = "serviceException format err;fmt=" + fmt;
            }
        }
    }

    /**
     * 拿到抛异常的具体堆栈信息，方便定位
     * @return
     */
    private String getThrowableStack(){
        Throwable throwable = new Throwable();
        StackTraceElement[] stacks = throwable.getStackTrace();
        StackTraceElement stack = stacks[3];
        String className = stack.getClassName();
        return getClassName(className) + "::" + stack.getMethodName() + ":" + stack.getLineNumber() + ": ";
    }

    private static String getClassName(String cls) {
        if (cls == null) {
            return null;
        }
        int pos = cls.lastIndexOf('.');
        if (pos > 0) {
            cls = cls.substring(pos + 1);
        }
        return cls;
    }

    /**
     * 格式化
     * @param fmt
     * @param args
     */
    private static String format(String fmt, Object... args) {
        if (args == null || args.length == 0) {
            return fmt;
        }
        return String.format(fmt, args);
    }

    public long getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
