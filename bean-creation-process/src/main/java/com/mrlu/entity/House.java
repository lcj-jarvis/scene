package com.mrlu.entity;

import lombok.ToString;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author 简单de快乐
 * @create 2024-12-22 22:55
 *
 * 手动指定使用AbstractBeanDefinition.AUTOWIRE_BY_TYPE
 */
@ToString
public class House implements InitializingBean {

    private Person person;

    private Tv tv;

    private Desk desk;

    // 如果获取到多个候选的bean，在确定唯一的依赖时，如果通过@Primary注解、@Priority注解都没有得到唯一的bean后，
    // 因为这里重写返回null，则依赖描述器的依赖名称都是不等于候选的beanName的，最后无法确定唯一的依赖。
    // 就会报错，即使AutowireByTypeDependencyDescriptor设置required=false。
    // 因为注入的类型不是集合、数组、Map等多个bean的类型。具体看resolveDependency方法
    private River river;

    // 这样也不行。因为创建的AutowireByTypeDependencyDescriptor本质是用set方法注入的，相当于方法参数注入
    // 所以获取不到属性上的注解，走不到判断@Qualifier的逻辑。
    // 如果在set方法参数上使用@Qualifier("r1")就可以确定唯一的候选对象。
    // @Qualifier("r1")
    // private River river;

    //public void setRiver(River river) {
    //    this.river = river;
    //}

    public void setRiver(@Qualifier("r1") River river) {
        this.river = river;
    }

    public void setTv(Tv tv) {
        this.tv = tv;
    }

    public void setDesk(Desk desk) {
        this.desk = desk;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(this);
    }
}
