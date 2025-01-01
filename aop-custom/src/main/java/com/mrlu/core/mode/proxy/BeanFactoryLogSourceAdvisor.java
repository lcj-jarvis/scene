package com.mrlu.core.mode.proxy;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * @author 简单de快乐
 * @create 2024-07-03 15:24
 *
 * 日志增强器器
 */
public class BeanFactoryLogSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {


    private Pointcut logPointcut;

    public void setLogPointcut(Pointcut logPointcut) {
        this.logPointcut = logPointcut;
    }

    @Override
    public Pointcut getPointcut() {
        return logPointcut;
    }
}
