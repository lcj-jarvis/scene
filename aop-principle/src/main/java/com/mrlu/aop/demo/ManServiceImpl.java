package com.mrlu.aop.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author 简单de快乐
 * @create 2024-07-19 14:48
 */
@Service
@Slf4j
public class ManServiceImpl implements ManService {

    @Override
    public void save() {
       log.info("=============save===============");
    }
}
