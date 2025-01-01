package com.tag;

import com.linkcm.demo.Demo;
import org.springframework.context.annotation.Bean;

/**
 * @author 简单de快乐
 * @create 2023-08-29 23:21
 */
// 为什么不用@Configuration也可以呢？因为我们再xml文件通过bean标签配置了这个类，而且类里有@Bean方法，就会被认为是配置类。
//@Configuration
public class TagDemoConfig {


    @Bean
    public Demo tagDemo() {
        return new Demo();
    }
}
