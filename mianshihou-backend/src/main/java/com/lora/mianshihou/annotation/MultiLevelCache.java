package com.lora.mianshihou.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
@Target(ElementType.METHOD)  // 标记该注解只能用于方法（Method）上
@Retention(RetentionPolicy.RUNTIME)// 标记该注解在运行时依然保留，可以通过反射机制读取
public @interface MultiLevelCache {
//  缓存名称/前缀，例如  "question_detail" 配合hotkey

    String value() default "";

    // 缓存key ，支持SpEL表达式，例如/#id/
    String key() default "";

    // redis 缓存 的过期时间  ，默认30分钟
    long expire() default 1800;

    // redis 的锁的过期时间,(秒)防止死锁，默认10秒
    long longExpire() default 10;


}
