import { useEffect, useState } from "react";
import { addUserSignInUsingPost } from "@/api/userController";

interface Props {}
/**
 * 添加用户刷题记录钩子
 * @param props
 * @constructor
 */
const useAddUserSignInRecord = () => {
  //签到日期列表（[1,200]） ,表示第一天和第200天签到记录
  const [loading, setLoading] = useState<boolean>(true);

  //请求后端获取数据

  const doFetch = async () => {
    setLoading(true);
    try {
      const res = await addUserSignInUsingPost({});
    } catch (e: any) {
      // 在服务端渲染时，不能使用 message 组件
      console.error("获取刷题记录失败，", e.message);
    }
    setLoading(false);
  };
  //保证只会调用一次
  useEffect(() => {
    doFetch();
  }, []);

  return { loading };
};

export default useAddUserSignInRecord;
