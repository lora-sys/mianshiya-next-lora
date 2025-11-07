package com.lora.mianshihou.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lora.mianshihou.common.ErrorCode;
import com.lora.mianshihou.constant.CommonConstant;
import com.lora.mianshihou.exception.BusinessException;
import com.lora.mianshihou.exception.ThrowUtils;
import com.lora.mianshihou.mapper.QuestionBankQuestionMapper;
import com.lora.mianshihou.model.dto.questionBank_question.QuestionBankQuestionQueryRequest;
import com.lora.mianshihou.model.entity.Question;
import com.lora.mianshihou.model.entity.QuestionBank;
import com.lora.mianshihou.model.entity.QuestionBankQuestion;
//import com.lora.mianshihou.model.entity.QuestionBankQuestionFavour;
//import com.lora.mianshihou.model.entity.QuestionBankQuestionThumb;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.model.vo.QuestionBankQuestionVO;
import com.lora.mianshihou.model.vo.UserVO;
import com.lora.mianshihou.service.QuestionBankQuestionService;
import com.lora.mianshihou.service.QuestionBankService;
import com.lora.mianshihou.service.QuestionService;
import com.lora.mianshihou.service.UserService;
import com.lora.mianshihou.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.desktop.AppEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 题库题目服务实现
 *
 * @author lora
 *
 */
@Service
@Slf4j
public class QuestionBankQuestionServiceImpl extends ServiceImpl<QuestionBankQuestionMapper, QuestionBankQuestion> implements QuestionBankQuestionService {

    @Resource
    private UserService userService;
    @Resource
    private QuestionBankService questionBankService;

    @Resource
    @Lazy
    private QuestionService questionService;

    /**
     * 校验数据
     *
     * @param questionBank_question
     * @param add                   对创建的数据进行校验
     */
    @Override
    public void validQuestionBank_Question(QuestionBankQuestion questionBank_question, boolean add) {
        ThrowUtils.throwIf(questionBank_question == null, ErrorCode.PARAMS_ERROR);
        //题库和题目必须存在
        Long QuestionId = questionBank_question.getQuestionId();
        Long QuestionBankId = questionBank_question.getQuestionBankId();
        if (QuestionId != null) {
            Question question = questionService.getById(QuestionId);
            ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR, "未发现题目存在");
        }
        if (QuestionBankId != null) {
            QuestionBank questionBank = questionBankService.getById(QuestionBankId);
            ThrowUtils.throwIf(questionBank == null, ErrorCode.PARAMS_ERROR, "未发现题库存在");
        }


        //
        //
        // todo 从对象中取值
        //        String title = questionBank_question.getTitle();
        //        // 创建数据时，参数不能为空
        //        if (add) {
        //            // todo 补充校验规则
        //            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        //        }
        //        // 修改数据时，有参数则校验
        //        // todo 补充校验规则
        //        if (StringUtils.isNotBlank(title)) {
        //            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        //        }
    }

    /**
     * 获取查询条件
     *
     * @param questionBank_questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBank_questionQueryRequest) {
        QueryWrapper<QuestionBankQuestion> queryWrapper = new QueryWrapper<>();
        if (questionBank_questionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionBank_questionQueryRequest.getId();
        Long notId = questionBank_questionQueryRequest.getNotId();
        Long questionId = questionBank_questionQueryRequest.getQuestionId();
        Long questionBankId = questionBank_questionQueryRequest.getQuestionBankId();
        String sortField = questionBank_questionQueryRequest.getSortField();
        String sortOrder = questionBank_questionQueryRequest.getSortOrder();
        Long userId = questionBank_questionQueryRequest.getUserId();
        // todo 补充需要的查询条件


        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题库题目封装
     *
     * @param questionBank_question
     * @param request
     * @return
     */
    @Override
    public QuestionBankQuestionVO getQuestionBank_QuestionVO(QuestionBankQuestion questionBank_question, HttpServletRequest request) {
        // 对象转封装类
        QuestionBankQuestionVO questionBank_questionVO = QuestionBankQuestionVO.objToVo(questionBank_question);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = questionBank_question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBank_questionVO.setUser(userVO);

        // endregion

        return questionBank_questionVO;
    }

    /**
     * 分页获取题库题目封装
     *
     * @param questionBank_questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionBankQuestionVO> getQuestionBank_QuestionVOPage(Page<QuestionBankQuestion> questionBank_questionPage, HttpServletRequest request) {
        List<QuestionBankQuestion> questionBank_questionList = questionBank_questionPage.getRecords();
        Page<QuestionBankQuestionVO> questionBank_questionVOPage = new Page<>(questionBank_questionPage.getCurrent(), questionBank_questionPage.getSize(), questionBank_questionPage.getTotal());
        if (CollUtil.isEmpty(questionBank_questionList)) {
            return questionBank_questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBankQuestionVO> questionBank_questionVOList = questionBank_questionList.stream().map(questionBank_question -> {
            return QuestionBankQuestionVO.objToVo(questionBank_question);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionBank_questionList.stream().map(QuestionBankQuestion::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        questionBank_questionVOList.forEach(questionBank_questionVO -> {
            Long userId = questionBank_questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBank_questionVO.setUser(userService.getUserVO(user));
        });
        // endregion

        questionBank_questionVOPage.setRecords(questionBank_questionVOList);
        return questionBank_questionVOPage;
    }

    /**
     * 批量添加题目到题库
     *
     * @param questionIdList
     * @param questionBankId
     * @param loginUser
     */
    @Override
