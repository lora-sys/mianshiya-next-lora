package com.lora.mianshihou.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 创建本地快照，最为一个默认的配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "crawler.local-config")
public class LocalCrawlerConfig {
    // 本地开关控制
    private boolean enableLocalFallback = true;

    // 本地爬虫规则配置
    private Map<String, Object> crawlerRules = new HashMap<>();

    // 默认配置
    public void initDefaultConfig() {
        crawlerRules.put("maxRequestsPerMinute", 20);
        crawlerRules.put("warningThreshold", 10);
        crawlerRules.put("blockDuration", 3600); // 1小时
        crawlerRules.put("enableDFA", true);
        crawlerRules.put("enableBloomFilter", true);
        crawlerRules.put("sensitiveWords", "爬虫,抓取,采集,scrapy,crawler");
        crawlerRules.put("ipWhitelist", "127.0.0.1,localhost");
        crawlerRules.put("userAgentBlacklist", "python-requests,scrapy,bot,crawler");
    }
}
