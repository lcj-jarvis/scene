package com.mrlu.service.impl;

import com.mrlu.service.BirdService;
import org.springframework.stereotype.Service;

import javax.annotation.Priority;

/**
 * @author 简单de快乐
 * @create 2023-10-13 17:01
 */
@Service("firstBird")
@Priority(1)
public class FirstBirdServiceImpl implements BirdService {
}
