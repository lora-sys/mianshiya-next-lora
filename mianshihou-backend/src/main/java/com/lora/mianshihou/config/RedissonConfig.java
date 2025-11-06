package com.lora.mianshihou.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.api.RedissonClient;

/**
 * Redssion 配置
 *
 *
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    private String host;
    private Integer port;
    private Integer database;
    private String password;


    @Bean
    public RedissonClient redssionClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+host+":"+port)
                .setDatabase(database)
                .setPassword(password);
        return Redisson.create(config);

    }


}
