package com.lora.mianshihou.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lora.mianshihou.model.dto.questionBank_question.QuestionBankQuestionQueryRequest;
import com.lora.mianshihou.model.entity.QuestionBankQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lora.mianshihou.model.vo.QuestionBankQuestionVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author yanBingZhao
* @description 针对表【question_bank_question(题库题目)】的数据库操作Service
* @createDate 2025-10-18 15:44:00
*/
public interface QuestionBankQuestionService extends IService<QuestionBankQuestion> {

    void validQuestionBank_Question(QuestionBankQuestion questionBank_question, boolean add);

    QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBank_questionQueryRequest);

    QuestionBankQuestionVO getQuestionBank_QuestionVO(QuestionBankQuestion questionBank_question, HttpServletRequest request);

    Page<QuestionBankQuestionVO> getQuestionBank_QuestionVOPage(Page<QuestionBankQuestion> questionBank_questionPage, HttpServletRequest request);
}
