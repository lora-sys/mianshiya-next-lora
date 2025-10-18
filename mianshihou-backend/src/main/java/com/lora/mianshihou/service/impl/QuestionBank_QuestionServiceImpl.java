package com.lora.mianshihou.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lora.mianshihou.common.ErrorCode;
import com.lora.mianshihou.constant.CommonConstant;
import com.lora.mianshihou.exception.ThrowUtils;
import com.lora.mianshihou.mapper.QuestionBank_QuestionMapper;
import com.lora.mianshihou.model.dto.questionBank_question.QuestionBank_QuestionQueryRequest;
import com.lora.mianshihou.model.entity.QuestionBank_Question;
import com.lora.mianshihou.model.entity.QuestionBank_QuestionFavour;
import com.lora.mianshihou.model.entity.QuestionBank_QuestionThumb;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.model.vo.QuestionBank_QuestionVO;
import com.lora.mianshihou.model.vo.UserVO;
import com.lora.mianshihou.service.QuestionBank_QuestionService;
import com.lora.mianshihou.service.UserService;
import com.lora.mianshihou.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题库题目服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Service
@Slf4j
public class QuestionBank_QuestionServiceImpl extends ServiceImpl<QuestionBank_QuestionMapper, QuestionBank_Question> implements QuestionBank_QuestionService {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param questionBank_question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestionBank_Question(QuestionBank_Question questionBank_question, boolean add) {
        ThrowUtils.throwIf(questionBank_question == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = questionBank_question.getTitle();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionBank_questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionBank_Question> getQueryWrapper(QuestionBank_QuestionQueryRequest questionBank_questionQueryRequest) {
        QueryWrapper<QuestionBank_Question> queryWrapper = new QueryWrapper<>();
        if (questionBank_questionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionBank_questionQueryRequest.getId();
        Long notId = questionBank_questionQueryRequest.getNotId();
        String title = questionBank_questionQueryRequest.getTitle();
        String content = questionBank_questionQueryRequest.getContent();
        String searchText = questionBank_questionQueryRequest.getSearchText();
        String sortField = questionBank_questionQueryRequest.getSortField();
        String sortOrder = questionBank_questionQueryRequest.getSortOrder();
        List<String> tagList = questionBank_questionQueryRequest.getTags();
        Long userId = questionBank_questionQueryRequest.getUserId();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 精确查询
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题库题目封装
     *
     * @param questionBank_question
     * @param request
     * @return
     */
    @Override
    public QuestionBank_QuestionVO getQuestionBank_QuestionVO(QuestionBank_Question questionBank_question, HttpServletRequest request) {
        // 对象转封装类
        QuestionBank_QuestionVO questionBank_questionVO = QuestionBank_QuestionVO.objToVo(questionBank_question);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = questionBank_question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionBank_questionVO.setUser(userVO);
        // 2. 已登录，获取用户点赞、收藏状态
        long questionBank_questionId = questionBank_question.getId();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<QuestionBank_QuestionThumb> questionBank_questionThumbQueryWrapper = new QueryWrapper<>();
            questionBank_questionThumbQueryWrapper.in("questionBank_questionId", questionBank_questionId);
            questionBank_questionThumbQueryWrapper.eq("userId", loginUser.getId());
            QuestionBank_QuestionThumb questionBank_questionThumb = questionBank_questionThumbMapper.selectOne(questionBank_questionThumbQueryWrapper);
            questionBank_questionVO.setHasThumb(questionBank_questionThumb != null);
            // 获取收藏
            QueryWrapper<QuestionBank_QuestionFavour> questionBank_questionFavourQueryWrapper = new QueryWrapper<>();
            questionBank_questionFavourQueryWrapper.in("questionBank_questionId", questionBank_questionId);
            questionBank_questionFavourQueryWrapper.eq("userId", loginUser.getId());
            QuestionBank_QuestionFavour questionBank_questionFavour = questionBank_questionFavourMapper.selectOne(questionBank_questionFavourQueryWrapper);
            questionBank_questionVO.setHasFavour(questionBank_questionFavour != null);
        }
        // endregion

        return questionBank_questionVO;
    }

    /**
     * 分页获取题库题目封装
     *
     * @param questionBank_questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionBank_QuestionVO> getQuestionBank_QuestionVOPage(Page<QuestionBank_Question> questionBank_questionPage, HttpServletRequest request) {
        List<QuestionBank_Question> questionBank_questionList = questionBank_questionPage.getRecords();
        Page<QuestionBank_QuestionVO> questionBank_questionVOPage = new Page<>(questionBank_questionPage.getCurrent(), questionBank_questionPage.getSize(), questionBank_questionPage.getTotal());
        if (CollUtil.isEmpty(questionBank_questionList)) {
            return questionBank_questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionBank_QuestionVO> questionBank_questionVOList = questionBank_questionList.stream().map(questionBank_question -> {
            return QuestionBank_QuestionVO.objToVo(questionBank_question);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionBank_questionList.stream().map(QuestionBank_Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> questionBank_questionIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> questionBank_questionIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            Set<Long> questionBank_questionIdSet = questionBank_questionList.stream().map(QuestionBank_Question::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // 获取点赞
            QueryWrapper<QuestionBank_QuestionThumb> questionBank_questionThumbQueryWrapper = new QueryWrapper<>();
            questionBank_questionThumbQueryWrapper.in("questionBank_questionId", questionBank_questionIdSet);
            questionBank_questionThumbQueryWrapper.eq("userId", loginUser.getId());
            List<QuestionBank_QuestionThumb> questionBank_questionQuestionBank_QuestionThumbList = questionBank_questionThumbMapper.selectList(questionBank_questionThumbQueryWrapper);
            questionBank_questionQuestionBank_QuestionThumbList.forEach(questionBank_questionQuestionBank_QuestionThumb -> questionBank_questionIdHasThumbMap.put(questionBank_questionQuestionBank_QuestionThumb.getQuestionBank_QuestionId(), true));
            // 获取收藏
            QueryWrapper<QuestionBank_QuestionFavour> questionBank_questionFavourQueryWrapper = new QueryWrapper<>();
            questionBank_questionFavourQueryWrapper.in("questionBank_questionId", questionBank_questionIdSet);
            questionBank_questionFavourQueryWrapper.eq("userId", loginUser.getId());
            List<QuestionBank_QuestionFavour> questionBank_questionFavourList = questionBank_questionFavourMapper.selectList(questionBank_questionFavourQueryWrapper);
            questionBank_questionFavourList.forEach(questionBank_questionFavour -> questionBank_questionIdHasFavourMap.put(questionBank_questionFavour.getQuestionBank_QuestionId(), true));
        }
        // 填充信息
        questionBank_questionVOList.forEach(questionBank_questionVO -> {
            Long userId = questionBank_questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionBank_questionVO.setUser(userService.getUserVO(user));
            questionBank_questionVO.setHasThumb(questionBank_questionIdHasThumbMap.getOrDefault(questionBank_questionVO.getId(), false));
            questionBank_questionVO.setHasFavour(questionBank_questionIdHasFavourMap.getOrDefault(questionBank_questionVO.getId(), false));
        });
        // endregion

        questionBank_questionVOPage.setRecords(questionBank_questionVOList);
        return questionBank_questionVOPage;
    }

}
