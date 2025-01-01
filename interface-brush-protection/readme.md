## 1、引言

暴露在外网的api接口需要做到**防篡改**和**防重放**才能称之为安全的接口

参考博客：https://mp.weixin.qq.com/s/g3LkQL9nXtQCh7Wnu27vVQ

## 2、实践

### 2.1 新建工程，引入依赖

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
    <artifactId>interface-brush-protection</artifactId>
    <version>1.0.0</version>
    <name>interface-brush-protection</name>
    <description>接口防刷(防篡改和防重放)</description>


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

### 2.2 编写yaml

```yaml
server:
  port: 8080
  servlet:
    context-path: /protect/

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

### 2.3 编写主启动类和配置类

* 主启动类

```java
@SpringBootApplication
public class ProtectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProtectionApplication.class, args);
    }

}
```

* 配置类

```java
package com.mrlu.protect.config;


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

### 2.4 编写业务代码

#### 2.4.1 构建请求头对象

```java
@Data
@Builder
public class RequestHeader {
    private String sign ;
    private Long timestamp;
    private String nonce;
}
```

#### 2.4.2 工具类从HttpServletRequest获取请求参数

```java
public class HttpDataUtil {

    /**
     * post请求处理：获取 Body 参数，转换为SortedMap
     * @param request
     */
    public static SortedMap<String, Object> getBodyParams(HttpServletRequest request) throws IOException {
         byte[] requestBody = StreamUtils.copyToByteArray(request.getInputStream());
         String body = new String(requestBody);
         return JSON.parseObject(body, SortedMap.class);
    }


    /**
     * get请求处理：将URL请求参数转换成SortedMap
     */
    public static SortedMap<String, Object> getUrlParams(HttpServletRequest request) {
        String params = request.getQueryString();
        if (StringUtils.isEmpty(params)) {
            return new TreeMap<>();
        }
        return getSortedMapByUrlParams(params);
    }


    private static SortedMap<String, Object> getSortedMapByUrlParams(String params) {
        try {
            params = URLDecoder.decode(params, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        SortedMap<String, Object> paramMap = new TreeMap<>();
        String[] paramArray = params.split("&");
        for (String param : paramArray) {
            String[] array = param.split("=");
            paramMap.put(array[0], array[1]);
        }
        return paramMap;
    }


}
```

#### 2.4.3 签名验证工具类

```java
@Slf4j
public class SignUtil {

    /**
     * 验证签名
     * 验证算法：把timestamp + JsonUtil.object2Json(SortedMap)合成字符串，然后MD5
     */
    public static boolean verifySign(SortedMap<String, Object> paramMap, RequestHeader requestHeader) {
        // 通常还会加个签名的key来加密
        String params = requestHeader.getNonce() + requestHeader.getTimestamp() + JSONObject.toJSON(paramMap);
        return verifySign(params, requestHeader);
    }

    public static boolean verifySign(String params, RequestHeader requestHeader) {
        log.info("客户端签名：{}", requestHeader.getSign());
        if (StringUtils.isEmpty(params)) {
            return false;
        }
        log.info("客户端上传内容: {}", params);
        String paramsSign = DigestUtils.md5DigestAsHex(params.getBytes());
        log.info("客户端上传内容加密后的签名结果: {}", paramsSign);
        return requestHeader.getSign().equals(paramsSign);
    }

}
```

#### 2.4.4 HttpServletRequest包装类

