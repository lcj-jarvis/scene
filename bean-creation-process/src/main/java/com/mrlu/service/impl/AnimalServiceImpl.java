package com.mrlu.service.impl;

import com.mrlu.entity.*;
import com.mrlu.service.AnimalService;
import com.mrlu.service.DogService;
import com.mrlu.service.PersonService;
import com.mrlu.service.PigService;
import lombok.ToString;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author 简单de快乐
 * @create 2023-10-13 16:40
 *
 * @Autowired注解不能用于静态方法和无参方法，不能用于静态属性。
 *
 * 具体源码位置
 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean(String, RootBeanDefinition, Object[])
 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance(String, RootBeanDefinition, Object[])
 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#populateBean(String, RootBeanDefinition, BeanWrapper)
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor#postProcessProperties(PropertyValues, Object, String)
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.AutowiredFieldElement#inject(Object, String, PropertyValues)
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.AutowiredMethodElement#inject(Object, String, PropertyValues)
 *
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#resolveDependency(DependencyDescriptor, String, Set, TypeConverter)
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#doResolveDependency(DependencyDescriptor, String, Set, TypeConverter)
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#isAutowireCandidate(String, RootBeanDefinition, DependencyDescriptor, AutowireCandidateResolver)
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#findAutowireCandidates(String, Class, DependencyDescriptor)
 * @see org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver#checkQualifier(BeanDefinitionHolder, Annotation, TypeConverter)
 *
 */
@Service
// 设置成懒加载便于调试
//@Lazy
@ToString
public class AnimalServiceImpl implements AnimalService, InitializingBean {

    // 可以这样写，但是不会被注入。因为会忽略静态属性
    @Autowired
    private static Mountain mountain;

    // 可以这样写，但是不会被注入。因为会忽略静态方法
    @Autowired
    public static void setMountain(Mountain mountain) {
        AnimalServiceImpl.mountain = mountain;
    }

    @Autowired(required = false)
    public AnimalServiceImpl(Person person, Parent parent) {
        this.person = person;
        this.parent = parent;
    }

    private Parent parent;
    @Autowired(required = false)
    public AnimalServiceImpl(Person person, House house, Parent parent) {
        this.person = person;
        this.house = house;
        this.parent = parent;
    }

    private  Son son;
    // 宽松模式，获取到匹配类型最近的
    @Autowired(required = false)
    public AnimalServiceImpl(Person person, House house, Son son) {
        this.person = person;
        this.house = house;
        this.son = son;
    }

    @Autowired(required = false)
    public AnimalServiceImpl(Person person, House house, Dog dog) {
        this.person = person;
        this.house = house;
        this.dog = dog;
    }

    //@Autowired(required = false)
    public AnimalServiceImpl() {
        System.out.println("no arg constructor");
    }

    //@Autowired
    //private CatService tomCatServiceImpl;
    //
    //@Autowired
    //private CatService nettyCat;
    //
    //@Autowired
    //private BirdService birdServiceByPriority;
    //
    //@Autowired
    //@Qualifier("secondBird")
    //private BirdService birdServiceByQualifier;
    //
    //@Autowired
    //private DogService dogServiceByPrimary;
    //
    //@Autowired
    //private List<DogService> dogServiceList;
    //
    //@Autowired
    //private Map<String, DogService> dogServiceMap;
    //
    //@Autowired
    //private DogService[] dogServiceArray;
    //
    //@Autowired
    //private Collection<DogService> dogServiceCollection;

    // 注入失败
    //@Autowired
    //private Stream<DogService> dogServiceStream;

    // 构造方法注入
    //（1） 有多个构造方法的时候，如果一个构造方法使用@Autowired注解，而且required属性是true，
    //     则其他构造方法不能@Autowired注解，不管其他构造方法注解的required属性是true还是false
    //（2） 有多个构造方法的时候，如果所有构造方法使用@Autowired注解，而且required属性是false，
    //     这种情况是允许的
    private Person person;
    @Autowired(required = false)
    public AnimalServiceImpl(Person person) {
        this.person = person;
        System.out.println("autowired by constructor(person)");
    }

    private House house;
    // 有多个构造方法的时候，只有一个构造方法可以用@Autowired注解，不管required属性是true还是false
    //@Autowired
    @Autowired(required = false)
    public AnimalServiceImpl(Person person, House house) {
        this.person = person;
        this.house = house;
        System.out.println("autowired by constructor(person,house)");
    }



    private Dog dog;
    // 有多个构造方法的时候，只有一个构造方法可以用@Autowired注解，不管required属性是true还是false
    //@Autowired(required = false)
    public AnimalServiceImpl(Dog dog) {
        this.dog = dog;
    }

    // 使用setter方法注入。有多个类型的bean的时候，如果没有指定优先级，则用方法参数名称和bean名称进行匹配，如果一样，则说明符合条件
    private PigService pigService;
    @Autowired
    public void setPigService(PigService pigService) {
        this.pigService = pigService;
    }

