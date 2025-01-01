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
    @Limit(key = "customer_limit_test", period = 10, count = 3, limitType = LimitType.CUSTOMER)
    public CommonResults testLimiter2() {
        return CommonResults.ok(ATOMIC_INTEGER_2.incrementAndGet());
    }

    @GetMapping("/limitTest3")
    @Limit(key = "ip_limit_test", period = 10, count = 3, limitType = LimitType.IP)
    public CommonResults testLimiter3() {
        return CommonResults.ok(ATOMIC_INTEGER_3.incrementAndGet());
    }

}
