"use client";

import { GithubFilled, LogoutOutlined, UserOutlined } from "@ant-design/icons";
import { ProLayout } from "@ant-design/pro-components";
import { Dropdown, message } from "antd";
import React from "react";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import Link from "next/link";
import GlobalFooter from "@/components/GlobalFooter";
import "./index.css";
import { menus } from "../../../config/menu";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "@/stores";
import getAccessibleMenus from "@/app/access/menuAccess";
import { userLogoutUsingPost } from "@/api/userController";
import { setLoginUser } from "@/stores/loginUser";
import { DEFAULT_USER } from "@/constants/user";
import SearchInput from "@/layouts/BasicLayout/component/searchInput";

interface Props {
  children: React.ReactNode;
}

export function BasicLayout({ children }: Props) {
  //登录用户信息
  const loginUser = useSelector((state: RootState) => state.loginUser);

  const pathname = usePathname();

  const dispatch = useDispatch<AppDispatch>();
  const router = useRouter();
  const userLogout = async () => {
    try {
      const response: any = await userLogoutUsingPost();

      // 如果响应拦截器正常工作，这里应该直接是处理后的数据
      // 检查响应结构是拦截器处理后的还是原始的
      let result;
      if (response.code !== undefined) {
        // 响应拦截器已处理，response 就是我们需要的数据
        result = response;
      } else {
        // 响应拦截器未处理，需要使用 response.data
        result = response.data;
      }

      message.success("已经退出登录");
      dispatch(setLoginUser(DEFAULT_USER));
      router.push("/user/login");
    } catch (e: any) {
      message.error("注销失败：" + (e.message || "未知错误"));
    }
  };

  return (
    <div
      id="basiclayout"
      style={{
        height: "100vh",
        overflow: "auto",
      }}
    >
      <ProLayout
        title="面试猴刷题平台"
        logo={
          <Image
            src="/assets/logo.jpg"
            height={32}
            width={32}
            alt="面试猴刷题平台-作者lora"
            style={{ width: "auto", height: "auto" }}
          />
        }
        layout="top"
        location={{
          pathname,
        }}
        avatarProps={{
          src: loginUser?.userAvatar || "/assets/logo.png",
          size: "small",
          title: loginUser?.userName || "面试猴",
          render: (props, dom) => {
            if (!loginUser.id) {
              return (
                <div onClick={() => router.push("/user/login")}>{dom}</div>
              );
            }
            return (
              <Dropdown
                menu={{
                  items: [
                    {
                      key: "userCenter",
                      icon: <UserOutlined />,
                      label: "个人中心",
                    },
                    {
                      key: "logout",
                      icon: <LogoutOutlined />,
                      label: "退出登录",
                    },
                  ],
                  onClick: async (event: { key: React.Key }) => {
                    const { key } = event;
                    if (key === "logout") {
                      await userLogout();
                    } else if (key === "userCenter") {
                      router.push("/user/center");
                    }
                  },
                }}
              >
                {dom}
              </Dropdown>
            );
          },
        }}
        actionsRender={(props) => {
          if (props.isMobile) return [];
          //如果当前页面包含/questions ,则不显示搜索框
          if (pathname.includes("/questions")) {
            return [
              <a
                key="github"
                href="https://github.com/liyupi/mianshiya-next"
                target="_blank"
              >
                <GithubFilled key="GithubFilled" />
              </a>,
            ];
          }
          return [
            <SearchInput key="search" />,
            <a
              key="github"
              href="https://github.com/liyupi/mianshiya-next"
              target="_blank"
            >
              <GithubFilled key="GithubFilled" />
            </a>,
          ];
        }}
        headerTitleRender={(logo, title, _) => {
          return (
            <a>
              {logo}
              {title}
            </a>
          );
        }}
        //渲染底部栏
        footerRender={() => {
          return <GlobalFooter />;
        }}
        menuDataRender={() => {
          return getAccessibleMenus(loginUser, menus);
        }}
        onMenuHeaderClick={(e) => console.log(e)}
        //定义了菜单项如何渲染
        menuItemRender={(item, dom) => (
          <Link href={item.path || "/"} target={item.target}>
            {dom}
          </Link>
        )}
      >
        {children}
      </ProLayout>
    </div>
  );
}
