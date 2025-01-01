package com.mrlu.tx.circular;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author 简单de快乐
 * @create 2024-07-17 23:55
 *
 * 用于debug。spring三级缓存解决循环依赖问题
 */
@Transactional
@Service
public class An implements AnService{

    @Autowired
    private BnService ba;


    @Override
    public void ta() {
        System.out.println("=======An=========");
        ba.tb();
    }

}
