package com.mrlu.entity;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;

import javax.annotation.PreDestroy;
import java.security.AccessControlContext;
import java.util.List;

/**
 * @author 简单de快乐
 * @create 2023-10-18 18:10
 *
 *
 * 销毁方法的存在情况以及执行顺序：
 *   bean实例没有通过xml或者@Bean指定自定义的销毁方法
 *   (1) 实现DisposableBean接口(无论是否实现AutoCloseable接口)
 *       @PreDestroy注解标注的方法  --> DisposableBean的destroy方法
 *   (2) 只实现AutoCloseable接口
 *       @PreDestroy注解标注的方法  --> AutoCloseable的close方法
 *   (3) 没有实现DisposableBean接口和AutoCloseable接口.
 *       通过@Bean注解注入的bean
 *          @PreDestroy注解标注的方法  --> 无参close方法(不是来自AutoCloseable,类中自己定义的)
 *          @PreDestroy注解标注的方法  --> 无参shutdown方法(类中自己定义的)
 *       通过xml引入的bean
 *          @PreDestroy注解标注的方法
 *       通过@Service等注解引入的bean
 *          @PreDestroy注解标注的方法
 *
 *   bean实例通过xml或者@Bean指定自定义的销毁方法
 *    (1) 实现DisposableBean接口(无论是否实现AutoCloseable接口)
 *     @PreDestroy注解标注的方法  --> DisposableBean的destroy方法 --> @Bean的destroyMethod属性指定的方法/xml中配置的销毁方法
 *    (2) 只实现AutoCloseable接口。AutoCloseable的close方法不会当做销毁方法
 *     @PreDestroy注解标注的方法  --> @Bean的destroyMethod属性指定的方法/xml中配置的销毁方法
 *    (3) 没有实现DisposableBean接口和AutoCloseable接口
 *     @PreDestroy注解标注的方法  --> @Bean的destroyMethod属性指定的方法/xml中配置的销毁方法
 *     @PreDestroy注解标注的方法  --> @Bean的destroyMethod属性指定的方法/xml中配置的销毁方法
 *
 *
 * 具体源码见以下类
 * @see CommonAnnotationBeanPostProcessor#CommonAnnotationBeanPostProcessor()
 * @see CommonAnnotationBeanPostProcessor#postProcessMergedBeanDefinition(RootBeanDefinition, Class, String)
 * @see org.springframework.beans.factory.support.AbstractBeanFactory#registerDisposableBeanIfNecessary(String, Object, RootBeanDefinition)
 * @see org.springframework.beans.factory.support.DisposableBeanAdapter#DisposableBeanAdapter(Object, String, RootBeanDefinition, List, AccessControlContext)
 * @see org.springframework.beans.factory.support.DisposableBeanAdapter#inferDestroyMethodIfNecessary(Object, RootBeanDefinition)
 * @see org.springframework.beans.factory.support.DisposableBeanAdapter#destroy()
 */
public class Apple implements DisposableBean,AutoCloseable {

    /**
     * 跟所有的Apple类型的bean实例挂钩
     */
    @PreDestroy
    public void preDestroy() {
        System.out.println("=========Apple=preDestroy====1===================");
    }

    /**
     * 跟所有的Apple类型的bean实例挂钩
     */
    @Override
    public void destroy() throws Exception {
        System.out.println("======Apple====DisposableBean====destroy====2===============");
    }

    /**
     * 跟所有的Apple类型的bean实例挂钩
     */
    @Override
    public void close() {
        System.out.println("==========Apple====close方法来自AutoCloseable========3===========");
    }

    /**
     * close方法作为自定义的销毁方法(不是来自AutoCloseable)，不能带参数。跟所有的Apple类型的bean实例挂钩
     */
    /*public void close() {
        System.out.println("=========Apple=====close方法不是来自AutoCloseable========3===========");
    }*/

    /**
     * shutdown方法作为自定义的销毁方法，不能带参数。跟所有的Apple类型的bean实例挂钩
     */
    public void shutdown() {
        System.out.println("======Apple======名为close的方法不存在，使用shutdown方法====3===============");
    }

    /**
     * 通过@Bean的destroyMethod属性指定自定义的销毁方法，该销毁方法只跟@Bean注入的bean实例挂钩的
     * 方法要么带一个布尔类型参数(而且不能是布尔类型的包装类)，要么不带。
     * 同时spring调用该方法的时候，会给这个参数设置true
     * @param arg
     */
    public void custom(boolean arg) {
        System.out.println("=======apple1===@Bean====custom=================arg=" + arg);
    }

}
