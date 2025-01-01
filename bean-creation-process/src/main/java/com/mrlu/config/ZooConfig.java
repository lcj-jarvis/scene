package com.mrlu.config;

import com.mrlu.entity.Cat;
import com.mrlu.entity.Dog;
import com.mrlu.entity.Person;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2023-10-13 17:15
 */
@Configuration
public class ZooConfig {
    @Bean(destroyMethod = "captainDestroy")
    public Person captain() {
        return new Person();
    }

    @Bean
    public Dog dog() {
        Dog d1 = new Dog();
        d1.setName("d1");
        return d1;
    }

    @Bean
    public Cat cat() {
        return new Cat();
    }

    @Bean
    public Cat cat01() {
        return new Cat();
    }

    @Bean
    public Cat cat02() {
        return new Cat();
    }

    @Bean
    public Cat cat03() {
        return new Cat();
    }
}
