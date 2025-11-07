package com.lora.mianshihou.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lora.mianshihou.common.ErrorCode;
import com.lora.mianshihou.constant.CommonConstant;
import com.lora.mianshihou.exception.ThrowUtils;
import com.lora.mianshihou.mapper.QuestionMapper;
import com.lora.mianshihou.model.dto.question.QuestionEsDTO;
import com.lora.mianshihou.model.dto.question.QuestionQueryRequest;
import com.lora.mianshihou.model.entity.Question;
import com.lora.mianshihou.model.entity.QuestionBankQuestion;
import com.lora.mianshihou.model.entity.User;
import com.lora.mianshihou.model.vo.QuestionVO;
import com.lora.mianshihou.model.vo.UserVO;
import com.lora.mianshihou.service.QuestionBankQuestionService;
import com.lora.mianshihou.service.QuestionService;
import com.lora.mianshihou.service.UserService;
import com.lora.mianshihou.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 * <p>
 * lora
 *
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private UserService userService;
    @Autowired
    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String title = question.getTitle();
        String content = question.getContent();
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
        if (StringUtils.isNotBlank(content)) {
            ThrowUtils.throwIf(content.length() > 10240, ErrorCode.PARAMS_ERROR, "内容过长");
        }

    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        String searchText = questionQueryRequest.getSearchText();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        List<String> tagList = questionQueryRequest.getTags();
        Long userId = questionQueryRequest.getUserId();
        String answer = questionQueryRequest.getAnswer();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
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
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // region 可选
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);

        // endregion

        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            return QuestionVO.objToVo(question);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        //region 可选
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //2.填充信息
        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUser(userService.getUserVO(user));
        });

        // endregion

        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    /**
     * 从 ES 查询 题目
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {

        // 获取参数
        Long id = questionQueryRequest.getId();
        Long notId = questionQueryRequest.getNotId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        String searchText = questionQueryRequest.getSearchText();
        List<String> tags = questionQueryRequest.getTags();
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        Long userId = questionQueryRequest.getUserId();

        //注意 es 的起始页面为0
        Long current = questionQueryRequest.getCurrent() - 1;
        Long pageSize = questionQueryRequest.getPageSize();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        log.info("分页参数: page={}, size={}, sortField={}", current, pageSize, sortField);

        //构造查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //过滤
        boolQueryBuilder.must(QueryBuilders.matchQuery("isDelete", 0));
        if (id != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("id", id));

        }
        if (notId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("notId", notId));
        }
        if (userId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("userId", userId));
        }
        if (questionBankId != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("questionBankId", questionBankId));
        }

        //必须包含所有标签
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("tags", tag));
            }
        }

        //按关键词检索
        if (StringUtils.isNotBlank(searchText)) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("title", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("content", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("answer", searchText));
            boolQueryBuilder.minimumShouldMatch(1);
        }


        //排序
        SortBuilder<?> sortBuilder = SortBuilders.scoreSort();
        if (StringUtils.isNotBlank(sortField)) {
            sortBuilder = SortBuilders.fieldSort(sortField);

            sortBuilder.order(CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ? SortOrder.ASC : SortOrder.DESC);
        }
        // 分页
        PageRequest pageRequest = PageRequest.of(Math.toIntExact(current), Math.toIntExact(pageSize));

        // 构造查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withSorts(sortBuilder)
                .build();
        SearchHits<QuestionEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, QuestionEsDTO.class);
        log.info("ES 查询语句: {}", searchQuery.getQuery());


        //复用mybatis plus的分页对象，封装返回结果
        Page<Question> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        List<Question> resourceList = new ArrayList<>();
        if (searchHits.hasSearchHits()) {
            List<SearchHit<QuestionEsDTO>> searchHitsList = searchHits.getSearchHits();
            for (SearchHit<QuestionEsDTO> questionEsDTOSearchHits : searchHitsList) {
                resourceList.add(QuestionEsDTO.dtoToObj(questionEsDTOSearchHits.getContent()));
            }
        }
        page.setRecords(resourceList);

        return page;
    }


    /**
     * 分页获取题目列表（封装类）
     *
     * @param questionQueryRequest
     * @return
     */
    public Page<Question> listQuestionByPage(QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();

        //题目表查询条件
        QueryWrapper<Question> queryWrapper = this.getQueryWrapper(questionQueryRequest);


        //根据题库查询题目列表接口
        //判断，取出值
        Long questionBankId = questionQueryRequest.getQuestionBankId();
        if (questionBankId != null) {
            //查询题库里的id,  都查？/分页查询？
            LambdaQueryWrapper<QuestionBankQuestion> LambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                    .select(QuestionBankQuestion::getQuestionId)
                    .eq(QuestionBankQuestion::getQuestionBankId, questionBankId);
            List<QuestionBankQuestion> questionList = questionBankQuestionService.list(LambdaQueryWrapper);
            if (CollUtil.isNotEmpty(questionList)) {
                //取出对象中所有的题目id存储在集合去重作为一个集合
                Set<Long> questionIdList = questionList.stream().map(QuestionBankQuestion::getQuestionId).collect(Collectors.toSet());
                queryWrapper.in("id", questionIdList);
            } else {
                //题库为空，返回空列表
                return new Page<>(current, size, 0);
            }

        }
        // 查询数据库
        Page<Question> questionPage = this.page(new Page<>(current, size), queryWrapper);
        // 获取封装类
        return questionPage;
    }

    /**
     * 批量删除题目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteQuestions(List<Long> questionIdList) {
        ThrowUtils.throwIf(CollUtil.isEmpty(questionIdList), ErrorCode.PARAMS_ERROR, "要删除的题目列表不能为空");
//        for (Long questionId : questionIdList) {
        //使用matbatis plus 的批量删除功能
        // removeBatchByIds：专门为批量删除设计，性能更好
        //removeByIds：通用方法，内部也会批量处理
//            boolean result = this.removeById(questionId);
        //从N次数据库操作减少到1次（或M次，M=批次数量），比如删除1000条题目，从1000次操作减少到1次操作！
        //分批次插入
        // 手动分批，避免SQL语句过长
//        int batchSize = 1000;
//        int totalSize = questionIdList.size();
//
//        for (int i = 0; i < totalSize; i += batchSize) {
//            List<Long> subList = questionIdList.subList(i, Math.min(i + batchSize, totalSize));
//
//            // 批量删除题目
//            boolean result = this.removeByIds(subList);
//            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除题目失败");
//
//            // 批量删除题目题库关系
//            LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
//                    .in(QuestionBankQuestion::getQuestionId, subList);
//            result = questionBankQuestionService.remove(lambdaQueryWrapper);
//            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除题目题库关联失败");
//        }
        boolean result = this.removeBatchByIds(questionIdList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除题目失败");
        // 移除题目题库关系
        // 构造查询
        LambdaQueryWrapper<QuestionBankQuestion> lambdaQueryWrapper = Wrappers.lambdaQuery(QuestionBankQuestion.class)
                .eq(QuestionBankQuestion::getQuestionId, questionIdList);
        result = questionBankQuestionService.remove(lambdaQueryWrapper);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除题目题库关联失败");
    }
//    }
}
