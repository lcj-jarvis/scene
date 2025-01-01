package com.mrlu.tx.entity;



import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PostConstruct;
import java.io.Serializable;


@Data
@Accessors(chain = true)
@TableName(value = "t_animal", autoResultMap = true)
@ToString
public class Animal implements Serializable, InitializingBean {
    private static final long serialVersionUID = -75941339471069023L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private Integer age;

    private void customizeInit() {
        System.out.println("==>customizeInit");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("==>afterPropertiesSet");
    }

    @PostConstruct
    public void init() {
        System.out.println("==>PostConstruct init");
    }

    private void customizeInitUseInXml() {
        System.out.println("==>customizeInitUseInXml");
    }

}

