import Title from "antd/es/typography/Title";
import { Divider, Flex } from "antd";
import Link from "next/link";
import { listQuestionBankVoByPageUsingPost } from "@/api/questionBankController";
import { listQuestionVoByPageUsingPost } from "@/api/questionController";
import QuestionBankList from "@/components/QuestionBankList";
import QuestionList from "@/components/QuestionList";
import "./index.css";
import { doubleRequest } from "@/libs/concurrentRequest";
import { Metadata } from "next";

console.log(" ssr on server  ", typeof window); // 判断此时是否是跑在服务端的方法
// 配置页面使用服务端渲染
export const dynamic = "force-dynamic";



//  动态生成页面元数据
export async function generateMetadata(): Promise<Metadata> {
  try {
    // 并发获取最新题库和题目数据
    const [questionBankRes, questionListRes] = await doubleRequest(
      listQuestionBankVoByPageUsingPost,
      {
        pageSize: 12,
        sortField: "createTime",
        sortOrder: "descend",
      },
      listQuestionVoByPageUsingPost,
      {
        pageSize: 12,
        sortField: "createTime",
        sortOrder: "descend",
      }
    );

    const questionBankList = questionBankRes?.data.records ?? [];
    const questionList = questionListRes?.data.records ?? [];

    // 获取最新的题库名称和题目名称用于元数据
    const latestBankNames = questionBankList.slice(0, 3).map((bank: any) => bank.title).join(", ");
    const latestQuestionTitles = questionList.slice(0, 3).map((question: any) => question.title).join(", ");

    return {
      title: "面试刷题平台 - 提升面试技能",
      description: `海量面试题库，涵盖各类技术面试题目。最新题库：${latestBankNames}。最新题目：${latestQuestionTitles}`,
      keywords: "面试,刷题,技术面试,编程,算法,数据结构,前端,后端,全栈",
      openGraph: {
        title: "面试刷题平台 - 提升面试技能",
        description: `海量面试题库，涵盖各类技术面试题目。最新题库：${latestBankNames}`,
        type: "website",
        locale: "zh_CN",
        url: "https://mianshiya.com",
        siteName: "面试刷题平台",
        images: [
          {
            url: "/logo.png",
            width: 1200,
            height: 630,
            alt: "面试刷题平台",
          },
        ],
      },
      twitter: {
        card: "summary_large_image",
        title: "面试刷题平台 - 提升面试技能",
        description: `海量面试题库，涵盖各类技术面试题目。最新题库：${latestBankNames}`,
        images: ["/logo.png"],
      },
      robots: {
        index: true,
        follow: true,
        googleBot: {
          index: true,
          follow: true,
          "max-video-preview": -1,
          "max-image-preview": "large",
          "max-snippet": -1,
        },
      },
    };
  } catch (error) {
    console.error("生成元数据失败:", error);
    // 如果获取数据失败，返回默认元数据
    return {
      title: "面试刷题平台 - 提升面试技能",
      description: "海量面试题库，涵盖各类技术面试题目，助您轻松通过技术面试",
      keywords: "面试,刷题,技术面试,编程,算法,数据结构,前端,后端,全栈",
    };
  }
}




/**
 * 主页
 * @constructor
 */
export default async function HomePage() {
  // let questionBankList: any[] = [];
  // let questionList: any[] = [];
  //
  // try {
  //   const res: any = await listQuestionBankVoByPageUsingPost({
  //     pageSize: 12,
  //     sortField: "createTime",
  //     sortOrder: "descend",
  //   });
  //   questionBankList = res.data.records ?? [];
  // } catch (e: any) {
  //   // 在服务端渲染时，不能使用 message 组件
  //   console.error("获取题库列表失败，", e.message);
  // }
  //
  // try {
  //   const res: any = await listQuestionVoByPageUsingPost({
  //     pageSize: 12,
  //     sortField: "createTime",
  //     sortOrder: "descend",
  //   });
  //   questionList = res.data.records ?? [];
  // } catch (e: any) {
  //   // 在服务端渲染时，不能使用 message 组件
  //   console.error("获取题目列表失败，", e.message);
  // }

  // 并发请求数据
  let questionBankList: any[] = [];
  let questionList: any[] = [];

  // try {
  //   const [questionBankRes, questionlistRes] = await Promise.all([
  //     listQuestionBankVoByPageUsingPost({
  //       pageSize: 12,
  //       sortField: "createTime",
  //       sortOrder: "descend",
  //     }),
  //     listQuestionVoByPageUsingPost({
  //       pageSize: 12,
  //       sortField: "createTime",
  //       sortOrder: "descend",
  //     }),
  //   ]);
  //
  //   questionList = questionlistRes.data.records ?? [];
  //   questionBankList = questionBankRes.data.records ?? [];
  // } catch (e: any) {
  //   // 在服务端渲染时，不能使用 message 组件
  //   console.error("获取题目和题库列表数据失败", e.message);
  // }
  const [questionBankRes, questionListRes] = await doubleRequest(
    listQuestionBankVoByPageUsingPost,
    {
      pageSize: 12,
      sortField: "createTime",
      sortOrder: "descend",
    },
    listQuestionVoByPageUsingPost,
    {
      pageSize: 12,
      sortField: "createTime",
      sortOrder: "descend",
    },
  );
  questionList = questionListRes?.data.records ?? [];
  questionBankList = questionBankRes?.data.records ?? [];
  return (
    <div id="homePage" className="max-width-content">
      <Flex justify="space-between" align="center">
        <Title level={3}>最新题库</Title>
        <Link href={"/banks"}>查看更多</Link>
      </Flex>
      <QuestionBankList questionBankList={questionBankList} />
      <Divider />
      <Flex justify="space-between" align="center">
        <Title level={3}>最新题目</Title>
        <Link href={"/questions"}>查看更多</Link>
      </Flex>
      <QuestionList questionList={questionList} />
    </div>
  );
}
