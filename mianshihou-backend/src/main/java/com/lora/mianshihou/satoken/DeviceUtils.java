package com.lora.mianshihou.satoken;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.lora.mianshihou.common.ErrorCode;
import com.lora.mianshihou.exception.ThrowUtils;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * 设备工具类方便同端互斥检测
 */

public class DeviceUtils
{

    /**
     * 根据请求获取设备信息
     */
    public static String getRequestDevice(HttpServletRequest request){
        String userAgentStr = request.getHeader(Header.USER_AGENT.toString());
        // 使用hutool 解析 useragent
        UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
        ThrowUtils.throwIf(userAgent==null, ErrorCode.OPERATION_ERROR,"非法请求");
        // 默认设备值是pc
        String Device = "pc";
        // 是否是小程序
        if(isMiniProgram(userAgentStr)){
            Device="miniProgram";
        } else if (isPad(userAgentStr)){
            // 是否是pad
            Device="pad";
        } else if (userAgent.isMobile()){
            // 是否是手机
            Device="mobile";
        }
        // 返回默认值
        return Device;
    }


    /***
     * 判断是否是小程序
     */
    private static boolean isMiniProgram(String userAgent){
        // 判断user-Agent 中是否包含"MicroMessenger" 表示是微信环境
        return StrUtil.containsIgnoreCase(userAgent, "MiniProgram")
        && StrUtil.containsIgnoreCase(userAgent,"MicroMessenger");
    }

    /**
     * 判断是否为平板设备
     * 支持 iOS（如 iPad）和 Android 平板的检测
     **/
    private static boolean isPad(String userAgentStr) {
        // 检查 iPad 的 User-Agent 标志
        boolean isIpad = StrUtil.containsIgnoreCase(userAgentStr, "iPad");

        // 检查 Android 平板（包含 "Android" 且不包含 "Mobile"）
        boolean isAndroidTablet = StrUtil.containsIgnoreCase(userAgentStr, "Android")
                && !StrUtil.containsIgnoreCase(userAgentStr, "Mobile");

        // 如果是 iPad 或 Android 平板，则返回 true
        return isIpad || isAndroidTablet;
    }

}
