//高级组件设计，初始化状态
import { AppDispatch, RootState } from "@/stores";
import { usePathname } from "next/navigation";
import { useSelector } from "react-redux";
import React, { useEffect, useState } from "react";
import { Spin } from "antd";

import { findAllMenuItemByPath } from "../../../config/menu";
import ACCESS_ENUM from "@/app/access/accessEnum";
import checkAccess from "@/app/access/checkAccess";
import Forbidden from "@/app/forbidden";

//SSR 友好做法：保持首屏 DOM 结构一致，避免 SSR 阶段显示 Spin，可以渲染骨架或“静态壳”，但不要在 SSR 返回时与客户端结构不一致
//服务器端渲染阶段：!isClient，直接渲染大转圈。
// 客户端水合后：还要再等至少 1 秒（isChecking=true）才显示内容。

const AccessLayout: React.FC<
  Readonly<{
    children: React.ReactNode;
  }>
> = ({ children }) => {
  const pathName = usePathname();
  const loginUser = useSelector((state: RootState) => state.loginUser);
  //获取当前路径需要的权限
  const menu = findAllMenuItemByPath(pathName);
  const needAccess = menu?.access ?? ACCESS_ENUM.NOT_LOGIN;
  //判断是否有权限访问
  const canAccess = checkAccess(loginUser, needAccess);

  if (!canAccess) {
    return <Forbidden />;
  }

  return <>{children}</>;
};
export default AccessLayout;
