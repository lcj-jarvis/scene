## 如何用 Redis实现微博好友（关注，取关，共同关注）

### 一、总体思路

我们采用MySQL + Redis的方式结合完成。MySQL主要是保存落地数据，而利用Redis的Sets数据类型进行集合操作。

Sets拥有去重(我们不能多次关注同一用户)功能。

一个用户我们存贮两个集合：一个是保存用户关注的人  另一个是保存关注用户的人。

（1）SADD 添加成员： 命令格式: SADD key member [member …] ----- 关注
（2）SREM 移除某个成员： 命令格式: SREM key member [member …] -------取关
（3）SCARD 统计集合内的成员数： 命令格式: SCARD key -------关注/粉丝个数
（4）SISMEMBER 判断是否是集合成员： 命令格式:SISMEMBER key member ---------判断是否关注（如果关注那么只可以点击取关）
（5）SMEMBERS 查询集合内的成员： 命令格式: SMEMBERS key -------列表使用（关注列表和粉丝列表）
（6）SINTER 查询集合的交集： 命令格式: SINTER key [key …] --------共同关注、我关注的人关注了他

### 二、数据库表设计

```sql
drop table if exists t_follow;
CREATE TABLE t_follow (
  id int(11) NOT NULL AUTO_INCREMENT,
  user_id int(11) DEFAULT NULL COMMENT '当前登录用户的id',
  follow_user_id int(11) DEFAULT NULL COMMENT '当前登录用户关注的用户的id',
  is_valid tinyint(1) DEFAULT NULL COMMENT '关注状态，0-没有关注，1-关注了',
  create_date datetime DEFAULT NULL,
  update_date datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='用户和用户关注表';
```

说明：主要记录了用户id、用户关注的id和关注状态

### 三、新建好友功能微服务

#### 1、新建工程，引入依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.5.6</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>com.mrlu</groupId>
    <artifactId>weibo-follow</artifactId>
    <version>1.0.0</version>
    <description>微博关注好友</description>

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
</project>
```

#### 2、编写yaml

```yaml
server:
  port: 8080
  servlet:
    context-path: /weibo/

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
        max-active: 8
        max-wait: -1ms
        max-idle: 8
        min-idle: 0

#mybatisplus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

#### 3、编写业务代码

3.1 配置类与实体

（1）配置类

```java
package com.mrlu.weibo.config;


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

（2）实体

```java
package com.mrlu.weibo.entity;

import java.util.Date;
import java.io.Serializable;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Accessors;

/**
 * 用户和用户关注表(Follow)实体类
 *
 * @author 简单de快乐
 * @since 2023-05-25 16:46:41
 */
@Data
@Accessors(chain = true)
@TableName("t_follow")
public class Follow implements Serializable {
    private static final long serialVersionUID = 267563877201767199L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 当前登录用户的id
     */
    private Integer userId;
    /**
     * 当前登录用户关注的用户的id
     */
    private Integer followUserId;
    /**
     * 关注状态，0-没有关注，1-关注了
     */
    private Integer isValid;

    private Date createDate;

    private Date updateDate;

}
```

3.2 mapper

```java
package com.mrlu.weibo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mrlu.weibo.entity.Follow;

/**
 * 用户和用户关注表(Follow)表数据库访问层
 *
 * @author 简单de快乐
 * @since 2023-05-25 16:46:41
 */
public interface FollowMapper extends BaseMapper<Follow> {

}
```

3. 3 controller

```java
package com.mrlu.weibo.controller;

import com.mrlu.response.CommonResults;
import com.mrlu.weibo.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 用户和用户关注表(Follow)表控制层
 * @author 简单de快乐
 * @since 2023-05-25 16:46:41
 */
@RestController
@RequestMapping("/friend")
public class FollowController {

    /**
     * 服务对象
     */
    @Autowired
    private FollowService followService;

