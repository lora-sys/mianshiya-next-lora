package com.lora.mianshihou.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lora.mianshihou.common.BaseResponse;
import com.lora.mianshihou.model.dto.post.PostQueryRequest;
import com.lora.mianshihou.model.dto.question.QuestionQueryRequest;
import com.lora.mianshihou.model.entity.Post;
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


    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);

    /**
     * 分页获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);


    /**
     * 从 ES 查询 题目
     *
     * @param questionQueryRequest
     * @return
     */
    Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest);

    /**
     * 分页获取题目列表（封装类）
     *
     * @param questionQueryRequest
     * @return
     */
   Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest) ;
}