```java
package com.mrlu.protect.request;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.TreeMap;

/**
 * @author 简单de快乐
 * @create 2023-06-05 11:35
 */
@Slf4j
public class SignRequestWrapper extends HttpServletRequestWrapper {

    /**
     * request数据流只能读取一次，需要自己实现HttpServletRequestWrapper对数据流包装，目的是将request流保存下来
     */
    private byte[] requestBody;

//    public SignRequestWrapper(HttpServletRequest request) throws IOException {
//        super(request);
//
////        Map<String, String[]> parameterMap = request.getParameterMap();
////        System.out.println(parameterMap);
//
//        // 【注意】
//        // post请求application/x-www-form-urlencoded提交 request.getInputStream()获取不到请求参数。
//        // 只能通过request.getParameterMap();request.getParameter();request.getParameterNames();request.getParameterValues();方法来协助获取
//        // 那为什么调用request.getParameterMap()方法，下面的request.getInputStream()就返回空流呢？
//        // 因为request.getParameterMap()底层代码实际上调用了
//        /*public Map<String, String[]> getParameterMap() {
//            if (this.parameterMap.isLocked()) {
//                return this.parameterMap;
//            } else {
//                // 从这debug进去
//                Enumeration enumeration = this.getParameterNames();
//
//                while(enumeration.hasMoreElements()) {
//                    String name = (String)enumeration.nextElement();
//                    String[] values = this.getParameterValues(name);
//                    this.parameterMap.put(name, values);
//                }
//
//                this.parameterMap.setLocked(true);
//                return this.parameterMap;
//            }
//        }*
//
//        // 一直debug进去到下面这个代码位置
//        if ("application/x-www-form-urlencoded".equals(contentType)) {
//              ......
//
//              try {
//                        // 然后从这里进去
//                        if (this.readPostBody(formData, len) != len) {
//                            parameters.setParseFailedReason(FailReason.REQUEST_BODY_INCOMPLETE);
//                            return;
//                        }
//                    } catch (IOException var19) {
//                        Context context = this.getContext();
//                        if (context != null && context.getLogger().isDebugEnabled()) {
//                            context.getLogger().debug(sm.getString("coyoteRequest.parseParameters"), var19);
//                        }
//
//                        parameters.setParseFailedReason(FailReason.CLIENT_DISCONNECT);
//                        return;
//                    }
//
//                    parameters.processParameters(formData, 0, len);
//                }
//
//        }
//
//        protected int readPostBody(byte[] body, int len) throws IOException {
//            int offset = 0;
//
//            do {
//                // debug进去getStream()方法
//                // 流在这里读取了一次，后续的request.getInputStream()都拿不到流了。
//                int inputLen = this.getStream().read(body, offset, len - offset);
//                if (inputLen <= 0) {
//                    return offset;
//                }
//
//                offset += inputLen;
//            } while(len - offset > 0);
//
//            return len;
//        }
//
//        // request.getInputStream() 实际也是使用了this.inputStream。
//        public InputStream getStream() {
//            if (this.inputStream == null) {
//                this.inputStream = new CoyoteInputStream(this.inputBuffer);
//            }
//            return this.inputStream;
//        }
//
//
//
//        一路看下来为什么我们的/sign/test03接口注入参数没有成功，因为
//         post请求以 application/x-www-form-urlencoded 提交时，
//         springmvc底层是用类似下面这端代码获取参数然后注入到接口的实体类的，
//         如果我们没有先调用request.getParameterMap()或者下面这段代码优先获取流，
//         当前构造方法只有requestBody = StreamUtils.copyToByteArray(request.getInputStream());获取了流，
//         就会导致springmvc底层就获取不到流了，Enumeration<String> paramNames 就是空。
//            Enumeration<String> paramNames = request.getParameterNames();
//            while (paramNames.hasMoreElements()) {
//                String paramName = paramNames.nextElement();
//                String value = request.getParameter(paramName);
//                paramMap.put(paramName, value);
//            }
//         */
//
//
//        /*
//         public ServletInputStream getInputStream() throws IOException {
//            if (this.usingReader) {
//                throw new IllegalStateException(sm.getString("coyoteRequest.getInputStream.ise"));
//            } else {
//
//                // 设置成true，然后request.getParameterNames();就会返回空
//                this.usingInputStream = true;
//                if (this.inputStream == null) {
//                    this.inputStream = new CoyoteInputStream(this.inputBuffer);
//                }
//
//                return this.inputStream;
//            }
//        }
//         */
//        requestBody = StreamUtils.copyToByteArray(request.getInputStream());
//
//
//        // post请求以 application/x-www-form-urlencoded 提交
//        // 如果先使用request.getInputStream()或者request.getReader()。以下4个方法就会获取不到请求参数
//        // Map<String, String[]> parameterMap = request.getParameterMap();
//        // String id1 = request.getParameter("id");
//        //  request.getParameterNames();
//        // String[] id2 = request.getParameterValues("id");
//        // System.out.println(parameterMap);
//        // System.out.println(id1);
//        // System.out.println(Arrays.toString(id2));
//        // 以下4个方法底层调用同一个方法。
//        /*
//       protected void parseParameters() {
//            this.parametersParsed = true;
//            Parameters parameters = this.coyoteRequest.getParameters();
//            boolean success = false;
//
//            try {
//                parameters.setLimit(this.getConnector().getMaxParameterCount());
//                Charset charset = this.getCharset();
//                boolean useBodyEncodingForURI = this.connector.getUseBodyEncodingForURI();
//                parameters.setCharset(charset);
//                if (useBodyEncodingForURI) {
//                    parameters.setQueryStringCharset(charset);
//                }
//
//                parameters.handleQueryParameters();
//                if (this.usingInputStream || this.usingReader) {
//                    // 使用了request.getInputStream();usingInputStream就会设置为true，就会直接结束。
//
//                    success = true;
//                    return;
//                }
//            }
//
//            .....
//        }
//        */
//
//
//        /**
//         * 总结：
//         * （1）当form表单内容采用 enctype=application/x-www-form-urlencoded编码时，
//         * 先通过调用 request.getParameterMap();
//         *         request.getParameter();
//         *         request.getParameterNames();
//         *         request.getParameterValues();方法得到参数后，
//         * 再调用request.getInputStream()或request.getReader()已经得不到流中的内容，
//         * 因为在调用 request.getParameterMap();
//         *           request.getParameter();
//         *           request.getParameterNames();
//         *           request.getParameterValues();
//         * 时系统可能对表单中提交的数据以流的形式读了一次,反之亦然。
//         *
//         *
//         * （2）当form表单内容采用enctype=multipart/form-data编码时，
//         *     也是同enctype=application/x-www-form-urlencoded一样的道理
//         *
//         * 参考：
//         * https://www.cnblogs.com/sfnz/p/16416039.html.
//         * 但是需要注意博客关于multipart/form-data时的request.getParameter()
//         *      方法和request.getInputStream()或request.getReader()获取结果的描述是错误的。
//         * request.getQueryString()获取get请求url后面的请求参数，如获取到：k1=v1&k2=v2
//         */
//
//    }

    public SignRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        init(request);
    }

    private void init(HttpServletRequest request) throws IOException {
        String header = request.getHeader("Content-Type");
        log.info("Content-Type={}", header);
        if (StringUtils.isNotEmpty(header)) {
            if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(header)) {
                // post application/x-www-form-urlencoded
                // 解决controller注入参数失败和校验是否重复请求失败
                setRequestBodyWithForm(request);
            } else if (header.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)) {
                // post multipart/form-data
                // 解决controller注入参数失败和校验是否重复请求失败
                setRequestBodyWithForm(request);
            } else {
                requestBody = StreamUtils.copyToByteArray(request.getInputStream());
            }
        } else {
            requestBody = StreamUtils.copyToByteArray(request.getInputStream());
        }
    }

    private void setRequestBodyWithForm(HttpServletRequest request) {
        TreeMap<String, Object> paramMap = new TreeMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String value = request.getParameter(paramName);
            paramMap.put(paramName, value);
        }
        String params2Json = JSONObject.toJSONString(paramMap);
        requestBody = params2Json.getBytes();
    }


    @Override
    public ServletInputStream getInputStream() throws IOException {

        // 只能获取json形式传过来的
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(requestBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}

```

