package com.mrlu.server;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * @author 简单de快乐
 * @create 2024-04-20 1:51
 */
public class TempTest {

    @Test
    public void t() throws NoSuchMethodException {
        Method method = Child.class.getMethod("test");
        AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
                method, A.class, false, false);
        System.out.println(attributes);
        // 有
        A annotation01 = AnnotatedElementUtils.findMergedAnnotation(method, A.class);
        // 无
        A annotation02 = AnnotatedElementUtils.getMergedAnnotation(method, A.class);
        System.out.println(annotation01);
    }

}
