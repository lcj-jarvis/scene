package com.mrlu.aop.service.impl;


import com.mrlu.aop.service.WebLogAnno;
import com.mrlu.aop.service.WhInterface;
import org.springframework.cache.annotation.Cacheable;

/**
 * @author 简单de快乐
 * @create 2024-05-20 14:54
 */
public abstract class WhParentService implements WhInterface {

    @Override
    @WebLogAnno
    @Cacheable(cacheNames = "wh-tt", key = "#type")
    public void tt(String type) {
        System.out.println("ttttttttttttttt");
    }
}
