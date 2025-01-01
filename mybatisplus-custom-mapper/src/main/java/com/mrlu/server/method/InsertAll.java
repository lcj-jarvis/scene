package com.mrlu.server.method;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * @author 简单de快乐
 * @create 2024-04-17 16:19
 */
@Slf4j
public class InsertAll extends AbstractMethod {

    public static final String METHOD_NAME = "insertAll";

    public InsertAll() {
        super(METHOD_NAME);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        String sql = "insert into %s %s values %s";
        StringBuilder fieldSql = new StringBuilder();
        // 插入的列
        fieldSql.append(tableInfo.getKeyColumn()).append(",");
        StringBuilder valueSql = new StringBuilder();
        valueSql.append("#{").append(tableInfo.getKeyProperty()).append("},");
        tableInfo.getFieldList().forEach(x->{
            fieldSql.append(x.getColumn()).append(",");
            valueSql.append("#{").append(x.getProperty()).append("},");
        });
        // 去掉多余的,
        fieldSql.delete(fieldSql.length()-1, fieldSql.length());
        fieldSql.insert(0, "(");
        fieldSql.append(")");

        valueSql.insert(0, "(");
        // 去掉多余的,
        valueSql.delete(valueSql.length()-1, valueSql.length());
        valueSql.append(")");

        String filledSql = String.format(sql, tableInfo.getTableName(), fieldSql, valueSql);
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, filledSql, modelClass);
        log.info("InsertAll fieldSql={},valueSql={},sql={},filledSql={}", fieldSql, valueSql, sql, filledSql);
        return this.addInsertMappedStatement(mapperClass, modelClass, methodName, sqlSource, new NoKeyGenerator(), null, null);
    }

}
