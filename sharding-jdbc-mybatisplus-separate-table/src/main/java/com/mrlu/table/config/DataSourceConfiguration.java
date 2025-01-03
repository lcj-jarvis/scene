package com.mrlu.table.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.provider.AbstractDataSourceProvider;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Map;
/**
 * 动态数据源配置：
 *
 * 使用{@link com.baomidou.dynamic.datasource.annotation.DS}注解，切换数据源
 *
 * <code>@DS(DataSourceConfiguration.SHARDING_DATA_SOURCE_NAME)</code>
 */
@Configuration
@AutoConfigureBefore({ShardingSphereAutoConfiguration.class, DynamicDataSourceAutoConfiguration.class})
public class DataSourceConfiguration {
    // 分表数据源名称
    private static final String SHARDING_DATA_SOURCE_NAME = "sharding";
    //动态数据源配置项
    @Autowired
    private DynamicDataSourceProperties properties;
    /**
     * shardingjdbc有四种数据源，需要根据业务注入不同的数据源
     *
     * <p>1. 未使用分片, 脱敏的名称(默认): shardingDataSource;
     * <p>2. 主从数据源: masterSlaveDataSource;
     * <p>3. 脱敏数据源：encryptDataSource;
     * <p>4. 影子数据源：shadowDataSource
     */
    //@Lazy
    //@Resource(name = "shardingDataSource")
    //@Autowired
    //AbstractDataSourceAdapter shardingDataSource;

    //@Autowired
    @Resource(name = "shardingSphereDataSource")
    DataSource shardingSphereDataSource;


    @Bean
    public DynamicDataSourceProvider dynamicDataSourceProvider() {
        Map<String, DataSourceProperty> datasourceMap = properties.getDatasource();
        return new AbstractDataSourceProvider() {
            @Override
            public Map<String, DataSource> loadDataSources() {
                Map<String, DataSource> dataSourceMap = createDataSourceMap(datasourceMap);
                // 将 shardingjdbc 管理的数据源也交给动态数据源管理
                dataSourceMap.put(SHARDING_DATA_SOURCE_NAME, shardingSphereDataSource);
                return dataSourceMap;
            }
        };
    }
    /**
     * 将动态数据源设置为首选的
     * 当spring存在多个数据源时, 自动注入的是首选的对象
     * 设置为主要的数据源之后，就可以支持shardingjdbc原生的配置方式了
     */
    @Primary
    @Bean
    public DataSource dataSource() {
        DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource();
        dataSource.setPrimary(properties.getPrimary());
        dataSource.setStrict(properties.getStrict());
        dataSource.setStrategy(properties.getStrategy());

        //// 设置sharding-jdbc为primary，覆盖配置的
        //properties.setPrimary(SHARDING_DATA_SOURCE_NAME);
        //dataSource.setPrimary(SHARDING_DATA_SOURCE_NAME);

        dataSource.setP6spy(properties.getP6spy());
        dataSource.setSeata(properties.getSeata());
        return dataSource;
    }
}