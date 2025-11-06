"use client";
import "./index.css";
import { Card } from "antd";
import Title from "antd/es/typography/Title";
import TagList from "@/components/TagList";
import MdViewer from "@/components/MdViewer";
import useAddUserSignInRecord from "@/hooks/useAddUserSignInRecord";

interface Props {
  question: API.QuestionVO;
}

/**
 * 题目卡片
 * @param props
 * @constructor
 */
const QuestionCard = (props: Props) => {
  const { question } = props;
  //签到钩子
 useAddUserSignInRecord();
  return (
    <div className="question-card">
      <Card>
        <Title level={1} style={{ fontSize: 24 }}>
          {question.title}
        </Title>
        <TagList tagList={question.tagList} />
        <div style={{ marginBottom: "16px" }}></div>
        <MdViewer value={question.content}></MdViewer>
      </Card>
      <Card title="推荐答案">
        <MdViewer value={question.answer}></MdViewer>
      </Card>
    </div>
  );
};

export default QuestionCard;
