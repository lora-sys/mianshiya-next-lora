package com.lora.mianshihou.model.vo;

import cn.hutool.json.JSONUtil;
import com.lora.mianshihou.model.entity.QuestionBank_Question;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 题库题目视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Data
public class QuestionBank_QuestionVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 封装类转对象
     *
     * @param questionBank_questionVO
     * @return
     */
    public static QuestionBank_Question voToObj(QuestionBank_QuestionVO questionBank_questionVO) {
        if (questionBank_questionVO == null) {
            return null;
        }
        QuestionBank_Question questionBank_question = new QuestionBank_Question();
        BeanUtils.copyProperties(questionBank_questionVO, questionBank_question);
        List<String> tagList = questionBank_questionVO.getTagList();
        questionBank_question.setTags(JSONUtil.toJsonStr(tagList));
        return questionBank_question;
    }

    /**
     * 对象转封装类
     *
     * @param questionBank_question
     * @return
     */
    public static QuestionBank_QuestionVO objToVo(QuestionBank_Question questionBank_question) {
        if (questionBank_question == null) {
            return null;
        }
        QuestionBank_QuestionVO questionBank_questionVO = new QuestionBank_QuestionVO();
        BeanUtils.copyProperties(questionBank_question, questionBank_questionVO);
        questionBank_questionVO.setTagList(JSONUtil.toList(questionBank_question.getTags(), String.class));
        return questionBank_questionVO;
    }
}
