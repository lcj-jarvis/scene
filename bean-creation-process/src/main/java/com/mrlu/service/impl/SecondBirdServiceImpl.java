package com.mrlu.service.impl;

import com.mrlu.service.BirdService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Priority;

/**
 * @author 简单de快乐
 * @create 2023-10-13 17:02
 */
@Service("secondBird")
@Priority(2)
public class SecondBirdServiceImpl implements BirdService {

}
