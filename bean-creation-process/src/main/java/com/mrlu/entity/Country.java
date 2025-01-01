package com.mrlu.entity;

import lombok.ToString;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * The type Country.
 *
 * @author 简单de快乐
 * @create 2024 -12-30 16:28
 */
@ToString
public class Country implements InitializingBean {

    private String name;

    private Mountain mountain;

    private Man man;

    private River river;

    /**
     * Instantiates a new Country.
     *
     * @param name the name
     */
    public Country(String name) {
        this.name = name;
    }

    /**
     * Instantiates a new Country.
     *
     * @param name     the name
     * @param mountain the mountain
     */
    public Country(String name, Mountain mountain) {
        this.name = name;
        this.mountain = mountain;
    }

    /**
     * Instantiates a new Country.
     *
     * @param name     the name
     * @param mountain the mountain
     * @param man      the man
     */
    public Country(String name, Mountain mountain, Man man) {
        this.name = name;
        this.mountain = mountain;
        this.man = man;
    }

    /**
     * Instantiates a new Country.
     * 最终使用这个方法实例化。前三个参数从bean定义指定的构造方法参数获取，river参数从beanFactory解析依赖获取
     * @param name     the name
     * @param mountain the mountain
     * @param man      the man
     * @param river    the river
     */
    public Country(String name, Mountain mountain, Man man, @Qualifier("r1") River river) {
        this.name = name;
        this.mountain = mountain;
        this.man = man;
        this.river = river;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(this);
    }
}
