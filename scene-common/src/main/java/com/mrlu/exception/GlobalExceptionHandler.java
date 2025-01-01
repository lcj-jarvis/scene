package com.mrlu.exception;

import com.mrlu.response.ApiErrorCode;
import com.mrlu.response.CommonResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 简单de快乐
 * @create 2023-05-25 17:03
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = ServiceException.class)
    @ResponseBody
    public CommonResults serviceExceptionHandler(HttpServletRequest request, ServiceException e) {
        log.error(e.getMsg());
        return new CommonResults().setCode(e.getCode()).setMsg(e.getMsg());
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public CommonResults exceptionHandler(HttpServletRequest request, Exception e) {
        String uri = request.getRequestURI();
        log.error("receive error;url={}", uri, e);
        return CommonResults.failed(ApiErrorCode.FAILED);
    }
}
