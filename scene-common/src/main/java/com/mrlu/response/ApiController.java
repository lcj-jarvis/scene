package com.mrlu.response;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiController {

    /**
     * 请求成功
     *
     * @param data 数据内容
     * @param <T>  对象泛型
     * @return ignore
     */
    protected <T> CommonResults<T> success(T data) {
        return CommonResults.ok(data);
    }

    /**
     * 请求失败
     *
     * @param msg 提示内容
     * @return ignore
     */
    protected <T> CommonResults<T> failed(String msg) {
        return CommonResults.failed(msg);
    }

    /**
     * 请求失败
     *
     * @param errorCode 请求错误码
     * @return ignore
     */
    protected <T> CommonResults<T> failed(IErrorCode errorCode) {
        return CommonResults.failed(errorCode);
    }

}
