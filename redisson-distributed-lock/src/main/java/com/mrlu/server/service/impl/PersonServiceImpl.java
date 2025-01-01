package com.mrlu.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.server.entity.Person;
import com.mrlu.server.mapper.PersonMapper;
import com.mrlu.server.service.AnimalService;
import com.mrlu.server.service.PersonService;
import lombok.extern.slf4j.Slf4j;
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
public class PersonServiceImpl extends ServiceImpl<PersonMapper, Person> implements PersonService {

    @Autowired
    private AnimalService animalService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean testAddAge(Integer id) {
        Person person = getById(id);
        Integer age = person.getAge();
        person.setAge(++age);
        updateById(person);
        //int i = 1/0;
        return animalService.testAddAge(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean testLockByIdAndName(Person person) {
        Person target = getById(person.getId());
        Integer age = target.getAge();
        //int i = 1/0;
        target.setAge(++age);
        return updateById(target);
    }
}
