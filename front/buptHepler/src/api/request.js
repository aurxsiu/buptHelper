import { clearSession, getToken } from '../utils/storage.js'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/+$/, '')
const DEFAULT_TIMEOUT = 15000

const HTTP_STATUS_MESSAGE = {
  400: '请求参数有误',
  401: '登录状态已失效，请重新登录',
  403: '没有访问权限',
  404: '请求的资源不存在',
  409: '数据冲突，请刷新后重试',
  429: '操作过于频繁，请稍后再试',
  500: '服务器内部错误，请稍后再试',
  502: '无法连接服务器，请确认后端服务已启动',
  503: '服务暂不可用，请稍后再试',
  504: '服务器响应超时，请稍后再试',
}

/** 统一的接口错误对象，code 对应 doc/接口设计-登录模块.md 1.3 */
export class ApiError extends Error {
  constructor(message, { code = -1, status = 0, details = null } = {}) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.details = details
  }
}

let unauthorizedHandler = null

/** 注册“未登录 / 登录过期”处理函数，由各页面按需设置 */
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

function handleUnauthorized() {
  clearSession()
  if (typeof unauthorizedHandler === 'function') unauthorizedHandler()
}

function buildUrl(path, query) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = new URL(`${API_BASE_URL}${normalizedPath}`, window.location.origin)

  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, String(value))
      }
    })
  }

  return url.toString()
}

async function parseBody(response) {
  if (response.status === 204) return null
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

/**
 * 发送请求并解包统一响应结构 { code, message, data }
 * @param {string} path 以 / 开头的接口路径，例如 /auth/login
 * @param {object} [options]
 * @param {'GET'|'POST'|'PUT'|'DELETE'} [options.method]
 * @param {object} [options.body] 请求体，自动 JSON 序列化
 * @param {object} [options.query] URL 查询参数
 * @param {object} [options.headers] 额外请求头
 * @param {boolean} [options.auth] 是否携带令牌，默认 true
 * @param {number} [options.timeout] 超时时间（毫秒）
 * @param {AbortSignal} [options.signal] 外部取消信号
 * @returns {Promise<any>} 成功时返回响应体中的 data
 */
export async function request(
  path,
  { method = 'GET', body, query, headers = {}, auth = true, timeout = DEFAULT_TIMEOUT, signal } = {},
) {
  const finalHeaders = { Accept: 'application/json', ...headers }
  const payload = body === undefined ? undefined : JSON.stringify(body)

  if (payload !== undefined && !finalHeaders['Content-Type']) {
    finalHeaders['Content-Type'] = 'application/json'
  }

  const token = getToken()
  if (auth && token) finalHeaders.Authorization = `Bearer ${token}`

  const controller = new AbortController()
  const timer = window.setTimeout(
    () => controller.abort(new DOMException('请求超时', 'TimeoutError')),
    timeout,
  )
  const abortByCaller = () => controller.abort(signal.reason)

  if (signal) {
    if (signal.aborted) abortByCaller()
    else signal.addEventListener('abort', abortByCaller, { once: true })
  }

  let response
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers: finalHeaders,
      body: payload,
      signal: controller.signal,
    })
  } catch (error) {
    if (error?.name === 'AbortError' || error?.name === 'TimeoutError') {
      if (signal?.aborted) throw new ApiError('请求已取消', { code: -3 })
      throw new ApiError('请求超时，请检查网络后重试', { code: -2 })
    }
    throw new ApiError('无法连接服务器，请确认后端服务已启动', { code: -1 })
  } finally {
    window.clearTimeout(timer)
    signal?.removeEventListener('abort', abortByCaller)
  }

  const data = await parseBody(response)
  const envelope = data && typeof data === 'object' && 'code' in data ? data : null
  const code = envelope?.code

  if (!response.ok) {
    if (response.status === 401 || code === 40102) handleUnauthorized()
    const message =
      envelope?.message || HTTP_STATUS_MESSAGE[response.status] || `请求失败（${response.status}）`
    throw new ApiError(message, {
      code: code ?? response.status * 100,
      status: response.status,
      details: envelope?.data ?? null,
    })
  }

  if (envelope) {
    if (envelope.code !== 0) {
      if (envelope.code === 40102) handleUnauthorized()
      throw new ApiError(envelope.message || '请求失败', {
        code: envelope.code,
        status: response.status,
        details: envelope.data ?? null,
      })
    }
    return envelope.data ?? null
  }

  // 兼容非统一结构的响应
  return data
}

export const http = {
  get: (path, options) => request(path, { ...options, method: 'GET' }),
  post: (path, body, options) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => request(path, { ...options, method: 'PUT', body }),
  delete: (path, options) => request(path, { ...options, method: 'DELETE' }),
}
