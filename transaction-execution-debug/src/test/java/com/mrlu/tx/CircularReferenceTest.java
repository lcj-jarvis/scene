package com.mrlu.tx;

import com.mrlu.tx.circular.AnService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 简单de快乐
 * @create 2024-07-18 17:42
 */
@SpringBootTest
public class CircularReferenceTest {

    @Autowired
    private AnService an;

    /*
    场景：An里面依赖Ba，Ba里面依赖An。同时这两个bean会生成(事务)代理对象。
    分析循环依赖如何解决：
    1、创建An实例，暴露an实例的创建工厂到Spring的第三级缓存，注入属性ba，创建Ba实例
    2、创建Ba实例，注入属性an
      （1）从Spring第三级缓存获取an实例的创建工厂，创建an实例的代理对象anAop(标记an实例为已经创建过代理对象)，
       此时创建好的anAop实例保存着an实例(被代理对象)，最后将anAop实例保存到二级缓存，返回anAop用于Ba实例的an属性注入。
    3、Ba实例完成an属性注入，使用AOP后置处理器，创建Ba的代理对象baAop，baAop实例中保存着实例ba(被代理对象)。
       此时Ba类型的bean创建完成，添加到一级缓存中，返回baAop用于An实例的ba属性注入。
    4、An实例完成ba属性注入（同时anAop实例保存的实例an里的ba属性也自动完成了注入，因为是同一个对象）
    5、使用AOP后置处理器，判断是否需要创建An代理实例。
       因为在创建Ba实例的过程中，已经创建过了，所以不需要再次创建代理实例。
    6、从Spring三级缓存结构的第二级缓存中获取保存的anAop实例，作为An类型的bean返回，然后到添加到一级缓存

    所以最后的关系如下：
    anAop[包含实例an，里面的ba属性(使用cglib动态代理时才有该属性)为null]实例
    an实例[包含baAop实例]
    baAop[包含实例ba，里面的an属性(使用cglib动态代理时才有该属性)为null]示例
    ba实例[包含anAop实例]

    调用分析：
    调用被代理的方法，如通过@Autowired注入 An类型bean，调用ta()方法
    @Autowired注入是anAop，通过anAop调用完增强器逻辑后，再使用an实例通过反射调用ta()方法。
    an实例里面注入的是baAop实例，通过baAop调用完增强器逻辑后，再使用实例ba通过反射调用tb()方法。

    调用非被代理的方法：
    @Autowired注入是anAop，无需执行增强器逻辑，直接使用实例an调用非代理方法。
     */
    @Test
    public void referenceDebug() {
        an.ta();
    }

}
