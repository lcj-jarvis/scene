package com.mrlu.entity;

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * The type Mountain.
 */
@Data
@ToString
public class Mountain implements InitializingBean {

    private String name;

    private Tree tree;

    private Bird bird;

    private River river;

    /**
     * Instantiates a new Mountain.
     */
    public Mountain() {
        System.out.println("Mountain无参构造方法");
    }

    /**
     * Instantiates a new Mountain.
     *
     * @param name the name
     * @param tree the tree
     */
    public Mountain(String name, Tree tree) {
        this.name = name;
        this.tree = tree;
        System.out.println("Mountain(String name, Tree tree)构造方法");
    }

    /**
     * Instantiates a new Mountain.
     *
     * @param tree the tree
     * @param bird the bird
     */
    public Mountain(Tree tree, Bird bird) {
        this.tree = tree;
        this.bird = bird;
        System.out.println("Mountain(Tree tree, Bird bird)构造方法");
    }

    /**
     * Instantiates a new Mountain.
     *
     * @see com.mrlu.register.CustomRegister
     * @param name the name
     * @param tree the tree
     * @param bird the bird
     */
    public Mountain(String name, Tree tree, Bird bird) {
        this.name = name;
        this.tree = tree;
        this.bird = bird;
        System.out.println("Mountain(String name, Tree tree, Bird bird)构造方法");
    }


    /**
     * Instantiates a new Mountain.
     *
     * 最终使用这个方法实例化。前三个参数从bean定义指定的构造方法参数获取，river参数从beanFactory解析依赖获取
     * @see com.mrlu.register.CustomRegister
     * @param name the name
     * @param tree the tree
     * @param bird the bird
     * @param river the river
     */
    public Mountain(String name, Tree tree, Bird bird, @Qualifier("r1") River river) {
        this.name = name;
        this.tree = tree;
        this.bird = bird;
        this.river = river;
        System.out.println("Mountain(String name, Tree tree, Bird bird, River river)构造方法");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(this);
    }
}
