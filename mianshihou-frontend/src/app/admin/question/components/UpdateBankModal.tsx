import { Form, message, Modal, Select } from "antd";
import React, { useEffect, useState } from "react";
import {
  addQuestionBankQuestionUsingPost,
  listQuestionBankQuestionVoByPageUsingPost,
  removeQuestionBankQuestionUsingPost,
} from "@/api/questionBankQuestionController";
import { listQuestionBankVoByPageUsingPost } from "@/api/questionBankController";

interface Props {
  questionId?: number;
  visible: boolean;
  onCancel: () => void;
}

/**
 * 更新题目所属题库弹窗
 * @param props
 * @constructor
 */
const UpdateBankModal: React.FC<Props> = (props) => {
  const { questionId, visible, onCancel } = props;
  const [form] = Form.useForm();
  const [questionBankList, setQuestionBankList] = useState<
    API.QuestionBankVO[]
  >([]);

  // 使用Promise.all并发获取数据
  const initModalData = async () => {
    if (!questionId) {
      return;
    }

    try {
      console.log("查询题目ID:", questionId);
      
      // 使用Promise.all并发请求
      const [questionBankQuestionRes, questionBankRes] = await Promise.all([
        // 获取题目所属题库列表
        listQuestionBankQuestionVoByPageUsingPost({
          questionId: questionId,
          pageSize: 20,
        }),
        // 获取所有题库列表
        listQuestionBankVoByPageUsingPost({
          pageSize: 20,
          sortField: "createTime",
          sortOrder: "descend",
        }),
      ]);
      
      const relatedBankIds = (questionBankQuestionRes.data?.records ?? [])
        .filter((item: any) => item.questionId?.toString() === questionId?.toString())
        .map((item: any) => item.questionBankId);
      console.log("题目关联的题库ID列表:", relatedBankIds);
      
      // 处理获取到的题库列表
      setQuestionBankList(questionBankRes.data?.records ?? []);
      
      // 设置表单值
      form.setFieldValue("questionBankIdList" as any, relatedBankIds);

    } catch (e: any) {
      message.error("获取数据失败，" + e.message);
    }
  };

  useEffect(() => {
    console.log("useEffect 触发, questionId:", questionId, "visible:", visible);
    if (visible && questionId) {
      initModalData();
    }
  }, [questionId, visible]);

  return (
    <Modal
      destroyOnHidden
      title={"更新所属题库"}
      open={visible}
      footer={null}
      onCancel={() => {
        form.resetFields();
        onCancel?.();
      }}
      afterOpenChange={(open) => {
        console.log("Modal 开关状态:", open);
        if (!open) {
          form.resetFields();
        }
      }}
    >
      <Form 
        form={form} 
        style={{ marginTop: 24 }}
        onValuesChange={(changedValues, allValues) => {
          console.log("表单值变化:", changedValues, allValues);
        }}
      >
        <Form.Item label="所属题库" name="questionBankIdList">
          <Select
            mode="multiple"
            style={{ width: "100%" }}
            placeholder="请选择所属题库"
            options={questionBankList.map((questionBank) => {
              console.log("题库选项:", questionBank);
              return {
                label: questionBank.title,
                value: questionBank.id,
              };
            })}
            onSelect={async (value) => {
              console.log("选择题库ID:", value);
              const hide = message.loading("正在更新");
              try {
                await addQuestionBankQuestionUsingPost({
                  questionId,
                  questionBankId: value,
                });
                hide();
                message.success("绑定题库成功");
              } catch (error: any) {
                hide();
                message.error("绑定题库失败，" + error.message);
              }
            }}
            onDeselect={async (value) => {
              console.log("取消选择题库ID:", value);
              const hide = message.loading("正在更新");
              try {
                await removeQuestionBankQuestionUsingPost({
                  questionId,
                  questionBankId: value,
                });
                hide();
                message.success("取消绑定题库成功");
              } catch (error: any) {
                hide();
                message.error("取消绑定题库失败，" + error.message);
              }
            }}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};
export default UpdateBankModal;
