//封装次页面为客户端组件，然后从服务端组件引入
"use client";
import {
  listQuestionVoByPageUsingPost,
  searchQuestionVoByPageUsingPost,
} from "@/api/questionController";
import type {
  ActionType,
  ProColumns,
  ProTableProps,
} from "@ant-design/pro-components";
import { ProTable } from "@ant-design/pro-components";
import React, { useRef, useState } from "react";
import TagList from "@/components/TagList";
import { TablePaginationConfig } from "antd";
import Link from "next/link";

//定义属性

interface Props {
  //默认值，用于展示服务端渲染的数据
  defaultQuestionList?: API.QuestionVO[];
  defaultTotal?: number;
  //默认搜索条件
  defaultSearchParams?: API.QuestionQueryRequest;
}

/**
 * 题目表格组件
 *
 * @constructor
 */
const QuestionTablePage: React.FC<Props> = (props) => {
  const actionRef = useRef<ActionType>();
  const { defaultQuestionList, defaultTotal, defaultSearchParams = {} } = props;
  const [questionList, setQuestionList] = useState<API.QuestionVO[]>(
    defaultQuestionList || [],
  );
  //题目总数
  const [total, setTotal] = useState<number>(defaultTotal || 0);
  //是否首次加载
  const [init, setInit] = useState<boolean>(true);
  /**
   * 表格列配置
   */
  const columns: ProColumns<API.QuestionVO>[] = [
    {
      title: "标题",
      dataIndex: "title",
      valueType: "text",
      hideInSearch: true,
      render: (_, record) => {
        return (
          <Link href={`/question/${record.id}`} prefetch={false}>
            {record.title}
          </Link>
        );
      },
    },
    {
      title: "搜索",
      dataIndex: "searchText",
      valueType: "text",
      hideInTable: true,
    },
    {
      title: "标签",
      dataIndex: "tagList",
      valueType: "select",
      fieldProps: {
        mode: "tags",
      },
      render: (_, record) => {
        return <TagList tagList={record?.tagList} />;
      },
    },
  ];

  return (
    <div className="question-table">
      <ProTable<API.Question>
        actionRef={actionRef}
        size={"small"}
        search={{
          labelWidth: 120,
        }}
        form={{
          initialValues: defaultSearchParams,
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
        request={async (params, sort, filter) => {
          // 首次请求
          if (init) {
            setInit(false);
            // 如果已有外层传来的默认数据，直接返回，无需再次查询
            if (defaultQuestionList && defaultTotal) {
              return {
                success: true,
                data: defaultQuestionList,
                total: defaultTotal,
              };
            }
          }

          try {
            const sortField = Object.keys(sort || {})?.[0] || "createTime";
            const sortOrder =
              (sort as Record<string, any>)?.[sortField] || "desc";

            const response: any = await searchQuestionVoByPageUsingPost({
              ...params,
              sortField,
              sortOrder,
              ...filter,
            } as API.QuestionQueryRequest);

            // 响应拦截器已处理，response 就是我们需要的数据
            // 检查响应结构是拦截器处理后的还是原始的
            let result;
            if (response.code !== undefined) {
              // 响应拦截器已处理，response 就是我们需要的数据
              result = response;
            } else {
              // 响应拦截器未处理，需要使用 response.data
              result = response.data;
            }

            // 更新结果
            const newData = (result?.data?.records && Array.isArray(result.data.records) ? result.data.records : []) || (result?.records && Array.isArray(result.records) ? result.records : []);
            const newTotal = result?.data?.total || result?.total || 0;
            // 更新状态
            setQuestionList(newData);
            setTotal(newTotal);

            return {
              success: result?.code === 0 || true,
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
        }}
        columns={columns}
      />
    </div>
  );
};
export default QuestionTablePage;
