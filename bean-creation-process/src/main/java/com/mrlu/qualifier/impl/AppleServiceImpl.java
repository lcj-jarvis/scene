package com.mrlu.qualifier.impl;

import com.mrlu.config.FruitConfig;
import com.mrlu.entity.*;
import com.mrlu.qualifier.AppleService;
import com.mrlu.qualifier.GrapeService;
import lombok.ToString;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 *
 *  @Autowired  + @Qualifier 注入依赖源码概述
 * 一、根据方法进行Qualifier
 * 1、根据方法参数类型获取相应的bean
 * 2、获取方法参数的@Qualifier注解，作为期望注解，判断bean是否符合条件
 *      执行"判断bean是否属于候选的bean"逻辑，如果符合，则执行步骤3，不符合，则直接结束
 *    如果方法参数上不存在@Qualifier注解，则步骤2返回"符合条件"，则执行步骤3
 * 3、如果方法的返回值类型属于void类型，获取方法上的@Qualifier注解，作为期望注解，判断bean是否符合条件
 *        执行"判断bean是否属于候选的bean"，如果符合，说明是符合条件的bean
 *    如果方法的返回值类型不属于void类型，则采用步骤2的判断结果
 *
 * 总结：
 *    对于void返回值类型的方法，
 *    如果在方法或者方法参数上同时使用了 @Qualifier注解，
 *    会先判断方法参数的 @Qualifier注解，再判断方法上的@Qualifier注解，
 *    两者都符合条件才算符合条件
 *    对于不是void返回值类型的方法，判断方法参数的 @Qualifier注解，符合条件即可
 *
 * 二、属性直接进行Qualifier
 *    获取属性上的@Qualifier注解，作为期望注解，判断bean是否属于候选的bean
 *    如果属于，则进行注入
 *
 *
 * 【判断bean是否属于候选的bean】
 *  分为以下两大情况
 *  一、bean定义没有(程序员手动指定) AutowireCandidateQualifier
 *   (1)如果bean定义中有@Qualifier注解，则直接获取。判断是否和期望注解相同，相同则说明符合条件
 *      如果没有，则执行(2)
 *   (2)如果该bean是由@Bean方法创建的，获取@Bean方法上的@Qualifier注解。判断是否和期望注解相同，相同则说明符合条件
 *      如果不是，则执行(3)
 *   (3)获取bean的Class对象, 获取Class对象的@Qualifier注解。判断是否和期望注解相同，相同则说明符合条件
 *      如果没有@Qualifier注解，则执行(4)
 *  （4）获取bean的名称, 获取期望注解的value属性。判断bean名称是否和value相同，相同则说明符合条件
 *      如果不相同，则说明不符合条件
 *
 *  判断是否和期望注解相同：实际上重写了注解的equals方法，会判断注解的属性值是否相等。
 *
 *  二、bean定义有(程序员手动指定) AutowireCandidateQualifier
 *  如果注入的属性使用@Qualifier注解，没有指定value属性，则使用默认的空串作为期望值。
 * （1）候选的bean没有使用@Qualifier注解，则判断beanName是否等于期望值。
 * （2）候选的bean使用@Qualifier注解，但是没有指定value属性。都符合条件，都是候选bean
 * （3）候选的bean使用@Qualifier注解，指定value属性。判断指定的value值是否等于期望值
 *
 * 如果注入的属性使用@Qualifier注解， 指定value属性，则使用指定的值作为期望值。
 * （1）候选的bean没有使用@Qualifier注解，则判断beanName是否等于期望值。
 * （2）候选的bean使用@Qualifier注解，但是没有指定value属性。则判断beanName是否等于期望值。
 * （3）候选的bean使用@Qualifier注解，指定value属性。判断指定的value值是否等于期望值
 *
 *
 * 程序员手动指定AutowireCandidateQualifier，参考以下
 * @see com.mrlu.register.CustomRegister
 *
 *
 *
 * 具体源码位置
 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean(String, RootBeanDefinition, Object[])
 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance(String, RootBeanDefinition, Object[])

 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean(String, RootBeanDefinition, BeanWrapper)
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#postProcessProperties(PropertyValues, Object, String)
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.AutowiredFieldElement#inject(Object, String, PropertyValues)
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.AutowiredMethodElement#inject(Object, String, PropertyValues)
 *
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#resolveDependency(DependencyDescriptor, String, Set, TypeConverter)
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#doResolveDependency(DependencyDescriptor, String, Set, TypeConverter)
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#isAutowireCandidate(String, RootBeanDefinition, DependencyDescriptor, AutowireCandidateResolver)
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#findAutowireCandidates(String, Class, DependencyDescriptor)
 * @see org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver#checkQualifier(BeanDefinitionHolder, Annotation, TypeConverter)
 *
 * 见 AutowiredAnnotationBeanPostProcessor、QualifierAnnotationAutowireCandidateResolver类
 *
 * 工厂方法(@Bean方法)注入
 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#instantiateUsingFactoryMethod(String, RootBeanDefinition, Object[])
 * @see org.springframework.beans.factory.support.ConstructorResolver#instantiateUsingFactoryMethod(String, RootBeanDefinition, Object[])
 *
 */
