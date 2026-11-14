import { getCurrentUser, logout } from '../api/auth.js'
import { setUnauthorizedHandler } from '../api/request.js'
import { clearSession, getToken, getUser, saveUser } from './storage.js'
import { setButtonLoading, showAlert } from './ui.js'

export const LOGIN_PAGE = './index.html'

export function redirectToLogin(params = '') {
  clearSession()
  window.location.replace(`${LOGIN_PAGE}${params}`)
}

export function isUnauthorized(error) {
  return error?.code === 40102 || error?.status === 401
}

/**
 * 登录后页面公共初始化：登录态校验、导航栏用户信息、退出登录、未登录跳转。
 * @param {{ alertBox?: Element|null, onUser?: (user: object) => void }} [options]
 * @returns {{ userPromise: Promise<object|null>, alertBox: Element|null }}
 */
export function initPage({ alertBox = document.querySelector('#page-alert'), onUser } = {}) {
  const navUser = document.querySelector('#nav-user')
  const navRole = document.querySelector('#nav-role')
  const logoutButton = document.querySelector('#logout-button')

  setUnauthorizedHandler(() => redirectToLogin('?logout=1'))

  function renderUser(user) {
    if (navUser) navUser.textContent = user.username || user.realName || ''
    if (navRole && user.userTypeName) {
      navRole.textContent = user.userTypeName
      navRole.classList.remove('d-none')
    }
  }

  logoutButton?.addEventListener('click', async () => {
    setButtonLoading(logoutButton, true)
    try {
      await logout()
    } catch {
      // 令牌可能已失效，本地清理照常进行
    } finally {
      redirectToLogin('?logout=1')
    }
  })

  const userPromise = (async () => {
    if (!getToken()) {
      redirectToLogin()
      return null
    }

    const cached = getUser()
    if (cached) renderUser(cached)

    try {
      const user = await getCurrentUser()
      saveUser(user)
      renderUser(user)
      onUser?.(user)
      return user
    } catch (error) {
      if (isUnauthorized(error)) return null
      showAlert(alertBox, error?.message || '用户信息加载失败，请刷新重试', 'danger')
      return null
    }
  })()

  return { userPromise, alertBox }
}
