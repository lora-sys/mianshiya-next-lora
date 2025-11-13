"use client";
import "./globals.css";
import { AntdRegistry } from "@ant-design/nextjs-registry";
import React, { useCallback, useEffect, useState } from "react";
import { BasicLayout } from "@/layouts/BasicLayout";
import { Provider, useDispatch } from "react-redux";
import stores, { AppDispatch } from "@/stores";
import { getLoginUserUsingGet } from "@/api/userController";
import { setLoginUser } from "@/stores/loginUser";
import { usePathname } from "next/navigation";
import AccessLayout from "@/app/access/AccessLayout";
import LoginConflictAlert from "@/components/LoginConflictAlert";
import { useCurrentUser } from "@/services/userServvice";
import { SWRProvider } from "@/providers/swr-provider";

/**
 * 全局初始化逻辑
 * @param children
 */
//高级组件设计，初始化状态
const InitLayout: React.FC<
  Readonly<{
    children: React.ReactNode;
  }>
> = ({ children }) => {
  const pathname = usePathname();
  /***
   * 全局初始化函数，有全局单次调用的函数，都可以写到这里
   *
   */
  //初始化全局用户状态
  const dispatch = useDispatch<AppDispatch>();
  // const doInitLoginUser = useCallback(async () => {
  //   try {
  //     const response: any = await getLoginUserUsingGet();
  //
  //     // 如果响应拦截器正常工作，这里应该直接是处理后的数据
  //     // 检查响应结构是拦截器处理后的还是原始的
  //     let result;
  //     if (response.code !== undefined) {
  //       // 响应拦截器已处理，response 就是我们需要的数据
  //       result = response;
  //     } else {
  //       // 响应拦截器未处理，需要使用 response.data
  //       result = response.data;
  //     }
  //
  //     if (
  //       !pathname.startsWith("/user/login") &&
  //       !pathname.startsWith("/user/register")
  //     ) {
  //       if (result.data) {
  //         //更新全局状态
  //         dispatch(setLoginUser(result.data));
  //       } else {
  //         //仅用于测试
  //         // setTimeout(() => {
  //         //   const testUser = {
  //         //     userName: "测试用户",
  //         //     id: 1,
  //         //     userAvatar: "https://www.code-nav.cn/logo.png",
  //         //     userRole: AccessEnum.ADMIN
  //         //   };
  //         //   dispatch(setLoginUser(testUser));
  //         // }, 3000);
  //       }
  //     }
  //   } catch (error) {
  //     // 获取用户信息失败，可能是未登录，不做特殊处理
  //     console.log('获取登录用户信息失败', error);
  //   }
  // // }, [dispatch, pathname]);
  //
  // useEffect(() => {
  //   doInitLoginUser();
  // }, []);

  // 使用SWR获取当前用户信息
  const { data: userData, isLoading, error } = useCurrentUser();

  useEffect(() => {
    if (
      !pathname.startsWith("/user/login") &&
      !pathname.startsWith("/user/register") &&
      userData?.data
    ) {
      dispatch(setLoginUser(userData.data));
    }
  }, [userData, dispatch, pathname]);

  // 处理错误
  useEffect(() => {
    if (error) {
      console.log("获取登录用户信息失败", error);
    }
  }, [error]);

  //这里渲染的dom树子节点，一定要直接渲染整的节点的，不然会导致服务端渲染的结构（容器）和客户端水合接受渲染（布局加容器）的不一致，引发水合问题
  //保持首屏服务端与客户端渲染结构一致：不要再用条件渲染只返回 {children}；你现在 return <> {children} </> 已正确。
  //如需彻底发挥 RSC/SSR：考虑把根 layout.tsx 去掉 "use client"，把需要 hooks 的 Provider/布局迁到一个单独的客户端组件里再包裹（可选，现状 SSR 已可用）。
  // 如果还看到零星 hydration 警告，排查页面中是否有首屏非确定性渲染（如 Date.now()、Math.random()、依赖窗口尺寸/媒体查询的条件 DOM）
  //不返回对象字面量。还是返回函数组件
  return <>{children}</>;
};

const RootLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <html lang="zh">
      <body>
        <AntdRegistry>
          <SWRProvider>
            <Provider store={stores}>
              <InitLayout>
                <BasicLayout>
                  <AccessLayout>
                    {children}
                    <LoginConflictAlert />
                  </AccessLayout>
                </BasicLayout>
              </InitLayout>
            </Provider>
          </SWRProvider>
        </AntdRegistry>
      </body>
    </html>
  );
};
export default RootLayout;