@Service
@ToString
public class AppleServiceImpl implements AppleService, InitializingBean {

    @Autowired
    // 这样可以注入
    @Qualifier("r1")
    // 这样也可以注入
    //@Qualifier("river1")
    private River river;

    @Autowired
    @Qualifier(value = "f1")
    //@Qualifier
    //@Qualifier(value = "f1Forest")
    private Forest forest;

    // phoneFactoryBean
    @Autowired
    private Phone phone;

    // 有多个FactoryBean。想要PhoneFactoryBean，需要指定具体的名称。
    // FactoryBean的beanName以&开头，但是变量命名不能以&开头，需要换写法
    //@Autowired
    //private FactoryBean &phoneFactoryBean;

    @Autowired
    @Qualifier("&phoneFactoryBean")
    private FactoryBean phoneFactoryBean;

    @Autowired
    private PhoneFactoryBean factoryBean;


    @Autowired
    public AppleServiceImpl() {
    }

    @Autowired
    private City city;

    @Autowired
    @Qualifier("gs1")
    private GrapeService grapeService;

    @Autowired
    @Qualifier("a1")
    private Apple first;

    @Autowired
    @Qualifier("apple2")
    private Apple second;

    private Grape g2;
    @Autowired
    public void setG2(@Qualifier("g2") Grape grape) {
        this.g2 = grape;
    }

    private Grape g1;
    //获取方法参数的名称g1，如果有bean名称为g1的就注入
    @Autowired
    @Qualifier("g1")
    public void setG1(Grape g1) {
        this.g1 = g1;
    }

    //private Grape grapeUseMethodAnno;
    //// 注入失败
    ///**
    // * 方法参数上没有@Qualifier注解，然后方法的返回值类型不是Void，
    // * 则判断所有的Grape类型中，是否有名称为ggg的bean。如果有的话，就可以注入，没有的话，就会抛出异常。
    // * @param ggg
    // * @return
    // */
    //@Autowired
    //@Qualifier("g1")
    //public Grape setGrapeUseMethodAnno(Grape ggg) {
    //    this.grapeUseMethodAnno = ggg;
    //    return grapeUseMethodAnno;
    //}

    // void返回值类型的方法，方法参数和方法上同时有@Qualifier注解，要求@Qualifier注解的value一样才能注入
    private Grape grape;
    @Autowired
    @Qualifier("g2")
    public void assembleGrape(@Qualifier("g2") Grape grape) {
        this.grape = grape;
    }

    // 返回值类型非void类型，则会以方法参数上的@Qualifier注解为准.
    // 所以这里会注入名为g1的bean
    private Grape useMethodParameterGrape;
    @Autowired
    @Qualifier("g2")
    public Grape useMethodParameterGrape(@Qualifier("g1") Grape grape) {
        this.useMethodParameterGrape = grape;
        return useMethodParameterGrape;
    }

    // 注入失败
    //private Grape grape1;
    //@Autowired
    //@Qualifier("g1")
    //public void setGrape1(@Qualifier("g2") Grape grape1) {
    //    this.grape1 = grape1;
    //}

    // 这样无效。因为识别不到@Autowired注解，找不到注入点
    /*private Grape g1;
    public void setG1(@Autowired @Qualifier("g1") Grape g1) {
        this.g1 = g1;
    }*/


    // 自己依赖自己的情况，可以注入。这里注释掉，因为用Lambook的@ToString生成toString方法会报错，会一直递归
    //@Autowired
    //private AppleService appleService;

    // 这种情况不是自己注入自己，会注入失败
    // @Autowired
    // private Collection<AppleService> appleService;

    // 这种情况不是自己注入自己，会注入失败
    // @Autowired
    // private Collection<AppleService> appleServiceCollection;

    @Autowired
    private List<Grape> grapes;

    @Autowired
    private FruitConfig fruitConfig;


    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(this);
        System.out.println("==========================================");
    }

    public void close()  {
        System.out.println("======AppleServiceImpl====close方法不是来自AutoCloseable======2============");
    }

    public void shutdown()  {
        System.out.println("==AppleServiceImpl========shutdown======2============");
    }



}
