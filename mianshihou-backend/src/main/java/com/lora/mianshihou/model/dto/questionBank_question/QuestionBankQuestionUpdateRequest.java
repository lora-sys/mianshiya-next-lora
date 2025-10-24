package com.lora.mianshihou.model.dto.questionBank_question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新题库请求
 *
 * @author lora
 *
 */
@Data
public class QuestionBankQuestionUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;
    /**
     * 题库 id
     */
    private Long questionBankId;

    /**
     * 题目 id
     */
    private Long questionId;


    private static final long serialVersionUID = 1L;
}