package com.lora.mianshihou.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestionBank_Question(QuestionBankQuestion questionBank_question, boolean add) {
        ThrowUtils.throwIf(questionBank_question == null, ErrorCode.PARAMS_ERROR);
           //题库和题目必须存在
        Long QuestionId = questionBank_question.getQuestionId();
        Long QuestionBankId = questionBank_question.getQuestionBankId();
    if(QuestionId !=null){
        Question question = questionService.getById(QuestionId);
         ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR,"未发现题目存在");
    }
    if(QuestionBankId !=null){
        QuestionBank questionBank = questionBankService.getById(QuestionBankId);
         ThrowUtils.throwIf(questionBank == null, ErrorCode.PARAMS_ERROR,"未发现题库存在");
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

}
