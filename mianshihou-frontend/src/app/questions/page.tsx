import Title from "antd/es/typography/Title";
import "./index.css";
import { listQuestionVoByPageUsingPost } from "@/api/questionController";
import { message } from "antd";
import QuestionTable from "@/components/QuestionTable";
import { Metadata } from "next";

// 本页面使用服务端渲染，禁用静态生成
export const dynamic = "force-dynamic";


// 动态生成页面元数据
export async function generateMetadata({ searchParams }: any): Promise<Metadata> {
  // 获取搜索参数
  const { q: searchText } = searchParams;

  try {
    // 获取题目列表数据
    const res: any = await listQuestionVoByPageUsingPost({
      title: searchText,
      pageSize: 12,
      sortField: "createTime",
      sortOrder: "descend",
    });
    const questionList = res.data.records ?? [];
    const total = res.data.total ?? 0;

    // 获取前几个题目名称用于元数据
    const questionTitles = questionList.slice(0, 3).map((question: any) => question.title).join(", ");

    // 根据是否有搜索词生成不同的元数据
    const title = searchText
      ? `搜索"${searchText}"的题目 - 面试刷题平台`
      : "题目大全 - 面试刷题平台";

    const description = searchText
      ? `搜索"${searchText}"的面试题目，找到${total}个相关题目，包括：${questionTitles}等。`
      : `浏览我们的面试题目集合，共${total}道题目，包括：${questionTitles}等。涵盖前端、后端、算法等多个技术领域。`;

    return {
      title,
      description,
      keywords: searchText
        ? `${searchText},面试题目,技术面试,编程题目,前端面试,后端面试,算法面试`
        : "面试题目,技术面试,编程题目,前端面试,后端面试,算法面试,数据结构",
      openGraph: {
        title,
        description,
        type: "website",
        url: searchText
          ? `https://yourdomain.com/questions?q=${encodeURIComponent(searchText)}`
          : "https://yourdomain.com/questions",
        images: [
          {
            url: "/logo.png",
            width: 1200,
            height: 630,
            alt: "面试刷题平台 - 题目大全",
          },
        ],
      },
      twitter: {
        card: "summary_large_image",
        title,
        description,
        images: ["/logo.png"],
      },
    };
  } catch (error) {
    console.error("生成题目列表页元数据失败:", error);
    // 如果获取数据失败，返回默认元数据
    const title = searchText
      ? `搜索"${searchText}"的题目 - 面试刷题平台`
      : "题目大全 - 面试刷题平台";

    return {
      title,
      description: searchText
        ? `搜索"${searchText}"的面试题目，找到相关的技术面试题目。`
        : "浏览我们的面试题目集合，涵盖前端、后端、算法等多个技术领域。",
      keywords: searchText
        ? `${searchText},面试题目,技术面试,编程题目`
        : "面试题目,技术面试,编程题目,前端面试,后端面试,算法面试",
    };
  }
}


/**
 * 题目列表组件
 * @constructor
 */
export default async function QuestionsPage({ searchParams }: any) {
  //获取url的查询参数
  const { q: searchText } = searchParams;
  //题目列表和总数
  let total = 0;
  let questionList = [];

  try {
    const res: any = await listQuestionVoByPageUsingPost({
      title: searchText,
      pageSize: 12,
      sortField: "createTime",
      sortOrder: "descend",
    });
    questionList = res.data.records ?? [];
    total = res.data.total ?? 0;
  } catch (e: any) {
    // 在服务端渲染时，不能使用 message 组件
    message.error("获取题目列表失败，", e.message);
  }

  return (
    <div id="questionsPage" className="max-width-content">
      <Title level={3}>题目大全</Title>
      <QuestionTable
        defaultQuestionList={questionList}
        defaultTotal={total}
        defaultSearchParams={{ title: searchText }}
      />
    </div>
  );
}
