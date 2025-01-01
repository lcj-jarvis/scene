package com.mrlu.protect.util;

import com.alibaba.fastjson.JSONObject;
import com.mrlu.protect.entity.RequestHeader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.SortedMap;

/**
 * @author 简单de快乐
 */
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
