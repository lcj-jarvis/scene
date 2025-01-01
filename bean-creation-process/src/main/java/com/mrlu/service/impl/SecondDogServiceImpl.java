package com.mrlu.service.impl;

import com.mrlu.service.DogService;
import org.springframework.stereotype.Service;

/**
 * @author 简单de快乐
 * @create 2023-10-13 16:37
 */
@Service("secondDogService")
// FirstDogServiceImpl已经设置了@Primary，SecondDogServiceImpl再设置的话就报错
//@Primary
public class SecondDogServiceImpl implements DogService {


}
