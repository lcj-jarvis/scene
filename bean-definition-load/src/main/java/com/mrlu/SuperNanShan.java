package com.mrlu;

import com.linkcm.demo.Demo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2023-08-28 16:25
 */
@Configuration
public class SuperNanShan {

    @Bean
    public Demo parentDemo() {
        return new Demo();
    }
}
