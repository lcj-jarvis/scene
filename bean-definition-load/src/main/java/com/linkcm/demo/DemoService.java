package com.linkcm.demo;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

/**
 * @author 简单de快乐
 * @create 2023-08-23 14:50
 */
@Service
@P2("1")
@P2("2")
public abstract class DemoService {

    /**
     * @Lookup注解标注的方法，如果@Lookup注解设置了value值，则根据value的值，去ioc中获取bean
     * 如果没有设置，根据方法的返回值类型，去ioc中获取bean，如果找到多个bean就会抛异常
     * @return
     */
    // 从ioc中指定获取demo01这个bean
    @Lookup("demo01")
    public abstract Demo absDemo();

    /*@Lookup
    public abstract Demo demo04();*/

}
