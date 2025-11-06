package com.lora.mianshihou.constant;

/**
 *
 * Redis 常量
 */
public interface RedissonConstant {


    /**
     * 用户签到记录的 Redis key 常量
     *
     */
    String USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins";


    /**
     *
     * 获取用户签到记录的 Redis key
     *
     * @param year  年份
     * @param userId  用户id
     * @return  拼接好的redis key
     */
    static String getUSER_SIGN_IN_REDIS_KEY_PREFIX(int year, long userId) {
        return String.format("%s:%s:%S", USER_SIGN_IN_REDIS_KEY_PREFIX, year, userId);
    }
}
