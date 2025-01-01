package com.mrlu.config;

import com.mrlu.entity.Bird;
import com.mrlu.entity.Tree;
import com.mrlu.entity.Village;
import org.springframework.context.annotation.Configuration;

/**
 * The type Village config.
 *
 * @author 简单de快乐
 * @create 2024 -12-26 22:49
 */
@Configuration
public class VillageConfig {

    /**
     * Village village.
     *
     * 最终使用这个方法实例化。
     * @see com.mrlu.register.CustomRegister
     *
     * @param name the name
     * @param tree the tree
     * @param bird the bird
     * @return the village
     */
    public Village village(String name, Tree tree, Bird bird) {
        Village village = new Village();
        village.setName(name);
        village.setTree(tree);
        village.setBird(bird);
        System.out.println("使用village(String name, Tree tree, Bird bird)工厂方法");
        return village;
    }

    /**
     * Village village.
     *
     * @param name the name
     * @param tree the tree
     * @return the village
     */
    public Village village(String name, Tree tree) {
        Village village = new Village();
        village.setName(name);
        village.setTree(tree);
        System.out.println("使用village(String name, Tree tree)工厂方法");
        return village;
    }

}
