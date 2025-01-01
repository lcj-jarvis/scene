package com.mrlu.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.mrlu.server.entity.StudentDemo;
import com.mrlu.server.mapper.StudentDemoMapper;
import com.mrlu.server.service.StudentDemoService;
import org.springframework.stereotype.Service;

/**
 * (StudentDemo)表服务实现类
 *
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
@Service
public class StudentDemoServiceImpl extends ServiceImpl<StudentDemoMapper, StudentDemo> implements StudentDemoService {

}
