/**
 *  并发请求
 *  import { doubleRequest } from "@/libs/concurrentRequest";
 * import { getQuestionBankVoByIdUsingGet } from "@/api/questionBankController";
 * import { getQuestionVoByIdUsingGet } from "@/api/questionController";
 *
 * export default async function BankQuestionPage({ params }: any) {
 *   const { questionBankId, questionId } = params;
 *
 *   // 使用并发请求工具
 *   const [bankRes, questionRes] = await doubleRequest(
 *     getQuestionBankVoByIdUsingGet,
 *     {
 *       id: questionBankId,
 *       needQueryQuestionList: true,
 *       pageSize: 200,
 *     },
 *     getQuestionVoByIdUsingGet,
 *     {
 *       id: questionId,
 *     },
 *     {
 *       failFast: false,
 *       onError: (error, failedIndex) => {
 *         console.error(`请求 ${failedIndex} 失败:`, error);
 *       }
 *     }
 *   );
 *
 *   // 检查结果
 *   if (!bankRes) {
 *     return <div>获取题库详情失败，请刷新重试</div>;
 *   }
 *
 *   if (!questionRes) {
 *     return <div>获取题目详情失败，请刷新重试</div>;
 *   }
 *
 *   const bank = bankRes.data;
 *   const question = questionRes.data;
 *
 *   // ... 其余组件代码
 * }
 */
// 并发请求工具函数（修正版）
type ApiFunction<T = any, P = any> = (params: P) => Promise<T>;

interface ConcurrentRequestOptions {
  failFast?: boolean;
  onError?: (error: any, failedIndex: number) => void;
}

interface ConcurrentRequestResult<T extends any[]> {
  data: T;
  success: boolean;
  failedIndexes: number[];
}

/**
 * 核心：T 表示结果元组类型；P 表示参数元组类型（与 T 一一对应）
 * requests: 每个请求的参数类型由 P[K] 指定
 * paramsArray: 必须是与 requests 一一对应的参数元组
 */
export async function concurrentRequest<
  T extends any[],
  P extends { [K in keyof T]: any }
>(
  requests: { [K in keyof T]: ApiFunction<T[K], P[K]> },
  paramsArray: P,
  options: ConcurrentRequestOptions = {}
): Promise<ConcurrentRequestResult<T>> {
  const { failFast = true, onError } = options;
  const failedIndexes: number[] = [];

  // 为了既保留类型，又能按索引调用，我们走手动循环构建 promises 数组
  const promises: Promise<any>[] = [];
  for (let i = 0; i < (requests as any).length; i++) {
    const req = (requests as any)[i] as ApiFunction<any, any>;
    const params = (paramsArray as any)[i];
    const p = req(params).catch((error: any) => {
      failedIndexes.push(i);
      if (onError) onError(error, i);
      if (failFast) throw error;
      return null;
    });
    promises.push(p);
  }

  try {
    const results = await Promise.all(promises);
    return {
      data: results as unknown as T,
      success: failedIndexes.length === 0,
      failedIndexes,
    };
  } catch (error) {
    // 若 failFast 为 true，会进入这里
    return {
      data: [] as unknown as T,
      success: false,
      failedIndexes,
    };
  }
}

/** 双并发版本：显式指定参数元组类型 */
export async function doubleRequest<T1, P1, T2, P2>(
  request1: ApiFunction<T1, P1>,
  params1: P1,
  request2: ApiFunction<T2, P2>,
  params2: P2,
  options: ConcurrentRequestOptions = {}
): Promise<[T1 | null, T2 | null]> {
  const result = await concurrentRequest<[T1, T2], [P1, P2]>(
    [request1, request2],
    [params1, params2],
    options
  );
  return result.data;
}

/** 三并发版本 */
export async function tripleRequest<T1, P1, T2, P2, T3, P3>(
  request1: ApiFunction<T1, P1>,
  params1: P1,
  request2: ApiFunction<T2, P2>,
  params2: P2,
  request3: ApiFunction<T3, P3>,
  params3: P3,
  options: ConcurrentRequestOptions = {}
): Promise<[T1 | null, T2 | null, T3 | null]> {
  const result = await concurrentRequest<[T1, T2, T3], [P1, P2, P3]>(
    [request1, request2, request3],
    [params1, params2, params3],
    options
  );
  return result.data;
}
