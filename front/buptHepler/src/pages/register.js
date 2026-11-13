import 'bootstrap/dist/css/bootstrap.min.css'
import '../styles/base.css'
import '../styles/auth.css'

import { isUsernameAvailable, register } from '../api/auth.js'
import {
  PASSWORD_HINT,
  validateBio,
  validatePassword,
  validatePhone,
  validateRealName,
  validateUsername,
} from '../utils/validate.js'
import {
  applyFieldErrors,
  bindPasswordToggle,
  clearAllFields,
  hideAlert,
  rememberFieldHints,
  restoreField,
  setButtonLoading,
  setFieldError,
  setFieldSuccess,
  showAlert,
} from '../utils/ui.js'

const form = document.querySelector('#register-form')
const alertBox = document.querySelector('#register-alert')
const submitButton = document.querySelector('#register-submit')

const usernameInput = form.elements.namedItem('username')
const realNameInput = form.elements.namedItem('realName')
const phoneInput = form.elements.namedItem('phone')
const passwordInput = form.elements.namedItem('password')
const confirmPasswordInput = form.elements.namedItem('confirmPassword')
const bioInput = form.elements.namedItem('bio')

const VALIDATORS = {
  username: validateUsername,
  realName: validateRealName,
  phone: validatePhone,
  password: validatePassword,
  bio: validateBio,
  confirmPassword: (value) => {
    if (!value) return '请再次输入密码'
    if (value !== passwordInput.value) return '两次输入的密码不一致'
    return ''
  },
}

let availableUsername = ''

function showFieldMessage(name, message) {
  const input = form.elements.namedItem(name)
  if (!input) return
  if (message) {
    setFieldError(input, message)
    return
  }
  restoreField(input, name === 'username' ? '登录时使用，注册后不可修改' : undefined)
}

function validateField(name) {
  const input = form.elements.namedItem(name)
  if (!input) return true

  const message = VALIDATORS[name](input.value)
  showFieldMessage(name, message)
  return !message
}

/** 用户名失焦时异步校验是否被占用 */
async function checkUsernameAvailability() {
  const username = usernameInput.value.trim()
  if (!username || validateUsername(username)) return
  if (username === availableUsername) return

  try {
    const result = await isUsernameAvailable(username)
    if (usernameInput.value.trim() !== username) return

    if (result?.available) {
      availableUsername = username
      setFieldSuccess(usernameInput, '该用户名可以使用')
    } else {
      availableUsername = ''
      setFieldError(usernameInput, '该用户名已被占用，请更换')
    }
  } catch {
    // 可用性校验失败不阻塞用户，提交时由服务端再次校验
  }
}

function initForm() {
  rememberFieldHints(form)
  document.querySelector('#password-hint').textContent = PASSWORD_HINT
  bindPasswordToggle(document.querySelector('#toggle-password'), passwordInput)

  Object.keys(VALIDATORS).forEach((name) => {
    const input = form.elements.namedItem(name)
    if (!input) return
    input.addEventListener('blur', () => {
      if (input.value) validateField(name)
    })
    input.addEventListener('input', () => {
      if (input.classList.contains('is-invalid')) validateField(name)
      if (name === 'password' && confirmPasswordInput.value) validateField('confirmPassword')
    })
  })

  usernameInput.addEventListener('blur', checkUsernameAvailability)
  usernameInput.focus()
}

function collectPayload() {
  return {
    username: usernameInput.value.trim(),
    password: passwordInput.value,
    realName: realNameInput.value.trim(),
    phone: phoneInput.value.trim(),
    bio: bioInput.value.trim() || undefined,
  }
}

function validateAll() {
  clearAllFields(form)

  const invalidFields = Object.keys(VALIDATORS).filter((name) => !validateField(name))
  if (invalidFields.length > 0) {
    form.elements.namedItem(invalidFields[0])?.focus()
    return false
  }

  return true
}

async function handleSubmit(event) {
  event.preventDefault()
  hideAlert(alertBox)
  if (!validateAll()) return

  const payload = collectPayload()

  setButtonLoading(submitButton, true, { text: '注册中…' })
  try {
    await register(payload)
    const query = `username=${encodeURIComponent(payload.username)}&registered=1`
    window.location.assign(`./index.html?${query}`)
  } catch (error) {
    const hasFieldErrors = applyFieldErrors(form, error?.details)
    if (!hasFieldErrors) showAlert(alertBox, error?.message || '注册失败，请稍后重试', 'danger')
  } finally {
    setButtonLoading(submitButton, false)
  }
}

form.addEventListener('submit', handleSubmit)
initForm()
