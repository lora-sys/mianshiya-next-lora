package com.lora.mianshihou.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lora.mianshihou.model.dto.questionBank.QuestionBankQueryRequest;
import com.lora.mianshihou.model.entity.QuestionBank;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lora.mianshihou.model.vo.QuestionBankVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author yanBingZhao
* @description 针对表【question_bank(题库)】的数据库操作Service
* @createDate 2025-10-18 15:44:00
*/
public interface QuestionBankService extends IService<QuestionBank> {

    void validQuestionBank(QuestionBank questionBank, boolean add);

    QueryWrapper<QuestionBank> getQueryWrapper(QuestionBankQueryRequest questionBankQueryRequest);

    QuestionBankVO getQuestionBankVO(QuestionBank questionBank, HttpServletRequest request);

    Page<QuestionBankVO> getQuestionBankVOPage(Page<QuestionBank> questionBankPage, HttpServletRequest request);
}
