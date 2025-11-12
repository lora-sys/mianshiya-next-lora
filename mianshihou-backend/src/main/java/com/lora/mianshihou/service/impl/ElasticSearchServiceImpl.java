package com.lora.mianshihou.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lora.mianshihou.common.ErrorCode;
import com.lora.mianshihou.esdao.QuestionEsDao;
import com.lora.mianshihou.exception.ThrowUtils;
import com.lora.mianshihou.mapper.QuestionMapper;
import com.lora.mianshihou.model.dto.question.QuestionQueryRequest;
import com.lora.mianshihou.model.entity.Question;
import com.lora.mianshihou.service.ElasticSearchService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ES服务实现类
 *
 * @author lora
 */
@Slf4j
@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

    @Resource
    private QuestionEsDao questionEsDao;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private QuestionMapper questionMapper;


    /**
     * 从ES搜索题目 , es 负责搜索，数据库负责存储完整数据
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {
        if (!isElasticsearchAvailable()) {
            ThrowUtils.throwIf(isElasticsearchAvailable() == false, ErrorCode.OPERATION_ERROR, "elasticsearch不可用，无法执行搜索");
            return null;
        }

        long current = questionQueryRequest.getCurrent();
        long pageSize = questionQueryRequest.getPageSize();
        String searchText = questionQueryRequest.getSearchText();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 构造查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


        // 添加过滤条件：不查询已删除的题目
        boolQueryBuilder.must(QueryBuilders.termQuery("isDelete", 0));

        // 搜索条件
        if (StrUtil.isNotBlank(searchText)) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("title", searchText))
                    .should(QueryBuilders.matchQuery("content", searchText));

        } else {
            if (StrUtil.isNotBlank(title)) {
                boolQueryBuilder.should(QueryBuilders.matchQuery("title", title));
            }
            if (StrUtil.isNotBlank(content)) {
                boolQueryBuilder.should(QueryBuilders.matchQuery("content", content));
            }
        }

        // 标签过滤
        if (CollectionUtils.isNotEmpty(tags)) {
            for (String tag : tags) {
                boolQueryBuilder.must(QueryBuilders.termsQuery("tags", tag));
            }
        }

        // 排序
        Sort sort = Sort.by(Sort.Direction.DESC,"createTime");

        // 分页参数
        long page = current - 1;
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(PageRequest.of((int) page, (int) pageSize, sort))
                .build();

        SearchHits<Question> SearchHits = elasticsearchRestTemplate.search(searchQuery, Question.class);
        List<Long> idList = SearchHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getId())
                .collect(Collectors.toList());


        // 回表拿到动态字段
        Page<Question> pageObj = new Page<>(questionQueryRequest.getCurrent(), pageSize);
        if (idList.isEmpty()) {
            pageObj.setTotal(SearchHits.getTotalHits());
            return pageObj;
        }

        List<Question> questions = questionMapper.selectBatchIds(idList);
        pageObj.setRecords(questions);
        pageObj.setTotal(SearchHits.getTotalHits());
        return pageObj;

    }


    /**
     * 检查ES是否可用
     *
     * @return
     */
    @Override
    public boolean isElasticsearchAvailable() {
        try {
            // 检查索引是否存在
            CountRequest countRequest = new CountRequest("question");
            CountResponse countResponse = restHighLevelClient.count(countRequest, RequestOptions.DEFAULT);
            return countResponse != null;

        } catch (Exception e) {
            log.error("检查elasticsearch可用性失败", e);
            return false;
        }
    }
}