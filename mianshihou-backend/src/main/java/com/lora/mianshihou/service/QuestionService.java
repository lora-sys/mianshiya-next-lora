package com.lora.mianshihou.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lora.mianshihou.model.dto.question.QuestionQueryRequest;
import com.lora.mianshihou.model.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lora.mianshihou.model.vo.QuestionVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author yanBingZhao
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2025-10-18 15:44:00
*/
public interface QuestionService extends IService<Question> {

    void validQuestion(Question question, boolean b);

    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);

    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);
}
