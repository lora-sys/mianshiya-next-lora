"use client";

import {
  GithubFilled,
  LogoutOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import { ProConfigProvider, ProLayout } from "@ant-design/pro-components";
import { Dropdown, Input, theme } from "antd";
import React, { useState } from "react";
import Image from "next/image";
import { usePathname } from "next/navigation";
import Link from "next/link";
import GlobalFooter from "@/components/GlobalFooter";
import "./index.css";
import { menus } from "../../../config/menu";
import { listQuestionBankVoByPageUsingPost } from "@/api/questionBankController";
import { useSelector } from "react-redux";
import { RootState } from "@/stores";
import getAccessibleMenus from "@/app/access/menuAccess";
import MdEditor from "@/components/MdEditor";
import MdViewer from "@/components/MdViewer";

//搜索条
const SearchInput = () => {
  const { token } = theme.useToken();
  return (
    <div
      key="SearchOutlined"
      aria-hidden
      style={{
        display: "flex",
        alignItems: "center",
        marginInlineEnd: 24,
      }}
      onMouseDown={(e) => {
        e.stopPropagation();
        e.preventDefault();
      }}
    >
      <Input
        style={{
          borderRadius: 4,
          marginInlineEnd: 12,
          backgroundColor: token.colorBgTextHover,
        }}
        prefix={
          <SearchOutlined
            style={{
              color: token.colorTextLightSolid,
            }}
          />
        }
        placeholder="搜索题目"
        variant="borderless"
      />
    </div>
  );
};

interface Props {
  children: React.ReactNode;
}

/**
 *
 * 通用布局
 * @param children
 * @constructor
 */
export default function BasicLayout({ children }: Props) {
  //登录用户信息
  const loginUser = useSelector((state: RootState) => state.loginUser);

  const pathname = usePathname();
  const [text,setText] = useState<string>("");

  listQuestionBankVoByPageUsingPost({}).then((res) => {
    console.log(res);
  });

  return (
    <div
      id="basiclayout"
      style={{
        height: "100vh",
        overflow: "auto",
      }}
    >
      <ProConfigProvider hashed={false}>
        <ProLayout
          title="面试猴刷题平台"
          logo={
            <Image
              src="/assets/logo.jpg"
              height={32}
              width={32}
              alt="面试猴刷题平台-作者lora"
            />
          }
          layout="top"
          location={{
            pathname,
          }}
          menu={{
            collapsedShowGroupTitle: true,
          }}
          siderMenuType="group"
          avatarProps={{
            src: loginUser?.userAvatar || "/assets/logo.png",
            size: "small",
            title: loginUser?.userName || "面试猴",
            render: (props, dom) => {
              return (
                <Dropdown
                  menu={{
                    items: [
                      {
                        key: "logout",
                        icon: <LogoutOutlined />,
                        label: "退出登录",
                      },
                    ],
                  }}
                >
                  {dom}
                </Dropdown>
              );
            },
          }}
          actionsRender={(props) => {
            if (props.isMobile) return [];
            return [
              <SearchInput key="search" />,
              <a
                key="github"
                href="https://github.com/lora-sys/mianshiya-next-lora.git"
                target="_blank"
                rel="noreferrer noopener"
              >
                <GithubFilled />
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
            return getAccessibleMenus(loginUser,menus);
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
      </ProConfigProvider>
    </div>
  );
}
