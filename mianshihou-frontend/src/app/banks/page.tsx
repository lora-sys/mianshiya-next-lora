import Title from "antd/es/typography/Title";
import { listQuestionBankVoByPageUsingPost } from "@/api/questionBankController";
import QuestionBankList from "@/components/QuestionBankList";
import "./index.css";

// 本页面使用服务端渲染，禁用静态生成
export const dynamic = "force-dynamic";

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
    questionBankList = res.data.records ?? [];
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
