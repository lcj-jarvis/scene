package com.mrlu.config;

import com.mrlu.entity.Bird;
import com.mrlu.entity.Cloud;
import com.mrlu.entity.Colour;
import com.mrlu.entity.Sky;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Sky config.
 *
 * @author 简单de快乐
 * @create 2024 -12-30 20:12
 *
 * 在CustomRegister里注册了Sky的工厂方法
 */
@Configuration
public class SkyConfig {

    /**
     * Sky sky.
     *
     * @return the sky
     */
    public Sky sky() {
        return new Sky();
    }

    /**
     * Sky sky.
     *
     * @param number the number
     * @return the sky
     */
    public Sky sky(Integer number) {
        Sky sky = new Sky();
        sky.setNumber(number);
        return sky;
    }

    /**
     * Sky sky.
     *
     * @param number the number
     * @param colour the colour
     * @return the sky
     */
    public Sky sky(Integer number, Colour colour) {
        Sky sky = new Sky();
        sky.setNumber(number);
        sky.setColour(colour);
        return sky;
    }

    /**
     * Sky sky.
     *
     * @param number the number
     * @param colour the colour
     * @param bird   the bird
     * @return the sky
     */
    public Sky sky(Integer number, Colour colour, Bird bird) {
        Sky sky = new Sky();
        sky.setNumber(number);
        sky.setColour(colour);
        sky.setBird(bird);
        return sky;
    }

    /**
     * Sky sky.
     * 最终使用这个工厂方法实例化。
     * 前三个参数从bean定义指定的工厂方法参数获取，第四个参数从bean工厂进行依赖注入
     *
     * @param number the number
     * @param colour the colour
     * @param bird   the bird
     * @param cloud  the cloud
     * @return the sky
     */
    public Sky sky(Integer number, Colour colour, Bird bird, Cloud cloud) {
        Sky sky = new Sky();
        sky.setNumber(number);
        sky.setColour(colour);
        sky.setBird(bird);
        sky.setCloud(cloud);
        return sky;
    }

    @Bean
    public Cloud cloud() {
        Cloud cloud = new Cloud();
        cloud.setName("白云");
        return cloud;
    }

}
