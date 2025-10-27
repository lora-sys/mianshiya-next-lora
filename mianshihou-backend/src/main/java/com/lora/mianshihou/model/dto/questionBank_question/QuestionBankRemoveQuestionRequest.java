package com.lora.mianshihou.model.dto.questionBank_question;

import lombok.Data;
import java.io.Serializable;

@Data
public class QuestionBankRemoveQuestionRequest implements Serializable
{
    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 题库id
     */
    private Long questionBankId;

    private static final long serialVersionUID = 1L;
}
