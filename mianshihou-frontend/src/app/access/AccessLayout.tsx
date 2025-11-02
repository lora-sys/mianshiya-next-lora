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

  // 使用一个状态来判断是否首次加载，避免页面在状态更新时闪烁
  const [isChecking, setIsChecking] = useState(true);

  // 当loginUser状态更新后，检查是否还在初始状态
  useEffect(() => {
    // 判断是否为初始状态（用户角色为NOT_LOGIN且用户名为"未登录"）
    const isInitialState = loginUser?.userRole === ACCESS_ENUM.NOT_LOGIN && 
                          loginUser?.userName === "未登录";
    
    // 如果是管理员页面，且用户处于初始状态，则继续等待
    // 否则停止检查并显示相应内容
    const isAdminPage = needAccess === ACCESS_ENUM.ADMIN;
    if (isAdminPage && isInitialState) {
      // 继续等待，不设置isChecking为false
    } else {
      // 不是管理员页面，或用户状态已更新，停止等待
      setIsChecking(false);
    }
  }, [loginUser, needAccess]);

  // 如果仍在检查中，显示加载状态
  if (isChecking) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!canAccess) {
    return <Forbidden />;
  }

  return children;
};
export default AccessLayout;