    /**
     * 关注/取关
     * @param userId 当前用户id（实际上在项目上用户信息，包括用户id，一般都是用登录的token中获取的）
     * @param followUserId 关注/取关的用户id
     * @param isFollowed 1:关注 0:取关
     */
    @GetMapping("/follow")
    public CommonResults follow(Integer userId, Integer followUserId, int isFollowed) {
        return followService.follow(userId, followUserId, isFollowed);
    }

    /**
     * 其他用户是否关注了当前用户
     * @param currentUserId 当前用户id（实际上在项目上用户信息，包括用户id，一般都是用登录的token中获取的）
     * @param otherUserId 其他用户id
     * @return true：是  false：否
     */
    @GetMapping("/isFollow")
    public CommonResults isFollow(@RequestParam Integer currentUserId, @RequestParam Integer otherUserId) {
        return followService.isFollow(currentUserId, otherUserId);
    }

    /**
     * 共同关注列表
     * @param currentUserId 当前用户id（实际上在项目上用户信息，包括用户id，一般都是用登录的token中获取的）
     * @param otherUserId 其他用户id
     * @return 返回currentUserId和otherUserId共同关注的好友
     */
    @GetMapping("/commons")
    public CommonResults findCommonsFriends(@RequestParam Integer currentUserId, @RequestParam Integer otherUserId{
        return followService.findCommonsFriends(currentUserId, otherUserId);
    }

    /**
     * 获取关注的总人数
     * @param userId
     */
    @GetMapping("/total")
    public CommonResults total(Integer userId) {
        return followService.total(userId);
    }

    /**
     * 获取关注的用户列表
     * @param userId
     */
    @GetMapping("/list")
    public CommonResults getFollowingFriends(Integer userId) {
        return followService.getFollowingFriends(userId);
    }

}
```

3. 4 service

（1）接口

```java
public interface FollowService extends IService<Follow> {

    CommonResults follow(Integer userId, Integer followUserId, int isFollowed);

    CommonResults findCommonsFriends(Integer currentUserId, Integer otherUserId);

    CommonResults total(Integer userId);

    CommonResults getFollowingFriends(Integer userId);

