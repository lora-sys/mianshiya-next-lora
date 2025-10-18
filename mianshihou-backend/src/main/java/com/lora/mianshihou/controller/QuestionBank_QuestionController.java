package com.lora.mianshihou.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lora.mianshihou.annotation.AuthCheck;
import com.lora.mianshihou.common.BaseResponse;
import com.lora.mianshihou.common.DeleteRequest;
import com.lora.mianshihou.common.ErrorCode;
import com.lora.mianshihou.common.ResultUtils;
import com.lora.mianshihou.constant.UserConstant;
import com.lora.mianshihou.exception.BusinessException;
import com.lora.mianshihou.exception.ThrowUtils;
import com.lora.mianshihou.model.dto.questionBank_question.QuestionBank_QuestionAddRequest;
import com.lora.mianshihou.model.dto.questionBank_question.QuestionBank_QuestionEditRequest;
import com.lora.mianshihou.model.dto.questionBank_question.QuestionBank_QuestionQueryRequest;
import com.lora.mianshihou.model.dto.questionBank_question.QuestionBank_QuestionUpdateRequest;
import com.lora.mianshihou.model.entity.QuestionBank_Question;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.model.vo.QuestionBank_QuestionVO;
import com.lora.mianshihou.service.QuestionBank_QuestionService;
import com.lora.mianshihou.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题库题目接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/questionBank_question")
@Slf4j
public class QuestionBank_QuestionController {

    @Resource
    private QuestionBank_QuestionService questionBank_questionService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建题库题目
     *
     * @param questionBank_questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestionBank_Question(@RequestBody QuestionBank_QuestionAddRequest questionBank_questionAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionBank_questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank_Question questionBank_question = new QuestionBank_Question();
        BeanUtils.copyProperties(questionBank_questionAddRequest, questionBank_question);
        // 数据校验
        questionBank_questionService.validQuestionBank_Question(questionBank_question, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        questionBank_question.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionBank_questionService.save(questionBank_question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionBank_QuestionId = questionBank_question.getId();
        return ResultUtils.success(newQuestionBank_QuestionId);
    }

    /**
     * 删除题库题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestionBank_Question(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionBank_Question oldQuestionBank_Question = questionBank_questionService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank_Question == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestionBank_Question.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBank_questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题库题目（仅管理员可用）
     *
     * @param questionBank_questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestionBank_Question(@RequestBody QuestionBank_QuestionUpdateRequest questionBank_questionUpdateRequest) {
        if (questionBank_questionUpdateRequest == null || questionBank_questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank_Question questionBank_question = new QuestionBank_Question();
        BeanUtils.copyProperties(questionBank_questionUpdateRequest, questionBank_question);
        // 数据校验
        questionBank_questionService.validQuestionBank_Question(questionBank_question, false);
        // 判断是否存在
        long id = questionBank_questionUpdateRequest.getId();
        QuestionBank_Question oldQuestionBank_Question = questionBank_questionService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank_Question == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionBank_questionService.updateById(questionBank_question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题库题目（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionBank_QuestionVO> getQuestionBank_QuestionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        QuestionBank_Question questionBank_question = questionBank_questionService.getById(id);
        ThrowUtils.throwIf(questionBank_question == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(questionBank_questionService.getQuestionBank_QuestionVO(questionBank_question, request));
    }

    /**
     * 分页获取题库题目列表（仅管理员可用）
     *
     * @param questionBank_questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuestionBank_Question>> listQuestionBank_QuestionByPage(@RequestBody QuestionBank_QuestionQueryRequest questionBank_questionQueryRequest) {
        long current = questionBank_questionQueryRequest.getCurrent();
        long size = questionBank_questionQueryRequest.getPageSize();
        // 查询数据库
        Page<QuestionBank_Question> questionBank_questionPage = questionBank_questionService.page(new Page<>(current, size),
                questionBank_questionService.getQueryWrapper(questionBank_questionQueryRequest));
        return ResultUtils.success(questionBank_questionPage);
    }

    /**
     * 分页获取题库题目列表（封装类）
     *
     * @param questionBank_questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionBank_QuestionVO>> listQuestionBank_QuestionVOByPage(@RequestBody QuestionBank_QuestionQueryRequest questionBank_questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionBank_questionQueryRequest.getCurrent();
        long size = questionBank_questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank_Question> questionBank_questionPage = questionBank_questionService.page(new Page<>(current, size),
                questionBank_questionService.getQueryWrapper(questionBank_questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBank_questionService.getQuestionBank_QuestionVOPage(questionBank_questionPage, request));
    }

    /**
     * 分页获取当前登录用户创建的题库题目列表
     *
     * @param questionBank_questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionBank_QuestionVO>> listMyQuestionBank_QuestionVOByPage(@RequestBody QuestionBank_QuestionQueryRequest questionBank_questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionBank_questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionBank_questionQueryRequest.setUserId(loginUser.getId());
        long current = questionBank_questionQueryRequest.getCurrent();
        long size = questionBank_questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank_Question> questionBank_questionPage = questionBank_questionService.page(new Page<>(current, size),
                questionBank_questionService.getQueryWrapper(questionBank_questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBank_questionService.getQuestionBank_QuestionVOPage(questionBank_questionPage, request));
    }

    /**
     * 编辑题库题目（给用户使用）
     *
     * @param questionBank_questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestionBank_Question(@RequestBody QuestionBank_QuestionEditRequest questionBank_questionEditRequest, HttpServletRequest request) {
        if (questionBank_questionEditRequest == null || questionBank_questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank_Question questionBank_question = new QuestionBank_Question();
        BeanUtils.copyProperties(questionBank_questionEditRequest, questionBank_question);
        // 数据校验
        questionBank_questionService.validQuestionBank_Question(questionBank_question, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionBank_questionEditRequest.getId();
        QuestionBank_Question oldQuestionBank_Question = questionBank_questionService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank_Question == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestionBank_Question.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBank_questionService.updateById(questionBank_question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
