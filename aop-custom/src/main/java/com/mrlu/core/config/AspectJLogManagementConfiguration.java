package com.mrlu.core.config;

import com.mrlu.core.mode.aspectj.LoggingAspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Role;

@Configuration
@EnableAspectJAutoProxy
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJLogManagementConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
