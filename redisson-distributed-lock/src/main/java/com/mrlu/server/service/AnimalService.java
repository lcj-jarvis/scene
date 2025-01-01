package com.mrlu.server.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.mrlu.server.entity.Animal;

/**
 * (StudentDemo)表服务接口
 *
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
public interface AnimalService extends IService<Animal> {

    Boolean testAddAge(Integer id);
}
