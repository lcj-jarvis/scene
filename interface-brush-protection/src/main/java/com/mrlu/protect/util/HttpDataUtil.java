package com.mrlu.protect.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author 简单de快乐
 */
public class HttpDataUtil {

    /**
     * post请求处理：获取 Body 参数，转换为SortedMap
     * @param request
     */
    public static SortedMap<String, Object> getBodyParams(HttpServletRequest request) throws IOException {
         byte[] requestBody = StreamUtils.copyToByteArray(request.getInputStream());
         String body = new String(requestBody);
         return JSON.parseObject(body, SortedMap.class);
    }


    /**
     * get请求处理：将URL请求参数转换成SortedMap
     */
    public static SortedMap<String, Object> getUrlParams(HttpServletRequest request) {
        String params = request.getQueryString();
        if (StringUtils.isEmpty(params)) {
            return new TreeMap<>();
        }
        return getSortedMapByUrlParams(params);
    }


    private static SortedMap<String, Object> getSortedMapByUrlParams(String params) {
        try {
            params = URLDecoder.decode(params, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        SortedMap<String, Object> paramMap = new TreeMap<>();
        String[] paramArray = params.split("&");
        for (String param : paramArray) {
            String[] array = param.split("=");
            paramMap.put(array[0], array[1]);
        }
        return paramMap;
    }


}
