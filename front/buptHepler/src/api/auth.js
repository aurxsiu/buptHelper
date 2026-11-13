import { request } from './request.js'

/**
 * 用户登录
 * @param {{ username: string, password: string, rememberMe?: boolean }} payload
 * @returns {Promise<{ token: string, tokenType: string, expiresIn: number, user: object }>}
 */
export function login({ username, password, rememberMe = false }) {
  return request('/auth/login', {
    method: 'POST',
    auth: false,
    body: { username, password, rememberMe },
  })
}

/** 注销当前登录状态 */
export function logout() {
  return request('/auth/logout', { method: 'POST' })
}

/** 获取当前登录用户信息，用于刷新页面后恢复登录态 */
export function getCurrentUser() {
  return request('/auth/me')
}

/**
 * 注册普通用户
 * @param {{ username: string, password: string, realName: string, phone: string, bio?: string }} payload
 */
export function register({ username, password, realName, phone, bio }) {
  return request('/auth/register', {
    method: 'POST',
    auth: false,
    body: { username, password, realName, phone, bio },
  })
}

/**
 * 修改个人联系信息与简介（手机号码、用户简介）
 * @param {{ phone: string, bio?: string }} payload
 * @returns {Promise<object>} 修改后的用户信息
 */
export function updateProfile({ phone, bio }) {
  return request('/auth/profile', { method: 'PUT', body: { phone, bio } })
}

/**
 * 修改登录密码
 * @param {{ oldPassword: string, newPassword: string }} payload
 */
export function changePassword({ oldPassword, newPassword }) {
  return request('/auth/password', { method: 'PUT', body: { oldPassword, newPassword } })
}

/**
 * 校验用户名是否可用
 * @param {string} username
 * @returns {Promise<{ username: string, available: boolean }>}
 */
export function isUsernameAvailable(username) {
  return request('/auth/username-available', { query: { username }, auth: false })
}
