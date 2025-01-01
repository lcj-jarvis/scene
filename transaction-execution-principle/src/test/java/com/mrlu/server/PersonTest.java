package com.mrlu.server;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mrlu.server.entity.Person;
import com.mrlu.server.mapper.PersonMapper;
import com.mrlu.server.service.PersonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 简单de快乐
 * @create 2024-04-22 20:28
 */
@SpringBootTest
public class PersonTest {

    @Autowired
    private PersonService personService;

    @Test
    public void debug() {
        personService.testSave();
    }

    @Test
    public void testFirstManual() {
        personService.testFirstManual();
    }

    @Test
    public void testSecondManual() {
        personService.testSecondManual();
    }

    @Test
    public void testThirdManual() {
        personService.testThirdManual();
    }


    @Test
    public void testSave() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 1;
        Person p1 = new Person().setName(name).setAge(age);
        personService.save(p1);
        personService.getById(p1.getId());
        personService.updateById(p1);
        personService.removeById(p1.getId());
    }

    @Test
    public void testSaveBatch() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Person p1 = new Person().setName(name).setAge(1);
        Person p2 = new Person().setName(name).setAge(2);
        List<Person> persons = Arrays.asList(p1, p2);
        personService.saveBatch(persons);
        personService.updateBatchById(persons);
        personService.removeBatchByIds(persons.stream().map(Person::getId).collect(Collectors.toList()));
    }


    @Test
    public void testUpdate() {
        personService.save(new Person().setName("666name").setAge(666));
        personService.update(new Person().setAge(888), new LambdaQueryWrapper<Person>()
                .eq(Person::getName, "666name").eq(Person::getAge, 666));
        personService.remove(new LambdaQueryWrapper<Person>()
                .eq(Person::getName, "666name").eq(Person::getAge, 888));
    }


    @Autowired
    private PersonMapper personMapper;

    @Test
    public void test01() {
        personMapper.insert(new Person().setName("666name").setAge(666));
        personMapper.update(new Person().setAge(888), new LambdaQueryWrapper<Person>()
                .eq(Person::getName, "666name").eq(Person::getAge, 666));
        personMapper.delete(new LambdaQueryWrapper<Person>()
                .eq(Person::getName, "666name").eq(Person::getAge, 888));
        Map<String, Object> columnMap = new HashMap<>();
        columnMap.put("name", "666name");
        columnMap.put("age", "888");
        personMapper.deleteByMap(columnMap);
    }

    @Test
    public void test02() {
        System.out.println(personMapper.customById(200));
    }

}
