package com.mrlu.protect.entity;

import lombok.Builder;
import lombok.Data;

/**
 * @author 简单de快乐
 */
@Data
@Builder
public class RequestHeader {
    private String sign ;
    private Long timestamp;
    private String nonce;
}
