package com.mrlu.resource.impl;

import com.mrlu.entity.Apple;
import com.mrlu.entity.Grape;
import com.mrlu.resource.ResourceService;
import com.mrlu.service.DogService;
import lombok.ToString;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 简单de快乐
 * @create 2023-11-24 16:40
 *
 * @Resource注解只能用于只有一个参数的实例方法，而且不能用于静态属性。
 *
 * @see org.springframework.context.annotation.CommonAnnotationBeanPostProcessor.ResourceElement#autowireResource(BeanFactory, CommonAnnotationBeanPostProcessor.LookupElement, String)
 *
 * 不指定name属性，采用默认名称，而且bean工厂无默认名称的bean，走根据类型获取逻辑
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#resolveDependency(DependencyDescriptor, String, Set, TypeConverter)
 */
@Service
@ToString
public class ResourceServiceImpl implements ResourceService, InitializingBean {

    // 不指定name属性，则可以注入
    @Resource
    private List<Grape> grapes;

    // 不指定name属性，则可以注入
    private List<Grape> grapeList;
    @Resource
    public void setGrapeList(List<Grape> grapeList) {
        this.grapeList = grapeList;
    }

    // 指定name属性，就会根据name和类型来获取。注入失败
    //@Resource(name = "givenGrapes")
    //private List<Grape> givenGrapes;

    @Resource(name = "apple1")
    private Apple apple;

    @Resource
    private DogService thirdDogServiceByResource;

    @Resource
    private Grape g1;

    @Resource(name = "g1")
    private Grape grapeField;

    private Grape g2;
    @Resource
    public void setG2(Grape ggg) {
        this.g2 = ggg;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(this);
    }

    /**
     * 执行报错。为什么呢？？？
     * 因为 @Resource没有指定name属性值，则使用默认生成的名称。即查找的名称为byMethod，而bean工厂没有该名称的bean。会走以下逻辑
     * @see org.springframework.context.annotation.CommonAnnotationBeanPostProcessor.ResourceElement#autowireResource(BeanFactory, CommonAnnotationBeanPostProcessor.LookupElement, String)
     * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#resolveDependency(DependencyDescriptor, String, Set, TypeConverter)
     *
     * 根据Grape类型获取bean，获取到beanName等于g1、g2的实例。
     * 找到有多个实例，但是需要注入的类型不是多个实例的（如List，Map，数组），就要确定唯一的bean实例。
     *
     * 根据方法参数名称ggg，判断是否和g1或者g2相等，从而确定唯一的bean实例。
     * 因为都不相等，获取到null。而@Resource不允许注入null，就会抛出异常
     * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#determineAutowireCandidate(Map, DependencyDescriptor)
     */
    //private Grape g2ByMethod;
    //@Resource
    //public void setByMethod(Grape ggg) {
    //    this.g2 = ggg;
    //}

    //@Resource注解只能用于只有一个参数的实例方法，而且不能用于静态属性。
    // 报错
    //private static Grape grape;
    //@Resource
    //public static void setG1(Grape ggg) {
    //    grape = ggg;
    //}

    //@Resource注解只能用于只有一个参数的实例方法，而且不能用于静态属性。
    // 报错
    //@Resource
    //private static Apple apple2;

    //@Resource注解只能用于只有一个参数的实例方法，而且不能用于静态属性。
    // 报错
    //@Resource
    //public void setGrapeAndApple(Grape ggg,  Apple apple1) {
    //    this.g2 = ggg;
    //    this.apple = apple1;
    //}

}
