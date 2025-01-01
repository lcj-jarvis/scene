package com.mrlu.server.injector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.mrlu.server.method.InsertAll;
import com.mrlu.server.method.InsertAllBatch;
import com.mrlu.server.method.SoftDeleteAll;

import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-04-17 15:53
 */
public class CustomBaseSqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        //1、MybatisPlus使用的DefaultSqlInjector加载BaseMapper中对应的sql。所以我们要保留原来的
        List<AbstractMethod> methodList = super.getMethodList(mapperClass, tableInfo);
        //2、加入我们自己自定义的AbstractMethod的
        methodList.add(new InsertAll());
        methodList.add(new SoftDeleteAll());
        methodList.add(new InsertAllBatch());
        return methodList;
    }
}
