package com.mrlu.aop.cglib;


import org.springframework.cglib.proxy.*;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * cglib动态代理demo
 */
public class CglibProxyExample {
    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        // 设置被代理的类
        enhancer.setSuperclass(MyClass.class);
        // 设置回调列表
        Callback[] mainCallbacks = new Callback[] {
            new AopInterceptor(),
            NoOp.INSTANCE,
            new SerializableNoOp(),
            new TargetDispatcher(),
            new AdvisedDispatcher(),
            new EqualsInterceptor(),
            new HashCodeInterceptor()
        };
        enhancer.setCallbacks(mainCallbacks);
        // 设置回调过滤器。确定回调列表的索引，决定使用的回调器。
        // 多个回调器的时候，必须设置
        enhancer.setCallbackFilter(new MyCallbackFilter());
        MyClass proxy = (MyClass) enhancer.create();
        proxy.publicMethod();
        proxy.advisedDispatcherLoad();
        proxy.targetDispatcherLoad();
        proxy.noOp();
        System.out.println(proxy.hashCode());
        System.out.println(proxy.equals(new MyClass()));
        System.out.println("========================================");

        // 以下是指定一个回调，无需设置过滤器
        Enhancer enhancer02 = new Enhancer();
        // 设置被代理的类
        enhancer02.setSuperclass(MyClass02.class);
        enhancer02.setCallback(new AopInterceptor());
        MyClass02 myClass02 = (MyClass02) enhancer02.create();
        myClass02.publicMethod();
    }
}

class MyCallbackFilter implements CallbackFilter {
    @Override
    public int accept(Method method) {
        if (method.getName().equals("equals")) {
            return 5;
        } else if (method.getName().equals("hashCode")) {
            return 6;
        } else if (method.getName().equals("targetDispatcherLoad")) {
            return 3;
        } else if (method.getName().equals("advisedDispatcherLoad")) {
            return 4;
        } else if (method.getName().equals("noOp")) {
            return 1;
        }
        return 0;
    }
}
class MyClass02 {
    public void publicMethod() {
        System.out.println("MyClass02 Public method called");
    }

}


class MyClass {
    public void publicMethod() {
        System.out.println("Public method called");
    }

    public void targetDispatcherLoad() {
        System.out.println("targetDispatcherLoad");
    }

    public void advisedDispatcherLoad() {
        System.out.println("advisedDispatcherLoad");
    }

    public void noOp() {
        System.out.println("=========noOp=========");
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof MyClass;
    }

    @Override
    public int hashCode() {
        return 42;
    }
}

class AopInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("begin AOP Interceptor: " + method.getName());
        Object value = proxy.invokeSuper(obj, args);
        System.out.println("finish AOP Interceptor: " + method.getName());
        return value;
    }
}

class SerializableNoOp implements NoOp, Serializable {}

class TargetDispatcher implements Dispatcher {
    @Override
    public Object loadObject() throws Exception {
        System.out.println("TargetDispatcher loadObject");
        return new MyClass();
    }
}

class AdvisedDispatcher implements Dispatcher {
    @Override
    public Object loadObject() throws Exception {
        System.out.println("AdvisedDispatcher loadObject");
        return new MyClass();
    }
}

class EqualsInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("Equals Interceptor");
        return proxy.invokeSuper(obj, args);
    }
}

class HashCodeInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("HashCode Interceptor");
        return proxy.invokeSuper(obj, args);
    }
}

