package com.linkcm;


import com.feign.ApiFeignClient;
import com.linkcm.demo.Decorate;
import com.linkcm.demo.Demo;
import com.linkcm.demo.DemoService;
import com.mrlu.DeferredDemoConfig;
import com.mrlu.SuperNanShan;
import com.tag.TagDemoConfig;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 调试加载bean定义的过程
 *
 * @ConfigurationProperties的用法
 * https://zhuanlan.zhihu.com/p/139145432
 *
 */

// 配置不在org.springframework.boot.autoconfigure.EnableAutoConfiguration配置的就会报错
//@SpringBootApplication(exclude = DemoConfig.class)
@SpringBootApplication
@Slf4j
@ComponentScan("com.linkcm")
@ImportResource({"classpath:config/*.xml"})
@EnableScheduling
@EnableTransactionManagement
@EnableFeignClients(basePackages = "com.feign")
public class NanshanServerApplication extends SuperNanShan implements Decorate {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(NanshanServerApplication.class, args);
        String ip = InetAddress.getLocalHost().getHostAddress();
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String port = environment.getProperty("server.port");
        String path = environment.getProperty("server.servlet.context-path");
        log.info("访问地址：http://{}:{}{}doc.html",ip,port,path);


        // 获取自动配置的导入情况类
        ConditionEvaluationReport report = applicationContext.getBean("autoConfigurationReport", ConditionEvaluationReport.class);
        System.out.println(report.getExclusions());
        System.out.println(report.getUnconditionalClasses());

        // 测试内部类导入
        System.out.println(applicationContext.containsBeanDefinition("nanshanServerApplication.NestedConfig"));
        System.out.println("" + applicationContext.getBean("nanshanServerApplication.NestedConfig"));
        System.out.println("nestedConfig：" + applicationContext.getBean("demo02"));

        // 测试父类SuperNanShan注入的bean，特意不在扫描路径下，也不在自动配置类中设置。
        // 这里实际获取到的是NanshanServerApplication，父类是不会spring注入的。其实这样的设计也是合理的
        System.out.println("superNanShan：" + applicationContext.getBean(SuperNanShan.class));
        System.out.println("parentDemo：" + applicationContext.getBean("parentDemo"));


        // 测试实现接口里的@Bean方法
        System.out.println("decorate：" + applicationContext.getBean("decorate"));
        System.out.println("aDemo：" + applicationContext.getBean("aDemo"));


        // 测试抽象类里面的@Lookup方法注入的bean
        DemoService demoService = (DemoService) applicationContext.getBean("demoService");
        System.out.println("demoService：" + applicationContext.getBean("demoService"));
        System.out.println("absDemo：" + demoService.absDemo());
        System.out.println("absDemo eq demo01:" + demoService.absDemo().equals(applicationContext.getBean("demo01")));
        // @Lookup找到多个bean，抛出异常
        // System.out.println(demoService.demo04().equals(applicationContext.getBean("demo04")));


        // 测试org.springframework.boot.autoconfigure.EnableAutoConfiguration配置的HelloWorld配置类导入的
        System.out.println(applicationContext.getBean("helloDemo"));
        // System.out.println(applicationContext.getBean("helloWorld"));
        // org.springframework.boot.autoconfigure.EnableAutoConfiguration配置是全类目作为bean名称
        System.out.println(applicationContext.getBean("com.mrlu.HelloWorld"));

        //测试org.springframework.boot.autoconfigure.EnableAutoConfiguration配置的DemoDeferredImportSelector配置类导入的
        // 自动配置类，全类名
        System.out.println(applicationContext.getBean("com.mrlu.DeferredDemoConfig"));
        // 报错（bean名称不对）
        // System.out.println(applicationContext.getBean("deferredDemoConfig"));
        System.out.println(applicationContext.getBean(DeferredDemoConfig.class));
        System.out.println(applicationContext.getBean("deferredDemo"));


        // 通过xml设置的配置类
        System.out.println("TagDemoConfig: " + applicationContext.getBean(TagDemoConfig.class));
        System.out.println("tagDemo: " + applicationContext.getBean("tagDemo"));



        // 通过xml设置的配置类
        System.out.println("NestedConfigWithoutConfigAnnotation: " + applicationContext.getBean(NestedConfigWithoutConfigAnnotation.class));
        System.out.println("nest: " + applicationContext.getBean("nest"));


        // 获取feign的client
        System.out.println("ApiFeignClient: " + applicationContext.getBean(ApiFeignClient.class));

    }


    @Bean
    public Demo demo01() {
        return new Demo();
    }

    @Bean
    public Demo demo03() {
        return new Demo();
    }

    // @Bean method  must not be private or final;
    /*@Bean
    private Demo demo04() {
        return new Demo();
    }

    @Bean
    public final Demo demo05() {
        return new Demo();
    }*/

    @Configuration
    @Data
    @ToString
    @PropertySource("classpath:/nest.properties")
    @ConfigurationProperties(prefix = "nested")
    public static class NestedConfig {
        private String ip;
        private String port;

        @Bean
        public Demo demo02() {
            return new Demo();
        }
    }

    // 测试判断内部配置类是否为配置类
    public static class NestedConfigWithoutConfigAnnotation {
        @Bean
        public Demo nest() {
            return new Demo();
        }
    }
}
