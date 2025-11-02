"use client";
import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { LoginForm, ProFormText, ProForm } from "@ant-design/pro-components";
import React from "react";
import Image from "next/image";
import Link from "next/link";
import { userLoginUsingPost } from "@/api/userController";
import { message } from "antd";
import { useDispatch } from "react-redux";
import { AppDispatch } from "@/stores";
import { setLoginUser } from "@/stores/loginUser";
import { useRouter } from "next/navigation";

const UserLoginPage: React.FC = () => {
  const [form] = ProForm.useForm();
  const dispatch = useDispatch<AppDispatch>();
  const router = useRouter();

  const doSubmit = async (values: API.UserLoginRequest) => {
    try {
      const response: any = await userLoginUsingPost(values);
      
      // 检查响应结构是拦截器处理后的还是原始的
      let result;
      if (response.code !== undefined) {
        // 响应拦截器已处理，response 就是我们需要的数据
        result = response;
      } else {
        // 响应器未处理，需要使用 response.data
        result = response.data;
      }
      
      if (result.code === 0 && result.data) {
        message.success("登录成功");
        // 保存用户登录态状态
        dispatch(setLoginUser(result.data));
        router.replace("/");
        form.resetFields();
      } else {
        message.error("登录失败：" + (result.message || "未知错误"));
      }
    } catch (e: any) {
      message.error("登录失败：" + (e.message || "未知错误"));
    }
  };

  return (
    <div id="UserLoginPage">
      <LoginForm
        form={form}
        logo={
          <Image src="/assets/logo.jpg" height={44} width={44} alt="面试猴" />
        }
        title="面试猴-用户登录"
        subTitle="程序员面试刷题网站"
        onFinish={doSubmit}
      >
        <>
          <ProFormText
            name="userAccount"
            fieldProps={{
              size: "large",
              prefix: <UserOutlined />,
            }}
            placeholder={"请输入用户名账号"}
            rules={[
              {
                required: true,
                message: "请输入用户账号!",
              },
            ]}
          />
          <ProFormText.Password
            name="userPassword"
            fieldProps={{
              size: "large",
              prefix: <LockOutlined />,
            }}
            placeholder={"密码 "}
            rules={[
              {
                required: true,
                message: "请输入密码！",
              },
            ]}
          />
        </>

        <div
          style={{
            marginBlockEnd: 24,
            textAlign: "end",
          }}
        >
          还没有账号?
          <Link href={"/user/register"}>去注册</Link>
        </div>
      </LoginForm>
    </div>
  );
};
export default UserLoginPage;