#### 2.4.5 创建过滤器实现安全校验

* 创建过滤器

```java
package com.mrlu.protect.filter;

import com.alibaba.fastjson.JSON;
import com.mrlu.protect.entity.RequestHeader;
import com.mrlu.protect.request.SignRequestWrapper;
import com.mrlu.protect.util.HttpDataUtil;
import com.mrlu.protect.util.SignUtil;
import com.mrlu.response.ApiErrorCode;
import com.mrlu.response.CommonResults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.SortedMap;

/**
 * @author 简单de快乐
 * @create 2023-06-05 11:40
 */
@Slf4j
public class SignFilter implements Filter {

    @Resource
    private RedisTemplate redisTemplate;

    //从filter配置中获取sign过期时间
    private Long signMaxTime;

    private static final String NONCE_KEY = "x-nonce-key-";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String signTime = filterConfig.getInitParameter("signMaxTime");
        signMaxTime = Long.parseLong(signTime);
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        log.info("过滤的url：{}", request.getRequestURL());
        SignRequestWrapper signRequestWrapper = new SignRequestWrapper(request);

        // 构建请求头
        RequestHeader header = RequestHeader.builder()
                .nonce(request.getHeader("x-nonce"))
                .timestamp(Long.parseLong(request.getHeader("x-time")))
                .sign(request.getHeader("x-sign"))
                .build();

        // 验证请求头是否存在
        boolean isHeaderEmpty = StringUtils.isEmpty(header.getSign()) || StringUtils.isEmpty(header.getNonce())
                || header.getTimestamp() == null;
        if (isHeaderEmpty) {
            responseFail(response, ApiErrorCode.AUTHENTICATION_ARGS_EMPTY);
            return;
        }


         /* 1.重放验证
         * 判断timestamp时间戳与当前时间是否操过signMaxTime
         * （过期时间根据业务情况设置）,如果超过了就提示签名过期。*/
        Long timestamp = header.getTimestamp();
        Long now = System.currentTimeMillis();
        Long diff = (now - timestamp) / 1000;

        if (diff > signMaxTime) {
            responseFail(response, ApiErrorCode.SIGN_EXPIRED);
            return;
        }

        // 2、判断nonce
        // 实际使用用户信息+时间戳+随机数等信息做个哈希之后，作为nonce参数
        // 这里是可能同时来两个nonce相关的请求，然后返回false。
        // 所以这里的判断是否存在和设置是同一个操作。要写在lua脚本中
        /*boolean nonceExist = redisTemplate.hasKey();
        if (nonceExist) {
            responseFail(response, ApiErrorCode.REPLAY_ERROR);
        } else {
            redisTemplate.opsForValue().set(NONCE_KEY + header.getNonce(), header.getNonce(), signMaxTime);
        }*/
        String luaScript = getScript();
        DefaultRedisScript<Long> script = new DefaultRedisScript<Long>(luaScript, Long.class);
        script.setResultType(Long.class);
        Long count = (Long) redisTemplate.execute(script, Collections.singletonList(NONCE_KEY + header.getNonce()), header.getNonce(), signMaxTime);
        boolean nonceExist = (count == 1);
        if (nonceExist) {
            responseFail(response, ApiErrorCode.REPLAY_ERROR);
            return;
        }

        // 获取请求参数，判断鉴权是否通过
        SortedMap<String, Object> paramMap;
        boolean pass;
        String method = signRequestWrapper.getMethod();
        switch (method) {
            case "POST":
                paramMap = HttpDataUtil.getBodyParams(signRequestWrapper);
                pass = SignUtil.verifySign(paramMap, header);
                break;
            case "GET":
                paramMap = HttpDataUtil.getUrlParams(signRequestWrapper);
                pass = SignUtil.verifySign(paramMap, header);
                break;
            default:
                pass = false;
        }

        if (pass) {
            // 鉴权通过
            filterChain.doFilter(signRequestWrapper, response);
        } else {
            // 鉴权不通过
            responseFail(response, ApiErrorCode.AUTHENTICATION_ERROR);
        }
    }

    /*
     * 脚本
     */
    public String getScript() {
        String script = "local key = KEYS[1]\n" +
                "local exist = tonumber(redis.call('EXISTS', key))\n" +
                "if exist == 1 then\n" +
                "return 1\n" +
                "end\n" +
                "redis.call('SET', key, ARGV[1])\n" +
                "redis.call('EXPIRE', key, ARGV[2])\n" +
                "return 0\n";
        return script;
    }

    private void responseFail(HttpServletResponse httpResponse, ApiErrorCode returnCode) throws IOException {
        CommonResults resultData = new CommonResults(returnCode);
        // 解决：getWriter() has already been called for this response
        // 错误方式
        // PrintWriter writer = httpResponse.getWriter();
        // 方式一：
        /*Writer writer = new BufferedWriter(new OutputStreamWriter(httpResponse.getOutputStream()));
        writer.write(JSON.toJSONString(resultData));
        writer.flush();*/
        // 方式二：
        httpResponse.getOutputStream().write(JSON.toJSONString(resultData).getBytes());
    }

}
```