    CommonResults isFollow(Integer currentUserId, Integer otherUserId);
}
```

（2）实现

```java
package com.mrlu.weibo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mrlu.response.CommonResults;
import com.mrlu.weibo.entity.Follow;
import com.mrlu.weibo.mapper.FollowMapper;
import com.mrlu.weibo.service.FollowService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Set;

/**
 * 用户和用户关注表(Follow)表服务实现类
 *
 * @author 简单de快乐
 * @since 2023-05-25 16:46:41
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 关注/取关
     * @param userId 当前用户id（实际上在项目上用户信息，包括用户id，一般都是用登录的token中获取的）
     * @param followUserId 关注/取关的用户id (有谁关注了userId)
     * @param isFollowed 1:关注 0:取关
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonResults follow(Integer userId, Integer followUserId, Integer isFollowed) {
        Follow followInfo = getFollowInfo(userId, followUserId);
        if (isFollowed == 1 && followInfo == null) {
            // 首次关注，添加关注信息
            Date now = new Date();
            Follow firstFollow = new Follow().setFollowUserId(followUserId)
                    .setIsValid(1)
                    .setUserId(userId)
                    .setCreateDate(now)
                    .setUpdateDate(now);
            boolean isFollow = save(firstFollow);
            if (isFollow) {
                // 添加关注信息到redis
                addToRedisSet(userId, followUserId);
            }
            return CommonResults.ok("关注成功");
        }

        if (isFollowed == 1 && followInfo.getIsValid() == 0) {
            // 之前取关过了，再次关注
            boolean success = setIsValid(1, followInfo.getId());
            if (success) {
                // 添加关注信息到redis
                addToRedisSet(userId, followUserId);
            }
            return CommonResults.ok("关注成功");
        }

        if (isFollowed == 0 && followInfo != null && followInfo.getIsValid() == 1) {
            // 取消关注
            boolean success = setIsValid(0, followInfo.getId());
            if (success) {
                // 移除redis的关注信息
                removeFromRedisSet(userId, followUserId);
            }
            return CommonResults.ok("成功取关");
        }

        return CommonResults.ok("操作成功");
    }

    @Override
    public CommonResults findCommonsFriends(Integer currentUserId, Integer otherUserId) {
        String currentUserKey = USER_FOLLOWING + currentUserId;
        String otherUserKey = USER_FOLLOWING + otherUserId;
        // 计算交集
        Set<Integer> userIds = redisTemplate.opsForSet().intersect(currentUserKey, otherUserKey);
        // todo 这里就不根据用户id去组装用户信息了
        return CommonResults.ok(userIds);
    }

    @Override
    public CommonResults total(Integer userId) {
        String userKey = USER_FOLLOWING + userId;
        Long size = redisTemplate.opsForSet().size(userKey);
        return CommonResults.ok(size);
    }

    @Override
    public CommonResults getFollowingFriends(Integer userId) {
        String userKey = USER_FOLLOWING + userId;
        Set<Integer> members = redisTemplate.opsForSet().members(userKey);
        return CommonResults.ok(members);
    }

    @Override
    public CommonResults isFollow(Integer currentUserId, Integer otherUserId) {
        String userKey = USER_FOLLOWERS + otherUserId;
        Boolean isMember = redisTemplate.opsForSet().isMember(userKey, currentUserId);
        return CommonResults.ok(isMember);
    }

    private boolean setIsValid(int valid, int id) {
        Follow againFollow = new Follow().setIsValid(valid)
                .setUpdateDate(new Date()).setId(id);
        boolean success = updateById(againFollow);
        return success;
    }


    private Follow getFollowInfo(Integer userId, Integer followUserId) {
        LambdaQueryWrapper<Follow> condition = new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId);
        return getOne(condition);
    }

    private static final String USER_FOLLOWING = "user_following_";
    private static final String USER_FOLLOWERS = "user_followers_";

    /**
     * 添加关注列表到 Redis
     * @param userId
     * @param followUserId
     */
    private void addToRedisSet(Integer userId, Integer followUserId) {
        // 当前用户userId下有谁关注了
        redisTemplate.opsForSet().add(USER_FOLLOWING + userId, followUserId);
        // 用户followUserId 关注了谁
        redisTemplate.opsForSet().add(USER_FOLLOWERS + followUserId, userId);
    }

    /**
     * 移除 Redis 关注列表
     *
     * @param userId
     * @param followUserId
     */
    private void removeFromRedisSet(Integer userId, Integer followUserId) {
        redisTemplate.opsForSet().remove(USER_FOLLOWING + userId, followUserId);
        redisTemplate.opsForSet().remove(USER_FOLLOWERS + followUserId, userId);
    }


}

```

3.5 启动类

```java
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.mrlu.**.mapper")
public class WeiboApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeiboApplication.class, args);
    }
}
```

#### 4、测试

##### 4.1 关注/取关

（1）关注

![image-20230526120309917](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526120309917.png)

![image-20230526120327214](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526120327214.png)

![image-20230526120349739](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526120349739.png)

![image-20230526120402513](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526120402513.png)

![image-20230526120414784](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526120414784.png)

![image-20230526141757975](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526141757975.png)

![image-20230526141832493](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526141832493.png)

![image-20230526141906419](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526141906419.png)

（2）取关

![image-20230526142118143](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526142118143.png)

（3）重新关注

![image-20230526142215292](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526142215292.png)

##### 4.2 获取用户的关注列表

![image-20230526142253534](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526142253534.png)

![image-20230526143655027](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526143655027.png)

##### 4.3 获取关注的总人数

![image-20230526143734773](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526143734773.png)

![image-20230526143750758](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526143750758.png)

##### 4.4 获取共同关注好友

![image-20230526143846356](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526143846356.png)

##### 4.5 其他用户是否关注了当前用户

![image-20230526144824219](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/scene/image-20230526144824219.png)

