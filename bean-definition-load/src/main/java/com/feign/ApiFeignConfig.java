package com.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

/**
 * 深圳服务apiFeign定制配置
 */
public class ApiFeignConfig {

    /**
     * 请求拦截器
     */
    @Bean
    public RequestInterceptor szApiInterceptor() {
        return new SzApiInterceptor();
    }

    /**
     * 深圳服务api请求拦截器
     * 用于对feign请求设置请求头
     */
    static class SzApiInterceptor implements RequestInterceptor {
        //private final String signKey = PropertiesUtil.getProperty("szApi.signKey");
        /**
         * 签名
         */
        @Override
        public void apply(RequestTemplate requestTemplate) {
            //String nonce = String.valueOf(System.currentTimeMillis());
            //String sign = DigestUtil.md5Hex(nonce + signKey);
            //requestTemplate.header("nonce", nonce);
            //requestTemplate.header("sign", sign);
        }
    }

}
