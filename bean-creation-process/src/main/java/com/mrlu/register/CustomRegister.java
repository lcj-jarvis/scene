package com.mrlu.register;

import com.mrlu.config.SkyConfig;
import com.mrlu.config.VillageConfig;
import com.mrlu.entity.*;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.*;
import org.springframework.stereotype.Component;

/**
 * @author 简单de快乐
 * @create 2024-12-22 22:51
 *
 * 编程式注册bean定义到Spring
 *
 * @MppaerScan 注解加载的MapperScannerConfigurer 也是类似的原理
 */
@Component
public class CustomRegister implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        System.out.println("===================================开始手动注册bean=================================");
        // 注入beanName分别为f1Forest，f2Forest的Forest的Bean定义，同时分别为它们指定@Qualifier("f1")、@Qualifier("f2");
        Class<Forest> forestClass = Forest.class;
        BeanDefinitionBuilder f1Builder = BeanDefinitionBuilder.genericBeanDefinition(forestClass);
        AbstractBeanDefinition f1 = f1Builder.getBeanDefinition();
        // 相当于手动添加@Qualifier("f1");
        AutowireCandidateQualifier f1Qualifier = new AutowireCandidateQualifier(Qualifier.class, "f1");
        // 相当于手动添加@Qualifier，不指定value属性
        //AutowireCandidateQualifier f1Qualifier = new AutowireCandidateQualifier(Qualifier.class);
        f1.addQualifier(f1Qualifier);
        // 指定beanName
        String f1ForestName = "f1Forest";
        // 注册bean定义
        registry.registerBeanDefinition(f1ForestName, f1);

        BeanDefinitionBuilder f2Builder = BeanDefinitionBuilder.genericBeanDefinition(forestClass);
        AbstractBeanDefinition f2 = f2Builder.getBeanDefinition();
        // 相当于手动添加@Qualifier("f2");
        AutowireCandidateQualifier f2Qualifier = new AutowireCandidateQualifier(Qualifier.class, "f2");
        // 相当于手动添加@Qualifier，不指定value属性
        //AutowireCandidateQualifier f2Qualifier = new AutowireCandidateQualifier(Qualifier.class);
        f2.addQualifier(f2Qualifier);
        // 指定beanName
        String f2ForestName = "f2Forest";
        // 注册bean定义
        registry.registerBeanDefinition(f2ForestName, f2);


        // 注入house bean
        Class<House> houseClass = House.class;
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(houseClass);
        // 设置house的person属性
        Person person = new Person();
        person.setName("lu");
        builder.addPropertyValue("person", person);
        // 除person属性外，所有的属性设置根据类型注入。要求要有属性的设置方法。
        builder.getBeanDefinition().setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        // 注册bean定义
        registry.registerBeanDefinition(getBeanName(houseClass), builder.getBeanDefinition());


        // 注入dragonClass bean
        Class<Dragon> dragonClass = Dragon.class;
        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(dragonClass);
        // 设置实例提供器
        definition.getBeanDefinition().setInstanceSupplier(() -> {
            Dragon dragon = new Dragon();
            dragon.setName("dragon");
            return dragon;
        });
        registry.registerBeanDefinition(getBeanName(dragonClass), definition.getBeanDefinition());


        // 创建Table实例，指定使用有参构造方法
        Class<Table> tableClass = Table.class;
        Desk desk = new Desk();
        desk.setBrand("Black-Desk");
        BeanDefinitionBuilder tableDefinition = BeanDefinitionBuilder.genericBeanDefinition(tableClass);
        // 设置构造方法使用的参数
        tableDefinition.getBeanDefinition().getConstructorArgumentValues()
                .addGenericArgumentValue(desk);
        registry.registerBeanDefinition(getBeanName(tableClass), tableDefinition.getBeanDefinition());

        // 创建并注册Mountain Bean定义
        Class<Mountain> mountainClass = Mountain.class;
        BeanDefinitionBuilder mountainDefinition = BeanDefinitionBuilder.genericBeanDefinition(mountainClass);
        // 添加构造方法参数
        ConstructorArgumentValues constructorArgumentValues = mountainDefinition.getBeanDefinition()
                .getConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, "珠峰");
        Tree tree = new Tree();
        tree.setName("树木");
        constructorArgumentValues.addIndexedArgumentValue(1, tree);
        Bird bird = new Bird();
        bird.setName("小鸟");
        constructorArgumentValues.addIndexedArgumentValue(2, bird);
        String mountainBean = getBeanName(mountainClass);
        // 对没有指定的参数值，使用构造方法参数注入
        mountainDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        registry.registerBeanDefinition(mountainBean, mountainDefinition.getBeanDefinition());

        // 创建并注册country Bean定义
        Class<Country> countryClass = Country.class;
        BeanDefinitionBuilder countryBuilder = BeanDefinitionBuilder.genericBeanDefinition(countryClass);
        // 添加构造方法参数
        ConstructorArgumentValues argumentValues = countryBuilder.getBeanDefinition().getConstructorArgumentValues();
        argumentValues.addIndexedArgumentValue(0, "China");
        // 添加mountain Bean引用
        RuntimeBeanReference reference = new RuntimeBeanReference(mountainBean);
        argumentValues.addIndexedArgumentValue(1, reference);
        Man man = new Man();
        man.setName("男人");
        argumentValues.addGenericArgumentValue(man);
        // 对没有指定的参数值，使用构造方法参数注入
        countryBuilder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        registry.registerBeanDefinition(getBeanName(countryClass), countryBuilder.getBeanDefinition());


        // 创建Container实例
        Class<Container> containerClass = Container.class;
        BeanDefinitionBuilder containerDefinition = BeanDefinitionBuilder.genericBeanDefinition(containerClass);
        // 设置构造方法参数为”bean工厂中类型为Parent的bean“
        RuntimeBeanReference beanReference = new RuntimeBeanReference(Parent.class);
        containerDefinition.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(beanReference);
        registry.registerBeanDefinition(getBeanName(containerClass), containerDefinition.getBeanDefinition());


        // 使用工厂方法创建Village。指定工厂方法的参数
        Class<Village> villageClass = Village.class;
        BeanDefinitionBuilder villageDefinition = BeanDefinitionBuilder.genericBeanDefinition(villageClass);
        // 设置工厂方法名称和工厂bean名称
        String factoryMethod = "village";
        villageDefinition.setFactoryMethodOnBean(factoryMethod, getBeanName(VillageConfig.class));
        // 指定工厂方法的三个参数
        ConstructorArgumentValues villageArgs = villageDefinition.getBeanDefinition().getConstructorArgumentValues();
        villageArgs.addIndexedArgumentValue(0, "梅里雪山");
        Tree huYang = new Tree();
        huYang.setName("胡杨");
        villageArgs.addIndexedArgumentValue(1, huYang);
        Bird maQue = new Bird();
        maQue.setName("麻雀");
        villageArgs.addIndexedArgumentValue(2, maQue);
        registry.registerBeanDefinition(getBeanName(villageClass), villageDefinition.getBeanDefinition());


        // 使用工厂方法创建Sky。指定工厂方法的参数
        Class<Sky> skyClass = Sky.class;
        BeanDefinitionBuilder skyBuilder = BeanDefinitionBuilder.genericBeanDefinition(skyClass);
        String skyFactoryMethod = "sky";
        skyBuilder.setFactoryMethodOnBean(skyFactoryMethod, getBeanName(SkyConfig.class));
        // 指定工厂方法的三个参数
        AbstractBeanDefinition skyBeanDefinition = skyBuilder.getBeanDefinition();
        ConstructorArgumentValues skyArgs = skyBeanDefinition.getConstructorArgumentValues();
        skyArgs.addIndexedArgumentValue(0, 666);
        Colour colour = new Colour();
        colour.setName("蓝色");
        skyArgs.addIndexedArgumentValue(1, colour);
        Bird dayan = new Bird();
        dayan.setName("大雁");
        skyArgs.addIndexedArgumentValue(2, dayan);
        // 对没有指定的参数值，使用构造方法参数注入
        skyBuilder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        registry.registerBeanDefinition(getBeanName(skyClass), skyBeanDefinition);
        System.out.println("===================================手动注册bean完成=================================");
    }

    public static String getBeanName(Class<?> beanClass) {
        return toLowerCamelCase(beanClass.getSimpleName());
    }

    /**
     * 将类名的首字母转换为小写
     * @param className 类名
     * @return 转换后的类名
     */
    public static String toLowerCamelCase(String className) {
        if (className == null || className.isEmpty()) {
            return className; // 如果输入为空，返回空字符串
        }
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
