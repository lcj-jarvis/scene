package com.mrlu.config;

import com.mrlu.entity.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2024-12-24 23:53
 */
@Configuration
public class Config {

    //工厂bean不能注入工厂自身。这种情况是不允许的，会报错
    //@Bean
    //public Config config() {
    //    Config config = new Config();
    //    return config;
    //}

    // 情况一：报错。不能存在参数个数相同，但是参数类型不一样的重载方法
    //throw new BeanCreationException(mbd.getResourceDescription(), beanName,
    //                "Ambiguous factory method matches found on class [" + factoryClass.getName() + "] " +
    //                        "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
    //ambiguousFactoryMethods);
    /*@Bean
    public City city(House house, Person person, Parent parent) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(parent);
        return city;
    }

    @Bean
    public City city(House house, Person person, Son son) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(son);
        return city;
    }*/

    /*
    // 情况二：使用c1。factoryMethodName=c1，c1方法先定义
    @Bean(value="city")
    public City c1(House house, Person person, Son son) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(son);
        return city;
    }

    @Bean(value="city")
    public City c2(House house, Person person, Son son) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(son);
        return city;
    }*/

    // 情况三：使用c2方法作为候选方法。因为c2方法先定义，所以factoryMethodName=c2。
    // 最终选择参数最多的c2方法实例化，即c2(House house, Person person, Son son) 方法
    @Bean(value="city")
    public City c2(House house, Person person) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        return city;
    }

    @Bean(value="city")
    public City c2(House house, Person person, Son son) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(son);
        return city;
    }

    @Bean(value="city")
    public City c1(House house, Person person, Son son) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(son);
        return city;
    }

    @Bean(value="city")
    public City c1(House house, Person person) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        return city;
    }


    // 情况四：报错。不能存在参数个数相同，但是参数类型不一样的重载方法
    //@Bean(value="city")
    //public City c2(House house, Person person) {
    //    City city = new City();
    //    city.setPerson(person);
    //    city.setHouse(house);
    //    return city;
    //}
    //
    //@Bean(value="city")
    //public City c2(House house, Son son) {
    //    City city = new City();
    //    city.setHouse(house);
    //    city.setParent(son);
    //    return city;
    //}
}
