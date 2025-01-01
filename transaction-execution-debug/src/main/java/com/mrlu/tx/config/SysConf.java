package com.mrlu.tx.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @author 简单de快乐
 * @create 2024-08-02 17:38
 *
 * 使用 @EnableConfigurationProperties(SysConf.class) ，Spring会注入SysConf到IOC中，不会注入Conf。
 * 但是会给SysConf的configuration属性，创建一个Conf对象
 */
@Data
@ConfigurationProperties("sys")
@ToString
public class SysConf {
    private String location;


    @NestedConfigurationProperty
    private Conf configuration;
}
