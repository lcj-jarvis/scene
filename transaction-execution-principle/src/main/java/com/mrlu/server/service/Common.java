package com.mrlu.server.service;

import org.springframework.transaction.annotation.Transactional;

/**
 * @author 简单de快乐
 * @create 2024-04-19 22:40
 */
public interface Common<T> {

    @Transactional
    T aCommon();

}
