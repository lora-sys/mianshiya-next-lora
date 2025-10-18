package generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import generator.domain.QuestionBank;
import generator.service.QuestionBankService;
import generator.mapper.QuestionBankMapper;
import org.springframework.stereotype.Service;

/**
* @author yanBingZhao
* @description 针对表【question_bank(题库)】的数据库操作Service实现
* @createDate 2025-10-18 15:44:00
*/
@Service
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank>
    implements QuestionBankService{

}




