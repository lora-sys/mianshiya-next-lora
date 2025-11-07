package com.lora.mianshihou.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;

/**
 * 题库题目
 * @TableName question_bank_question
 */
@TableName(value ="question_bank_question")
@Data
public class QuestionBankQuestion {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题库 id
     */
    private Long questionBankId;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;


    /**
     * 删除逻辑
     */
     @TableLogic
     private Integer isDelete;
    /**
     * 更新时间
     */
    private Date updateTime;
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}