package com.lora.mianshihou.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.lora.mianshihou.annotation.AuthCheck;
import com.lora.mianshihou.annotation.MultiLevelCache;
import com.lora.mianshihou.common.BaseResponse;
import com.lora.mianshihou.common.DeleteRequest;
import com.lora.mianshihou.common.ErrorCode;
import com.lora.mianshihou.common.ResultUtils;
import com.lora.mianshihou.constant.UserConstant;
import com.lora.mianshihou.exception.BusinessException;
import com.lora.mianshihou.exception.ThrowUtils;
import com.lora.mianshihou.model.dto.question.QuestionQueryRequest;
import com.lora.mianshihou.model.dto.questionBank.QuestionBankAddRequest;
import com.lora.mianshihou.model.dto.questionBank.QuestionBankEditRequest;
import com.lora.mianshihou.model.dto.questionBank.QuestionBankQueryRequest;
import com.lora.mianshihou.model.dto.questionBank.QuestionBankUpdateRequest;
import com.lora.mianshihou.model.entity.Question;
import com.lora.mianshihou.model.entity.QuestionBank;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.model.vo.QuestionBankVO;
import com.lora.mianshihou.model.vo.QuestionVO;
import com.lora.mianshihou.service.QuestionBankService;
import com.lora.mianshihou.service.QuestionService;
import com.lora.mianshihou.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
/**
 * 题库接口
 *
 * @author lora
 *
 */
@RestController
@RequestMapping("/questionBank")
@Slf4j
public class QuestionBankController {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private UserService userService;


    @Resource
    QuestionService questionService;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    // region 增删改查

    /**
     * 创建题库
     *
     * @param questionBankAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
   @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestionBank(@RequestBody QuestionBankAddRequest questionBankAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankAddRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        questionBank.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionBankService.save(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionBankId = questionBank.getId();
        return ResultUtils.success(newQuestionBankId);
    }

    /**
     * 删除题库
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
@SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestionBank(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestionBank.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题库（仅管理员可用）
     *
     * @param questionBankUpdateRequest
     * @return
     */
    @PostMapping("/update")
@SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestionBank(@RequestBody QuestionBankUpdateRequest questionBankUpdateRequest) {
        if (questionBankUpdateRequest == null || questionBankUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankUpdateRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);
        // 判断是否存在
        long id = questionBankUpdateRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题库（封装类）
     *
     * @param questionbankqueryrequest
     * @return
     * 热点key检测
     * 本地缓存获取
     * Redis缓存查询
     * 分布式锁
     * 双重检查
     * 缓存回填
     * 随机TTL设置
     */
    @GetMapping("/get/vo")
  @MultiLevelCache(value="bank_detail",key="#id",expire = 1800,longExpire = 10)
    public BaseResponse<QuestionBankVO> getQuestionBankVOById(QuestionBankQueryRequest questionbankqueryrequest, HttpServletRequest request) {
        //多级缓存架构：Hotkey(本地) → Redis(分布式) → Database
        //防雪崩：Redis缓存设置随机过期时间
        //防击穿：使用分布式锁保护数据库
        //性能优先：热点数据优先从本地缓存返回
        //自动回填：Redis中的数据自动回填到Hotkey
        ThrowUtils.throwIf(questionbankqueryrequest == null, ErrorCode.PARAMS_ERROR);
        Long id = questionbankqueryrequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
//        // 如果是热key
//        if (JdHotKeyStore.isHotKey(key)) {
//            // 从本地缓存获取缓存值
//            Object cacheQuestionBankVO = JdHotKeyStore.get(key);
//            if (cacheQuestionBankVO != null) {
//                // 如果本地缓存已经有值。直接返回查询的值
//                System.out.println("命中热key缓存: " + key);
//                return ResultUtils.success((QuestionBankVO) cacheQuestionBankVO);
//
//            } else {
//                // 这里可以添加等待或重试逻辑，或者直接走数据库
//                System.out.println("热key识别但缓存为空，可能存在推送延迟: " + key);
//                // 继续执行数据库查询
//            }
//        }
//        //查询redis，使用hotkey获取redis 的分布式缓存，先redis，再数据库
//        // 查询数据库
//        QuestionBank questionBank = questionBankService.getById(id);
//        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);
//        //查询题库封装类
//        QuestionBankVO questionBankV0 = questionBankService.getQuestionBankVO(questionBank, request);
//        //是否要关联查询题库下的题目列表
//        boolean needQuestionQueryList = questionbankqueryrequest.isNeedQueryQuestionList();
//        if (needQuestionQueryList) {
//            QuestionQueryRequest questionQueryRequest = new QuestionQueryRequest();
//            questionQueryRequest.setQuestionBankId(id);
//            //可以按需要支持更多的题目搜索参数，比如分页,
//            questionQueryRequest.setPageSize(questionbankqueryrequest.getPageSize());
//            questionQueryRequest.setCurrent(questionbankqueryrequest.getCurrent());
//            Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
//            Page<QuestionVO> questionVoPage = questionService.getQuestionVOPage(questionPage, request);
//            questionBankV0.setQuestionPage(questionVoPage);
//        }
//        //设置本地缓存(如果不是热key，这个方法不会设置热key)
//        JdHotKeyStore.smartSet(key, questionBankV0);
//        // 获取封装类
//        return ResultUtils.success(questionBankV0);



            // 降级策略,使用数据库查询
            QuestionBank questionBank = questionBankService.getById(id);
            ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);
            //查询题库封装类
            QuestionBankVO questionBankV0 = questionBankService.getQuestionBankVO(questionBank, request);
            //是否要关联查询题库下的题目列表
            boolean needQuestionQueryList = questionbankqueryrequest.isNeedQueryQuestionList();
            if (needQuestionQueryList) {
                QuestionQueryRequest questionQueryRequest = new QuestionQueryRequest();
                questionQueryRequest.setQuestionBankId(id);
                //可以按需要支持更多的题目搜索参数，比如分页,
                questionQueryRequest.setPageSize(questionbankqueryrequest.getPageSize());
                questionQueryRequest.setCurrent(questionbankqueryrequest.getCurrent());
                Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
                Page<QuestionVO> questionVoPage = questionService.getQuestionVOPage(questionPage, request);
                questionBankV0.setQuestionPage(questionVoPage);
            }

            return ResultUtils.success(questionBankV0);



    }

