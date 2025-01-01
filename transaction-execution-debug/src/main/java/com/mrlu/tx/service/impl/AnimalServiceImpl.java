package com.mrlu.tx.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.exception.ServiceException;
import com.mrlu.tx.entity.Animal;
import com.mrlu.tx.exception.NotRunTimeException;
import com.mrlu.tx.mapper.AnimalMapper;
import com.mrlu.tx.service.AnimalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

/**
 * (StudentDemo)表服务实现类
 *
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
@Service
@Slf4j
public class AnimalServiceImpl extends ServiceImpl<AnimalMapper, Animal> implements AnimalService {

    @Autowired
    private AnimalMapper animalMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnimal() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 3;
        Animal p2 = new Animal().setName(name).setAge(age);
        save(p2);
        log.info("animal={}", p2);
        throw new ServiceException("");
    }


    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    // 默认传播行为: PROPAGATION_REQUIRED
    @Autowired
    private TransactionDefinition transactionDefinition;

    @Override
    public void manualSaveAnimal() {
        // 手动开启事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        try {
            saveDemoAnimal();
            // 手动提交事务
            platformTransactionManager.commit(transactionStatus);
        } catch (Exception e) {
            log.error("error;",e);
            // 手动回滚事务
            platformTransactionManager.rollback(transactionStatus);
        }
    }

    /**===================================以下调试事务传播行为=============================================*/
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void testRequired() throws NotRunTimeException {
        saveDemoAnimal();
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRequiredNew() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
    public void testSupported() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void testMandatory() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED, rollbackFor = Exception.class)
    public void testNotSupported() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void testNever() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void testNested() throws NotRunTimeException {
        saveDemoAnimal();
    }

    private void saveDemoAnimal() throws NotRunTimeException {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = new Random().nextInt();
        Animal p2 = new Animal().setName(name).setAge(age);
        save(p2);
        log.info("animal={}", p2);
        // 编译时异常
        // throw new NotRunTimeException("saveDemoAnimal error");
        // 运行时异常
        //throw new ServiceException("saveDemoAnimal error");
    }




}
