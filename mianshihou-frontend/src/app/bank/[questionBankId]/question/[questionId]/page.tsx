import "./index.css";
import { Flex, Menu } from "antd";
import Title from "antd/es/typography/Title";
import { getQuestionVoByIdUsingGet } from "@/api/questionController";
import { getQuestionBankVoByIdUsingGet } from "@/api/questionBankController";
import Sider from "antd/es/layout/Sider";
import { Content } from "antd/es/layout/layout";
import QuestionCard from "@/components/QuestionCard";
import Link from "next/link";

// 本页面使用服务端渲染，禁用静态生成
export const dynamic = "force-dynamic";

/**
 * 题目题库详情页面组件
 * @constructor
 */
export default async function BankQuestionPage({ params }: any) {
  //获取url的查询参数
  const { questionBankId, questionId } = params;
  //题目列表和总数

  // 并发获取题库详情和题目详情
  let bank = undefined;
  let question: any = undefined;
  
  try {
    // 使用 Promise.all 并发请求
    const [bankRes, questionRes] = await Promise.all([
      getQuestionBankVoByIdUsingGet({
        id: questionBankId,
        needQueryQuestionList: true,
        //可以扩展为分页实现
        pageSize: 200,
      }),
      getQuestionVoByIdUsingGet({
        id: questionId,
      })
    ]);
    
    bank = bankRes.data;
    question = questionRes.data;
  } catch (e: any) {
    // 在服务端渲染时，不能使用 message 组件,全局异常处理改用
    console.error("获取题库或题目详情失败，", e.message);
  }
  
  //错误处理
  if (!bank) {
    return <div>获取题库详情失败，请刷新重试</div>;
  }
  
  if (!question) {
    return <div>获取题目详情失败，请刷新重试</div>;
  }

  //获取第一道题
  let firstQuestionId;
  if (bank.questionPage?.records && bank.questionPage?.records.length > 0) {
    firstQuestionId = bank.questionPage.records[0].id;
  }

  //题目菜单列表
  const questionMenuItemList = (bank.questionPage?.records || []).map(
    (q: { title: any; id: any }) => {
      return {
        label: (
          <Link href={`/bank/${questionBankId}/question/${q.id}`} prefetch={false}>
            {q.title}
          </Link>
        ),
        key: q.id,
      };
    },
  );


  return (
    <div id="QuestionPage">
      <Flex gap={24}>
        <Sider width={240} theme={"light"} style={{ padding: "24 0px" }}>
          <Title level={4} style={{ padding: "0 20px" }}>
            {bank.title}
          </Title>
          <Menu items={questionMenuItemList} selectedKeys={[questionId]}></Menu>
        </Sider>
        <Content>
          <QuestionCard question={question} />
        </Content>
      </Flex>
    </div>
  );
}
