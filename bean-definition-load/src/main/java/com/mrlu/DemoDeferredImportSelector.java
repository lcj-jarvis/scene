package com.mrlu;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author 简单de快乐
 * @create 2023-08-25 16:02
 */
public class DemoDeferredImportSelector implements DeferredImportSelector {


    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        // 同时也是bean名称
        return new String[]{"com.mrlu.DeferredDemoConfig"};
    }
}
