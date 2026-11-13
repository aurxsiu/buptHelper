function feedbackOf(input) {
  const form = input.form
  if (!form) return null
  return form.querySelector(`[data-feedback-for="${input.name}"]`)
}

/** 顶部提示条 */
export function showAlert(container, message, variant = 'danger') {
  if (!container) return
  container.className = `alert alert-${variant}`
  container.textContent = message
  container.classList.remove('d-none')
}

export function hideAlert(container) {
  if (!container) return
  container.textContent = ''
  container.className = 'alert d-none'
}

/** 按钮 loading：按钮内需包含 [data-loading-spinner] 与 [data-loading-text] */
export function setButtonLoading(button, loading, { text } = {}) {
  if (!button) return
  const spinner = button.querySelector('[data-loading-spinner]')
  const label = button.querySelector('[data-loading-text]')

  if (loading) {
    if (label) {
      button.dataset.idleText = label.textContent
      if (text) label.textContent = text
    }
    button.disabled = true
    spinner?.classList.remove('d-none')
    return
  }

  button.disabled = false
  spinner?.classList.add('d-none')
  if (label && button.dataset.idleText) label.textContent = button.dataset.idleText
}

export function setFieldError(input, message) {
  if (!input) return
  input.classList.remove('is-valid')
  input.classList.add('is-invalid')
  const feedback = feedbackOf(input)
  if (feedback) {
    feedback.className = 'invalid-feedback d-block'
    feedback.textContent = message
  }
}

export function setFieldSuccess(input, message) {
  if (!input) return
  input.classList.remove('is-invalid')
  input.classList.add('is-valid')
  const feedback = feedbackOf(input)
  if (feedback) {
    feedback.className = 'valid-feedback d-block'
    feedback.textContent = message
  }
}

/** 记录表单内各反馈节点的初始状态，供重置时还原 */
export function rememberFieldHints(root = document) {
  root.querySelectorAll('[data-feedback-for]').forEach((feedback) => {
    feedback.dataset.idleClass = feedback.className
    feedback.dataset.idleText = feedback.textContent
  })
}

/** 恢复单个字段的初始状态，hint 用于覆盖初始提示文案 */
export function restoreField(input, hint) {
  if (!input) return
  input.classList.remove('is-invalid', 'is-valid')
  const feedback = feedbackOf(input)
  if (!feedback) return
  feedback.className = feedback.dataset.idleClass ?? 'form-text'
  feedback.textContent = hint ?? feedback.dataset.idleText ?? ''
}

export function clearAllFields(form) {
  if (!form) return
  form.querySelectorAll('.is-invalid, .is-valid').forEach((element) => {
    element.classList.remove('is-invalid', 'is-valid')
  })
  form.querySelectorAll('[data-feedback-for]').forEach((feedback) => {
    feedback.className = feedback.dataset.idleClass ?? 'form-text'
    feedback.textContent = feedback.dataset.idleText ?? ''
  })
}

/** 渲染后端返回的字段级错误，返回是否至少命中一个字段 */
export function applyFieldErrors(form, errors) {
  if (!form || !errors || typeof errors !== 'object') return false
  let applied = false
  Object.entries(errors).forEach(([name, message]) => {
    const input = form.elements.namedItem(name)
    if (input && input.classList) {
      setFieldError(input, String(message))
      applied = true
    }
  })
  return applied
}

export function bindPasswordToggle(button, input) {
  if (!button || !input) return
  button.addEventListener('click', () => {
    const willShow = input.type === 'password'
    input.type = willShow ? 'text' : 'password'
    button.textContent = willShow ? '隐藏' : '显示'
    button.setAttribute('aria-label', willShow ? '隐藏密码' : '显示密码')
    input.focus({ preventScroll: true })
  })
}
