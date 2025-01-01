package com.mrlu.entity;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * The type Phone factory bean.
 *
 * @author 简单de快乐
 * @create 2024 -12-16 18:16
 */
@Component
public class PhoneFactoryBean implements FactoryBean {

    @Override
    public Object getObject() throws Exception {
        Phone temp = new Phone();
        return temp;
    }

    @Override
    public Class<?> getObjectType() {
        return Phone.class;
    }
}
