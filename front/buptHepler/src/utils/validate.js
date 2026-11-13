// 校验函数统一约定：返回空字符串表示通过，否则返回中文错误提示

export const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,20}$/
export const PHONE_PATTERN = /^1[3-9]\d{9}$/
export const BIO_MAX_LENGTH = 200

export const PASSWORD_HINT =
  '6~20 位，仅限字母和数字，至少 2 个数字，且不能全部为大写或全部为小写'

/** 密码规则见 doc/接口设计-登录模块.md 1.4 */
export function validatePassword(value) {
  const password = String(value ?? '')
  if (!password) return '请输入密码'
  if (password.length < 6) return '密码长度不能少于 6 位'
  if (password.length > 20) return '密码长度不能超过 20 位'
  if (!/^[A-Za-z0-9]+$/.test(password)) return '密码只能包含字母和数字'
  if ((password.match(/\d/g) ?? []).length < 2) return '密码至少包含 2 个数字'
  if (!/[a-z]/.test(password) || !/[A-Z]/.test(password)) {
    return '密码不能全部为大写或全部为小写，需同时包含大写和小写字母'
  }
  return ''
}

export function validateUsername(value) {
  const username = String(value ?? '').trim()
  if (!username) return '请输入用户名'
  if (!USERNAME_PATTERN.test(username)) return '用户名为 3~20 位字母、数字或下划线'
  return ''
}

export function validateRealName(value) {
  const realName = String(value ?? '').trim()
  if (!realName) return '请输入姓名'
  if (realName.length > 20) return '姓名不能超过 20 个字符'
  return ''
}

export function validatePhone(value) {
  const phone = String(value ?? '').trim()
  if (!phone) return '请输入手机号码'
  if (!PHONE_PATTERN.test(phone)) return '请输入正确的 11 位手机号码'
  return ''
}

export function validateBio(value) {
  const bio = String(value ?? '').trim()
  if (bio.length > BIO_MAX_LENGTH) return `简介不能超过 ${BIO_MAX_LENGTH} 个字符`
  return ''
}
