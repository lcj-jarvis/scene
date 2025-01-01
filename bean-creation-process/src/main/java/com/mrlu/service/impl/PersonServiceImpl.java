package com.mrlu.service.impl;

import com.mrlu.service.AnimalService;
import com.mrlu.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 简单de快乐
 * @create 2023-10-13 16:36
 */
@Service
public class PersonServiceImpl implements PersonService {

    @Autowired
    private AnimalService animalService;

}
