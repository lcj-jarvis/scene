## springboot + aop + Lua分布式限流的最佳实践

### 1、引言

![image-20230526151849178](D:\scene\images\image-20230526151849178.png)

参考文章：

https://mp.weixin.qq.com/s/Y4McReti8DDBSeG0XgHX8g

### 2、实践

#### 2.1 编写Lua脚本

```lua
-- 获取调用脚本时传入的第一个key值（用作限流的key）
local key = KEYS[1]
-- 获取调用脚本时传入的第一个参数值（用作限流的大小）
local limit = tonumber(ARGV[1])
-- 获取当前流量的大小
local currentLimit = tonumber(redis.call('get', key) or '0')
if currentLimit > limit then
	-- 返回拒绝
	return currentLimit
end
-- 访问次数加一
currentLimit = redis.call('INCRBY', key, 1)
if currentLimit == 1 then
	-- 第一次访问，对限流的key设置过期时间
	redis.call('EXPIRE', key, ARGV[2])
end
return currentLimit
```

说明：

- 通过`KEYS[1]` 获取传入的key参数
- 通过`ARGV[1]`获取传入的`limit`参数
- 通过`ARGV[1]`获取传入的 限流时间范围
- `redis.call`方法，从缓存中`get`和`key`相关的值，如果为`null`那么就返回0
- 接着判断缓存中记录的数值是否会大于限制大小，如果超出表示该被限流，返回当前访问次数
- 如果未超过，那么该key的缓存值+1，并设置过期时间，返回当前访问次数

#### 2.2 新建工程引入依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.5.6</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>com.mrlu</groupId>
    <artifactId>redis-lua-aop-current-limiting</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>1.8</java.version>
        <mysql.version>5.1.43</mysql.version>
        <commons-pool2.version>2.11.1</commons-pool2.version>
        <mybatis-plus-boot-starter.version>3.5.3.1</mybatis-plus-boot-starter.version>
        <druid-spring-boot-starter.version>1.2.16</druid-spring-boot-starter.version>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
    </properties>

    <dependencies>
        <!--mvc-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Mybatis-plus-boot-starter -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus-boot-starter.version}</version>
        </dependency>

        <!--数据库-->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-starter</artifactId>
            <version>${druid-spring-boot-starter.version}</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>

        <!--redis-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
            <version>${commons-pool2.version}</version>
        </dependency>

        <!--aop依赖-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!--工具包-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>21.0</version>
        </dependency>

        <!--单元测试-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.mrlu</groupId>
            <artifactId>scene-common</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

#### 2.3 编写yaml

```yaml
server:
  port: 8080
  servlet:
    context-path: /limit-server/

#数据库配置
spring:
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://192.168.15.104:3306/test
    username: root
    password: root

  # redis配置
  redis:
    host: 192.168.15.104
    port: 6379
    database: 0
    timeout: 10000ms
    lettuce:
      pool:
        max-active: 50
        max-wait: 50
        max-idle: 50
        min-idle: 0

#mybatisplus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

#### 2.4 编写配置类

```java
package com.mrlu.limit.config;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author 简单de快乐
 * @version 1.0
 * @email 1802772962@qq.com
 * @createDate 2021-03-25 13:48
 *
 * 自定义一个RedisTemplate
 */
@Configuration
public class RedisConfig {

    /**
     * 固定好的模板，可以直接使用
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        //我们为了自己开发方便，一般直接使用<String,Object>
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        //配置具体的序列化方式
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        //key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        //hash的key采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        //value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        //hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.setConnectionFactory(redisConnectionFactory);
        template.afterPropertiesSet();
        return template;
    }
}
```

2.5 编写启动类

```java
@SpringBootApplication
public class CurrentLimitingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CurrentLimitingApplication.class, args);
    }
}
```

#### 2.6 自定义限流注解和枚举

* 枚举

```java
public enum LimitType {

    /**
     * 自定义的可以
     */
    CUSTOMER,

    /**
     * 请求IP
     */
    IP

}
```

* 注解

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Limit {

    /**
     * 名字
     */
    String name() default "";

    /**
     * key
     */
    String key() default "";

    /**
     * key的前缀
     * @return
     */
    String prefix() default "";

    /**
     * 给定的时间范围 单位(秒)
     * @return
     */
    int period();

    /**
     * 一定时间内最多访问次数
    */
    int count();

    /**
     * 限流的类型（用户自定义key或者ip）
     * @return
     */
    LimitType limitType() default LimitType.CUSTOMER;

}
```

#### 2.7 编写注解切面实现限流

