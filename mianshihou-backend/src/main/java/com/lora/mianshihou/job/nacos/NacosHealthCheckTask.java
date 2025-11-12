
package com.lora.mianshihou.job.nacos;


import com.lora.mianshihou.service.CrawlerConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NacosHealthCheckTask {

    @Autowired
    private CrawlerConfigService configService;

    // 每5分钟检查一次Nacos连接
    @Scheduled(fixedRate = 300000)
    public void checkNacosHealth() {
        if (!configService.isNacosAvailable()) {
            configService.tryReconnectNacos();
        }
    }
}
