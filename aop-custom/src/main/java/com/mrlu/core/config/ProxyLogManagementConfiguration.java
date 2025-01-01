package com.mrlu.core.config;

import com.mrlu.core.anno.Log;
import com.mrlu.core.mode.proxy.BeanFactoryLogSourceAdvisor;
import com.mrlu.core.mode.proxy.LogInterceptor;
import com.mrlu.core.mode.proxy.LogSourcePointCut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * @author 简单de快乐
 * @create 2024-07-03 15:11
 *
 * 日志增强器配置类
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyLogManagementConfiguration extends AbstractLogManagementConfiguration{

    // 注入增强器
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BeanFactoryLogSourceAdvisor logAdvisor(LogSourcePointCut logSourcePointCut, LogInterceptor logInterceptor) {
        BeanFactoryLogSourceAdvisor advisor = new BeanFactoryLogSourceAdvisor();
        // 增强器包含两大项：（1）切入点 （2）通知
        // 设置切入点
        advisor.setLogPointcut(logSourcePointCut);
        // 设置通知
        advisor.setAdvice(logInterceptor);
        // 设置增强器的顺序
        if (this.enableLog != null) {
            advisor.setOrder(this.enableLog.<Integer>getNumber("order"));
        }
        return advisor;
    }

    // 注入切入点
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LogSourcePointCut logSourcePointCut() {
        return new LogSourcePointCut(Log.class, true);
    }

    //注入通知
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LogInterceptor logInterceptor() {
        return new LogInterceptor();
    }

}
