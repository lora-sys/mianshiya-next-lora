import "./index.css";
import { Flex, Menu, MenuProps } from "antd";
import Title from "antd/es/typography/Title";
import { getQuestionVoByIdUsingGet } from "@/api/questionController";
import { getQuestionBankVoByIdUsingGet } from "@/api/questionBankController";
import Sider from "antd/es/layout/Sider";
import { Content } from "antd/es/layout/layout";
import QuestionCard from "@/components/QuestionCard";
import Link from "next/link";
import { doubleRequest } from "@/libs/concurrentRequest";
import { Metadata } from "next";

// 本页面使用服务端渲染，禁用静态生成
export const dynamic = "force-dynamic";


// 动态生成页面元数据
export async function generateMetadata({ params }: any): Promise<Metadata> {
  // 获取路由参数
  const { questionBankId, questionId } = params;

  try {
    // 并发获取题库详情和题目详情
    const [bankRes, questionRes] = await doubleRequest(
      getQuestionBankVoByIdUsingGet,
      {
        id: questionBankId,
        needQueryQuestionList: false, // 不需要题目列表
      },
      getQuestionVoByIdUsingGet,
      {
        id: questionId,
      },
      {
        failFast: false,
        onError: (error, failedIndex) => {
          console.error(`生成元数据时请求 ${failedIndex} 失败:`, error);
        }
      }
    );

    const bank = bankRes?.data;
    const question = questionRes?.data as API.QuestionVO; // 明确指定类型

    if (!bank || !question) {
      throw new Error("题库或题目不存在");
    }

    // 获取题目内容的简短描述（取前100个字符）
    const questionContent = question.content || "";
    const shortContent = questionContent.length > 100
      ? questionContent.substring(0, 100) + "..."
      : questionContent;

    // 获取题目标签 - 使用 tagList 而不是 tags
    const tags = question.tagList || [];
    const tagString = tags.join(", ");

    return {
      title: `${question.title} - ${bank.data?.title} - 面试刷题平台`,
      description: `${question.title} - ${bank.data?.description}。${shortContent}。标签：${tagString}`,
      keywords: `${question.title},${bank.data?.title},面试题目,技术面试,${tagString}`,
      openGraph: {
        title: `${question.title} - ${bank.data?.title}`,
        description: `${bank.data?.description}。${shortContent}`,
        type: "article",
        url: `https://yourdomain.com/bank/${questionBankId}/question/${questionId}`,
        images: [
          {
            url: bank.data?.picture || "/logo.png",
            width: 1200,
            height: 630,
            alt: `${question.title} - ${bank.data?.title}`,
          },
        ],
        tags: tags,
      },
      twitter: {
        card: "summary_large_image",
        title: `${question.title} - ${bank.data?.title}`,
        description: `${bank.data?.description}。${shortContent}`,
        images: [bank.data?.picture || "/logo.png"],
      },
    };
  } catch (error) {
    console.error("生成题目详情页元数据失败:", error);
    // 如果获取数据失败，返回默认元数据
    return {
      title: "题目详情 - 面试刷题平台",
      description: "浏览面试题目详情，获取相关技术领域的面试题目和解答。",
      keywords: "面试题目,技术面试,编程题目,刷题,面试准备",
    };
  }
}



/**
 * 题目题库详情页面组件
 * @constructor
 */
export default async function BankQuestionPage({ params }: any) {
  //获取url的查询参数
  const { questionBankId, questionId } = params;
  //题目列表和总数

  // 并发获取题库详情和题目详情
  let bank: any = undefined;
  let question: any = undefined;

  // try {
  //   // 使用 Promise.all 并发请求
  //   const [bankRes, questionRes] = await Promise.all([
  //     getQuestionBankVoByIdUsingGet({
  //       id: questionBankId,
  //       needQueryQuestionList: true,
  //       //可以扩展为分页实现
  //       pageSize: 200,
  //     }),
  //     getQuestionVoByIdUsingGet({
  //       id: questionId,
  //     })
  //   ]);
  //
  //   bank = bankRes.data;
  //   question = questionRes.data;
  // } catch (e: any) {
  //   // 在服务端渲染时，不能使用 message 组件,全局异常处理改用
  //   console.error("获取题库或题目详情失败，", e.message);
  // }
  const [bankRes, questionRes] = await doubleRequest(
    getQuestionBankVoByIdUsingGet,
    {
      id: questionBankId,
      needQueryQuestionList: true,
      pageSize: 200,
    },
    getQuestionVoByIdUsingGet,
    {
      id: questionId,
    },
    {
      failFast: false, // 不在第一个请求失败时立即返回
      onError: (error, failedIndex) => {
        console.error(`请求 ${failedIndex} 失败:`, error);
        // 可以在这里添加错误日志或通知
      }
    }
  );

  bank = bankRes?.data;
  question = questionRes?.data;


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
  const questionMenuItemList: MenuProps['items'] = (bank.questionPage?.records || []).map(
    (q: API.QuestionVO) => {
      return {
        key: q.id?.toString(),
        label: (
          <Link href={`/bank/${questionBankId}/question/${q.id}`} prefetch={false}>
            {q.title || '未命名题目'}
          </Link>
        ),
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