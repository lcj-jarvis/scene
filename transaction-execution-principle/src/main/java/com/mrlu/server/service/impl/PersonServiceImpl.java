package com.mrlu.server.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.server.config.Gender;
import com.mrlu.server.entity.Person;
import com.mrlu.server.mapper.PersonMapper;
import com.mrlu.server.service.AnimalService;
import com.mrlu.server.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
@Service
@Slf4j
//@Transactional(rollbackFor = Exception.class)
public class PersonServiceImpl extends ServiceImpl<PersonMapper, Person> implements PersonService {

    @Autowired
    private AnimalService animalService;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean testSave() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 1;
        Person p1 = new Person().setName(name).setAge(age);
        save(p1);
        log.info("P1={}", p1);

        // 直接通过this调用的话，走不到事务增强器的
        System.out.println(this);
        PersonService bean = applicationContext.getBean(PersonService.class);
        System.out.println("PersonService：" + bean);
        // 通过aop工具类判断是否为代理。发现获取到的是代理对象
        log.info("bean is proxy={};", AopUtils.isAopProxy(bean));

        // this 目标对象 直接调用，并不是代理对象进行调用，secondSave是不会触发走事务的aop的。
        // 对于controller来说，通过PersonService的实现类来调用testSave方法，即通过代理对象调用，是会走到事务的aop的
        secondSave();

        // animalService的saveAnimal方法为spring事务动态代理的方法，会走到事务的aop的
        animalService.saveAnimal();
        return Boolean.TRUE;
    }

    @Override
    public Person getPerson() {
        // 方法上没有 @Transactional，没有动态代理，不走事务的aop的
        // 如果方法上没有，但是类上有@Transactional，会给所有的public方法动态代理，走事务的aop
        return getById(5);
    }

    @Transactional(rollbackFor = Exception.class)
    public void secondSave() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 2;
        Person p2 = new Person().setName(name).setAge(age);
        save(p2);
        log.info("P2={}", p2);
        // throw new ServiceException("=============ex=============");
    }
    //========================================================================

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    // 默认传播行为: PROPAGATION_REQUIRED
    @Autowired
    private TransactionDefinition transactionDefinition;


    /**
     * 该方法PersonService使用编程式事务，AnimalService使用声明式事务，
     * 使用发现同一个数据库连接，看源码发现属于一个事务。
     * 默认传播行为: PROPAGATION_REQUIRED，animalService的事务会加入PersonService的事务
     * @return
     */
    @Override
    public Boolean testFirstManual() {
        // 手动开启事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        log.info("person transactionDefinition={}", transactionDefinition);
        log.info("person transactionStatus={}", transactionStatus);
        try {
            String name = UUID.randomUUID().toString().substring(0, 5);
            Integer age = 6;
            Person p2 = new Person().setName(name).setAge(age);
            save(p2);

            // 参考事务AOP的TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);的status = tm.getTransaction(txAttr);
            // 和PersonService使用同一个事务，因为是同一个链接。看源码也知道，当animalService发saveAnimal调用完时，没有提交事务
            animalService.saveAnimal();

            // int i = 1 / 0;

            // 手动提交事务
            platformTransactionManager.commit(transactionStatus);
        } catch (Exception e) {
            log.error("error;",e);
            // 手动回滚事务
            platformTransactionManager.rollback(transactionStatus);
        }
        return true;
    }

    /**
     * 该方法PersonService使用声明事务，AnimalService使用编程式事务
     * 使用发现同一个数据库连接，看源码发现属于一个事务。
     * 默认传播行为: PROPAGATION_REQUIRED，animalService的事务会加入PersonService的事务
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean testSecondManual() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 8;
        Person p2 = new Person().setName(name).setAge(age);
        save(p2);

        animalService.manualSaveAnimal();

        // int i = 1 / 0;

        return true;
    }

    /**
     * 该方法PersonService使用编程式事务，AnimalService使用编程式事务
     * 使用发现同一个数据库连接，看源码发现属于一个事务。
     * 默认传播行为: PROPAGATION_REQUIRED，animalService的事务会加入PersonService的事务
     * @return
     */
    @Override
    public Boolean testThirdManual() {
        // 手动开启事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        try {
            String name = UUID.randomUUID().toString().substring(0, 5);
            Integer age = 9;
            Person p2 = new Person().setName(name).setAge(age);
            save(p2);

            animalService.manualSaveAnimal();

             int i = 1 / 0;

            // 手动提交事务
            platformTransactionManager.commit(transactionStatus);
        } catch (Exception e) {
            log.error("error;",e);
            // 手动回滚事务
            platformTransactionManager.rollback(transactionStatus);
        }
        return true;
    }


    @Override
    public Boolean testOtherAspect() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 99;
        Person p2 = new Person().setName("other-" + name).setAge(age);
        save(p2);

        int i = 1/0;

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean threadRollBackFailed01(int i, String name) throws InterruptedException {
        Person school = new Person().setName(UUID.randomUUID().toString());
        if (name != null) {
            school.setName(name);
        }
        save(school);
        if (i == 3) {
            Thread.sleep(1000);
            int a = 1/0;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean single(int i, String name) throws InterruptedException {
        Person school = new Person().setName(UUID.randomUUID().toString()).setSex(Gender.MALE);
        //Person school = new Person();
        if (name != null) {
            school.setName(name);
        }
        save(school);
        if (i == 3) {
            Thread.sleep(1000);
            int a = 1/0;
        }
        return true;
    }

}
