package com.mrlu.server;

import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 简单de快乐
 * @create 2024-07-10 23:16
 */
public class DebugTest {


    private static int count = 1;


    private static void addCount() {
        count++;
    }

    private static void subCount() {
        count--;
    }

    @Test
    public void test() {
        addCount();
        System.out.println(count);
    }


    @Test
    public void debug() {
        List<Integer> list = new ArrayList<>();
        int count = 20;
        for (int i = 1; i <= count; i++) {
            list.add(i);
        }

        List<Integer> collect = list.stream().filter(num -> num > 5).map(num -> num * 2).collect(Collectors.toList());
        collect.forEach(num -> {
            System.out.println(num);
        });


        new Thread(() -> {
            Integer num = null;
            System.out.println(num + 1);
        }).start();


        List<Key> names = new ArrayList<>();
        names.add(new Key().setName("a").setValue(1));
        names.add(new Key().setName("b").setValue(2));
        names.add(new Key().setName("c").setValue(3));
        names.add(new Key().setName("d").setValue(4));
        names.add(new Key().setName("e").setValue(5));
        names.add(new Key().setName("f").setValue(5));
        Map<String, Integer> nameMap = toMap(names);
        Map<String, List<Key>> groupByName = groupByName(names);
        nameMap.forEach((key, value) -> System.out.println(key + ":" + value));
        System.out.println("======================================================");
        groupByName.forEach((key, value) -> System.out.println(key + ":" + value));

        addCount();
        subCount();


        CarLock carLock = new CarLock();
        carLock.lock();
        carLock.unlock();


        HouseLock houseLock = new HouseLock();
        houseLock.lock();
        houseLock.unlock();

        Integer num = null;
        System.out.println(num + 1);
    }


    public static Map<String, Integer> toMap(List<Key> names) {
        Map<String, Integer> nameMap = names.stream().filter(key -> key.getValue() > 2).collect(Collectors.toMap(Key::getName, Key::getValue));
        return nameMap;
    }


    public static Map<String, List<Key>> groupByName(List<Key> names) {
        Map<String, List<Key>> groupByName = names.stream().filter(key -> key.getValue() > 2).collect(Collectors.groupingBy(Key::getName));
        return groupByName;
    }



    @Test
    public void lockProxyTest() {
        Lock instance = (Lock) Proxy.newProxyInstance(Lock.class.getClassLoader(), new Class[]{Lock.class}, new LockProxy());
        instance.lock();
        instance.unlock();
    }

}

@Data
@Accessors(chain = true)
class Key{
    String name;
    Integer value;
}

interface Lock {
    void lock();
    void unlock();
}

class CarLock implements Lock{

    @Override
    public void lock() {
        System.out.println("=========car=lock=======");
    }

    @Override
    public void unlock() {
        System.out.println("=========car=unlock=======");
    }
}

class HouseLock implements Lock{

    @Override
    public void lock() {
        System.out.println("=========house=lock=======");
    }

    @Override
    public void unlock() {
        System.out.println("=========house=unlock=======");
    }
}

class LockProxy implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("==========LockProxy===========" + method.getName());
        return null;
    }
}

