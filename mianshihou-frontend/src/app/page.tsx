import Title from "antd/es/typography/Title";
import { Divider, Flex } from "antd";
import Link from "next/link";
import { listQuestionBankVoByPageUsingPost } from "@/api/questionBankController";
import { listQuestionVoByPageUsingPost } from "@/api/questionController";
import QuestionBankList from "@/components/QuestionBankList";
import QuestionList from "@/components/QuestionList";
import "./index.css";

console.log(" ssr on server  ",typeof window) // 判断此时是否是跑在服务端的
// 配置页面使用服务端渲染
export const dynamic = "force-dynamic";

/**
 * 主页
 * @constructor
 */
export default async function HomePage() {
  let questionBankList: any[] = [];
  let questionList : any[] = [];
  
  try {
    const res :any = await listQuestionBankVoByPageUsingPost({
      pageSize: 12,
      sortField: "createTime",
      sortOrder: "descend",
    });
    questionBankList = res.data.records ?? [];
  } catch (e:any) {
    // 在服务端渲染时，不能使用 message 组件
    console.error("获取题库列表失败，", e.message);
  }

  try {
    const res :any = await listQuestionVoByPageUsingPost({
      pageSize: 12,
      sortField: "createTime",
      sortOrder: "descend",
    });
    questionList = res.data.records ?? [];
  } catch (e :any) {
    // 在服务端渲染时，不能使用 message 组件
    console.error("获取题目列表失败，", e.message);
  }

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
