package com.lora.mianshihou.model.dto.questionBank_question;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建题库请求
 *
 * @author lora
 *
 */
@Data
public class QuestionBankAddQuestionRequest implements Serializable {

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