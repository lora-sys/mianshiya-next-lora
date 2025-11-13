package com.lora.mianshihou.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.lora.mianshihou.constant.UserConstant;
import com.lora.mianshihou.exception.BusinessException;
import com.lora.mianshihou.exception.LoginConflictException;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.service.LoginConflictService;
import com.lora.mianshihou.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * Sa-Token 全局拦截器（为了支持权限注解）
 */
@Configuration
@Slf4j
public class SaTokenConfigure implements WebMvcConfigurer {

    @Resource
    private UserService userService;

    @Resource
    private LoginConflictService loginConflictService;

    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 获取请求信息
            String requestPath = null;
            String clientIp = "unknown";
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = requestAttributes.getRequest();
            try {
                requestPath = request.getRequestURI();
                clientIp = getClientIp(request);
            } catch (Exception e) {
                log.warn("获取请求信息失败: {}", e.getMessage());
                return;
            }

            // 排除公开接口路径
            if (isPublicPath(requestPath)) {
                return;
            }

            // 检查是否已登录
            if (!StpUtil.isLogin()) {
                return;
            }

            // 获取当前登录用户ID
            Object loginId = StpUtil.getLoginId();
            if (loginId == null) {
                return;
            }

            try {
                // 从session中获取用户信息
                User user = (User) StpUtil.getSession().get(UserConstant.USER_LOGIN_STATE);

                if (user == null) {
                    // 如果session中没有用户信息，从数据库重新获取
                    user = userService.getById(Long.parseLong(loginId.toString()));
                    if (user != null) {
                        // 更新session中的用户信息
                        StpUtil.getSession().set(UserConstant.USER_LOGIN_STATE, user);
                    }
                }

                // 检查用户封号状态
                if (user != null && UserConstant.BAN_ROLE.equals(user.getUserRole())) {
                    log.warn("被封号用户尝试访问接口: userId={}, userRole={}, path={}, ip={}",
                            loginId, user.getUserRole(), requestPath, clientIp);

                    // 强制踢下线
                    StpUtil.kickout(loginId);

                    throw new BusinessException(403, "账号已被封禁，禁止访问");
                }

                if (request != null) {
                          // 检查登录冲突状态
                        // ✅ 修复：只检查冲突状态，不做冲突检测 ，提升性能
                        String tokenValue = StpUtil.getTokenValue();
                        if (tokenValue != null && loginConflictService.isInConflictState(Long.parseLong(loginId.toString()), tokenValue)) {
                            log.warn("用户登录冲突: userId={}, token={}, path={}", loginId, tokenValue.substring(0, 8) + "***", requestPath);
                            // 踢掉当前冲突的token
                            StpUtil.kickoutByTokenValue(tokenValue);
                            throw new LoginConflictException("您的账号在其他设备上登录，已被强制下线");
                        }
                }


            } catch (BusinessException e) {
                // 重新抛出业务异常
                throw e;
            } catch (Exception e) {
                // 其他异常记录日志，但不影响正常流程
                log.error("封号检查异常: userId={}, path={}, error={}", loginId, requestPath, e.getMessage(), e);
            }

        })).addPathPatterns("/**");
    }

    /**
     * 判断是否为公开路径（不需要登录即可访问）
     */
    private boolean isPublicPath(String path) {
        if (path == null) {
            return false;
        }
        // 登录注册相关
        if (path.contains("/user/login") || path.contains("/user/register")) {
            return true;
        }
        // 微信相关
        if (path.contains("/wx")) {
            return true;
        }
        // 静态资源
        if (path.contains("/static/") || path.contains("/public/")) {
            return true;
        }
        // Swagger文档
        if (path.contains("/doc.html") || path.contains("/swagger") || path.contains("/v3/api-docs")) {
            return true;
        }
        return false;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            log.warn("获取客户端IP失败: {}", e.getMessage());
        }
        return "unknown";
    }
}