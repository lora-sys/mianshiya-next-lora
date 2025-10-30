//高级组件设计，初始化状态
import { AppDispatch, RootState } from "@/stores";
import { usePathname } from "next/navigation";
import { useSelector } from "react-redux";
import React from "react";

import { findAllMenuItemByPath } from "../../../config/menu";
import ACCESS_ENUM from "@/app/access/accessEnum";
import checkAccess from "@/app/access/checkAccess";
import Forbidden from "@/app/forbidden";

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

  return children;
};
export default AccessLayout;
