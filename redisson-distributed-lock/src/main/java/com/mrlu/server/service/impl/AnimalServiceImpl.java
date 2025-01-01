package com.mrlu.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean testAddAge(Integer id) {
        Animal animal = getById(id);
        Integer age = animal.getAge();
        animal.setAge(++age);
        return updateById(animal);
    }
}
