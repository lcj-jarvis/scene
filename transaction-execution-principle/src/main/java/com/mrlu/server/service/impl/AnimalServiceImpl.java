package com.mrlu.server.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.exception.ServiceException;
import com.mrlu.server.entity.Animal;
import com.mrlu.server.mapper.AnimalMapper;
import com.mrlu.server.service.AnimalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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

    public AnimalServiceImpl() {
        System.out.println("a");
    }

    @Override
    public Animal aCommon() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 3;
        Animal p2 = new Animal().setName(name).setAge(age);
        save(p2);
        log.info("animal={}", p2);
        return p2;
    }

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

    @Override
    //@Transactional(rollbackFor = Exception.class)
    public void save4Tc(int i) {
        Animal animal = new Animal();
        animal.setName(UUID.randomUUID().toString());
        animalMapper.insert(animal);
        if (i == 3) {
            int a = 1/0;
        }
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
            String name = UUID.randomUUID().toString().substring(0, 5);
            Integer age = 8;
            Animal p2 = new Animal().setName(name).setAge(age);
            save(p2);
            log.info("animal={}", p2);

            // 手动提交事务
            platformTransactionManager.commit(transactionStatus);
        } catch (Exception e) {
            log.error("error;",e);
            // 手动回滚事务
            platformTransactionManager.rollback(transactionStatus);
        }
    }

    @Override
    public void threadRollBackFailed02(int i) {
        Animal hospital = new Animal();
        hospital.setName("测试" + UUID.randomUUID());
        save(hospital);
        if (i == 3) {
            int a = 1/0;
        }
    }

    @Autowired
    private AnimalMapper animalMapper;



}