* 注册过滤器

```java
package com.mrlu.protect.config;

import com.mrlu.protect.filter.SignFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 简单de快乐
 * @create 2023-06-05 17:58
 */
@Configuration
public class SignFilterConfig {

    // 默认10s
    @Value("${sign.maxTime:60}")
    private String signMaxTime;

    /**
     * filter中的初始化参数
     */
    private Map<String, String> initParametersMap =  new HashMap<>();

    @Bean
    public FilterRegistrationBean contextFilterRegistrationBean() {
        initParametersMap.put("signMaxTime",signMaxTime);
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(signFilter());
        registration.setInitParameters(initParametersMap);
        registration.addUrlPatterns("/sign/*");
        registration.setName("SignFilter");
        // 设置过滤器被调用的顺序
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public Filter signFilter() {
        return new SignFilter();
    }

}

```

### 2.5 编写controller测试

#### 2.5.1 测试实体类

```java
@Data
@EqualsAndHashCode
public class Product {
    private Double price;
    private String name;
    private Integer id;
}
```

#### 2.5.2 controller

```java
@RestController
public class SignController {

    @GetMapping("/sign/test01")
    public CommonResults getPdInfoUseGetMethod(Product product) {
        return CommonResults.ok(product);
    }

    @PostMapping("/sign/test02")
    public CommonResults getPdInfoUsePostMethod02(@RequestBody Product product) {
        return CommonResults.ok(product);
    }

    @PostMapping("/sign/test03")
    public CommonResults getPdInfoUsePostMethod03(Product product) {
        return CommonResults.ok(product);
    }

    @GetMapping("/getNonceStr")
    public String getNonceStr(Product product) {
        return String.valueOf(product.hashCode());
    }

}
```

