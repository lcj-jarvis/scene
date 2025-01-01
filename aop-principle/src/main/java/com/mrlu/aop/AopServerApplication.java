package com.mrlu.aop;

import com.mrlu.aop.demo.ManService;
import com.mrlu.aop.demo.SimpleService;
import com.mrlu.core.anno.EnableLogManagement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@Slf4j
@EnableTransactionManagement
@EnableCaching
//当使用JDK动态代理时，代理对象只能注入为接口类型，因此你应该注入接口类型的Bean，而不是具体实现类的Bean。
// 使用spring.aop.proxy-target-class=false的时候启动会报错。
// 因为WhService注入了具体的AopInjectServiceImpl类，应该注入接口

// 这里设置使用jdk动态代理也没用。
// 因为AopAutoConfiguration的CglibAutoProxyConfiguration最后加载，还是会设置proxyTargetClass=true。使用cglib动态代理
// 只能通过spring.aop.proxy-target-class=false配置来使用jdk动态代理
// @EnableAspectJAutoProxy(proxyTargetClass = false)
@EnableLogManagement
// 直接通过aspectJ形式
//@EnableLogManagement(mode = AdviceMode.ASPECTJ)
public class AopServerApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(AopServerApplication.class, args);
        String ip = InetAddress.getLocalHost().getHostAddress();
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String port = environment.getProperty("server.port");
        String path = environment.getProperty("server.servlet.context-path");
        log.info("访问地址：http://{}:{}{}doc.html",ip,port,path);

        // 通过ProxyFactoryBean创建代理对象
        ManService manServiceProxy = (ManService) applicationContext.getBean("manServiceProxy");
        manServiceProxy.save();

        System.out.println("==================");
        SimpleService simpleService = applicationContext.getBean(SimpleService.class);
        simpleService.doSomething();
    }

}
