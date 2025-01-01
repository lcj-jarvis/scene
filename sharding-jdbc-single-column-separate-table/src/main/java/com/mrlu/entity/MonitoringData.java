package com.mrlu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mrlu.config.CustomIdGenerator;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@TableName("t_ns_common_monitoring_data")
@ToString
public class MonitoringData {
    /**
       【注意】
        1、对于MyBatisPlus，如果实体中有名为id字段，且数据类型为数值类型，即使没有使用@TableId注解，也会用这个字段作为主键。
           同时默认id生成的类型为IdType.ASSIGN_ID，即使用DefaultIdentifierGenerator（雪花算法）生成id的值。

         对于这种情况，即使配置了ShardingSphere的以下内容，也不会使用ShardingSphere的雪花算法（SnowflakeKeyGenerateAlgorithm类）来生成id列的值。
            key-generate-strategy:
              # 指定生成的列为id
              column: id
              # 指定id生成的算法名称
              key-generator-name: id_key_generator

         为什么呢？？？
         因为实体中有id字段，MyBatisPlus最终生成的sql是
         INSERT INTO t_ns_common_monitoring_data  (id,buiness_type,equipment_no,last_collect_time,value ) VALUES  ( ?,?,?,? )
         插入的列中有名为id的列，包含了 key-generate-strategy:column配置项指定的值，就不会使用ShardingSphere的雪花算法来生成。
         也很好理解：
         因为如果插入的sql有id列，ShardingSphere就认为你在外部指定了id的取值（开发者觉得自己指定的值也是全局唯一的），也就不需要使用ShardingSphere的来生成

        2、如果我们需要使用ShardingSphere的雪花算法的来生成，应该要怎么办呢？？
         (1) 修改对应的配置
             key-generate-strategy:
                # 指定生成的列为data_id
                column: data_id
                # 指定id生成的算法名称
                key-generator-name: id_key_generator
         （2）修改表的ddl，改用data_id 为主键名称
         （3）实体类中移除名为id字段，加上long类型的dataId字段
          // TODO: 2024/11/18  缺点：worker-id的配置没有生效

        建议：最好使用MyBatisPlus自定义分布式id的生成，自定义分布式id使用雪花算法，
            这样主键名称沿用id也没关系，也可以根据需要设置workerId。
        @see CustomIdGenerator
     */
    //private Long id;

    private Long dataId;

    private Long oldDataId;  // 迁移的旧数据ID
    private String buinessType;  // 业务类型
    private String equipmentNo;  // 设备编码
    private String equipmentName;  // 设备名称
    private String equipmentStatus;  // 设备状态
    private String equipmentTypeCode;  // 设备类型编码
    private String monitorUnitCode;  // 监测单位编码
    private String monitorUnitName;  // 监测单位
    private String monitorType;  // 监测项类型
    private String monitorItemCode;  // 监测项编码
    private String monitorItemName;  // 监测项名称
    private String monitorDetail;  // 监测明细
    private Date lastCollectTime;  // 采集时间
    private Double value;  // 监测值
    private String valueUnit;  // 监测值单位
    private Double maxValue;  // 最小监测值
    private Double minValue;  // 最大检测值
    private String longitude;  // 经度
    private String latitude;  // 纬度
    private String remark;  // 详情
    private String address;  // 详细地址
    private Double valueBak1;  // 监测值备用列
    private String columnBak1;  // 备用列一
    private String columnBak2;  // 备用列二
    private Date sysCreateTime;  // 创建时间
    private Date sysUpdateTime;  // 修改时间
}
