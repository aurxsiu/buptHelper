const TOKEN_KEY = 'bupt_helper_token'
const USER_KEY = 'bupt_helper_user'
const REMEMBERED_USERNAME_KEY = 'bupt_helper_remembered_username'

function read(storage, key) {
  try {
    return storage.getItem(key)
  } catch {
    return null
  }
}

function write(storage, key, value) {
  try {
    storage.setItem(key, value)
  } catch {
    // 隐私模式下本地存储可能不可用，忽略即可
  }
}

function remove(storage, key) {
  try {
    storage.removeItem(key)
  } catch {
    // 忽略存储异常
  }
}

/** 读取令牌：勾选“记住我”时存 localStorage，否则存 sessionStorage */
export function getToken() {
  return read(window.localStorage, TOKEN_KEY) ?? read(window.sessionStorage, TOKEN_KEY)
}

export function getUser() {
  const raw = read(window.localStorage, USER_KEY) ?? read(window.sessionStorage, USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

/**
 * 保存登录态
 * @param {{ token: string, user?: object, rememberMe?: boolean }} session
 */
export function saveSession({ token, user, rememberMe = false }) {
  clearSession()
  const storage = rememberMe ? window.localStorage : window.sessionStorage
  write(storage, TOKEN_KEY, token)
  if (user) write(storage, USER_KEY, JSON.stringify(user))
}

/** 刷新缓存中的用户信息，令牌存在哪里就写到哪里 */
export function saveUser(user) {
  const storage = read(window.localStorage, TOKEN_KEY)
    ? window.localStorage
    : window.sessionStorage
  if (!user) {
    remove(storage, USER_KEY)
    return
  }
  write(storage, USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  ;[window.localStorage, window.sessionStorage].forEach((storage) => {
    remove(storage, TOKEN_KEY)
    remove(storage, USER_KEY)
  })
}

export function rememberUsername(username) {
  if (username) write(window.localStorage, REMEMBERED_USERNAME_KEY, username)
}

export function getRememberedUsername() {
  return read(window.localStorage, REMEMBERED_USERNAME_KEY)
}

export function clearRememberedUsername() {
  remove(window.localStorage, REMEMBERED_USERNAME_KEY)
}
