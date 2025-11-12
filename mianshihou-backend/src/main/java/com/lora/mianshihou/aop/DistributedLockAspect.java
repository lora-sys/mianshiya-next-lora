package com.lora.mianshihou.aop;

import com.lora.mianshihou.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 防止重复执行定时任务
 */
@Aspect
@Component
@Slf4j
public class DistributedLockAspect {
    @Resource
    private RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {

        String lockKey = distributedLock.key();
        long leaseTime = distributedLock.leaseTime();
        long waitTime = distributedLock.waitTime();
        TimeUnit timeUnit = distributedLock.timeUnit();
        boolean acquired = false;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (acquired) {
                // 获取锁成功，执行目标方法
                return joinPoint.proceed();
            } else {
                // 获取锁失败，抛出异常或者处理逻辑
                throw new RuntimeException("不能获取到锁" + lockKey);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
         if(acquired){
             // 释放锁
             lock.unlock();
         }

        }

    }
}
