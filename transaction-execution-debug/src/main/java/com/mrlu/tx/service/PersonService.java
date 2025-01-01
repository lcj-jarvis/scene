package com.mrlu.tx.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.mrlu.tx.entity.Person;
import com.mrlu.tx.exception.NotRunTimeException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * (StudentDemo)表服务接口
 *
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
public interface PersonService extends IService<Person> {

    void canGettedBeanUseTc();

    void savePerson();

    Boolean testSaveByDeclaredTcAndManualTc();

    Boolean testFirstManual();

    Boolean testSecondManual();

    Boolean testThirdManual();

    Boolean testFourthManual();

    void testFifthManual();


    void testRequired() throws NotRunTimeException;

    void testRequiredNew() throws NotRunTimeException;

    void testSupported() throws NotRunTimeException;

    void testMandatory() throws NotRunTimeException;

    void testNotSupported() throws NotRunTimeException;

    void testNever() throws NotRunTimeException;

    void testNested() throws NotRunTimeException;

    int addDemoPerson();

    void testRollBackRule() throws NotRunTimeException, Exception;


    void testGlobalRollback();


    void testTc();


    void testSyncConnection() throws NotRunTimeException;

}
