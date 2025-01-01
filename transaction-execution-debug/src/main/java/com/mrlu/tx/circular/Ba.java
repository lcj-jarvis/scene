package com.mrlu.tx.circular;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author 简单de快乐
 * @create 2024-07-17 23:56
 * 用于debug。spring三级缓存解决循环依赖问题
 */
@Service
@Transactional
public class Ba implements BnService{

    @Autowired
    private AnService an;

    @Override
    public void tb() {
        System.out.println("=======Ba=========");
    }
}
