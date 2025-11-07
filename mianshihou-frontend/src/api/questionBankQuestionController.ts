// @ts-ignore
/* eslint-disable */
import request from "@/libs/request";

/** addQuestionBank_Question POST /api/questionBank_question/add */
export async function addQuestionBankQuestionUsingPost(
  body: API.QuestionBankAddQuestionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>("/api/questionBank_question/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** batchAddQuestionToBank POST /api/questionBank_question/add/batch */
export async function batchAddQuestionToBankUsingPost(
  body: API.QuestionBankBatchAddQuestionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>(
    "/api/questionBank_question/add/batch",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** deleteQuestionBank_Question POST /api/questionBank_question/delete */
export async function deleteQuestionBankQuestionUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>(
    "/api/questionBank_question/delete",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** getQuestionBank_QuestionVOById GET /api/questionBank_question/get/vo */
export async function getQuestionBankQuestionVoByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getQuestionBankQuestionVOByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseQuestionBankQuestionVO_>(
    "/api/questionBank_question/get/vo",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** listQuestionBank_QuestionByPage POST /api/questionBank_question/list/page */
export async function listQuestionBankQuestionByPageUsingPost(
  body: API.QuestionBankQuestionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageQuestionBankQuestion_>(
    "/api/questionBank_question/list/page",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** listQuestionBank_QuestionVOByPage POST /api/questionBank_question/list/page/vo */
export async function listQuestionBankQuestionVoByPageUsingPost(
  body: API.QuestionBankQuestionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageQuestionBankQuestionVO_>(
    "/api/questionBank_question/list/page/vo",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** listMyQuestionBank_QuestionVOByPage POST /api/questionBank_question/my/list/page/vo */
export async function listMyQuestionBankQuestionVoByPageUsingPost(
  body: API.QuestionBankQuestionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageQuestionBankQuestionVO_>(
    "/api/questionBank_question/my/list/page/vo",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** removeQuestionBank_Question POST /api/questionBank_question/remove */
export async function removeQuestionBankQuestionUsingPost(
  body: API.QuestionBankRemoveQuestionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>(
    "/api/questionBank_question/remove",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** batchRemoveQuestionToBank POST /api/questionBank_question/remove/batch */
export async function batchRemoveQuestionToBankUsingPost(
  body: API.QuestionBankBatchRemoveQuestionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>(
    "/api/questionBank_question/remove/batch",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** updateQuestionBank_Question POST /api/questionBank_question/update */
export async function updateQuestionBankQuestionUsingPost(
  body: API.QuestionBankQuestionUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>(
    "/api/questionBank_question/update",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}
