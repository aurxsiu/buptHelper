import { request } from './request.js'

/**
 * 分页查询“我的响应”（scope 固定为当前登录用户）
 * @param {object} [params]
 * @param {number|string} [params.status] 0 待接受 / 1 同意 / 2 拒绝 / 3 取消
 * @param {number} [params.page]
 * @param {number} [params.pageSize]
 * @param {string} [params.sort] 形如 createTime,desc
 */
export function fetchMyResponses(params = {}) {
  return request('/responses', {
    query: {
      status: params.status,
      page: params.page,
      pageSize: params.pageSize,
      sort: params.sort,
    },
  })
}

/** 响应详情（响应者本人 / 需求发布者 / 管理员可查看） */
export function fetchResponseDetail(responseId) {
  return request(`/responses/${responseId}`)
}

/** 提交“我来也”响应 */
export function submitResponse(payload) {
  return request('/responses', { method: 'POST', body: payload })
}

/** 修改自己已提交且仍待接受的响应 */
export function updateResponse(responseId, payload) {
  return request(`/responses/${responseId}`, { method: 'PUT', body: payload })
}

/** 取消（删除）自己已提交且仍待接受的响应 */
export function cancelResponse(responseId) {
  return request(`/responses/${responseId}`, { method: 'DELETE' })
}
