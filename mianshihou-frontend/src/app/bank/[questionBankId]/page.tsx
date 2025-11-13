import "./index.css";
import { Avatar, Button, Card, message } from "antd";
import { getQuestionBankVoByIdUsingGet } from "@/api/questionBankController";
import Meta from "antd/es/card/Meta";
import Paragraph from "antd/es/typography/Paragraph";
import Title from "antd/es/typography/Title";
import QuestionList from "@/components/QuestionList";
import { Metadata } from "next";

// 本页面使用服务端渲染，禁用静态生成
export const dynamic = "force-dynamic";
// 动态生成页面元数据
export async function generateMetadata({ params }: any): Promise<Metadata> {
  // 获取路由参数
  const { questionBankId } = params;

  try {
    // 获取题库详情数据
    const res: any = await getQuestionBankVoByIdUsingGet({
      id: questionBankId,
      needQueryQuestionList: false, // 这里不需要获取题目列表，只需要基本信息
    });
    const bank = res.data;

    if (!bank) {
      throw new Error("题库不存在");
    }

    // 获取题目总数
    const questionCount = bank.questionCount || 0;

    return {
      title: `${bank.title} - 面试刷题平台`,
      description: `${bank.description}。本题库包含${questionCount}道面试题目，涵盖相关技术领域的核心知识点。`,
      keywords: `${bank.title},面试题库,技术面试,编程题目,刷题,面试准备`,
      openGraph: {
        title: `${bank.title} - 面试刷题平台`,
        description: `${bank.description}。本题库包含${questionCount}道面试题目，涵盖相关技术领域的核心知识点。`,
        type: "website",
        url: `https://yourdomain.com/bank/${questionBankId}`,
        images: [
          {
            url: bank.picture || "/logo.png",
            width: 1200,
            height: 630,
            alt: bank.title,
          },
        ],
      },
      twitter: {
        card: "summary_large_image",
        title: `${bank.title} - 面试刷题平台`,
        description: `${bank.description}。本题库包含${questionCount}道面试题目。`,
        images: [bank.picture || "/logo.png"],
      },
    };
  } catch (error) {
    console.error("生成题库详情页元数据失败:", error);
    // 如果获取数据失败，返回默认元数据
    return {
      title: "题库详情 - 面试刷题平台",
      description: "浏览面试题库详情，获取相关技术领域的面试题目。",
      keywords: "面试题库,技术面试,编程题目,刷题,面试准备",
    };
  }
}
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
  if (bank.questionPage?.records && Array.isArray(bank.questionPage.records) && bank.questionPage.records.length > 0) {
    firstQuestionId = bank.questionPage.records[0]?.id;
  }
  return (
    <div id="bankPage" className="max-width-content">
      <Card>
        <Meta
          avatar={<Avatar src={bank.picture} size={72} />}
          title={<Title level={3}>{bank.title}</Title>}
          description={
            <>
              <Paragraph type="secondary">{bank.description}</Paragraph>
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
