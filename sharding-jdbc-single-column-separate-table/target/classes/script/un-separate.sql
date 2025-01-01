-- 不参与分表的表
CREATE TABLE t_un_separate_data (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            type VARCHAR(255),
                                            name VARCHAR(255),
                                            code VARCHAR(255),
                                            value DOUBLE,
                                            collect_time DATETIME
);
