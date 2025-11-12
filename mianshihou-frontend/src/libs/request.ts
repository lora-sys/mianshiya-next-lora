/**
 * axios 全局请求配置文件
 *  第三方工具类
 */

import axios from "axios";
import { loginConflictState } from "@/constants/globalState";

// 在服务端渲染时，可能需要通过内部网络访问 API
// 检查是否在服务端环境
const isServer = typeof window === "undefined";
let baseURL = "http://localhost:8101";

// 如果是在服务端环境中，可能需要使用不同的 URL
if (isServer) {
  // 在 Docker 或容器环境中，可能需要访问内部服务
  // 可以通过环境变量配置
  baseURL =
    process.env.INTERNAL_API_URL ||
    process.env.API_URL ||
    "http://localhost:8101";
} else {
  // 浏览器环境中，使用配置的 API 地址或默认值
  baseURL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8101";
}

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL,
  timeout: isServer ? 5000 : 10000, // 服务端渲染时使用更短的超时时间
  withCredentials: !isServer, // 服务端通常不使用 cookies
  // `withCredentials` 表示跨域请求时是否需要使用凭证
});

// 创建请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // 请求执行前执行
    return config;
  },
  function (error) {
    // 处理请求错误
    return Promise.reject(error);
  },
);

// // 创建响应拦截器
// myAxios.interceptors.response.use(
//   // 2xx 响应触发
//   function (response) {
//     // 处理响应数据
//     const { data } = response;
//     // 未登录 - 仅在浏览器环境中处理跳转
//     if (data.code === 40100) {
//       // 检查是否在浏览器环境中
//       if (typeof window !== "undefined") {
//         // 不是获取用户信息接口，或者不是登录页面，则跳转到登录页面
//         const requestUrl = response.config?.url || "";
//         if (
//           !requestUrl.includes("user/get/login") &&
//           !window.location.pathname.includes("/user/login")
//         ) {
//           window.location.href = `/user/login?redirect=${window.location.href}`;
//         }
//       }
//     } else if (data.code !== 0) {
//       // 其他错误
//       throw new Error(data.message ?? "服务器错误");
//     }
//     return data;
//   },
//   // 非 2xx 响应触发
//   function (error) {
//     // 处理响应错误
//     return Promise.reject(error);
//   },
// );
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response;

    // 处理未登录
    if (data.code === 40100) {
      if (typeof window !== "undefined") {
        const requestUrl = response.config?.url || "";
        if (
          !requestUrl.includes("user/get/login") &&
          !window.location.pathname.includes("/user/login")
        ) {
          window.location.href = `/user/login?redirect=${window.location.href}`;
        }
      }
    }
    // 处理登录冲突
    else if (data.code === 40110) {
      if (typeof window !== "undefined") {
        loginConflictState.showConflictAlert();
      }
      return Promise.reject(new Error("登录冲突"));
    }
    // 其他错误
    else if (data.code !== 0) {
      throw new Error(data.message ?? "服务器错误");
    }

    return data;
  },
  function (error) {
    return Promise.reject(error);
  }
);


export default myAxios;
