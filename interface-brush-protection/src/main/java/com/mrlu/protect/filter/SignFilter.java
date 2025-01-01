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
