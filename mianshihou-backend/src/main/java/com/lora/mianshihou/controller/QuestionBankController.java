package com.lora.mianshihou.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.lora.mianshihou.annotation.AuthCheck;
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
import com.lora.mianshihou.model.entity.QuestionBankQuestion;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.model.vo.QuestionBankQuestionVO;
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
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
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
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
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
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
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
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionBankVO> getQuestionBankVOById(QuestionBankQueryRequest questionbankqueryrequest, HttpServletRequest request) {

        ThrowUtils.throwIf(questionbankqueryrequest == null, ErrorCode.PARAMS_ERROR);
        Long id = questionbankqueryrequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 生成一个 key
        String key = "bank_detail_" + id;
        String lockKey = "lock:" + key;  // 互斥锁防止击穿，防止单个热点key失效，大量并发请求这个key
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
//
//
//
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

        try {
            // 判断是不是热key
            if (JdHotKeyStore.isHotKey(key)) {
                // 从本地缓存获取值
                Object cacheBank = JdHotKeyStore.get(key);
                if (cacheBank != null) {
                    System.out.println("命中hotkey缓存");
                    return ResultUtils.success(new QuestionBankVO());
                }
            }

            // 使用redis 分布式缓存，查询
            Object redisCache = redisTemplate.opsForValue().get(key);
            if (redisCache != null) {
                // 优化方案。判断是不是热 key ，是了回退到hotkey缓存
                if (JdHotKeyStore.isHotKey(key)) {
                    JdHotKeyStore.smartSet(key, redisCache);
                }
                return ResultUtils.success(new QuestionBankVO());
            }

            // 使用互斥锁
            boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            final int LOCK_WAIT_TIME = 20; // 20ms
            final int MAX_RETRY_COUNT = 2; // 最多重试2次
            if (!locked) {
                // 如果没有拿到锁，可以先重试或者递归重新来
                // 超过最大重试次数，降级处理
                Thread.sleep(LOCK_WAIT_TIME);
                return getQuestionBankVOById(questionbankqueryrequest, request);
            }

            // 双重缓存检查，防止等待锁加载期间，数据已经被别的线程获取了
            try {


                Object doubleCheck = redisTemplate.opsForValue().get(key);
                if (doubleCheck != null) {
                    return ResultUtils.success((QuestionBankVO) doubleCheck);
                }
                //查询数据库
                System.out.println("🚀 线程 " + Thread.currentThread().getName() + " 获取到锁，查询数据库");
                QuestionBank questionBank = questionBankService.getById(id);
                ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);
                //查询题库封装类
                QuestionBankVO questionBankVO = questionBankService.getQuestionBankVO(questionBank, request);
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
                    questionBankVO.setQuestionPage(questionVoPage);
                }


                // 设置多级缓存
                // redis缓存 (随机过期时间)
                long timeout = 30 * 60 + ThreadLocalRandom.current().nextInt(0, 300);
                redisTemplate.opsForValue().set(key, questionBankVO, timeout, TimeUnit.SECONDS);

                // hotkey 缓存
                JdHotKeyStore.smartSet(key, questionBankVO);
                System.out.println("✅ 数据加载完成并设置缓存");
                return ResultUtils.success(questionBankVO);


            } finally {
                redisTemplate.delete(lockKey);
            }


        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new RuntimeException("查询中断", e);

        } catch (Exception e) {

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


    }

    /**
     * 分页获取题库列表（仅管理员可用）
     *
     * @param questionBankQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
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
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
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
