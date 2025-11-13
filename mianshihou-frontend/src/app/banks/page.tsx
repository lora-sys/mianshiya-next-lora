import Title from "antd/es/typography/Title";
import { listQuestionBankVoByPageUsingPost } from "@/api/questionBankController";
import QuestionBankList from "@/components/QuestionBankList";
import "./index.css";
import { Metadata } from "next";

// 本页面使用服务端渲染，禁用静态生成
export const dynamic = "force-dynamic";



// 动态生成页面元数据
export async function generateMetadata(): Promise<Metadata> {
  try {
    // 获取题库列表数据
    const res: any = await listQuestionBankVoByPageUsingPost({
      pageSize: 12,
      sortField: "createTime",
      sortOrder: "descend",
    });
    const questionBankList = res.data?.records && Array.isArray(res.data.records) ? res.data.records : [];

    // 获取前几个题库名称用于元数据
    const bankNames = questionBankList.slice(0, 3).map((bank: any) => bank.title).join(", ");

    return {
      title: "题库列表 - 面试刷题平台",
      description: `浏览我们的面试题库集合，包括：${bankNames}等。涵盖前端、后端、算法等多个技术领域的面试题目。`,
      keywords: "面试题库,技术面试,编程题库,前端面试,后端面试,算法面试,数据结构",
      openGraph: {
        title: "题库列表 - 面试刷题平台",
        description: `浏览我们的面试题库集合，包括：${bankNames}等。涵盖前端、后端、算法等多个技术领域的面试题目。`,
        type: "website",
        url: "https://yourdomain.com/banks",
        images: [
          {
            url: "/logo.png",
            width: 1200,
            height: 630,
            alt: "面试刷题平台 - 题库列表",
          },
        ],
      },
      twitter: {
        card: "summary_large_image",
        title: "题库列表 - 面试刷题平台",
        description: `浏览我们的面试题库集合，包括：${bankNames}等。涵盖前端、后端、算法等多个技术领域的面试题目。`,
        images: ["/logo.png"],
      },
    };
  } catch (error) {
    console.error("生成题库列表页元数据失败:", error);
    // 如果获取数据失败，返回默认元数据
    return {
      title: "题库列表 - 面试刷题平台",
      description: "浏览我们的面试题库集合，涵盖前端、后端、算法等多个技术领域的面试题目。",
      keywords: "面试题库,技术面试,编程题库,前端面试,后端面试,算法面试,数据结构",
    };
  }
}
/**
 * 主页
 * @constructor
 */
export default async function BanksPage() {
  let questionBankList = [];
  try {
    const res: any = await listQuestionBankVoByPageUsingPost({
      pageSize: 12,
      sortField: "createTime",
      sortOrder: "descend",
    });
    questionBankList = res.data?.records && Array.isArray(res.data.records) ? res.data.records : [];
  } catch (e: any) {
    // 在服务端渲染时，不能使用 message 组件
    console.error("获取题库列表失败，", e.message);
  }

  return (
    <div id="BanksPage" className="max-width-content">
      <Title level={3}>最新题库</Title>

      <QuestionBankList questionBankList={questionBankList} />
    </div>
  );
}
