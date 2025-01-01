package com.mrlu.server.controller;


import com.mrlu.response.CommonResults;
import com.mrlu.server.aop.AspectAnno;
import com.mrlu.server.entity.Person;
import com.mrlu.server.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/test")
    public CommonResults<Boolean> testSave() {
        Boolean save = this.personService.testSave();
        // service调用完成，controller执行报错的情况
        // service中的事务aop已经将事务提交了，这时候controller报错也不会影响事务的提交结果
        // int a = 1 % 0;
        return CommonResults.ok(save);
    }

    @GetMapping("/info")
    public CommonResults<Person> getPerson() {
        return CommonResults.ok(personService.getPerson());
    }

    @GetMapping("/first-manual")
    public CommonResults<Boolean> testFirstManual() {
        Boolean save = this.personService.testFirstManual();
        return CommonResults.ok(save);
    }

    @GetMapping("/second-manual")
    public CommonResults<Boolean> testSecondManual() {
        Boolean save = this.personService.testSecondManual();
        return CommonResults.ok(save);
    }

    @GetMapping("/third-manual")
    public CommonResults<Boolean> testThirdManual() {
        Boolean save = this.personService.testThirdManual();
        return CommonResults.ok(save);
    }

    @GetMapping("/other-aspect")
    @Transactional(rollbackFor = Exception.class)
    @AspectAnno
    public CommonResults<Boolean> testOtherAspect() {
        Boolean save = this.personService.testOtherAspect();
        return CommonResults.ok(save);
    }

}

