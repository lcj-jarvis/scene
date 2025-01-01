package com.mrlu.tx;

import com.mrlu.tx.config.Conf;
import com.mrlu.tx.config.SysConf;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author 简单de快乐
 * @create 2024-07-15 16:46
 */
@SpringBootApplication
@EnableTransactionManagement
//@MapperScan("com.mrlu.**.mapper")
@EnableConfigurationProperties(SysConf.class)
public class TcExecAppServer {

    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(TcExecAppServer.class, args);

        SysConf sysConf = application.getBean(SysConf.class);
        System.out.println(sysConf);
        System.out.println(sysConf.getConfiguration());

        //Conf conf = application.getBean(Conf.class);
        //System.out.println(conf);
    }

}