```java
package com.mrlu.limit.aspect;

import com.mrlu.limit.anno.Limit;
import com.mrlu.limit.constant.LimitType;
import com.mrlu.response.CommonResults;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collections;

/**
 * @author 简单de快乐
 * @create 2023-05-26 15:41
 */
@Aspect
@Component
public class LimitAspect {
    private static final Logger logger = LoggerFactory.getLogger(LimitAspect.class);

    private static final String UNKNOWN = "unknown";

    @Autowired
    private RedisTemplate redisTemplate;

    @Around("execution(public * *(..)) && @annotation(com.mrlu.limit.anno.Limit)")
    public Object doAround(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 获取注解相应参数
        Limit annotation = method.getAnnotation(Limit.class);

        String name = annotation.name();
        int limitCount = annotation.count();
        int period = annotation.period();
        LimitType limitType = annotation.limitType();
        String prefix = annotation.prefix();

        String key;
        switch (limitType) {
            case IP:
                key = getIpAddress();
                break;
            case CUSTOMER:
                key = annotation.key();
                break;
            default:
                key = annotation.key();
                if (StringUtils.isEmpty(key)) {
                    // 获取用方法名做限流的key
                    key = StringUtils.upperCase(method.getName());
                }
        }

        String luaScript = getLimitLuaScript();
        DefaultRedisScript<Long> script = new DefaultRedisScript<Long>(luaScript, Long.class);
        script.setResultType(Long.class);
        String wholeKey = StringUtils.isEmpty(prefix) ? key : prefix + "-" + key;
        Long count = (Long) redisTemplate.execute(script, Collections.singletonList(wholeKey), limitCount, period);
        logger.info("Access try count is {} for name={} and key = {}", count, name, key);
        if (count != null && count.intValue() <= limitCount) {
            return point.proceed();
        }
        return CommonResults.failed("系统繁忙，请稍后重试。。。");
    }

    public String getLimitLuaScript() {
        String lua = "local key = KEYS[1]\n" +
                "local limit = tonumber(ARGV[1])\n" +
                "local currentLimit = tonumber(redis.call('get', key) or '0')\n" +
                "if currentLimit > limit then\n" +
                "return currentLimit\n" +
                "end\n" +
                "currentLimit = redis.call('INCRBY', key, 1)\n" +
                "if currentLimit == 1 then\n" +
                "redis.call('EXPIRE', key, ARGV[2])\n" +
                "end\n" +
                "return currentLimit\n";
        return lua;
    }


    /**
     * 获取ip地址
     */
    public String getIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}

```

#### 2.8 编写controller测试

```java
package com.mrlu.limit.controller;

import com.mrlu.limit.anno.Limit;
import com.mrlu.limit.constant.LimitType;
import com.mrlu.response.CommonResults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 简单de快乐
 * @create 2023-05-26 16:18
 */
@RestController
public class LimiterController {

    private static final AtomicInteger ATOMIC_INTEGER_1 = new AtomicInteger();
    private static final AtomicInteger ATOMIC_INTEGER_2 = new AtomicInteger();
    private static final AtomicInteger ATOMIC_INTEGER_3 = new AtomicInteger();


    @GetMapping("/limitTest1")
    @Limit(key = "limitTest", period = 10, count = 3)
    public CommonResults testLimiter1() {
        return CommonResults.ok(ATOMIC_INTEGER_1.incrementAndGet());
    }

    @GetMapping("/limitTest2")
    @Limit(key = "customer_limit_test", period = 5, count = 3, limitType = LimitType.CUSTOMER)
    public CommonResults testLimiter2() {
        return CommonResults.ok(ATOMIC_INTEGER_2.incrementAndGet());
    }

    @GetMapping("/limitTest3")
    @Limit(key = "ip_limit_test", period = 10, count = 3, limitType = LimitType.IP)
    public CommonResults testLimiter3() {
        return CommonResults.ok(ATOMIC_INTEGER_3.incrementAndGet());
    }

}

```

### 3、测试

* limitTest1：自定义key进行限流，限流时间为10s

（1）第一次访问

![image-20230529105945633](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230529105945633.png)

（2）10s内访问超过3次

![image-20230529110048293](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230529110048293.png)

* limitTest2：自定义key进行限流，限流时间为5s

（1）第一次访问

![image-20230529110252130](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230529110252130.png)

（2）5s内访问超过3次

![image-20230529110346742](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230529110346742.png)

* limitTest3：对ip进行限流

（1）第一次访问

![image-20230529110441876](D:\scene\images\image-20230529110441876.png)

（3）10s内访问超过3次

![image-20230529110604650](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230529110604650.png)

