import "./index.css";
import { Avatar, Button, Card, Flex, Menu } from "antd";
import Meta from "antd/es/card/Meta";
import Paragraph from "antd/es/typography/Paragraph";
import Title from "antd/es/typography/Title";
import QuestionList from "@/components/QuestionList";
import { getQuestionVoByIdUsingGet } from "@/api/questionController";
import { getQuestionBankVoByIdUsingGet } from "@/api/questionBankController";
import Sider from "antd/es/layout/Sider";
import { Content } from "antd/es/layout/layout";
import QuestionVO = API.QuestionVO;
import QuestionCard from "@/components/QuestionCard";
import Link from "next/link";

// 本页面使用服务端渲染，禁用静态生成
export const dynamic = "force-dynamic";

/**
 * 题目题库详情页面组件
 * @constructor
 */
export default async function QuestionPage({ params }: any) {
  //获取url的查询参数
  const { questionId } = await params;
  //题目列表和总数

  //获取题目详情
  let question: API.QuestionVO | undefined = undefined;
  try {
    const res : any = await getQuestionVoByIdUsingGet({
      id: questionId,
    });
    question = res.data;
  } catch (e: any) {
    // 在服务端渲染时，不能使用 message 组件,全局异常处理改用
    console.error("获取题目详情列表失败，", e.message);
  }
  //错误处理
  if (!question) {
    return <div>获取题目详情失败，请刷新重试</div>;
  }

  return (
    <div id="questionPage">
      <QuestionCard question={question} />
    </div>
  );
}
