package com.lora.mianshihou.job.once;

import cn.hutool.core.collection.CollUtil;
import com.lora.mianshihou.esdao.QuestionEsDao;
import com.lora.mianshihou.model.dto.question.QuestionEsDTO;
import com.lora.mianshihou.model.entity.Question;
import com.lora.mianshihou.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全量同步题目到 ES
 *
 * @author lora
 */
@Component
@Slf4j
public class FullSyncQuestionToEs implements CommandLineRunner {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionEsDao questionEsDao;

    @Override
    public void run(String... args) {
        // 全量获取题目
        List<Question> questionList = questionService.list();
        log.info("从数据库获取到 {} 条题目数据", questionList.size());
        if (CollUtil.isEmpty(questionList)) {
            log.warn("没有从数据库中读取到题目数据，任务结束。");
            return;
        }

        // 转为 ES 实体类，并修正 id 为 String 类型
        List<QuestionEsDTO> questionEsDTOList = questionList.stream()
                .map(QuestionEsDTO::objToDto
                )
                .collect(Collectors.toList());

        // 分页批量插入到 ES
        final int pageSize = 500;
        int total = questionEsDTOList.size();
        log.info("FullSyncQuestionToEs start, total {}", total);

        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            List<QuestionEsDTO> subList = questionEsDTOList.subList(i, end);
            log.info("Syncing batch: {} - {}", i, end);
            try {
                questionEsDao.saveAll(subList);
                // 如果使用的 ES 客户端是异步的，可以考虑手动 flush（可选）
                // questionEsDao.flush();
            } catch (Exception e) {
                log.error("Sync batch failed: {} - {}, error: {}", i, end, e.getMessage(), e);
            }
        }

        log.info("FullSyncQuestionToEs end, total {}", total);
    }
}
