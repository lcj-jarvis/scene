package com.mrlu.core.anno;

import com.mrlu.core.config.AspectJLogManagementConfiguration;
import com.mrlu.core.config.ProxyLogManagementConfiguration;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;

/**
 * @author 简单de快乐
 * @create 2024-07-03 15:09
 */
public class LogConfigurationSelector extends AdviceModeImportSelector<EnableLogManagement>{
    @Override
    protected String[] selectImports(AdviceMode adviceMode) {
        // 根据代理模式，加载配置类
        switch (adviceMode) {
            case PROXY:
                return new String[] {AutoProxyRegistrar.class.getName(),
                        ProxyLogManagementConfiguration.class.getName()};
            case ASPECTJ:
                return new String[] {AspectJLogManagementConfiguration.class.getName()};
            default:
                return null;
        }
    }
}
