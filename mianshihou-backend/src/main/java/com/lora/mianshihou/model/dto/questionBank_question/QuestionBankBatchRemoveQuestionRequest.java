package com.lora.mianshihou.model.dto.questionBank_question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量从题库移除请求
 *
 * @author lora
 *
 */
@Data
public class QuestionBankBatchRemoveQuestionRequest implements Serializable {

    /**
     * 题库 id
     */
    private Long questionBankId;
    /**
     * 题目 id列表
     */
    private List<Long> questionIdList;


    private static final long serialVersionUID = 1L;


}