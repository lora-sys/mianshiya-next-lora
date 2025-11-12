import { useApi, useApiWithParams } from '@/libs/swrConfig';

// 用户相关API
export const useCurrentUser = () => {
  return useApi('/api/user/get/login');
};

export const useUserSignInRecord = (year: number) => {
  return useApiWithParams('/api/user/get/sign_in', { year });
};

// 题库相关API
export const useQuestionBankList = (params: any) => {
  return useApiWithParams('/api/bank/list/page', params);
};

export const useQuestionBankDetail = (id: string) => {
  return useApiWithParams('/api/bank/get', { id });
};