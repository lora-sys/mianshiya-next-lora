package com.lora.mianshihou.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解，不含任何属性，只是一个标记
 */
@Target(ElementType.METHOD)  // 标记该注解只能用于方法（Method）上
@Retention(RetentionPolicy.RUNTIME)// 标记该注解在运行时依然保留，可以通过反射机制读取
public @interface DistributedLock {
// 配置

    /**
     * 锁的名称
     */
    String key();


    /**
     * 持续时间，默认30秒
     * @return
     */
    long leaseTime() default 30000;

    /**
     * 等待时间，默认10秒
     */
    long waitTime() default 10000;


    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}
