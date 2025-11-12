package com.lora.mianshihou.manager;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.IntegerCodec;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

// 代办，redis本地聚合后，返回结果，减少每次请求redis压力
// 增加并返回计数，默认统计一分钟内的计数结果



/**
 * 通用计数器（可用于实现频率统计、限流、封禁等等）
 */
@Slf4j
@Service
public class CounterMessager {

    @Resource
    private RedissonClient redissonClient;

    // 是否启用本地聚合（默认关闭，保持原逻辑作为降级）
    @Value("${counter.aggregate.enabled:false}")
    private boolean aggregateEnabled;

    // 刷新到 Redis 的时间间隔（毫秒）
    @Value("${counter.aggregate.flush-interval-ms:1000}")
    private long flushIntervalMs;

    // 本地桶数量上限，防止内存膨胀
    @Value("${counter.aggregate.max-buckets:10000}")
    private int maxBuckets;

    // 本地桶结构：按窗口 key 聚合计数与过期秒数
    private final ConcurrentHashMap<String, LocalBucket> localBuckets = new ConcurrentHashMap<>();
    // 最近一次已刷新的远端总数缓存，用于返回更接近真实的值（本地未刷新的增量 + 远端缓存）
    private final ConcurrentHashMap<String, Long> remoteCountCache = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> flushTaskFuture;

    /**
     * 增加并返回计数，默认统计一分钟内的计数结果
     *
     * @param key 缓存键
     * @return
     */
    public long incrAndGetCounter(String key) {
        return incrAndGetCounter(key, 1, TimeUnit.MINUTES);
    }

    /**
     * 增加并返回计数
     *
     * @param key          缓存键
     * @param timeInterval 时间间隔
     * @param timeUnit     时间间隔单位
     * @return
     */
    public long incrAndGetCounter(String key, int timeInterval, TimeUnit timeUnit) {
        int expirationTimeInSeconds;
        switch (timeUnit) {
            case SECONDS:
                expirationTimeInSeconds = timeInterval;
                break;
            case MINUTES:
                expirationTimeInSeconds = timeInterval * 60;
                break;
            case HOURS:
                expirationTimeInSeconds = timeInterval * 60 * 60;
                break;
            default:
                throw new IllegalArgumentException("Unsupported TimeUnit. Use SECONDS, MINUTES, or HOURS.");
        }

        return incrAndGetCounter(key, timeInterval, timeUnit, expirationTimeInSeconds);
    }

    /**
     * 增加并返回计数
     *
     * @param key                     缓存键
     * @param timeInterval            时间间隔
     * @param timeUnit                时间间隔单位
     * @param expirationTimeInSeconds 计数器缓存过期时间
     * @return
     */
    public long incrAndGetCounter(String key, int timeInterval, TimeUnit timeUnit, long expirationTimeInSeconds) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        // 根据时间粒度生成 Redis Key
        long timeFactor;
        switch (timeUnit) {
            case SECONDS:
                timeFactor = Instant.now().getEpochSecond() / timeInterval;
                break;
            case MINUTES:
                timeFactor = Instant.now().getEpochSecond() / timeInterval / 60;
                break;
            case HOURS:
                timeFactor = Instant.now().getEpochSecond() / timeInterval / 3600;
                break;
            default:
                throw new IllegalArgumentException("不支持的单位");
        }

        String redisKey = key + ":" + timeFactor;
        if (!aggregateEnabled) {
            // 原逻辑：每次直接写 Redis，返回准确值（作为降级）
            String luaScript =
                    "if redis.call('exists', KEYS[1]) == 1 then " +
                            "  return redis.call('incr', KEYS[1]); " +
                            "else " +
                            "  redis.call('set', KEYS[1], 1); " +
                            "  redis.call('expire', KEYS[1], ARGV[1]); " +
                            "  return 1; " +
                            "end";

            RScript script = redissonClient.getScript(IntegerCodec.INSTANCE);
            Object countObj = script.eval(
                    RScript.Mode.READ_WRITE,
                    luaScript,
                    RScript.ReturnType.INTEGER,
                    Collections.singletonList(redisKey), expirationTimeInSeconds
            );
            return (long) countObj;
        }

