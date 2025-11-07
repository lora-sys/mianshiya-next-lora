package com.lora.mianshihou.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lora.mianshihou.model.dto.questionBank_question.QuestionBankQuestionQueryRequest;
import com.lora.mianshihou.model.entity.QuestionBankQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.model.vo.QuestionBankQuestionVO;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author yanBingZhao
* @description 针对表【question_bank_question(题库题目)】的数据库操作Service
* @createDate 2025-10-18 15:44:00
*/
public interface QuestionBankQuestionService extends IService<QuestionBankQuestion> {
    /**
     * 校验数据
     *
     * @param questionBank_question
     * @param add 对创建的数据进行校验
     */
    void validQuestionBank_Question(QuestionBankQuestion questionBank_question, boolean add);
    /**
     * 获取查询条件
     *
     * @param questionBank_questionQueryRequest
     *
     * @return
     */
    QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBank_questionQueryRequest);
    /**
     * 获取题库题目关联封装
     *
     * @param questionBank_question
     * @param request
     * @return
     */
    QuestionBankQuestionVO getQuestionBank_QuestionVO(QuestionBankQuestion questionBank_question, HttpServletRequest request);
    /**
     * 分页获取题库题目关联封装
     *
     * @param questionBank_questionPage
     * @param request
     * @return
     */
    Page<QuestionBankQuestionVO> getQuestionBank_QuestionVOPage(Page<QuestionBankQuestion> questionBank_questionPage, HttpServletRequest request);

    /**
     * 批量添加题目到题库
     * @param questionIdList
     * @param questionBankId
     * @param loginUser
     */
    void batchAddQuestionToBank(List<Long> questionIdList, long questionBankId, User loginUser);
    /**
     * 批量移除题目到题库
     * @param questionIdList
     * @param questionBankId
     *
     */
    void batchRemoveQuestionFromToBank(List<Long> questionIdList, long questionBankId);


    /**
     * 批量添加题目到题库 事务，进攻内部使用
     *
     *
     */
    @Transactional(rollbackFor = Exception.class)
    void batchAddQuestionToBankInner(List<QuestionBankQuestion> questionBankQuestions);

    @Transactional(rollbackFor = Exception.class)
    void batchRemoveQuestionFromToBankInner(List<Long> questionIds, long questionBankId);
}
