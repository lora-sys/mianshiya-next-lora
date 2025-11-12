package com.lora.mianshihou.utils;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lora.mianshihou.annotation.DistributedLock;
import com.lora.mianshihou.model.entity.QuestionBank;
import com.lora.mianshihou.model.entity.QuestionBankQuestion;
import com.lora.mianshihou.service.QuestionBankQuestionService;
import com.lora.mianshihou.service.QuestionBankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class QuestionBankInitializer {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    /**
     * 每天凌晨2点执行，初始化题库题目总数
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @DistributedLock(key = "questionBank:initCount", leaseTime = 60000)
    public void initQuestionBankCount() {
        log.info("开始初始化题库题目总数");

        try {
            // 1. 查询所有题库
            List<QuestionBank> questionBanks = questionBankService.list();

            // 2. 为每个题库计算实际题目数量
            for (QuestionBank bank : questionBanks) {
                Long bankId = bank.getId();

                // 查询该题库下的实际题目数量
                long count = questionBankQuestionService.count(
                        Wrappers.lambdaQuery(QuestionBankQuestion.class)
                                .eq(QuestionBankQuestion::getQuestionBankId, bankId)
                );

                // 更新题库的题目总数
                bank.setQuestionCount((int) count);
                questionBankService.updateById(bank);
            }

            log.info("题库题目总数初始化完成");
        } catch (Exception e) {
            log.error("初始化题库题目总数失败", e);
            throw new RuntimeException("初始化题库题目总数失败", e);
        }
    }
}