
package com.lora.mianshihou.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LoginConflictService {

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    private static final String USER_DEVICE_KEY_PREFIX = "login:device:";
    private static final String USER_CONFLICT_KEY_PREFIX = "login:conflict:";

    /**
     * 登录冲突检测 - 事务版本
     */
    @Transactional
    public boolean checkLoginConflictWithTransaction(Long userId, String device, String newToken) {
        String deviceKey = USER_DEVICE_KEY_PREFIX + userId;

        // 使用 Redis 事务
        return redisTemplate.execute(new SessionCallback<Boolean>() {
            @Override
            public Boolean execute(RedisOperations operations) throws DataAccessException {
                operations.multi(); // 开启事务

                try {
                    // 1. 查询当前设备信息
                    Object oldDeviceInfo = operations.opsForValue().get(deviceKey);

                    if (oldDeviceInfo != null) {
                        String deviceInfo = oldDeviceInfo.toString();
                        String[] parts = deviceInfo.split(":");
                        if (parts.length >= 2) {
                            String oldDevice = parts[0];
                            String oldToken = parts[1];

                            // 同设备类型 = 冲突
                            if (device.equals(oldDevice) && !oldToken.equals(newToken)) {
                                // 2. 标记旧token冲突
                                String conflictKey = USER_CONFLICT_KEY_PREFIX + oldToken;
                                operations.opsForValue().set(conflictKey, "conflict", 1, TimeUnit.HOURS);

                                // 3. 更新新设备信息
                                operations.opsForValue().set(deviceKey, device + ":" + newToken, 7, TimeUnit.DAYS);

                                // 4. 踢下线旧token（如果集成Sa-Token）
                                try {
                                    cn.dev33.satoken.stp.StpUtil.kickoutByTokenValue(oldToken);
                                } catch (Exception e) {
                                    log.warn("踢掉旧token失败: token={}", oldToken.substring(0, 8) + "***", e);
                                }

                                operations.exec(); // 执行事务
                                log.warn("登录冲突处理完成: userId={}, oldToken={}, newToken={}",
                                        userId, oldToken.substring(0, 8) + "***", newToken.substring(0, 8) + "***");
                                return true;
                            }
                        }
                    }

                    // 没有冲突，直接更新设备信息
                    operations.opsForValue().set(deviceKey, device + ":" + newToken, 7, TimeUnit.DAYS);
                    operations.exec(); // 执行事务
                    return false;

                } catch (Exception e) {
                    operations.discard(); // 回滚事务
                    log.error("登录冲突检测事务失败: userId={}, error={}", userId, e.getMessage(), e);
                    throw e;
                }
            }
        });
    }

    /**
     * 用户注销 - 清理所有相关数据
     */
    @Transactional
    public void logoutWithTransaction(Long userId, String tokenValue) {
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();

                try {
                    // 1. 删除设备信息
                    String deviceKey = USER_DEVICE_KEY_PREFIX + userId;
                    operations.delete(deviceKey);

                    // 2. 删除冲突状态
                    String conflictKey = USER_CONFLICT_KEY_PREFIX + tokenValue;
                    operations.delete(conflictKey);

                    operations.exec();
                    log.info("用户注销清理完成: userId={}", userId);
                    return null;

                } catch (Exception e) {
                    operations.discard();
                    log.error("注销清理事务失败: userId={}, error={}", userId, e.getMessage(), e);
                    throw e;
                }
            }
        });
    }

    /**
     * 批量清理过期冲突状态 - 定时任务用
     */
    @Scheduled(fixedRate = 300000) // 5分钟执行一次
    public void cleanExpiredConflictStates() {
        try {
            // 使用 SCAN 命令批量清理过期的冲突key
            Set<Object> conflictKeys = redisTemplate.keys(USER_CONFLICT_KEY_PREFIX + "*");
            if (conflictKeys != null && !conflictKeys.isEmpty()) {
                redisTemplate.delete(conflictKeys);
                log.info("清理过期冲突状态: count={}", conflictKeys.size());
            }
        } catch (Exception e) {
            log.error("清理过期冲突状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理用户登录冲突 - 事务版本 基于redis rock traction SessionCallback
     */
    @Transactional
    public void clearLoginConflictWithTransaction(Long userId, String tokenValue) {
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi(); // 开启事务

                try {
                    // 1. 删除设备信息
                    String deviceKey = USER_DEVICE_KEY_PREFIX + userId;
                    operations.delete(deviceKey);

                    // 2. 删除基于token的冲突状态
                    if (tokenValue != null) {
                        String conflictKey = USER_CONFLICT_KEY_PREFIX + tokenValue;
                        operations.delete(conflictKey);
                    }

                    // 3. 删除基于userId的冲突状态(兼容老数据)
                    String userConflictKey = USER_CONFLICT_KEY_PREFIX + userId;
                    operations.delete(userConflictKey);

                    operations.exec(); // 执行事务
                    log.info("清理用户冲突状态完成: userId={}", userId);
                    return null;

                } catch (Exception e) {
                    operations.discard(); // 回滚事务
                    log.error("清理冲突状态事务失败: userId={}, error={}", userId, e.getMessage(), e);
                    throw e;
                }
            }
        });
    }

    /**
     * 检查当前token是否处于冲突状态 - 非事务版本（性能考虑）
     * @param userId 用户ID
     * @param tokenValue 当前token
     * @return true=冲突状态, false=正常状态
     */
    public boolean isInConflictState(Long userId, String tokenValue) {
        if (tokenValue == null) {
            return false;
        }

        // 用token作为key检查冲突状态
        String conflictKey = USER_CONFLICT_KEY_PREFIX + tokenValue;
        return Boolean.TRUE.equals(redisTemplate.hasKey(conflictKey));
    }
}