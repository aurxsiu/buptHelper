import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import '../styles/base.css'

import { getCurrentUser, logout } from '../api/auth.js'
import { setUnauthorizedHandler } from '../api/request.js'
import { clearSession, getToken, saveUser } from '../utils/storage.js'
import { setButtonLoading, showAlert } from '../utils/ui.js'

const LOGIN_PAGE = './index.html'

const alertBox = document.querySelector('#home-alert')
const logoutButton = document.querySelector('#logout-button')
const navUser = document.querySelector('#nav-user')
const navRole = document.querySelector('#nav-role')
const welcomeTitle = document.querySelector('#welcome-title')
const profileList = document.querySelector('#profile-list')

const PROFILE_FIELDS = [
  ['用户名', (user) => user.username],
  ['姓名', (user) => user.realName],
  ['用户类型', (user) => user.userTypeName || user.userType],
  ['手机号码', (user) => user.phone],
  ['用户简介', (user) => user.bio || '—'],
  ['注册时间', (user) => user.registerTime],
]

function redirectToLogin(params = '') {
  clearSession()
  window.location.replace(`${LOGIN_PAGE}${params}`)
}

setUnauthorizedHandler(() => redirectToLogin('?logout=1'))

function renderProfile(user) {
  const displayName = user.realName || user.username || '用户'
  welcomeTitle.textContent = `欢迎回来，${displayName}`
  navUser.textContent = user.username || ''

  if (user.userTypeName) {
    navRole.textContent = user.userTypeName
    navRole.classList.remove('d-none')
  }

  profileList.replaceChildren(
    ...PROFILE_FIELDS.flatMap(([label, resolve]) => {
      const term = document.createElement('dt')
      term.className = 'col-sm-3 text-body-secondary fw-normal'
      term.textContent = label

      const detail = document.createElement('dd')
      detail.className = 'col-sm-9 mb-2'
      detail.textContent = String(resolve(user) ?? '—')

      return [term, detail]
    }),
  )
}

async function loadProfile() {
  if (!getToken()) {
    redirectToLogin()
    return
  }

  try {
    const user = await getCurrentUser()
    saveUser(user)
    renderProfile(user)
  } catch (error) {
    if (error?.code === 40102 || error?.status === 401) return
    showAlert(alertBox, error?.message || '用户信息加载失败，请刷新重试', 'danger')
  }
}

async function handleLogout() {
  setButtonLoading(logoutButton, true)
  try {
    await logout()
  } catch {
    // 令牌可能已失效，本地清理照常进行
  } finally {
    redirectToLogin('?logout=1')
  }
}

logoutButton.addEventListener('click', handleLogout)
loadProfile()
