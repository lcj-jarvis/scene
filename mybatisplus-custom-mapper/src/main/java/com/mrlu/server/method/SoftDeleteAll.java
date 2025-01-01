package com.mrlu.server.method;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.mrlu.server.utils.TransformUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.lang.reflect.Field;

/**
 * @author 简单de快乐
 * @create 2024-04-17 15:59
 *
 * 自定义软删除全表的方法
 */
@Slf4j
public class SoftDeleteAll extends AbstractMethod {

    public static final String METHOD_NAME = "softDeleteAll";

    public SoftDeleteAll() {
        super(METHOD_NAME);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        /* 执行 SQL ，动态 SQL 参考类 SqlMethod */
        DeleteItem deleteItem = buildDeleteItem(modelClass);
        String sql = "update " + tableInfo.getTableName() + " set " + deleteItem.getColumnName() + " = " + deleteItem.getDelVal();
        /* mapper 接口方法名一致 */
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sql, modelClass);
        log.info("softDeleteAll sql={}", sql);
        return this.addDeleteMappedStatement(mapperClass, methodName, sqlSource);
    }


    private DeleteItem buildDeleteItem(Class<?> modelClass) {
        Field[] fields = modelClass.getDeclaredFields();
        Field deletedField = null;
        TableLogic deletedFieldAnno = null;
        for (Field field : fields) {
            TableLogic  annotation = field.getAnnotation(TableLogic.class);
            if (annotation != null) {
                deletedField = field;
                deletedFieldAnno = annotation;
                break;
            }
        }

        DeleteItem deleteItem = new DeleteItem();
        deleteItem.setDelVal(deletedFieldAnno.delval());
        deleteItem.setFieldName(deletedField.getName());
        deleteItem.setColumnName(TransformUtil.humpToUnderLine(deletedField.getName()));
        return deleteItem;
    }

    @Data
    public class DeleteItem {
        private String fieldName;
        private String columnName;
        private String delVal;
    }
}
