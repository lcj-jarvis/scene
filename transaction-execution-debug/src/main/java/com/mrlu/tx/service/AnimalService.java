package com.mrlu.tx.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.mrlu.tx.entity.Animal;
import com.mrlu.tx.exception.NotRunTimeException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * (StudentDemo)表服务接口
 *
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
public interface AnimalService extends IService<Animal> {

    void saveAnimal();

    void manualSaveAnimal();


    void testRequired() throws NotRunTimeException;

    void testRequiredNew() throws NotRunTimeException;

    void testSupported() throws NotRunTimeException;

    void testMandatory() throws NotRunTimeException;

    void testNotSupported() throws NotRunTimeException;

    void testNever() throws NotRunTimeException;

    void testNested() throws NotRunTimeException;


}
