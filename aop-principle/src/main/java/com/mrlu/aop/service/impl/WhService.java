package com.mrlu.aop.service.impl;


import com.mrlu.aop.service.AopInjectService;
import com.mrlu.aop.service.WebLogAnno;
import com.mrlu.aop.service.WhInterface;
import com.mrlu.core.anno.Log;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author 简单de快乐
 * @create 2024-05-20 14:55
 */
@Service
public class WhService extends WhParentService implements InitializingBean, WhInterface {

    // 使用cglib动态代理的时候，注入具体的类不会报错，因为cglib动态代理类是继承AopInjectServiceImpl来创建的
    // 使用jdk动态代理的时候，AopInjectService为合理的被代理接口，被代理对象基于实现接口来的，它本身已经继承Proxy类了，所以会报错。
    // 所以我们一般建议注入接口，不要注入具体的实现类
    /*
    The bean 'aopInjectServiceImpl' could not be injected because it is a JDK dynamic proxy
    The bean is of type 'com.sun.proxy.$Proxy71' and implements:
    com.mrlu.aop.service.AopInjectService
    org.springframework.beans.factory.InitializingBean
    org.springframework.aop.SpringProxy
    org.springframework.aop.framework.Advised
    org.springframework.core.DecoratingProxy
    */
    //@Autowired
    //private AopInjectServiceImpl aopInjectServiceImpl;

    @Autowired
    private AopInjectService aopInjectService;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("WhService");
    }

    @Transactional
    @WebLogAnno
    @Cacheable(cacheNames = "whCache", key = "#type")
    public String getWhList(String type) {
        System.out.println("getWhList");
        return "getWhList";
    }

    @Override
    @Log
    @Transactional
    public void testLog(String content) {
        System.out.println("testLog content：" + content);
    }

}
