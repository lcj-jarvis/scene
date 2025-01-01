package com.mrlu.aop.service;

import org.springframework.transaction.annotation.Transactional;

/**
 * @author 简单de快乐
 * @create 2024-06-25 17:06
 */
public interface WhInterface {

    @Transactional
    void tt(String type);

    void testLog(String content);

}
