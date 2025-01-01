package com.mrlu.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.entity.UnSeparateData;
import com.mrlu.mapper.UnSeparateDataMapper;
import com.mrlu.service.UnSeparateDataService;
import org.springframework.stereotype.Service;

/**
 * @author 简单de快乐
 * @create 2024-08-16 11:56
 *
 * 该表不参与分表，用于测试是否可以正常crud
 */
@Service
public class UnSeparateDataServiceImpl extends ServiceImpl<UnSeparateDataMapper, UnSeparateData> implements UnSeparateDataService {


}
