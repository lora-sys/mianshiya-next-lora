import useSWR from 'swr';
import myAxios from './request';

// SWR fetcher函数
export const fetcher = async (url: string) => {
  const response = await myAxios.get(url);
  return response.data;
};

// 带参数的fetcher
export const fetcherWithParams = async ([url, params]: [string, any]) => {
  const response = await myAxios.get(url, { params });
  return response.data;
};

// POST请求的fetcher
export const poster = async ([url, data]: [string, any]) => {
  const response = await myAxios.post(url, data);
  return response.data;
};

// 自定义SWR Hook
export const useApi = (url: string, config?: any) => {
  const { data, error, mutate, isLoading } = useSWR(url, fetcher, {
    revalidateOnFocus: false, // 窗口聚焦时不自动重新验证
    revalidateOnReconnect: true, // 网络重连时重新验证
    errorRetryCount: 3, // 错误重试次数
    errorRetryInterval: 5000, // 错误重试间隔
    ...config
  });

  return {
    data,
    error,
    mutate,
    isLoading,
    isError: !!error,
    isSuccess: !!data && !error
  };
};

// 带参数的API Hook
export const useApiWithParams = (url: string, params: any, config?: any) => {
  const { data, error, mutate, isLoading } = useSWR(
    params ? [url, params] : null,
    fetcherWithParams,
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: true,
      errorRetryCount: 3,
      errorRetryInterval: 5000,
      ...config
    }
  );

  return {
    data,
    error,
    mutate,
    isLoading,
    isError: !!error,
    isSuccess: !!data && !error
  };
};