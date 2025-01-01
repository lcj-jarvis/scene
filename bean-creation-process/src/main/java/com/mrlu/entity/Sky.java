package com.mrlu.entity;

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author 简单de快乐
 * @create 2024-12-30 20:08
 */
@Data
@ToString
public class Sky implements InitializingBean {
    private Integer number;
    private Colour colour;
    private Bird bird;
    private Cloud cloud;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(this);
    }
}
