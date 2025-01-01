package com.mrlu.server.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.mrlu.server.entity.Person;
import org.springframework.transaction.annotation.Transactional;

/**
 * (StudentDemo)表服务接口
 *
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
public interface PersonService extends IService<Person> {

    Boolean testSave();


    Person getPerson();

    Boolean testFirstManual();

    Boolean testSecondManual();

    Boolean testThirdManual();

    Boolean testOtherAspect();

    Boolean threadRollBackFailed01(int i, String name) throws InterruptedException;

    Boolean single(int i, String name) throws InterruptedException;
}
