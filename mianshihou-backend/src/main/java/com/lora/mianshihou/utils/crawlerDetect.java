package com.lora.mianshihou.utils;


import cn.dev33.satoken.stp.StpUtil;
import com.lora.mianshihou.annotation.DistributedLock;
import com.lora.mianshihou.common.ErrorCode;
import com.lora.mianshihou.exception.BusinessException;
import com.lora.mianshihou.manager.CounterMessager;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.service.CrawlerConfigService;
import com.lora.mianshihou.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 反爬虫机制
 */
@Component
public class crawlerDetect {
    @Resource
    private CounterMessager counterMessager;
    @Resource
    private UserService userService;
    @Resource
    private CrawlerConfigService crawlerConfigService;

    @DistributedLock(key = "crawler:detect:#loginUserId", leaseTime = 5000)
    public void crawlerCounterDetect(long loginUserId) {
        // 从配置服务获取警告和封号阈值，默认值为10和20
        final int WARN_COUNT = crawlerConfigService.getIntConfigValue("warning-threshold", 10);
        final int BAN_COUNT = crawlerConfigService.getIntConfigValue("max-requests-per-minute", 20);

        // 拼接访问key
        String key = String.format("user:access:%s",loginUserId);
        // 统计一分钟内访问次数，180秒过期
        long count = counterMessager.incrAndGetCounter(key,1, TimeUnit.MINUTES,180);

        // 是否封号
        if(count > BAN_COUNT){
            //踢下线
            StpUtil.kickout(loginUserId);
            // 封号
            User user = new User();
            user.setId(loginUserId);
            user.setUserRole("ban");
            userService.updateById(user);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"访问次数太多，已经封号");
        }

        //警告
        if(count > WARN_COUNT){
            // 改成管理员发送邮件或者信息
            throw new BusinessException(110,"警告:访问次数过多");
        }
    }
}