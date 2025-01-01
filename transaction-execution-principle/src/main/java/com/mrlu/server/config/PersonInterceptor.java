package com.mrlu.server.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/**
 * @author 简单de快乐
 * @create 2024-04-22 20:25
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class PersonInterceptor implements Interceptor {

    /**
     * 批量操作的话，内部循环执行，实际也是单个entity
     * @param invocation
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        if (sqlCommandType == SqlCommandType.INSERT) {
            // 只拦截有这个注解的
            InterceptEntity annotation = AnnotationUtils.findAnnotation(parameter.getClass(), InterceptEntity.class);
            if (annotation != null) {
                log.info("拦截到插入操作");
                // 处理插入操作
                log.info("insert entity={}", parameter);
            }
        } else if (sqlCommandType == SqlCommandType.UPDATE) {
            //MapperMethod.ParamMap 中的 et 和 ew 通常表示的是 MyBatis Mapper 方法中的参数对象。这两个参数通常在使用
            //MyBatis-Plus 进行数据库操作时会出现，用于构建查询条件和更新/删除条件，如LambdaQueryWrapper。
            //（1）et 一般表示实体对象（Entity），在插入和更新操作中用于传递待操作的实体对象。
            //（2）ew 则表示实体对象的更新条件（EntityWrapper），在更新和删除操作中用于指定更新或删除的条件。
            //这两个参数在 MyBatis 中并非固定的名称，可以根据实际情况而变化，如 ew 有时也可能表示条件构造器（Wrapper）。
            log.info("拦截到更新操作");
            MapperMethod.ParamMap updatedMap = (MapperMethod.ParamMap) parameter;
            Object entity = updatedMap.get("et");
            Object updateCondition = null;
            if (updatedMap.containsKey("ew")) {
                 updateCondition = updatedMap.get("ew");
            }
            // 处理更新操作
            log.info("update entity={};updateCondition={}", entity, updateCondition);
        } else if (sqlCommandType == SqlCommandType.DELETE) {
            log.info("拦截到删除操作");
            if (parameter instanceof  MapperMethod.ParamMap) {
                // MapperMethod.ParamMap 中的 et 和 ew 通常表示的是 MyBatis Mapper 方法中的参数对象。这两个参数通常在使用
                // MyBatis-Plus 进行数据库操作时会出现，用于构建查询条件和更新/删除条件，如LambdaQueryWrapper。
                //（1）et 一般表示实体对象（Entity），在插入和更新操作中用于传递待操作的实体对象。
                //（2）ew 则表示实体对象的更新条件（EntityWrapper），在更新和删除操作中用于指定更新或删除的条件。
                //这两个参数在 MyBatis 中并非固定的名称，可以根据实际情况而变化，如 ew 有时也可能表示条件构造器（Wrapper）
                MapperMethod.ParamMap deletedMap = (MapperMethod.ParamMap) parameter;
                Object deleteCondition = null;
                if (deletedMap.containsKey("ew")) {
                    deleteCondition = deletedMap.get("ew");
                }
                log.info("delete deleteCondition={}", deleteCondition);
            } else {
                // 处理删除操作
                // 如果 SQL 语句使用的参数是一个简单类型（如基本数据类型、String 等），
                // 那么 getParameterObject() 就返回这个简单类型的值。如果 SQL 语句使用的参数是一个对象，
                // 那么 getParameterObject() 就返回这个对象。
                //（1）delete FROM person WHERE id = #{userId} parameter是基本数据类型的值
                log.info("delete entity={}", parameter);
            }

        }
        return invocation.proceed();
    }
}
