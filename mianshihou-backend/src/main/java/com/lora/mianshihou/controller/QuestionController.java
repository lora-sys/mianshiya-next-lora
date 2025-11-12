package com.lora.mianshihou.controller;


import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
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
import com.lora.mianshihou.model.dto.question.*;
import com.lora.mianshihou.model.entity.Question;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.model.vo.QuestionVO;
import com.lora.mianshihou.service.ElasticSearchService;
import com.lora.mianshihou.service.QuestionBankQuestionService;
import com.lora.mianshihou.service.QuestionService;
import com.lora.mianshihou.service.UserService;
import com.lora.mianshihou.utils.crawlerDetect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 题目接口
 *
 * @author lora
 *
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {


    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;
    @Autowired
    private crawlerDetect crawlerDetect;

    @Resource
    private ElasticSearchService elasticSearchService;

    // region 增删改查

    /**
     * 创建题目
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 数据校验
        questionService.validQuestion(question, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        // 手动处理 tags 字段的类型转换，从 List<String> 转为 JSON 字符串
        if (questionUpdateRequest.getTags() != null) {
            question.setTags(JSONUtil.toJsonStr(questionUpdateRequest.getTags()));
        }
        // 数据校验
        questionService.validQuestion(question, false);
        // 判断是否存在
        long id = questionUpdateRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题目（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 进行反爬虫校验，此处可是检查登录状态设置观看权限
        // 检查用户是否登录
        User loginUser = userService.getLoginUserPermitNull(request);

        if (loginUser == null) {
            Question question = questionService.getById(id);
            ThrowUtils.throwIf(question == null, ErrorCode.NOT_LOGIN_ERROR);
            QuestionVO questionVO = questionService.getQuestionVO(question, request);
            questionVO.setAnswer("未登录,没有观看权限");
            return ResultUtils.success(questionVO);

        }
        if (loginUser != null) {
            crawlerDetect.crawlerCounterDetect(loginUser.getId());
        }
//        //使用hotkey 作为热点的探测
//        //生成一个key
//        String key = "question_detail_" + id;
//
//        //如果是一个热 key
//        if (JdHotKeyStore.isHotKey(key)) {
//            //从缓存获取
//            Object cacheQuestionVO = JdHotKeyStore.get(key);
//            if (cacheQuestionVO != null) {
//                //如果本地缓存已经有值，则直接返回
//                System.out.println("命中热key缓存: " + key);
//                 return ResultUtils.success((QuestionVO) cacheQuestionVO);
//            } else {
//                // 这里可以添加等待或重试逻辑，或者直接走数据库
//                System.out.println("热key识别但缓存为空，可能存在推送延迟: " + key);
//                // 继续执行数据库查询
//            }
//        }
//
//
//        //可以多久缓存，防止redis或者出现雪崩，击穿等问题是，请求大量涌向数据库，先从本地缓存caffeine/借助 hotkey 缓存
//        // 在下一步 之前通过hotkey 的检测将缓存存在redis 的分布式缓存，这一步，是为了防止redis 缓存失效，导致请求直接冲向数据库，
//        // 这一步查询redis缓存，最后查询数据库
//
//
//        // 查询数据库
//        Question question = questionService.getById(id);
//        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
//
//        QuestionVO questionVO = questionService.getQuestionVO(question, request);
//
//        // 缓存到 hotkey 缓存中
//        JdHotKeyStore.smartSet(key, questionVO);


//        // 获取封装类
//        return ResultUtils.success(questionVO);
        // 分布式缓存架构
        String key = "question_detail_" + id;
        String lockKey = "lock:" + key;  // 互斥锁防止击穿，防止单个热点key失效，大量并发请求这个key
        try {
            // 热点hotkey 检测
            //判断是不是热点key
            if (JdHotKeyStore.isHotKey(key)) {
                //获取本地缓存
                Object cachequestion = JdHotKeyStore.get(key);
                if (cachequestion != null) {
                    System.out.println("命中hotkey缓存");
                    return ResultUtils.success((QuestionVO) cachequestion);
                }
            }

            // redis 检测
            Object rediscache = redisTemplate.opsForValue().get(key);
            if (rediscache != null) {
                //如果是热key ，回填到hot key缓存
                if (JdHotKeyStore.isHotKey(key)) {
                    // 将redis缓存回填到hotkey缓存，实现优化
                    JdHotKeyStore.smartSet(key, rediscache);
                }
                return ResultUtils.success((QuestionVO) rediscache);
            }

            // 获取分布式锁
            boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            if (!locked) {
                //如果没有拿到所，说明有别的进程再拿数据，稍后等待重试，或者递归重试
                Thread.sleep(50);
                return getQuestionVOById(id, request);
            }
            //双重检查
            // 在加锁后必须再次检查缓存
            // 因为可能等待锁期间，数据已经被其他线程加载了
            try {

                Object doubleCheck = redisTemplate.opsForValue().get(key);
                if (doubleCheck != null) {
                    return ResultUtils.success((QuestionVO) doubleCheck);
                }
                //查询数据库
                System.out.println("🚀 线程 " + Thread.currentThread().getName() + " 获取到锁，查询数据库");
                Question question = questionService.getById(id);
                ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
                QuestionVO questionVO = questionService.getQuestionVO(question, request);

                // 设置多级缓存
                // redis缓存 (随机过期时间)
                long timeout = 30 * 60 + ThreadLocalRandom.current().nextInt(0, 300);
                redisTemplate.opsForValue().set(key, questionVO, timeout, TimeUnit.SECONDS);

                // hotkey 缓存
                JdHotKeyStore.smartSet(key, questionVO);
                System.out.println("✅ 数据加载完成并设置缓存");
                return ResultUtils.success(questionVO);
            } finally {
                // 释放锁，互斥锁，防止别的进程一直等待上一个进程的获取数据，导致系统堵塞，
                redisTemplate.delete(lockKey);
            }


        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new RuntimeException("查询中断", e);


        } catch (Exception e) {

            //保底策略，查询数据库

            System.err.println("缓存系统异常，降级查询: " + e.getMessage());
            Question question = questionService.getById(id);
            ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
            return ResultUtils.success(questionService.getQuestionVO(question, request));
        }
    }

    /**
     * 分页获取题目列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取题目列表（封装类-限流版）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/sentinel")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPageSentinel(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                       HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        //热点参数限流
        // 基于ip限流
        String remoteAddr = request.getRemoteAddr();
        Entry entry = null;
        try {
            entry = SphU.entry("listQuestionVOByPage", EntryType.IN, 1, remoteAddr);
            //被保护的业务逻辑
            // 查询数据库
            Page<Question> questionPage = questionService.page(new Page<>(current, size),
                    questionService.getQueryWrapper(questionQueryRequest));
            // 获取封装类
            return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
        } catch (Throwable ex) {
            //自定义义务异常
            if (!BlockException.isBlockException(ex)) {
                Tracer.trace(ex);
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
            }

            // 降级操作
            if (ex instanceof DegradeException) {
                return handleFallback(questionQueryRequest, request, ex);
            }
            // 限流操作
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统访问过于频繁，请稍后再试");
        } finally {
            if (entry != null) {
                entry.exit(1, remoteAddr);
            }
        }


    }

    /**
     * listQuestionVOByPage 流控操作（此处为了方便演示，写在同一个类中）
     * 限流：提示“系统压力过大，请耐心等待”
     *
     */
    public BaseResponse<Page<QuestionVO>> handleBlockException(@RequestBody QuestionQueryRequest questionBankQueryRequest,
                                                               HttpServletRequest request, BlockException ex) {
        // 降级操作
        if (ex instanceof DegradeException) {
            return handleFallback(questionBankQueryRequest, request, ex);
        }
        // 限流操作
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统压力过大，请耐心等待");
    }

    /**
     * listQuestionVOByPage 降级操作：直接返回本地数据（此处为了方便演示，写在同一个类中）
     */
    public BaseResponse<Page<QuestionVO>> handleFallback(@RequestBody QuestionQueryRequest questionBankQueryRequest,
                                                         HttpServletRequest request, Throwable ex) {
        // 可以返回本地数据或空数据
        return ResultUtils.success(null);
    }


    /**
     * 分页获取当前登录用户创建的题目列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑题目（给用户使用）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        // 手动处理 tags 字段的类型转换，从 List<String> 转为 JSON 字符串
        if (questionEditRequest.getTags() != null) {
            question.setTags(JSONUtil.toJsonStr(questionEditRequest.getTags()));
        }
        // 数据校验
        questionService.validQuestion(question, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionEditRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
    @PostMapping("/search/page/vo")
    public BaseResponse<Page<QuestionVO>> searchQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        // todo 取消注释开启 ES（须先配置 ES）
        Page<Question> questionPage;
        // 检查是否配置es,否则降级数据库查询
        if (elasticSearchService.isElasticsearchAvailable()) {
                log.info("使用ElasticSearch 进行搜索");
            // 查询 ES
           questionPage= questionService.searchFromEs(questionQueryRequest);

        } else {
            log.info("使用数据库进行降级进行搜索");
            // 降级策略，使用数据库查询
            questionPage = questionService.listQuestionByPage(questionQueryRequest);
        }


        // 查询数据库（作为没有 ES 的降级方案）
//        Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    @PostMapping("/batch/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> batchDeleteQuestion(@RequestBody QuestionBatchDeleteRequest questionBatchDeleteRequest) {
        ThrowUtils.throwIf(questionBatchDeleteRequest == null, ErrorCode.PARAMS_ERROR);
        questionService.batchDeleteQuestions(questionBatchDeleteRequest.getQuestionIdList());
        return ResultUtils.success(true);
    }
    @GetMapping("/es/health")
    public BaseResponse<String> checkElasticsearchHealth() {
        boolean isAvailable = elasticSearchService.isElasticsearchAvailable();
        String status = isAvailable ? "Elasticsearch is available" : "Elasticsearch is not available";
        return ResultUtils.success(status);
    }
}
