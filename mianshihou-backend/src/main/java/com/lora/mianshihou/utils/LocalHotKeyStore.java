package com.lora.mianshihou.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 模拟caffine 实际需要实现 访问频率统计： 在应用服务器端或专门的代理层，实时统计每个 Key 的访问频率（QPS）。
 * 阈值判断： 当某个 Key 的访问频率超过预设阈值时，系统将其标记为“热键”。
 * 主动推送/拉取： 标记为热键后，信息会被推送到一个中心服务，然后由所有应用服务器的客户端（例如 Jd 的 hotkey-client）拉取这个列表，并将其主动加载到本地缓存中。
 *
 * @author lora
 *
 */
@Aspect
@Component
@Slf4j
public class LocalHotKeyStore {
    // caffine 缓存机制
    private final Cache<String, Object> hotKeyCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.SECONDS) // 本地缓存时间5分钟
            .build();

    public Object get(String key) {
        return hotKeyCache.getIfPresent(key);
    }

    public void smarSet(String key, Object value) {
        // 实际场景，可以通过其他机制判断是否为热键，这里简化为直接存放
        hotKeyCache.put(key, value);
    }

    // 简化is hotkey检测，只要在本地缓存就认为是热键
    public boolean isHotKey(String key) {
        return hotKeyCache.getIfPresent(key) != null;
    }
}

