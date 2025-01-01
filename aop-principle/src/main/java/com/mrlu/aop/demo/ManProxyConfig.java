package com.mrlu.aop.demo;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2024-07-19 14:49
 */
@Configuration
public class ManProxyConfig {

    @Bean
    public ProxyFactoryBean manServiceProxy(ManServiceImpl manServiceImpl) throws ClassNotFoundException {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        // 设置代理
        proxyFactoryBean.setProxyInterfaces(new Class[]{ManService.class});
        // 设置被代理对象
        proxyFactoryBean.setTarget(manServiceImpl);
        // 设置增强器bean名称
        proxyFactoryBean.setInterceptorNames("myAdvisor", "debugAdvisor");
        return proxyFactoryBean;
    }

}
