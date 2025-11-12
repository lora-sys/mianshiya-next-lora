import React, { useState, useEffect } from 'react';
import { Alert, Button, Space } from 'antd';
import { clearConflictUsingPost } from '@/api/userController';
import { loginConflictState } from '@/constants/globalState';
import { useSWRConfig } from 'swr';

/**
 * 登录冲突提示Alert组件
 */
const LoginConflictAlert: React.FC = () => {
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const { mutate } = useSWRConfig();

  useEffect(() => {
    // 监听登录冲突事件
    const handleLoginConflict = () => {
      setVisible(true);
    };

    window.addEventListener('login-conflict', handleLoginConflict);

    return () => {
      window.removeEventListener('login-conflict', handleLoginConflict);
    };
  }, []);

  const handleClearConflict = async () => {
    setLoading(true);
    try {
      const result =await clearConflictUsingPost();
      // 检查API返回结果
      if (result.data.code !== 0) {
        throw new Error(result.data.message || '清除冲突状态失败');
      }
      // 清除所有SWR缓存
      mutate(key => typeof key === 'string' && key.includes('/api/user/'), undefined, { revalidate: true });
      loginConflictState.hideConflictAlert();
      setVisible(false);
    } catch (error) {
      console.error('清除冲突状态失败:', error);
    } finally {
      setLoading(false);
    }
  };

  if (!visible) return null;

  return (
    <div style={{
      position: 'fixed',
      top: 20,
      right: 20,
      zIndex: 1000,
      width: 400
    }}>
      <Alert
        message="登录冲突"
        description="您的账号在其他设备上登录，点击确认按钮清除冲突状态后可继续使用"
        type="warning"
        showIcon
        action={
          <Space>
            <Button size="small" type="primary" loading={loading} onClick={handleClearConflict}>
              确认
            </Button>
          </Space>
        }
        closable={false}
      />
    </div>
  );
};

export default LoginConflictAlert;