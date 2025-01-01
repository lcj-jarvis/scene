package com.mrlu.table.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author 简单de快乐
 * @create 2024-09-05 14:35
 */
@Configuration
@ConfigurationProperties(prefix = "system")
@ToString
@Data
public class SysConfig {
   private String name;
   private String code;
}
