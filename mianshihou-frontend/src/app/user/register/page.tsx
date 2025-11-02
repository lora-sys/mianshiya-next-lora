"use client";
import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { LoginForm, ProForm, ProFormText } from "@ant-design/pro-components";
import React from "react";
import Image from "next/image";
import Link from "next/link";
import { userRegisterUsingPost } from "@/api/userController";
import { message } from "antd";
import { useDispatch } from "react-redux";
import { AppDispatch } from "@/stores";
import { setLoginUser } from "@/stores/loginUser";
import { useRouter } from "next/navigation";

/**
 * 用户注册
 * @constructor
 */
const UserRegisterPage: React.FC = () => {
  const [form] = ProForm.useForm();
  const dispatch = useDispatch<AppDispatch>();
  const router = useRouter();

  const doSubmit = async (values: API.UserRegisterRequest) => {
    try {
      const response: any = await userRegisterUsingPost(values);

      // 检查响应结构是拦截器处理后的还是原始的
      let result;
      if (response.code !== undefined) {
        // 响应拦截器已处理，response 就是我们需要的数据
        result = response;
      } else {
        // 响应拦截器未处理，需要使用 response.data
        result = response.data;
      }

      if (result.data) {
        message.success("注册成功，请登录");
        // 保存用户登录态状态
        dispatch(setLoginUser(result.data));
        //前往登录页
        router.replace("/user/login");
        form.resetFields();
      } else {
        message.error("注册失败：用户数据为空");
      }
    } catch (e: any) {
      message.error("注册失败：" + (e.message || "未知错误"));
    }
  };

  return (
    <div id="UserRegisterPage">
      <LoginForm
        form={form}
        logo={
          <Image src="/assets/logo.jpg" height={44} width={44} alt="面试猴" />
        }
        title="面试猴-用户注册"
        subTitle="程序员面试刷题网站"
        submitter={{
          searchConfig: {
            submitText: "注册",
          },
        }}
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
          <ProFormText.Password
            name="checkPassword"
            fieldProps={{
              size: "large",
              prefix: <LockOutlined />,
            }}
            placeholder={"密码 "}
            rules={[
              {
                required: true,
                message: "请输入确认密码！",
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
          已经有账号?
          <Link href={"/user/login"}>去登录</Link>
        </div>
      </LoginForm>
    </div>
  );
};
export default UserRegisterPage;