    /**
     * 分页获取题库列表（仅管理员可用）
     *
     * @param questionBankQueryRequest
     * @return
     */
    @PostMapping("/list/page")
@SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuestionBank>> listQuestionBankByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest) {
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        return ResultUtils.success(questionBankPage);
    }

    /**
     * 分页获取题库列表（封装类）
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    @SentinelResource(value = "listQuestionBankVOByPage", blockHandler = "handleBlockException",fallback="handleFallback")
    public BaseResponse<Page<QuestionBankVO>> listQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                       HttpServletRequest request) {
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
    }

    /**
     * listQuestionBankVOByPage 流控操作（此处为了方便演示，写在同一个类中）
     * 限流：提示“系统压力过大，请耐心等待”
     *
     */
    public BaseResponse<Page<QuestionBankVO>> handleBlockException(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                   HttpServletRequest request, BlockException ex) {
        // 降级操作
        if (ex instanceof DegradeException) {
            return handleFallback(questionBankQueryRequest, request, ex);
        }
        // 限流操作
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统压力过大，请耐心等待");
    }

    /**
     * listQuestionBankVOByPage 降级操作：直接返回本地数据（此处为了方便演示，写在同一个类中）
     */
    public BaseResponse<Page<QuestionBankVO>> handleFallback(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                             HttpServletRequest request, Throwable ex) {
        // 可以返回本地数据或空数据
        return ResultUtils.success(null);
    }


    /**
     * 分页获取当前登录用户创建的题库列表
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionBankVO>> listMyQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionBankQueryRequest.setUserId(loginUser.getId());
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
    }

    /**
     * 编辑题库（给用户使用）
     *
     * @param questionBankEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
@SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> editQuestionBank(@RequestBody QuestionBankEditRequest questionBankEditRequest, HttpServletRequest request) {
        if (questionBankEditRequest == null || questionBankEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankEditRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionBankEditRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestionBank.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
