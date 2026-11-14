import { request } from './request.js'

/**
 * 分页查询“请帮忙”需求
 * @param {object} [params]
 * @param {'mine'|'all'} [params.scope] mine：我发布的；all：全部（“我来也”模块使用）
 * @param {number|string} [params.status] 0 已发布 / -1 已取消
 * @param {string} [params.typeCode] 帮忙类型编码
 * @param {number|string} [params.regionId] 地域标识
 * @param {string} [params.keyword] 主题/描述关键词
 * @param {number} [params.page] 页码，从 1 开始
 * @param {number} [params.pageSize] 每页条数
 * @returns {Promise<{ list: object[], page: number, pageSize: number, total: number, totalPages: number, hasPrev: boolean, hasNext: boolean }>}
 */
export function fetchRequests(params = {}) {
  return request('/requests', {
    query: {
      scope: params.scope ?? 'mine',
      status: params.status,
      typeCode: params.typeCode,
      regionId: params.regionId,
      keyword: params.keyword,
      page: params.page,
      pageSize: params.pageSize,
      sort: params.sort,
    },
  })
}

/** 查询需求详情 */
export function fetchRequestDetail(requestId) {
  return request(`/requests/${requestId}`)
}

/**
 * 发布“请帮忙”需求
 * @param {{ title: string, typeCode: string, regionId: number, description: string, attachmentIds: number[] }} payload
 */
export function createRequest(payload) {
  return request('/requests', { method: 'POST', body: payload })
}

/** 修改自己发布且尚无响应的需求 */
export function updateRequest(requestId, payload) {
  return request(`/requests/${requestId}`, { method: 'PUT', body: payload })
}

/** 删除（逻辑取消）自己发布且尚无响应的需求 */
export function deleteRequest(requestId) {
  return request(`/requests/${requestId}`, { method: 'DELETE' })
}

/**
 * 查询某需求收到的“我来也”响应（仅需求发布者 / 管理员）
 * @param {number} requestId
 * @param {{ page?: number, pageSize?: number, status?: number|string }} [params]
 */
export function fetchRequestResponses(requestId, params = {}) {
  return request(`/requests/${requestId}/responses`, {
    query: { page: params.page, pageSize: params.pageSize, status: params.status },
  })
}

/** 接受响应 */
export function acceptResponse(responseId) {
  return request(`/responses/${responseId}/accept`, { method: 'PUT' })
}

/** 拒绝响应 */
export function rejectResponse(responseId) {
  return request(`/responses/${responseId}/reject`, { method: 'PUT' })
}

/** 查询用户公开信息（查看发布者 / 响应者介绍） */
export function fetchUserProfile(userId) {
  return request(`/users/${userId}/profile`)
}

/** 帮忙类型字典 */
export function fetchRequestTypes() {
  return request('/requests/types')
}

/** 地域字典 */
export function fetchRegions() {
  return request('/regions')
}
