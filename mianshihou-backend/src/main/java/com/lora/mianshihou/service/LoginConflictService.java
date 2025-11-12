package com.lora.mianshihou.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LoginConflictService {

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;
    // 记录用户设备登录的redis 前缀
    private static final String USER_DEVICE_KEY_PREFIX = "login:device:";

    // 记录用户登录冲突的redis前缀
    private static final String USER_CONFLICT_KEY_PREFIX = "login:conflict:";

    /**
     * 记住用户登录信息
     *
     * @param UserId
     * @param device
     * @param tokenValue
     */
    public void recordUserDevice(Long UserId, String device, String tokenValue) {
        String key = USER_DEVICE_KEY_PREFIX + UserId;
        // 存储设备信息和token，设置过期时间为7天
        redisTemplate.opsForValue().set(key, device + ":" + tokenValue, 7, TimeUnit.DAYS);
    }

    /**
     * 判断此时是否登录冲突
     *
     * @param UserID
     * @param device
     * @param tokenValue
     * @return
     */
    public boolean checkLoginConflict(Long UserID, String device, String tokenValue) {
        String key = USER_CONFLICT_KEY_PREFIX + UserID;
        Object deviceInfoObj = redisTemplate.opsForValue().get(key);

        if (deviceInfoObj != null) {
            String deviceInfo = deviceInfoObj.toString();
            String[] parts = deviceInfo.split(":");
            String savedDevice = parts[0];
            String savedToken = parts[1];
            // 同设备类型，不同token，表示有冲突
            if (device.equals(savedDevice) && !savedToken.equals(tokenValue)) {
                // 标记冲突状态
                markLoginConflict(UserID, tokenValue);
                return true;
            }

        }
        // 没有冲突，更新设备信息
        recordUserDevice(UserID, device, tokenValue);
        return false;
    }

    /**
     * 标记登录冲突
     * @param userId
     * @param newToken
     */
       private void markLoginConflict(Long userId, String newToken) {
        String key = USER_CONFLICT_KEY_PREFIX+userId;
        // 存储冲突状态是，设置过期时间为1小时
           redisTemplate.opsForValue().set(key,newToken,1,TimeUnit.HOURS);
       }

    /**
     *  检查是否处于登录状态
     * @param userId
     * @param tokenValue
     * @return
     */
       public boolean isInConflictState(Long userId,String tokenValue){
           String key = USER_CONFLICT_KEY_PREFIX+userId;
           Object conflictTokenObj = redisTemplate.opsForValue().get(key);

           if(conflictTokenObj == null){
               return false;
           }

           String conflictToken = conflictTokenObj.toString();
           // 如果冲突状态且不是当前token，则表示当前token处于冲突状态
           return !conflictToken.equals(tokenValue);
       }

    /**
     *  清洗用户冲突
     * @param userId
     */
    public void clearLoginConflict(Long userId) {
        String key = USER_CONFLICT_KEY_PREFIX+userId;
        redisTemplate.delete(key);
    }


}
