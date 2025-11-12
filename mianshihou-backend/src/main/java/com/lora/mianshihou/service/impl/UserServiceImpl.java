package com.lora.mianshihou.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lora.mianshihou.common.ErrorCode;
import com.lora.mianshihou.constant.CommonConstant;
import com.lora.mianshihou.constant.RedissonConstant;
import com.lora.mianshihou.exception.BusinessException;
import com.lora.mianshihou.mapper.UserMapper;
import com.lora.mianshihou.model.dto.user.UserQueryRequest;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.model.enums.UserRoleEnum;
import com.lora.mianshihou.model.vo.LoginUserVO;
import com.lora.mianshihou.model.vo.UserVO;
import com.lora.mianshihou.satoken.DeviceUtils;
import com.lora.mianshihou.service.LoginConflictService;
import com.lora.mianshihou.service.UserService;
import com.lora.mianshihou.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.lora.mianshihou.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现
 *
 * @author lora
 *
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "lora";
    @Autowired
    private LoginConflictService loginConflictService;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
//        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        //使用sa-token登录,并指定设备，避免同端互斥
        StpUtil.login(user.getId(), DeviceUtils.getRequestDevice(request));
        String device = DeviceUtils.getRequestDevice(request);
        String tokenValue = StpUtil.getTokenValue();
        loginConflictService.recordUserDevice(user.getId(), device, tokenValue);
        StpUtil.getSession().set(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();
        // 单机锁
        synchronized (unionId.intern()) {
            // 查询用户是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("unionId", unionId);
            User user = this.getOne(queryWrapper);
            // 被封号，禁止登录
            if (user != null && UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
            }
            // 用户不存在则创建
            if (user == null) {
                user = new User();
                user.setUnionId(unionId);
                user.setMpOpenId(mpOpenId);
                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
                user.setUserName(wxOAuth2UserInfo.getNickname());
                boolean result = this.save(user);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
                }
            }
            // 记录用户的登录态
//            request.getSession().setAttribute(USER_LOGIN_STATE, user);
            StpUtil.login(user.getId(), DeviceUtils.getRequestDevice(request));
            StpUtil.getSession().set(USER_LOGIN_STATE, user);
            String device = DeviceUtils.getRequestDevice(request);
            String tokenValue = StpUtil.getTokenValue();
            loginConflictService.recordUserDevice(user.getId(), device, tokenValue);
            return getLoginUserVO(user);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录, 基于sa-token实现
        Object loginUserId = StpUtil.getLoginIdDefaultNull();
        if (loginUserId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }


//        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
//        User currentUser = (User) userObj;
//        if (currentUser == null || currentUser.getId() == null) {
//            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
//        }
//        // 从数据库查询（追求性能的话可以注释，直接走缓存）
//        long userId = currentUser.getId();
        User currentUser = this.getById((String) loginUserId);
//        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录 , 基于sa-token实现
        Object loginUserId = StpUtil.getLoginIdDefaultNull();
        if (loginUserId == null) {
            return null;
        }
//        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
//        User currentUser = (User) userObj;
//        if (currentUser == null || currentUser.getId() == null) {
//            return null;
//        }
//        // 从数据库查询（追求性能的话可以注释，直接走缓存）
//        long userId = currentUser.getId();
//        return this.getById(userId);
        return this.getById((String) loginUserId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
//        // 仅管理员可查询
//        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
//        User user = (User) userObj;
//        return isAdmin(user);
        Object userObj = StpUtil.getSession().get(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }


    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
//        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
//            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
//        }
        StpUtil.checkLogin();
        // 移除登录态
        StpUtil.logout();
//        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        if (StpUtil.isLogin()) {
            loginUserVO.setToken(StpUtil.getTokenValue());
        }
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     *
     * 添加用户id签到记录
     *
     * @param userId
     * @return 当前用户是否已经签到成功
     */
    @Override
    public boolean addUserSignIn(long userId) {
        LocalDate date = LocalDate.now();
        String key = RedissonConstant.getUSER_SIGN_IN_REDIS_KEY_PREFIX(date.getYear(), userId);
        //获取redis的bitmap
        RBitSet signInBitSet = redissonClient.getBitSet(key);
        //获取当前日期的式一年的第几天，，然后作为偏移量（从1开始计数）
        int offset = date.getDayOfYear();
        //查询当前天有没有签到
        if (!signInBitSet.get(offset)) {
            //如果当天没用签到，则设置
            signInBitSet.set(offset, true);
        }
        //当前天已经签到
        return true;
    }

    /**
     * 查询用户某个年份的签到记录
     *
     * @param userId
     * @param year
     * @return
     */
    @Override
    public List<Integer> getUserSignInRecord(long userId, Integer year) {
        if (year == null) {
            LocalDate date = LocalDate.now();
            year = date.getYear();
        }
        String key = RedissonConstant.getUSER_SIGN_IN_REDIS_KEY_PREFIX(year, userId);
        //获取redis的bitmap

        RBitSet signInBitSet = redissonClient.getBitSet(key);

        //性能优化 ，加载Bitset到内存，避免后端读取时，发送多次请求
        BitSet bitSet = signInBitSet.asBitSet();
        //构造返回结果
//        Map<LocalDate, Boolean> result = new LinkedHashMap<>();
        //统计签到的日期列表
        List<Integer> dayList = new ArrayList<>();
        //获取当前年份的总天数
        int totalDays = Year.of(year).length();
        //从索引0开始查找下一个被设置为1的位
        int index = bitSet.nextSetBit(0);
        while (index >= 0) {
            dayList.add(index);
            //查找下一个被设置为1的位
            index = bitSet.nextSetBit(index + 1);
        }
//        //获取每天的签到状态
//        for (int dayOfYear = 1; dayOfYear <= totalDays; dayOfYear++) {
//
//            //获取value:当天是否有刷题
//            boolean hasRecord = bitSet.get(dayOfYear);
//           //结果放入map
//            result.put(currentDate, hasRecord);
//
//            if( hasRecord) {
//                //如果当前天签到了
//                dayList.add(dayOfYear);
//            }
//        }
        return dayList;
    }
    //返回值优化，计算时间耗时，传输数据多，效率低
}
