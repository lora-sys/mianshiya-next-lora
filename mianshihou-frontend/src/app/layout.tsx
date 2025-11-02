"use client";
import "./globals.css";
import { AntdRegistry } from "@ant-design/nextjs-registry";
import React, { useCallback, useEffect } from "react";
import BasicLayout from "@/layouts/BasicLayout";
import { Provider, useDispatch } from "react-redux";
import stores, { AppDispatch } from "@/stores";
import { getLoginUserUsingGet } from "@/api/userController";
import { setLoginUser } from "@/stores/loginUser";
import { usePathname } from "next/navigation";
import AccessLayout from "@/app/access/AccessLayout";

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
  const doInitLoginUser = useCallback(async () => {
    try {
      const response: any = await getLoginUserUsingGet();
      
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
      
      if (
        !pathname.startsWith("/user/login") &&
        !pathname.startsWith("/user/register")
      ) {
        if (result.data) {
          //更新全局状态
          dispatch(setLoginUser(result.data));
        } else {
          //仅用于测试
          // setTimeout(() => {
          //   const testUser = {
          //     userName: "测试用户",
          //     id: 1,
          //     userAvatar: "https://www.code-nav.cn/logo.png",
          //     userRole: AccessEnum.ADMIN
          //   };
          //   dispatch(setLoginUser(testUser));
          // }, 3000);
        }
      }
    } catch (error) {
      // 获取用户信息失败，可能是未登录，不做特殊处理
      console.log('获取登录用户信息失败', error);
    }
  }, [dispatch, pathname]);

  useEffect(() => {
    doInitLoginUser();
  }, [doInitLoginUser]);
  return children;
};

const RootLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <html lang="zh">
      <body>
        <AntdRegistry>
          <Provider store={stores}>
            <InitLayout>
              <BasicLayout>
                <AccessLayout>{children}</AccessLayout>
              </BasicLayout>
            </InitLayout>
          </Provider>
        </AntdRegistry>
      </body>
    </html>
  );
};
export default RootLayout;