    @Resource(name = "secondDogService")
    private DogService secondDogService;

    @Resource
    private DogService thirdDogServiceByResource;

    @Autowired
    @Lazy
    private Cat cat;

    private Cat cat01;
    private Cat cat02;
    // 方法参数名称，要和bean名称或者bean的别名一致
    @Autowired
    public void setOtherCat(Cat cat01, Cat cat02) {
        this.cat01 = cat01;
        this.cat02 = cat02;
    }

    private Cat cat03;
    @Autowired
    public void setCat03(@Lazy Cat cat03) {
        this.cat03 = cat03;
    }



    // 不能这样套娃，注入过程不会报错，会注入Optional.ofNullable(null)。
    // 调用doubleDogOptional.get方法就会报错：NoSuchElementException
    //@Autowired
    //private Optional<Optional<Dog>> doubleDogOptional;

    @Autowired
    private Optional<Dog> dogOptional;

    @Autowired
    private Optional<Collection<Dog>> optionalDogCollection;

    //@Autowired
    private Optional<List<Dog>> optionalDogList;

    @Autowired
    public void setOptionalDogList(Optional<List<Dog>> optionalDogList) {
        this.optionalDogList = optionalDogList;
    }


    @Autowired
    private Optional<Map<String, DogService>> optionalDogServiceMap;

    @Autowired
    private Optional<DogService[]> optionalDogServiceArray;

    // DependencyObjectProvider
    @Autowired
    private ObjectFactory<Dog> dogFactory;

    // DependencyObjectProvider
    @Autowired
    private ObjectProvider<Dog> dogProvider;

    // ObjectFactory、ObjectProvider的泛型里可以指定常规注入的.以下以ObjectFactory演示，同时调试源码
    @Autowired
    private ObjectFactory<Collection<Dog>> dogCollectionByObjFactory;

    @Autowired
    private ObjectFactory<List<Dog>> dogListByObjFactory;

    @Autowired
    private ObjectFactory<Map<String, DogService>> dogServiceMapByObjFactory;

    // 不能这样套娃
    //@Autowired
    //private ObjectFactory<ObjectFactory<Map<String, DogService>>> doubleObjFactory;

    // 不能这样套娃
    //@Autowired
    //private ObjectFactory<ObjectFactory<PersonService>> doubleObjPersonService;


    @Autowired
    private ObjectFactory<DogService[]> dogServiceArrayByObjFactory;

    @Autowired
    private ObjectFactory<Optional<Dog>> optionalDogByObjFactory;

    @Autowired
    private ObjectFactory<Optional<Collection<Dog>>> optionalDogCollectionByObjFactory;

    @Autowired
    private ObjectFactory<Optional<List<Dog>>> optionalDogListByObjFactory;

    @Autowired
    private ObjectFactory<Optional<Map<String, DogService>>> optionalDogServiceMapByObjFactory;

    @Autowired
    private ObjectFactory<Optional<DogService[]>> optionalDogServiceArrayByObjFactory;

    @Autowired
    private PersonService personService;

    //@Autowired
    //private Stream<List<Dog>> streamDogs;

    //@Autowired
    //private Stream<Dog> streamDogs;


    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("mountain in animalServiceImpl：" + mountain);
        System.out.println("dogFactory：" + dogFactory.getObject());
        System.out.println("dogProvider：" + dogProvider.getObject());

        System.out.println("dogCollectionByObjFactory：" + dogCollectionByObjFactory.getObject());
        System.out.println("dogListByObjFactory：" + dogListByObjFactory.getObject());
        System.out.println("dogServiceMapByObjFactory：" + dogServiceMapByObjFactory.getObject());
        System.out.println("dogServiceArrayByObjFactory：" + Arrays.toString(dogServiceArrayByObjFactory.getObject()));

        System.out.println("optionalDogByObjFactory：" + optionalDogByObjFactory.getObject().get());
        System.out.println("optionalDogCollectionByObjFactory：" + optionalDogCollectionByObjFactory.getObject());
        System.out.println("optionalDogListByObjFactory：" + optionalDogListByObjFactory.getObject());
        System.out.println("optionalDogServiceMapByObjFactory：" + optionalDogServiceMapByObjFactory.getObject());
        System.out.println("optionalDogServiceArrayByObjFactory：" + optionalDogServiceArrayByObjFactory.getObject());

        //System.out.println("doubleObjFactory：" + doubleObjFactory.getObject());
        //System.out.println("doubleObjPersonService：" + doubleObjPersonService.getObject());

        // 报错
        //System.out.println("doubleDogOptional：" + doubleDogOptional.get());

        //System.out.println("streamDogs：" + streamDogs);

        //System.out.println(this);
    }

    //@Inject
    //private Dog dog;
}
