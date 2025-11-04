//封装次页面为客户端组件，然后从服务端组件引入
"use client";
import { listQuestionVoByPageUsingPost } from "@/api/questionController";
import type {
  ActionType,
  ProColumns,
  ProTableProps,
} from "@ant-design/pro-components";
import { ProTable } from "@ant-design/pro-components";
import React, { useRef, useState } from "react";
import TagList from "@/components/TagList";
import { TablePaginationConfig } from "antd";

//定义属性

interface Props {
  //默认值，用于展示服务端渲染的数据
  defaultQuestionList?: API.QuestionVO[];
  defaultTotal?: number;
}

/**
 * 题目表格组件
 *
 * @constructor
 */
const QuestionTablePage: React.FC<Props> = (props) => {
  const actionRef = useRef<ActionType>();
  const { defaultQuestionList, defaultTotal } = props;
  //题目列表
  const [questionList, setQuestionList] = useState<API.QuestionVO[]>(
    defaultQuestionList || [],
  );
  //题目总数
  const [total, setTotal] = useState<number>(defaultTotal || 0);

  /**
   * 表格列配置
   */
  const columns: ProColumns<API.Question>[] = [
    {
      title: "标题",
      dataIndex: "title",
      valueType: "text",
    },
    {
      title: "标签",
      dataIndex: "tags",
      valueType: "select",
      fieldProps: {
        mode: "tags",
      },
      render: (_, record) => {
        const tagList = JSON.parse(record.tags || "[]");
        return <TagList tagList={tagList} />;
      },
    },
  ];

  // 使用与 ProTable 同源的类型，避免不同版本类型不匹配
  const requestData: ProTableProps<
    API.Question,
    API.QuestionQueryRequest
  >["request"] = async (
    params,
    sort,
    filter,
  ) => {
    // 从 sort 对象中提取排序字段和顺序
    const sortField = Object.keys(sort || {})?.[0];
    const sortOrder = (sort as Record<string, any>)?.[sortField] ?? undefined;

    try {
      const { data, code }: any = await listQuestionVoByPageUsingPost({
        ...params,
        sortField,
        sortOrder,
        ...filter,
      } as API.QuestionQueryRequest);

      const newData: API.QuestionVO[] = data?.records || [];
      const newTotal: number = Number(data?.total || 0);

      // 同步到受控 dataSource（首屏 SSR -> CSR 的平滑过渡）
      setQuestionList(newData);
      setTotal(newTotal);

      return {
        success: code === 0,
        data: newData,
        total: newTotal,
      };
    } catch (error) {
      console.error("获取题目列表失败:", error);
      return {
        success: false,
        data: [],
        total: 0,
      };
    }
  };

  return (
    <div className="question-table">
      <ProTable<API.Question>
        actionRef={actionRef}
        size={"small"}
        search={{
          labelWidth: 120,
        }}
        pagination={
          {
            pageSize: 12,
            showTotal: (total) => `总共 ${total}条`,
            showSizeChanger: false,
            total,
          } as TablePaginationConfig
        }
        dataSource={questionList}
        request={requestData}
        columns={columns}
      />
    </div>
  );
};
export default QuestionTablePage;
