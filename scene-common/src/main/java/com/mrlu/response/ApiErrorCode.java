package com.mrlu.response;

public enum ApiErrorCode implements IErrorCode {
    /**
     * 失败
     */
    FAILED(-1, "操作失败"),
    /**
     * 成功
     */
    SUCCESS(0, "执行成功"),

    ARGS_ERROR(-2, "参数错误"),

    PARSE_ERROR(-3, "解析错误"),

    SIGN_EXPIRED(-4, "签名失效"),

    AUTHENTICATION_ERROR(-5, "鉴权失败"),

    AUTHENTICATION_ARGS_EMPTY(-6, "鉴权参数不存在"),

    REPLAY_ERROR(-7, "重复请求错误");

    private final long code;
    private final String msg;

    ApiErrorCode(final long code, final String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static ApiErrorCode fromCode(long code) {
        ApiErrorCode[] ecs = ApiErrorCode.values();
        for (ApiErrorCode ec : ecs) {
            if (ec.getCode() == code) {
                return ec;
            }
        }
        return SUCCESS;
    }

    @Override
    public long getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return String.format(" ErrorCode:{code=%s, msg=%s} ", code, msg);
    }
}
