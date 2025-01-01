package com.mrlu.server.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2024-04-11 22:10
 *
 * 不直接注入的话，使用以下方式也可以
 */
@Configuration
public class MPConfig implements InitializingBean {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Override
    public void afterPropertiesSet() throws Exception {
        //// 自定义拦截
        //sqlSessionFactory.getConfiguration().addInterceptor(new CustomInterceptor());
        //// 自定义参数解析器
        //sqlSessionFactory.getConfiguration().getTypeHandlerRegistry().register(new GenderTypeHandler());
    }
}
