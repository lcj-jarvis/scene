package com.mrlu.lock.core;

import com.mrlu.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author 简单de快乐
 * @create 2024-01-09 21:58
 */
@Service
@Slf4j
public class RedissonDistributedLock implements IDistributedLock {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 统一前缀
     */
    @Value("${redisson.lock.prefix:distributed-lock}")
    private String prefix;

    @Override
    public RLock lock(String key) {
        return this.lock(key, 0L, TimeUnit.SECONDS, false);
    }

    @Override
    public RLock lock(String key, long lockTime, TimeUnit unit, boolean fair) {
        if (StringUtils.isEmpty(key)) {
            throw new ServiceException("lock error, key is null or empty");
        }
        RLock lock = getLock(key, fair);
        // 获取锁,失败一直等待,直到获取锁,不支持自动续期
        if (lockTime > 0) {
            lock.lock(lockTime, unit);
        } else {
            // 具有Watch Dog 自动延期机制 默认续30s 每隔30/3=10 秒续到30
            lock.lock();
        }
        return lock;
    }

    @Override
    public RLock tryLock(String key, long tryTime) throws Exception {
        return this.tryLock(key, tryTime, 0L, TimeUnit.SECONDS, false);
    }

    @Override
    public RLock tryLock(String key, long tryTime, long lockTime, TimeUnit unit, boolean fair) throws Exception {
        if (tryTime <= 0) {
            throw new ServiceException("tryTime must be greater than 0");
        }
        RLock lock = getLock(key, fair);
        // 尝试获取锁，获取不到超时异常,不支持自动续期
        boolean lockAcquired;
        if (lockTime > 0) {
            lockAcquired = lock.tryLock(tryTime, lockTime, unit);
        } else {
            // 具有Watch Dog 自动延期机制 默认续30s 每隔30/3=10 秒续到30s
            lockAcquired = lock.tryLock(tryTime, unit);
        }
        if (lockAcquired) {
            return lock;
        }
        return null;
    }

    /**
     * 获取锁
     * @param key 加锁的key
     * @param fair true: 获取公布锁 false: 获取非公布锁
     * @return
     */
    private RLock getLock(String key, boolean fair) {
        RLock lock;
        String lockKey = prefix + key;
        if (fair) {
            // 公布锁
            lock = redissonClient.getFairLock(key);
        } else {
            // 非公布锁
            lock = redissonClient.getLock(lockKey);
        }
        return lock;
    }

    @Override
    public void unlock(RLock lock) {
        if (lock != null) {
            if (lock.isLocked()) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    log.error("释放分布式锁异常", e);
                    throw new ServiceException(e);
                }
            }
        }
    }
}
