package com.feign;


import org.springframework.cloud.openfeign.FeignClient;

/**
 * 深圳服务apiFeign客户端
 */
@FeignClient(
        name = "szApi",
        //url = "#{T(com.linkcm.common.utils.PropertiesUtil).getProperty('szApi.baseUrl')}",
        url = "https://www.baidu.com/",
        configuration = ApiFeignConfig.class
)
public interface ApiFeignClient {


}