//    @Async  异步执行，新开任务执行
    public void batchAddQuestionToBank(List<Long> questionIdList, long questionBankId, User loginUser) {
        //参数校验

        ThrowUtils.throwIf(questionIdList == null, ErrorCode.PARAMS_ERROR, "题目列表id不能为空");
        ThrowUtils.throwIf(questionBankId <= 0, ErrorCode.PARAMS_ERROR, "题库id非法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //检查题目id是否存在
        LambdaQueryWrapper<Question> questionLambdaQueryWrapper = Wrappers.lambdaQuery(Question.class)
                .select(Question::getId)
                .in(Question::getId, questionIdList);

        //使用 select(Question::getId) 只查询 id 字段，避免查询不必要的字段
        //使用 in(Question::getId, questionIdList) 批量查询，提高效率
        List<Long> validQuestionIdList = questionService.listObjs(questionLambdaQueryWrapper, obj -> (Long) obj); //sql 优化查询
        //合法的题目id 列表
//        List<Long> validQuestionIdList = questionList.stream()
//                .map(Question::getId)
//                .collect(Collectors.toList());
        //检查那些题目还不存在于题库里,只找出不存在题库列表的题目，避免重复插入

//先查询出来已经存在在题库的题目列表，然后再在有效列表里，使用这个已经存在的让题目列表来过滤，只存不存在的题目

        LambdaQueryWrapper<QuestionBankQuestion> LambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionBankId, questionBankId)
                .in(QuestionBankQuestion::getQuestionId, validQuestionIdList);
        List<QuestionBankQuestion> existQuestionList = this.list(LambdaQueryWrapper);
        //已经存在题库中的题目id
        Set<Long> existQuestionIds = existQuestionList.stream().map(QuestionBankQuestion::getQuestionId).collect(Collectors.toSet());

        //过滤出没有添加到题库的题目
        //不需要再次添加
        validQuestionIdList = validQuestionIdList.stream()
                .filter(questionId -> !existQuestionIds.contains(questionId))
                .collect(Collectors.toList());

        ThrowUtils.throwIf(CollUtil.isEmpty(validQuestionIdList), ErrorCode.PARAMS_ERROR, "所有题目都已经存在于题库中");


        //检查题库是否存在
        QuestionBank questionBank = questionBankService.getById(questionBankId);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.PARAMS_ERROR, "题库不存在");


        // io 密集性任务，考虑异步处理，避免阻塞主线程
        //IO密集型任务优化: 批量数据库操作属于IO密集型，多线程可以显著提高并发性能
        //避免阻塞主线程: 将耗时的数据库操作放到异步线程中处理
        //资源控制: 通过线程池限制并发数量，避免系统资源耗尽
        //提高响应速度: 多个批次可以并行处理，而不是串行执行
        //核心线程20: 适合中等并发量的数据库操作
        //最大线程50: 提供了2.5倍的扩展能力，应对突发流量
        //队列容量1000: 可以缓冲较多任务，但要注意内存使用
        //空闲时间60秒: 合理的回收时间，平衡性能和资源使用
        // 自定义线程池 io 密集性任务
        ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(
                20,            // 核心线程数
                50,            // 最大线程数
                60L,           // 线程空闲时间
                TimeUnit.SECONDS, // 时间单位
                new ArrayBlockingQueue<>(1000),// 任务队列
                new ThreadPoolExecutor.AbortPolicy() // 拒绝策略,由调用者处理拒绝任务
        );

        //保存所有批次任务

        List<CompletableFuture<Void>> futures = new ArrayList<>();


        //执行插入操作
        //分批处理，避免长事务，假设每次处理1000条数据
        int batchSize = 1000;
        int totalQuestionListSize = validQuestionIdList.size();

        for (int i = 0; i < totalQuestionListSize; i += batchSize) {
            //生成每批次的数据
            List<Long> subList;
            subList = validQuestionIdList.subList(i, Math.min(i + batchSize, totalQuestionListSize));
            List<QuestionBankQuestion> questionBankQuestions = subList.stream()
                    .map(questionId -> {
                        QuestionBankQuestion questionBankQuestion = new QuestionBankQuestion();
                        questionBankQuestion.setQuestionBankId(questionBankId);
                        questionBankQuestion.setQuestionId(questionId);
                        questionBankQuestion.setUserId(loginUser.getId());
                        return questionBankQuestion;
                    })
                    .collect(Collectors.toList());

            //使用事务处理每批数据 ,代理对象使用，不然事务不会生效，不能直接调用this,不然不生效
            //获取代理对象
            QuestionBankQuestionServiceImpl questionBankQuestionService = (QuestionBankQuestionServiceImpl) AopContext.currentProxy();

            //异步处理每批数据，将任务添加到异步任务列表
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                questionBankQuestionService.batchAddQuestionToBankInner(questionBankQuestions);
            }, customExecutor).exceptionally(ex -> {
                log.error("批次处理任务执行失败", ex);
                return null;
            });
            // 收集所有异步任务
            futures.add(future);
        }
        //阻塞，等待任务都完成 等待所有批次异步任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        //关闭线程池
        customExecutor.shutdown();

    }

    /**
     * 批量添加题目到题库事务,仅供内部使用
     *
     * @param questionBankQuestions
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddQuestionToBankInner(List<QuestionBankQuestion> questionBankQuestions) {
        try {
            boolean result = this.saveBatch(questionBankQuestions);
            ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR, "向题库添加题目失败");
        } catch (DataIntegrityViolationException e) {
            log.error("数据库唯一键冲突或违反其他完整性约束, 错误信息: {},", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目已存在于该题库，无法重复添加");
        } catch (DataAccessException e) {
            log.error("数据库连接问题、事务问题等导致操作失败, 错误信息: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据库操作失败");
        } catch (Exception e) {
            // 捕获其他异常，做通用处理
            log.error("添加题目到题库时发生未知错误，错误信息: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "向题库添加题目失败");
        }
    }


    /**
     * 批量移除题目到题库
     *
     * @param questionIdList
     * @param questionBankId
     *
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveQuestionFromToBank(List<Long> questionIdList, long questionBankId) {
        //参数校验

        ThrowUtils.throwIf(questionIdList == null, ErrorCode.PARAMS_ERROR, "题目列表id不能为空");
        ThrowUtils.throwIf(questionBankId <= 0, ErrorCode.PARAMS_ERROR, "题库id非法");

        //执行删除关联操作
        for (Long questionId : questionIdList) {
            //构造查询
            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .eq(QuestionBankQuestion::getQuestionId, questionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            boolean result = this.remove(lambdaQueryWrapper);
            ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR, "从题库删除题目");
        }

    }


}
