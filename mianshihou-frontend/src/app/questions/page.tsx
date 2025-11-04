import Title from "antd/es/typography/Title";
import "./index.css";
import { listQuestionVoByPageUsingPost } from "@/api/questionController";
import { message } from "antd";
import QuestionTable from "@/components/QuestionTable";

// 本页面使用服务端渲染，禁用静态生成
export const dynamic = "force-dynamic";

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
