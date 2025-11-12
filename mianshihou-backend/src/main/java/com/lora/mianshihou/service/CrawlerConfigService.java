package com.lora.mianshihou.service;

import com.alibaba.boot.nacos.config.properties.NacosConfigProperties;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.lora.mianshihou.config.LocalCrawlerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Properties;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class CrawlerConfigService {

    @Autowired
    private LocalCrawlerConfig localConfig;

    @Autowired(required = false)
    private NacosConfigProperties nacosConfigProperties;

    private ConfigService configService;
    private boolean nacosAvailable = false;

    // 配置数据ID和分组
    private static final String DATA_ID = "crawler-config.yml";
    private static final String GROUP = "DEFAULT_GROUP";

    @PostConstruct
    public void init() {
        // 初始化本地默认配置
        localConfig.initDefaultConfig();

        // 尝试连接Nacos
        try {
            initNacosConfig();
            nacosAvailable = true;
            log.info("Nacos配置中心连接成功，使用远程配置");

            // 添加配置监听
            addConfigListener();

            // 立即获取一次配置
            refreshConfigFromNacos();
        } catch (Exception e) {
            nacosAvailable = false;
            log.warn("Nacos配置中心连接失败，使用本地配置: {}", e.getMessage());
        }
    }

    private void initNacosConfig() throws NacosException {
        if (nacosConfigProperties == null) {
            throw new RuntimeException("Nacos配置未初始化");
        }

        Properties properties = new Properties();
        properties.put("serverAddr", nacosConfigProperties.getServerAddr());
        properties.put("namespace", nacosConfigProperties.getNamespace());

        configService = NacosFactory.createConfigService(properties);
    }

    private void addConfigListener() throws NacosException {
        configService.addListener(DATA_ID, GROUP, new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("收到Nacos配置更新");
                // 解析并更新配置
                updateConfigFromNacos(configInfo);
            }
        });
    }

    private void refreshConfigFromNacos() {
        try {
            String config = configService.getConfig(DATA_ID, GROUP, 5000);
            if (config != null) {
                updateConfigFromNacos(config);
            }
        } catch (Exception e) {
            log.error("从Nacos获取配置失败", e);
            // 不抛出异常，继续使用本地配置
        }
    }

    private void updateConfigFromNacos(String configInfo) {
        try {
            // 简单解析YAML配置
            String[] lines = configInfo.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("max-requests-per-minute:")) {
                    int value = Integer.parseInt(line.substring(line.indexOf(":") + 1).trim());
                    localConfig.getCrawlerRules().put("max-requests-per-minute", value);
                } else if (line.startsWith("warning-threshold:")) {
                    int value = Integer.parseInt(line.substring(line.indexOf(":") + 1).trim());
                    localConfig.getCrawlerRules().put("warning-threshold", value);
                }
            }
            log.info("更新配置成功");
        } catch (Exception e) {
            log.error("解析Nacos配置失败", e);
        }
    }

    /**
     * 获取配置值，优先从Nacos获取，失败则使用本地配置
     */
    public String getConfigValue(String key, String defaultValue) {
        if (!nacosAvailable || !localConfig.isEnableLocalFallback()) {
            // Nacos不可用或未启用降级，直接返回默认值
            return defaultValue;
        }

        try {
            // 尝试从Nacos获取
            if (configService != null) {
                String config = configService.getConfig(DATA_ID, GROUP, 1000);
                if (config != null) {
                    // 解析配置获取指定key的值
                    return parseConfigValue(config, key);
                }
            }
        } catch (Exception e) {
            log.warn("从Nacos获取配置失败，使用本地配置: {}", e.getMessage());
        }

        // Nacos获取失败，使用本地配置
        return getLocalConfigValue(key, defaultValue);
    }

    private String parseConfigValue(String config, String key) {
        try {
            String[] lines = config.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith(key + ":")) {
                    return line.substring(line.indexOf(":") + 1).trim();
                }
            }
        } catch (Exception e) {
            log.error("解析YAML配置失败", e);
        }
        return null;
    }

    private String getLocalConfigValue(String key, String defaultValue) {
        // 从本地配置获取值
        Object value = localConfig.getCrawlerRules().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 检查Nacos是否可用
     */
    public boolean isNacosAvailable() {
        return nacosAvailable;
    }

    /**
     * 尝试重新连接Nacos
     */
    public void tryReconnectNacos() {
        if (!nacosAvailable) {
            try {
                initNacosConfig();
                nacosAvailable = true;
                log.info("重新连接Nacos成功");
                refreshConfigFromNacos();
            } catch (Exception e) {
                log.warn("重新连接Nacos失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取整数类型的配置值
     */
    public int getIntConfigValue(String key, int defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置值{}不是有效整数: {}, 使用默认值: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}