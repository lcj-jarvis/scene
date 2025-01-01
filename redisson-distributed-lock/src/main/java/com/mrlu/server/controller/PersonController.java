package com.mrlu.server.controller;


import com.mrlu.lock.anno.DistributedLock;
import com.mrlu.response.CommonResults;
import com.mrlu.server.entity.Person;
import com.mrlu.server.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/person")
public class PersonController {

    /**
     * 服务对象
     */
    @Autowired
    private PersonService personService;

    @GetMapping("/add-age")
    @DistributedLock(key = "#id", keyPrefix = "person-animal-add-age", lockTime = 60)
    public CommonResults<Boolean> testAddAge(Integer id) {
        return CommonResults.ok(personService.testAddAge(id));
    }

    @PostMapping("/person/add-age")
    @DistributedLock(key = "#person.name + '-' + #person.id", keyPrefix = "person-add-age", lockTime = 60)
    public CommonResults<Boolean> testLockByIdAndName(Person person) {
        return CommonResults.ok(personService.testLockByIdAndName(person));
    }



}

