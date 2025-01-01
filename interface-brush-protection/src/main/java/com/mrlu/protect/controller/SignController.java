package com.mrlu.protect.controller;

import com.mrlu.protect.entity.Product;
import com.mrlu.response.CommonResults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 简单de快乐
 * @create 2023-06-05 18:06
 */
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
