package com.lora.mianshihou.aop;

import com.alibaba.nacos.common.utils.StringUtils;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.lora.mianshihou.annotation.MultiLevelCache;
import com.lora.mianshihou.common.ErrorCode;
import com.lora.mianshihou.common.ResultUtils;
import com.lora.mianshihou.exception.ThrowUtils;
import com.lora.mianshihou.model.entity.Question;
import com.lora.mianshihou.model.vo.QuestionVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.jd.platform.hotkey.client.callback.JdHotKeyStore.*;

/**
 * ProceedingJoinPoint就像是原始方法的"代理"，AOP切面可以通过它获取方法信息，并在适当的时候执行原始方法
 * generateCacheKey方法利用ProceedingJoinPoint获取方法信息，结合注解参数，生成唯一的缓存键
 * 添加HotKey本地缓存检查：在检查Redis缓存前，先检查HotKey本地缓存
 * 缓存回填机制：当Redis有数据且是热key时，将数据回填到HotKey本地缓存
 * 双重检查机制：获取分布式锁后，再次检查Redis缓存
 * 多级缓存设置：执行业务方法后，同时设置Redis缓存和HotKey本地缓存
 * 降级策略：在获取锁失败或异常情况下，直接执行业务方法确保业务逻辑继续
 * 进行多级缓存机制 基于hotkey ，本地缓存，redis的多级
 */
@Aspect
@Component
@Slf4j
public class MultiLevelCacheAspect {
    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private ExpressionParser expressionParser;
    @Resource
    private JdHotKeyStore jdHotKeyStore;


    @Around("@annotation(multiLevelCache)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, MultiLevelCache multiLevelCache) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String cacheKey = generateCacheKey(joinPoint, multiLevelCache);
        String lockKey = "lock:" + cacheKey;

        try {
            // 首先检查本地缓存hotkey
            if (isHotKey(cacheKey)) {
                //如果本地有热键
                Object hotKeyValue = get(cacheKey);
                if (hotKeyValue != null) {
                    log.debug("本地热键:{}", cacheKey);
                    return hotKeyValue;
                }
            }

            // 使用redis 进行检查,可以先看hotkey，执行回填
            Object redisCahce = redisTemplate.opsForValue().get(cacheKey);
            if (redisCahce != null) {
                log.debug("redis cache key:{}", cacheKey);
                // 如果redis有数据还是热key，回填hotkey到本地缓存
                if (isHotKey(cacheKey)) {
                    JdHotKeyStore.smartSet(cacheKey, redisCahce);
                    log.debug("回填到hotkey的键:{}", cacheKey);
                }
                return redisCahce;
            }
            // 获取分布式锁防止缓存击穿
            Boolean LockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
//            if (!LockAcquired) {
//                //如果没有拿到所，说明有别的进程再拿数据，稍后等待重试，或者递归重试
//                Thread.sleep(50);
//                //降级策略执行业务方法
//                return joinPoint.proceed();
//            }
            if (Boolean.TRUE.equals(LockAcquired)) {
                // 分布锁后，进行双重检查
                try {
                    // 获取锁之后进行双重检查redis缓存
                    Object doubleCheck = redisTemplate.opsForValue().get(cacheKey);
                    if (doubleCheck != null) {
                        log.debug("redis缓存二次检查上锁状态下{}", cacheKey);

                        // 回填如果此时redis缓存还是热键
                        if (JdHotKeyStore.isHotKey(cacheKey)) {
                            JdHotKeyStore.smartSet(cacheKey, doubleCheck);
                            log.debug("回填hotkey:{}", cacheKey);
                        }
                        return doubleCheck;
                    }
                } finally {

                    //释放锁，互斥锁
                    redisTemplate.delete(lockKey);
                }
            } else {
                //如果没有拿到所，说明有别的进程再拿数据，稍后等待重试，或者递归重试
                Thread.sleep(50);
                //降级策略执行业务方法
                return joinPoint.proceed();
            }
            // 继续进行业务方法

            log.debug("缓存错误，执行业务方法:{}",cacheKey);
            Object result = joinPoint.proceed();

            // 设置缓存
            if(result!=null)  {
                // 防止雪崩问题，设置随机ttl
                long randomOffset = ThreadLocalRandom.current().nextInt(0, 300);
                long ttl = multiLevelCache.expire()+randomOffset;

                // 设置redis 缓存

                redisTemplate.opsForValue().set(cacheKey,result,ttl,TimeUnit.SECONDS);
                log.debug("设置redis在随机秒后过期避免雪崩问题:{}",cacheKey);
                // 如果是热 key，设置到hotkey本地缓存

                // 回填如果此时redis缓存还是热键
                if (JdHotKeyStore.isHotKey(cacheKey)) {
                    JdHotKeyStore.smartSet(cacheKey, result);
                    log.debug("回填hotkey:{}", cacheKey);
                }

            }
             return result;

        } catch (Exception e) {

            log.error("发生查询错误 key: {}, error: {}", cacheKey, e.getMessage());
            // 降级策略：直接执行业务方法确保业务逻辑继续
            return joinPoint.proceed();

        }
    }

    private String generateCacheKey(ProceedingJoinPoint joinPoint, MultiLevelCache multiLevelCache) {
        String baseKey = multiLevelCache.value();
        String keyExpression = multiLevelCache.key();

        if (!StringUtils.hasText(baseKey)) {
            baseKey = joinPoint.getTarget().getClass().getSimpleName() + ":" + joinPoint.getSignature().getName();
        }

        if (!StringUtils.hasText(keyExpression)) {
            // 如果没有提供key表达式，使用方法参数生成key
            StringBuilder keyBuilder = new StringBuilder(baseKey);
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                keyBuilder.append(":").append(arg);
            }
            return keyBuilder.toString();
        }

        // 解析SpEL表达式
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterNames();
        for (int i = 0; i < args.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        Expression expression = expressionParser.parseExpression(keyExpression);
        String expressionResult = expression.getValue(context, String.class);
        return baseKey + ":" + expressionResult;
    }
}