#### 2.5.3 生成签名参数用于测试

```java
@SpringBootTest
class ProtectionApplicationTests {

    @Test
    void contextLoads() {

    }

    /**
     * 获取get请求的签名和请求参数
     */
    @Test
    void getGetMethodSign() {
        Product product = new Product();
        product.setName("手机");
        product.setPrice(3500.0);
        product.setId(1);

        long timeMillis = System.currentTimeMillis();

        TreeMap<String, String> paramMap = new TreeMap<>();
        paramMap.put("id", "1");
        paramMap.put("name", "手机");
        paramMap.put("price", "3500");

        System.out.println("x-nonce:" + Math.abs(product.hashCode()));
        System.out.println("x-time:" + timeMillis);
        System.out.println("json形式的请求参数:" + JSONObject.toJSON(paramMap));
        String params = String.valueOf(Math.abs(product.hashCode())) + timeMillis + JSONObject.toJSON(paramMap);
        System.out.println(params);
        System.out.println("x-sign:" + DigestUtils.md5DigestAsHex(params.getBytes()));
    }

    /**
     * 获取post请求的签名和请求参数
     */
    @Test
    void getPostMethodSign() {
        Product product = new Product();
        product.setName("手机");
        product.setPrice(4500.0);
        product.setId(2);

        long timeMillis = System.currentTimeMillis();

        TreeMap<String, Object> paramMap = new TreeMap<>();
        paramMap.put("id", "2");
        paramMap.put("name", "手机");
        paramMap.put("price", "4500");

        System.out.println("x-nonce:" + Math.abs(product.hashCode()));
        System.out.println("x-time:" + timeMillis);
        System.out.println("json形式的请求参数:" + JSONObject.toJSON(paramMap));
        String params = String.valueOf(Math.abs(product.hashCode())) + timeMillis + JSONObject.toJSON(paramMap);
        System.out.println(params);
        System.out.println("x-sign:" + DigestUtils.md5DigestAsHex(params.getBytes()));
    }

}
```

#### 2.5.4 测试

* GET请求防重放和防篡改

1、添加请求参数

![image-20230630153436650](D:\scene\images\image-20230630153436650.png)

2、添加请求头

![image-20230630153527354](D:\scene\images\image-20230630153527354.png)

3、首次请求，请求成功

![image-20230630153617952](D:\scene\images\image-20230630153617952.png)

4、有效时间内，再次请求，请求失败

![image-20230630153707304](D:\scene\images\image-20230630153707304.png)

5、超过有效时间，签名失效

![image-20230630153822517](D:\scene\images\image-20230630153822517.png)

* POST请求防重放和防篡改

1、添加请求参数

![image-20230630154851145](D:\scene\images\image-20230630154851145.png)

2、添加请求头参数

![image-20230630155925794](D:\scene\images\image-20230630155925794.png)

3、首次请求成功

![image-20230630155851823](D:\scene\images\image-20230630155851823.png)

4、有效时间内，再次请求，请求失败

![image-20230630160016463](D:\scene\images\image-20230630160016463.png)

5、超过有效时间，签名失效

![image-20230630160128823](D:\scene\images\image-20230630160128823.png)

