package com.mrlu.qualifier.impl;

import com.mrlu.qualifier.GrapeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author 简单de快乐
 * @create 2023-10-18 18:07
 */
@Service
@Qualifier("gs2")
public class SecondGrapeServiceImpl implements GrapeService {

}
