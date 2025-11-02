import { addQuestionBankUsingPost } from '@/api/questionBankController';
import { ProColumns, ProTable } from '@ant-design/pro-components';
import { message, Modal } from 'antd';
import React from 'react';

interface Props {
  visible: boolean;
  columns: ProColumns<API.QuestionBank>[];
  onSubmit: (values: API.QuestionBankAddRequest) => void;
  onCancel: () => void;
}

/**
 * 添加节点
 * @param fields
 */
const handleAdd = async (fields: API.QuestionBankAddRequest) => {
  const hide = message.loading('正在添加');
  try {
    const response: any = await addQuestionBankUsingPost(fields);
    
    // 检查响应结构是拦截器处理后的还是原始的
    let result;
    if (response.code !== undefined) {
      // 响应拦截器已处理，response 就是我们需要的数据
      result = response;
    } else {
      // 响应器未处理，需要使用 response.data
      result = response.data;
    }
    
    hide();
    message.success('创建成功');
    return true;
  } catch (error: any) {
    hide();
    message.error('创建失败，' + (error.message || '未知错误'));
    return false;
  }
};

/**
 * 创建弹窗
 * @param props
 * @constructor
 */
const CreateModal: React.FC<Props> = (props) => {
  const { visible, columns, onSubmit, onCancel } = props;

  return (
    <Modal
      destroyOnClose
      title={'创建'}
      open={visible}
      footer={null}
      onCancel={() => {
        onCancel?.();
      }}
    >
      <ProTable
        type="form"
        columns={columns}
        onSubmit={async (values: API.QuestionBankAddRequest) => {
          const success = await handleAdd(values);
          if (success) {
            onSubmit?.(values);
          }
        }}
      />
    </Modal>
  );
};
export default CreateModal;