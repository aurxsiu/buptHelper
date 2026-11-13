import 'bootstrap/dist/css/bootstrap.min.css'
import '../styles/base.css'
import '../styles/auth.css'

import { getCurrentUser, login } from '../api/auth.js'
import { setUnauthorizedHandler } from '../api/request.js'
import {
  clearRememberedUsername,
  clearSession,
  getRememberedUsername,
  getToken,
  rememberUsername,
  saveSession,
} from '../utils/storage.js'
import {
  applyFieldErrors,
  bindPasswordToggle,
  clearAllFields,
  hideAlert,
  rememberFieldHints,
  restoreField,
  setButtonLoading,
  showAlert,
} from '../utils/ui.js'

const DEFAULT_REDIRECT = './home.html'

const form = document.querySelector('#login-form')
const alertBox = document.querySelector('#login-alert')
const submitButton = document.querySelector('#login-submit')
const usernameInput = form.elements.namedItem('username')
const passwordInput = form.elements.namedItem('password')
const rememberInput = form.elements.namedItem('rememberMe')

const searchParams = new URLSearchParams(window.location.search)

setUnauthorizedHandler(() => {
  // 登录页本身无需跳转，仅清理本地残留状态
  clearSession()
})

function displayName(user) {
  return user?.realName || user?.username || '朋友'
}

/** 仅允许站内相对地址回跳，避免开放重定向 */
function resolveRedirect() {
  const raw = searchParams.get('redirect')
  if (!raw) return DEFAULT_REDIRECT
  const isExternal = /^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(raw) || raw.startsWith('//')
  return isExternal ? DEFAULT_REDIRECT : raw
}

/** 校验本地令牌是否仍然有效，有效则直接进入系统 */
async function restoreSession() {
  if (!getToken()) return

  setButtonLoading(submitButton, true, { text: '正在恢复登录状态…' })
  try {
    const user = await getCurrentUser()
    showAlert(alertBox, `检测到登录状态，欢迎回来，${displayName(user)}！正在进入系统…`, 'success')
    window.location.replace(resolveRedirect())
  } catch {
    // 令牌失效：保持登录页，允许重新登录
    clearSession()
  } finally {
    setButtonLoading(submitButton, false)
  }
}

function initForm() {
  rememberFieldHints(form)
  bindPasswordToggle(document.querySelector('#toggle-password'), passwordInput)

  const usernameFromQuery = searchParams.get('username')
  const remembered = getRememberedUsername()

  if (usernameFromQuery) usernameInput.value = usernameFromQuery
  else if (remembered) {
    usernameInput.value = remembered
    rememberInput.checked = true
  }

  if (searchParams.get('registered') === '1') {
    showAlert(alertBox, '注册成功，请使用新账号登录。', 'success')
  } else if (searchParams.get('passwordChanged') === '1') {
    showAlert(alertBox, '密码修改成功，请使用新密码重新登录。', 'success')
  } else if (searchParams.get('logout') === '1') {
    showAlert(alertBox, '已安全退出登录。', 'success')
  }

  if (usernameInput.value) passwordInput.focus()
  else usernameInput.focus()

  usernameInput.addEventListener('input', () => restoreField(usernameInput))
  passwordInput.addEventListener('input', () => restoreField(passwordInput))
}

function validate() {
  clearAllFields(form)
  const errors = {}

  if (!usernameInput.value.trim()) errors.username = '请输入用户名'
  if (!passwordInput.value) errors.password = '请输入密码'

  if (Object.keys(errors).length > 0) {
    applyFieldErrors(form, errors)
    const firstInvalid = Object.keys(errors)[0]
    form.elements.namedItem(firstInvalid)?.focus()
    return false
  }

  return true
}

async function handleSubmit(event) {
  event.preventDefault()
  hideAlert(alertBox)
  if (!validate()) return

  const username = usernameInput.value.trim()
  const password = passwordInput.value
  const rememberMe = rememberInput.checked

  setButtonLoading(submitButton, true, { text: '登录中…' })
  try {
    const result = await login({ username, password, rememberMe })
    saveSession({ token: result.token, user: result.user, rememberMe })

    if (rememberMe) rememberUsername(username)
    else clearRememberedUsername()

    showAlert(alertBox, `登录成功，欢迎回来，${displayName(result.user)}！`, 'success')
    window.setTimeout(() => window.location.assign(resolveRedirect()), 600)
  } catch (error) {
    const hasFieldErrors = applyFieldErrors(form, error?.details)
    showAlert(alertBox, error?.message || '登录失败，请稍后重试', 'danger')
    if (!hasFieldErrors) {
      passwordInput.focus()
      passwordInput.select()
    }
  } finally {
    setButtonLoading(submitButton, false)
  }
}

form.addEventListener('submit', handleSubmit)
initForm()
restoreSession()
