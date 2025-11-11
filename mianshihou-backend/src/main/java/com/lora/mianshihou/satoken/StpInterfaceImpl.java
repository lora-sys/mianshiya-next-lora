package com.lora.mianshihou.satoken;


import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.lora.mianshihou.model.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.lora.mianshihou.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */

@Component      //保证此类被springboot扫描，完成satoken的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {
    /**
     * 返回一个账号拥有的权限码集合 (目前没用)
     *
     * @param LoginId
     * @param loginType
     * @return
     */
    @Override
    public List<String> getPermissionList(Object LoginId, String loginType) {
        return new ArrayList<>();
    }
    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object LoginId, String loginType) {
        //从当前登录的用户获取信息

        User user = (User) StpUtil.getSessionByLoginId(LoginId).get(USER_LOGIN_STATE);
        return Collections.singletonList(user.getUserRole());
    }
}
