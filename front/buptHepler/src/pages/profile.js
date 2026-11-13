import 'bootstrap/dist/css/bootstrap.min.css'
import '../styles/base.css'

import { changePassword, getCurrentUser, logout, updateProfile } from '../api/auth.js'
import { setUnauthorizedHandler } from '../api/request.js'
import { clearSession, getToken, saveUser } from '../utils/storage.js'
import { PASSWORD_HINT, validateBio, validatePassword, validatePhone } from '../utils/validate.js'
import {
  applyFieldErrors,
  bindPasswordToggle,
  clearAllFields,
  hideAlert,
  rememberFieldHints,
  restoreField,
  setButtonLoading,
  setFieldError,
  showAlert,
} from '../utils/ui.js'

const LOGIN_PAGE = './index.html'

const alertBox = document.querySelector('#profile-alert')
const accountInfo = document.querySelector('#account-info')
const logoutButton = document.querySelector('#logout-button')
const navUser = document.querySelector('#nav-user')
const navRole = document.querySelector('#nav-role')

const contactForm = document.querySelector('#contact-form')
const contactAlert = document.querySelector('#contact-alert')
const contactSubmit = document.querySelector('#contact-submit')
const phoneInput = contactForm.elements.namedItem('phone')
const bioInput = contactForm.elements.namedItem('bio')

const passwordForm = document.querySelector('#password-form')
const passwordAlert = document.querySelector('#password-alert')
const passwordSubmit = document.querySelector('#password-submit')
const oldPasswordInput = passwordForm.elements.namedItem('oldPassword')
const newPasswordInput = passwordForm.elements.namedItem('newPassword')
const confirmPasswordInput = passwordForm.elements.namedItem('confirmPassword')

const ACCOUNT_FIELDS = [
  ['用户名', (user) => user.username],
  ['姓名', (user) => user.realName],
  ['用户类型', (user) => user.userTypeName || user.userType],
  ['注册时间', (user) => user.registerTime],
  ['最近修改', (user) => user.updateTime],
]

function redirectToLogin(params = '') {
  clearSession()
  window.location.replace(`${LOGIN_PAGE}${params}`)
}

setUnauthorizedHandler(() => redirectToLogin('?logout=1'))

function renderAccountInfo(user) {
  navUser.textContent = user.username || ''

  if (user.userTypeName) {
    navRole.textContent = user.userTypeName
    navRole.classList.remove('d-none')
  }

  accountInfo.replaceChildren(
    ...ACCOUNT_FIELDS.flatMap(([label, resolve]) => {
      const term = document.createElement('dt')
      term.className = 'col-5 text-body-secondary fw-normal'
      term.textContent = label

      const detail = document.createElement('dd')
      detail.className = 'col-7 mb-2'
      detail.textContent = String(resolve(user) ?? '—')

      return [term, detail]
    }),
  )
}

function fillForms(user) {
  phoneInput.value = user.phone ?? ''
  bioInput.value = user.bio ?? ''
  oldPasswordInput.value = ''
  newPasswordInput.value = ''
  confirmPasswordInput.value = ''
}

function phoneMessage() {
  return validatePhone(phoneInput.value)
}

function bioMessage() {
  return validateBio(bioInput.value)
}

function newPasswordMessage() {
  const message = validatePassword(newPasswordInput.value)
  if (message) return message
  if (newPasswordInput.value === oldPasswordInput.value) return '新密码不能与原密码相同'
  return ''
}

function confirmPasswordMessage() {
  if (!confirmPasswordInput.value) return '请再次输入新密码'
  if (confirmPasswordInput.value !== newPasswordInput.value) return '两次输入的新密码不一致'
  return ''
}

function bindFieldValidation(input, validator) {
  input.addEventListener('blur', () => {
    const message = validator()
    if (message) setFieldError(input, message)
    else restoreField(input)
  })
  input.addEventListener('input', () => {
    if (input.classList.contains('is-invalid') && !validator()) restoreField(input)
    if (input === newPasswordInput && confirmPasswordInput.value) {
      const message = confirmPasswordMessage()
      if (message) setFieldError(confirmPasswordInput, message)
      else restoreField(confirmPasswordInput)
    }
  })
}

