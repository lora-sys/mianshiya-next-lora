package com.lora.mianshihou.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lora.mianshihou.model.entity.Question;
import com.lora.mianshihou.model.dto.question.QuestionQueryRequest;

/**
 * ES服务接口
 *
 * @author lora
 */
 public interface ElasticSearchService {

    /**
     * 从ES搜索题目
     *
     * @param questionQueryRequest
     * @return
     */
    Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest);

    /**
     * 检查ES是否可用
     *
     * @return
     */
    boolean isElasticsearchAvailable();
}