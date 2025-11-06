package com.lora.mianshihou.mapper;

import com.lora.mianshihou.model.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author yanBingZhao
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2025-10-18 15:44:00
* @Entity model.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {


    /**
     * 查询题目列表 (包括已经被删除的信息)
     * @param minUpdateTime
     * @return
     */
    @Select("select * from question where updateTime >= #{minUpdateTime}")
    List<Question> listQuestionWithDelete(Date minUpdateTime);



}




