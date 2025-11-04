import "./index.css";
import { Avatar, Button, Card, message } from "antd";
import { getQuestionBankVoByIdUsingGet } from "@/api/questionBankController";
import Meta from "antd/es/card/Meta";
import Paragraph from "antd/es/typography/Paragraph";
import Title from "antd/es/typography/Title";
import QuestionList from "@/components/QuestionList";

// 本页面使用服务端渲染，禁用静态生成
export const dynamic = "force-dynamic";

/**
 * 题库详情页面组件
 * @constructor
 */
export default async function BankPage({ params }: any) {
  //获取url的查询参数
  const { questionBankId } = params;
  //题目列表和总数
  let bank = undefined;

  //获取题库详情
  try {
    const res: any = await getQuestionBankVoByIdUsingGet({
      id: questionBankId,
      needQueryQuestionList: true,
      //可以扩展为分页实现
      pageSize: 200,
    });
    bank = res.data;
  } catch (e: any) {
    // 在服务端渲染时，不能使用 message 组件,全局异常处理改用
   console.error("获取题库详情列表失败，", e.message);
  }

  //错误处理
  if (!bank) {
    return <div>获取题库详情失败，请刷新重试</div>;
  }

  //获取第一道题
  let firstQuestionId;
  if (bank.questionPage?.records && bank.questionPage?.records.length>0) {
    firstQuestionId = bank.questionPage.records[0].id;
  }
  return (
    <div id="bankPage" className="max-width-content">
      <Card>
        <Meta
          avatar={<Avatar src={bank.picture} size={72} />}
          title={<Title level={3}>{bank.title}</Title>}
          description={
            <>
              <Paragraph type="secondary">{bank.descendription}</Paragraph>
              <Button
                type="primary"
                shape="round"
                href={`/bank/${questionBankId}/question/${firstQuestionId}`}
                target="_blank"
                disabled={!firstQuestionId}
              >
                开始刷题
              </Button>
            </>
          }
        />
      </Card>
      <div style={{ marginBottom: 16 }}></div>
      <QuestionList
        questionBankId={questionBankId}
        questionList={bank.questionPage?.records ?? []}
        cardTitle={`题目列表 （${bank.questionPage.total || 0})`}
      ></QuestionList>
    </div>
  );
}
