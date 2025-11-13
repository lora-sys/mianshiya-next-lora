"use client";
import { Card, List, Pagination, Spin, Empty } from "antd";
import TagList from "@/components/TagList";
import Link from "next/link";
import { useRelatedQuestions } from "@/services/questionService";
import { useState, useEffect } from "react";
import "./index.css";

interface Props {
  currentQuestionId: string;
  tags?: string[];
  title?: string;
}

/**
 * 相关题目列表组件
 * @param props
 * @constructor
 */
const RelatedQuestions = (props: Props) => {
  const { currentQuestionId, tags = [], title = "相关题目" } = props;
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // 根据当前题目的标签获取相关题目
  const { data, error, isLoading } = useRelatedQuestions({
    current: currentPage,
    pageSize,
    tags: tags.join(","),
    // 排除当前题目
    notId: currentQuestionId,
    // 按相关度排序
    sortField: "createTime",
    sortOrder: "desc",
  });

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const records = (data?.data?.records && Array.isArray(data.data.records)) ? data.data.records : [];
  const total = data?.data?.total || 0;

  return (
    <Card className="related-questions" title={title}>
      {isLoading ? (
        <div className="loading-container">
          <Spin size="large" />
        </div>
      ) : error ? (
        <Empty description="加载相关题目失败" />
      ) : records.length > 0 ? (
        <>
          <List
            dataSource={records}
            renderItem={(item :any) => (
              <List.Item extra={<TagList tagList={item.tagList} />}>
                <List.Item.Meta
                  title={
                    <Link href={`/question/${item.id}`} prefetch={false}>
                      {item.title}
                    </Link>
                  }
                  description={item.content?.substring(0, 100) + "..."}
                />
              </List.Item>
            )}
          />
          {total > pageSize && (
            <div className="pagination-container">
              <Pagination
                current={currentPage}
                total={total}
                pageSize={pageSize}
                onChange={handlePageChange}
                showSizeChanger={false}
                showQuickJumper
                showTotal={(total) => `共 ${total} 条`}
              />
            </div>
          )}
        </>
      ) : (
        <Empty description="暂无相关题目" />
      )}
    </Card>
  );
};

export default RelatedQuestions;