function initForms() {
  rememberFieldHints(contactForm)
  rememberFieldHints(passwordForm)
  document.querySelector('#password-hint').textContent = PASSWORD_HINT
  bindPasswordToggle(document.querySelector('#toggle-password'), newPasswordInput)

  bindFieldValidation(phoneInput, phoneMessage)
  bindFieldValidation(bioInput, bioMessage)
  bindFieldValidation(newPasswordInput, newPasswordMessage)
  bindFieldValidation(confirmPasswordInput, confirmPasswordMessage)
}

async function handleContactSubmit(event) {
  event.preventDefault()
  hideAlert(contactAlert)
  clearAllFields(contactForm)

  const errors = { phone: phoneMessage(), bio: bioMessage() }
  const invalidFields = Object.keys(errors).filter((name) => errors[name])
  if (invalidFields.length > 0) {
    applyFieldErrors(contactForm, errors)
    contactForm.elements.namedItem(invalidFields[0])?.focus()
    return
  }

  const phone = phoneInput.value.trim()
  const bio = bioInput.value.trim()
  if (!window.confirm('确认保存对手机号码和用户简介的修改吗？')) return

  setButtonLoading(contactSubmit, true, { text: '保存中…' })
  try {
    const user = await updateProfile({ phone, bio })
    saveUser(user)
    renderAccountInfo(user)
    fillForms(user)
    clearAllFields(contactForm)
    showAlert(contactAlert, '个人信息修改成功。', 'success')
  } catch (error) {
    const hasFieldErrors = applyFieldErrors(contactForm, error?.details)
    if (!hasFieldErrors) showAlert(contactAlert, error?.message || '保存失败，请稍后重试', 'danger')
  } finally {
    setButtonLoading(contactSubmit, false)
  }
}

async function handlePasswordSubmit(event) {
  event.preventDefault()
  hideAlert(passwordAlert)
  clearAllFields(passwordForm)

  const errors = {
    oldPassword: oldPasswordInput.value ? '' : '请输入原密码',
    newPassword: newPasswordMessage(),
    confirmPassword: confirmPasswordMessage(),
  }
  const invalidFields = Object.keys(errors).filter((name) => errors[name])
  if (invalidFields.length > 0) {
    applyFieldErrors(passwordForm, errors)
    passwordForm.elements.namedItem(invalidFields[0])?.focus()
    return
  }

  if (!window.confirm('确认修改登录密码吗？修改成功后需要使用新密码重新登录。')) return

  setButtonLoading(passwordSubmit, true, { text: '提交中…' })
  try {
    await changePassword({
      oldPassword: oldPasswordInput.value,
      newPassword: newPasswordInput.value,
    })
    clearSession()
    window.location.replace(`${LOGIN_PAGE}?passwordChanged=1`)
  } catch (error) {
    const hasFieldErrors = applyFieldErrors(passwordForm, error?.details)
    if (!hasFieldErrors) showAlert(passwordAlert, error?.message || '修改失败，请稍后重试', 'danger')
    if (error?.code === 40103) oldPasswordInput.focus()
  } finally {
    setButtonLoading(passwordSubmit, false)
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

async function loadProfile() {
  if (!getToken()) {
    redirectToLogin()
    return
  }

  try {
    const user = await getCurrentUser()
    saveUser(user)
    renderAccountInfo(user)
    fillForms(user)
  } catch (error) {
    if (error?.code === 40102 || error?.status === 401) return
    showAlert(alertBox, error?.message || '用户信息加载失败，请刷新重试', 'danger')
  }
}

contactForm.addEventListener('submit', handleContactSubmit)
passwordForm.addEventListener('submit', handlePasswordSubmit)
logoutButton.addEventListener('click', handleLogout)
initForms()
loadProfile()
