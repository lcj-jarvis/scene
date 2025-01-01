package com.mrlu.protect;

import com.alibaba.fastjson.JSONObject;
import com.mrlu.protect.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.util.TreeMap;

//@SpringBootTest
class ProtectionApplicationTests {

    @Test
    void contextLoads() {

    }

    /**
     * 获取get请求的签名和请求参数
     */
    @Test
    void getGetMethodSign() {
        Product product = new Product();
        product.setName("手机");
        product.setPrice(3500.0);
        product.setId(1);

        long timeMillis = System.currentTimeMillis();

        TreeMap<String, String> paramMap = new TreeMap<>();
        paramMap.put("id", "1");
        paramMap.put("name", "手机");
        paramMap.put("price", "3500");

        System.out.println("x-nonce:" + Math.abs(product.hashCode()));
        System.out.println("x-time:" + timeMillis);
        System.out.println("json形式的请求参数:" + JSONObject.toJSON(paramMap));
        String params = String.valueOf(Math.abs(product.hashCode())) + timeMillis + JSONObject.toJSON(paramMap);
        System.out.println(params);
        System.out.println("x-sign:" + DigestUtils.md5DigestAsHex(params.getBytes()));
    }

    /**
     * 获取post请求的签名和请求参数
     */
    @Test
    void getPostMethodSign() {
        Product product = new Product();
        product.setName("手机");
        product.setPrice(4500.0);
        product.setId(2);

        long timeMillis = System.currentTimeMillis();

        TreeMap<String, Object> paramMap = new TreeMap<>();
        paramMap.put("id", "2");
        paramMap.put("name", "手机");
        paramMap.put("price", "4500");

        System.out.println("x-nonce:" + Math.abs(product.hashCode()));
        System.out.println("x-time:" + timeMillis);
        System.out.println("json形式的请求参数:" + JSONObject.toJSON(paramMap));
        String params = String.valueOf(Math.abs(product.hashCode())) + timeMillis + JSONObject.toJSON(paramMap);
        System.out.println(params);
        System.out.println("x-sign:" + DigestUtils.md5DigestAsHex(params.getBytes()));
    }

}
