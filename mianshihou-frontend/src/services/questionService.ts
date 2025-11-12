import { useApiWithParams } from '@/libs/swrConfig';

// 获取相关题目列表
export const useRelatedQuestions = (params: any) => {
  return useApiWithParams('/api/question/list/page/vo', params);
};

// 搜索题目
export const useSearchQuestions = (params: any) => {
  return useApiWithParams('/api/question/search/page/vo', params);
};