        // 本地聚合：累加到桶，定时批量刷新到 Redis
        LocalBucket bucket = localBuckets.compute(redisKey, (rk, old) -> {
            if (old == null) {
                if (localBuckets.size() >= maxBuckets) {
                    // 简单防护：超过上限不再聚合，直接走降级逻辑
                    return null;
                }
                LocalBucket nb = new LocalBucket(expirationTimeInSeconds);
                return nb;
            }
            // ttl 以首次设置为准
            return old;
        });

        if (bucket == null) {
            // 桶不可用则降级为直写 Redis
            String luaScript =
                    "if redis.call('exists', KEYS[1]) == 1 then " +
                            "  return redis.call('incr', KEYS[1]); " +
                            "else " +
                            "  redis.call('set', KEYS[1], 1); " +
                            "  redis.call('expire', KEYS[1], ARGV[1]); " +
                            "  return 1; " +
                            "end";
            RScript script = redissonClient.getScript(IntegerCodec.INSTANCE);
            Object countObj = script.eval(
                    RScript.Mode.READ_WRITE,
                    luaScript,
                    RScript.ReturnType.INTEGER,
                    Collections.singletonList(redisKey), expirationTimeInSeconds
            );
            return (long) countObj;
        }

        bucket.adder.increment();
        long localPending = bucket.adder.sum();
        long remoteCached = remoteCountCache.getOrDefault(redisKey, 0L);
        // 返回近似的当前窗口内总数（远端已刷新值 + 本地未刷新增量）
        return remoteCached + localPending;
    }

    @PostConstruct
    public void init() {
        if (aggregateEnabled) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "counter-aggregate-flusher");
                t.setDaemon(true);
                return t;
            });
            flushTaskFuture = scheduler.scheduleAtFixedRate(this::flushToRedisSafe, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
            log.info("CounterMessager 本地聚合已启用，flushIntervalMs={}ms, maxBuckets={}", flushIntervalMs, maxBuckets);
        } else {
            log.info("CounterMessager 本地聚合未启用，使用直写 Redis 模式");
        }
    }

    @PreDestroy
    public void destroy() {
        if (flushTaskFuture != null) {
            flushTaskFuture.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * 安全的批量刷新逻辑（捕获异常，避免任务中断）
     */
    private void flushToRedisSafe() {
        try {
            flushToRedis();
        } catch (Throwable t) {
            log.warn("CounterMessager 刷新到 Redis 异常", t);
        }
    }

    /**
     * 将本地桶的增量批量刷新到 Redis：INCRBY 并设置过期
     */
    private void flushToRedis() {
        if (localBuckets.isEmpty()) {
            return;
        }
        for (Map.Entry<String, LocalBucket> entry : localBuckets.entrySet()) {
            String redisKey = entry.getKey();
            LocalBucket bucket = entry.getValue();
            long delta = bucket.adder.sumThenReset();
            if (delta <= 0) {
                continue;
            }
            long ttlSeconds = bucket.expirationTimeInSeconds;

            String luaScript =
                    "if redis.call('exists', KEYS[1]) == 1 then " +
                            "  return redis.call('incrby', KEYS[1], ARGV[1]); " +
                            "else " +
                            "  redis.call('set', KEYS[1], ARGV[1]); " +
                            "  redis.call('expire', KEYS[1], ARGV[2]); " +
                            "  return ARGV[1]; " +
                            "end";

            RScript script = redissonClient.getScript(IntegerCodec.INSTANCE);
            Object countObj = script.eval(
                    RScript.Mode.READ_WRITE,
                    luaScript,
                    RScript.ReturnType.INTEGER,
                    Collections.singletonList(redisKey), delta, ttlSeconds
            );
            long newRemoteCount = ((Number) Objects.requireNonNull(countObj)).longValue();
            remoteCountCache.put(redisKey, newRemoteCount);
        }
    }

    /**
     * 主动触发一次刷新（可用于关键路径或关闭前）
     */
    public void flushNow() {
        if (aggregateEnabled) {
            flushToRedisSafe();
        }
    }

    /**
     * 本地桶结构
     */
    private static class LocalBucket {
        final LongAdder adder = new LongAdder();
        final long expirationTimeInSeconds;

        LocalBucket(long expirationTimeInSeconds) {
            this.expirationTimeInSeconds = expirationTimeInSeconds;
        }
    }
}














