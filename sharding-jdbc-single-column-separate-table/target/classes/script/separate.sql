CREATE TABLE t_ns_common_monitoring_data_1 (
                                               data_id                    BIGINT PRIMARY KEY,
                                               old_data_id           BIGINT,                -- 迁移的旧数据id，用于数据校验
                                               buiness_type          VARCHAR(100),   -- 业务类型，如积水、地灾、燃气、危化、人口热力图等
                                               equipment_no          VARCHAR(255) NOT NULL, -- 设备编码或表示监测的唯一编码
                                               equipment_name        VARCHAR(255) NOT NULL DEFAULT '',  -- 设备名称
                                               equipment_status      VARCHAR(100) NOT NULL DEFAULT '',  -- 设备状态
                                               equipment_type_code   VARCHAR(100) NOT NULL DEFAULT '',  -- 设备类型编码（同一业务类型下可能有多种设备类型）
                                               monitor_unit_code     VARCHAR(255) NOT NULL DEFAULT '',  -- 监测单位编码
                                               monitor_unit_name     VARCHAR(1000) NOT NULL DEFAULT '', -- 监测单位
                                               monitor_type          VARCHAR(100) NOT NULL DEFAULT '',  -- 监测项类型（同一设备类型下的设备可能有多个监测项）
                                               monitor_item_code     VARCHAR(255) NOT NULL DEFAULT '',  -- 监测项编码
                                               monitor_item_name     VARCHAR(255) NOT NULL DEFAULT '',  -- 监测项名称
                                               monitor_detail        VARCHAR(255) NOT NULL DEFAULT '',  -- 监测明细
                                               last_collect_time     DATETIME NOT NULL,                -- 采集时间
                                               value                 DOUBLE,                            -- 监测值
                                               value_unit            VARCHAR(100),                      -- 监测值单位
                                               max_value             DOUBLE,                            -- 最小监测值
                                               min_value             DOUBLE,                            -- 最大检测值
                                               longitude             VARCHAR(100),                      -- 经度
                                               latitude              VARCHAR(100),                      -- 纬度
                                               remark                VARCHAR(1000) NOT NULL DEFAULT '', -- 详情
                                               address               VARCHAR(1000) NOT NULL DEFAULT '', -- 详细地址
                                               value_bak1            DOUBLE,                            -- 监测值备用列
                                               column_bak1           VARCHAR(1000) NOT NULL DEFAULT '', -- 备用列一
                                               column_bak2           VARCHAR(1000) NOT NULL DEFAULT '', -- 备用列二
                                               sys_create_time       DATETIME NOT NULL DEFAULT NOW(),   -- 创建时间
                                               sys_update_time       DATETIME NOT NULL DEFAULT NOW()    -- 修改时间
);


CREATE TABLE t_ns_common_monitoring_data_2 LIKE t_ns_common_monitoring_data_1;
CREATE TABLE t_ns_common_monitoring_data_3 LIKE t_ns_common_monitoring_data_1;
CREATE TABLE t_ns_common_monitoring_data_4 LIKE t_ns_common_monitoring_data_1;
CREATE TABLE t_ns_common_monitoring_data_5 LIKE t_ns_common_monitoring_data_1;
CREATE TABLE t_ns_common_monitoring_data_6 LIKE t_ns_common_monitoring_data_1;
CREATE TABLE t_ns_common_monitoring_data_7 LIKE t_ns_common_monitoring_data_1;
CREATE TABLE t_ns_common_monitoring_data_8 LIKE t_ns_common_monitoring_data_1;
CREATE TABLE t_ns_common_monitoring_data_9 LIKE t_ns_common_monitoring_data_1;
CREATE TABLE t_ns_common_monitoring_data_10 LIKE t_ns_common_monitoring_data_1;