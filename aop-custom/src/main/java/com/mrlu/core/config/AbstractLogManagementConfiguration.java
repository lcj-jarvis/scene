package com.mrlu.core.config;

import com.mrlu.core.anno.EnableLogManagement;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * @author 简单de快乐
 * @create 2024-07-03 15:18
 */
@Configuration
public abstract class AbstractLogManagementConfiguration implements ImportAware {

    @Nullable
    protected AnnotationAttributes enableLog;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        enableLog = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(EnableLogManagement.class.getName(), false));
        if (enableLog == null) {
            throw new IllegalArgumentException("@EnableLogManagement is not present on importing class " + importMetadata.getClassName());
        }
    }
}
