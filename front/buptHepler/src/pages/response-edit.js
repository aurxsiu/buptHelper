import 'bootstrap/dist/css/bootstrap.min.css'
import '../styles/base.css'
import '../styles/requests.css'

import {
  IMAGE_MAX_COUNT,
  VIDEO_MAX_COUNT,
  removeUploadedFile,
  uploadFile,
  validateUploadFile,
} from '../api/files.js'
import { fetchRequestDetail } from '../api/requests.js'
import {
  fetchResponseDetail,
  submitResponse,
  updateResponse,
} from '../api/responses.js'
import { formatDateTime, regionLabel, resolveAssetUrl } from '../utils/display.js'
import { initPage, isUnauthorized, redirectToLogin } from '../utils/page.js'
import {
  applyFieldErrors,
  clearAllFields,
  hideAlert,
  rememberFieldHints,
  restoreField,
  setButtonLoading,
  setFieldError,
  showAlert,
} from '../utils/ui.js'

const DESCRIPTION_MAX = 1000

const params = new URLSearchParams(window.location.search)
const requestId = Number(params.get('requestId'))
const responseId = Number(params.get('responseId'))
const isEdit = Number.isInteger(responseId) && responseId > 0

const alertBox = document.querySelector('#page-alert')
const formAlert = document.querySelector('#form-alert')
const pageTitle = document.querySelector('#page-title')
const pageDesc = document.querySelector('#page-desc')
const form = document.querySelector('#response-form')
const descriptionInput = form.elements.namedItem('description')
const submitButton = document.querySelector('#response-submit')
const submitLabel = submitButton.querySelector('[data-loading-text]')
const cancelLink = document.querySelector('#cancel-link')
const descriptionCounter = document.querySelector('#description-counter')

const contextCard = document.querySelector('#request-context-card')
const contextTitle = document.querySelector('#context-title')
const contextMeta = document.querySelector('#context-meta')
const contextDescription = document.querySelector('#context-description')
const contextLink = document.querySelector('#context-link')

const imageInput = document.querySelector('#image-input')
const imageDrop = document.querySelector('#image-drop')
const imageList = document.querySelector('#image-list')
const imageError = document.querySelector('#image-error')
const videoInput = document.querySelector('#video-input')
const videoDrop = document.querySelector('#video-drop')
const videoList = document.querySelector('#video-list')
const videoError = document.querySelector('#video-error')

const uploadProgress = document.querySelector('#upload-progress')
const uploadProgressBar = document.querySelector('#upload-progress-bar')
const uploadProgressText = document.querySelector('#upload-progress-text')

const state = {
  requestId: Number.isInteger(requestId) && requestId > 0 ? requestId : null,
  responseId: isEdit ? responseId : null,
  attachments: [],
  uploading: false,
  submitting: false,
  ready: false,
}

function updateCounter() {
  descriptionCounter.textContent = `${descriptionInput.value.trim().length}/${DESCRIPTION_MAX}`
}

function attachmentsOf(mediaType) {
  return state.attachments.filter((item) => item.mediaType === mediaType)
}

function refreshControls() {
  const disabled = state.uploading
  imageDrop.classList.toggle('is-disabled', disabled)
  videoDrop.classList.toggle('is-disabled', disabled)
  imageInput.disabled = disabled
  videoInput.disabled = disabled
  if (!state.submitting) submitButton.disabled = disabled || !state.ready
}

function setAttachmentError(mediaType, message) {
  const target = mediaType === 'VIDEO' ? videoError : imageError
  target.className = message ? 'invalid-feedback d-block' : 'invalid-feedback d-none'
  target.textContent = message
}

function setProgress(percent, text) {
  uploadProgress.classList.remove('d-none')
  const value = Math.min(100, Math.max(0, Math.round(percent)))
  uploadProgressBar.style.width = `${value}%`
  uploadProgress.querySelector('.progress')?.setAttribute('aria-valuenow', String(value))
  uploadProgressText.textContent = text
}

function hideProgress() {
  uploadProgress.classList.add('d-none')
  uploadProgressBar.style.width = '0%'
  uploadProgressText.textContent = ''
}

function createAttachmentNode(file) {
  const wrapper = document.createElement('figure')
  wrapper.className = 'upload-item'

  if (file.mediaType === 'VIDEO') {
    const video = document.createElement('video')
    video.src = resolveAssetUrl(file.url)
    video.controls = true
    video.preload = 'metadata'
    wrapper.append(video)
  } else {
    const image = document.createElement('img')
    image.src = resolveAssetUrl(file.url)
    image.alt = file.fileName || '响应图片'
    image.loading = 'lazy'
    wrapper.append(image)
  }

  const removeButton = document.createElement('button')
  removeButton.type = 'button'
  removeButton.className = 'btn-close upload-item__remove'
  removeButton.setAttribute('aria-label', `移除 ${file.fileName || '附件'}`)
  removeButton.disabled = state.uploading
  removeButton.addEventListener('click', () => removeAttachment(file))

  const caption = document.createElement('figcaption')
  caption.className = 'upload-item__name small text-body-secondary text-truncate'
  caption.textContent = file.fileName || ''

  wrapper.append(removeButton, caption)
  return wrapper
}

function renderAttachments() {
  imageList.replaceChildren(...attachmentsOf('IMAGE').map(createAttachmentNode))
  videoList.replaceChildren(...attachmentsOf('VIDEO').map(createAttachmentNode))
  imageDrop.classList.toggle('d-none', attachmentsOf('IMAGE').length >= IMAGE_MAX_COUNT)
  videoDrop.classList.toggle('d-none', attachmentsOf('VIDEO').length >= VIDEO_MAX_COUNT)
}

async function removeAttachment(file) {
  if (state.uploading) return
  state.attachments = state.attachments.filter((item) => item !== file)
  renderAttachments()
  setAttachmentError(file.mediaType, '')
  if (file.temporary && file.fileId) {
    try {
      await removeUploadedFile(file.fileId)
    } catch {
      // 临时文件清理失败不影响主流程
    }
  }
}

async function addFiles(fileList, mediaType) {
  if (state.uploading) return

  const files = Array.from(fileList ?? [])
  if (files.length === 0) return

  const limit = mediaType === 'VIDEO' ? VIDEO_MAX_COUNT : IMAGE_MAX_COUNT
  const existing = attachmentsOf(mediaType)
  const accepted = []
  const messages = []
  const label = mediaType === 'VIDEO' ? '视频' : '图片'

  files.forEach((file) => {
    if (existing.length + accepted.length >= limit) {
      messages.push(`最多上传 ${limit} 个${label}`)
      return
    }
    const message = validateUploadFile(file, mediaType)
    if (message) {
      messages.push(`${file.name}：${message}`)
      return
    }
    accepted.push(file)
  })

  setAttachmentError(mediaType, messages.join('；'))
  if (accepted.length === 0) return

  state.uploading = true
  refreshControls()
  renderAttachments()

  try {
    for (let index = 0; index < accepted.length; index += 1) {
      const file = accepted[index]
      const prefix = `正在上传 ${index + 1}/${accepted.length}：${file.name}`
      setProgress((index / accepted.length) * 100, `${prefix}（0%）`)
      const uploaded = await uploadFile(file, {
        onProgress: (percent) => {
          setProgress(((index + percent / 100) / accepted.length) * 100, `${prefix}（${percent}%）`)
        },
      })
      state.attachments.push({ ...uploaded, mediaType: uploaded.mediaType || mediaType, temporary: true })
      renderAttachments()
    }
    setProgress(100, '上传完成')
    window.setTimeout(hideProgress, 800)
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    setAttachmentError(mediaType, error?.message || '文件上传失败，请稍后重试')
    hideProgress()
  } finally {
    state.uploading = false
    refreshControls()
    renderAttachments()
  }
}

function bindDropZone(drop, input, mediaType) {
  drop.addEventListener('click', () => {
    if (!state.uploading) input.click()
  })
  drop.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      if (!state.uploading) input.click()
    }
  })
  ;['dragenter', 'dragover'].forEach((type) => {
    drop.addEventListener(type, (event) => {
      event.preventDefault()
      if (!state.uploading) drop.classList.add('is-dragover')
    })
  })
  ;['dragleave', 'drop'].forEach((type) => {
    drop.addEventListener(type, () => drop.classList.remove('is-dragover'))
  })
  drop.addEventListener('drop', (event) => {
    event.preventDefault()
    addFiles(event.dataTransfer?.files, mediaType)
  })
  input.addEventListener('change', () => {
    const files = Array.from(input.files ?? [])
    input.value = ''
    addFiles(files, mediaType)
  })
}

function validateDescription() {
  const value = descriptionInput.value.trim()
  if (!value) return '请输入响应描述'
  if (value.length > DESCRIPTION_MAX) return `响应描述不能超过 ${DESCRIPTION_MAX} 个字符`
  return ''
}

function bindDescriptionValidation() {
  descriptionInput.addEventListener('blur', () => {
    const message = validateDescription()
    if (message) setFieldError(descriptionInput, message)
    else restoreField(descriptionInput)
  })
  descriptionInput.addEventListener('input', () => {
    updateCounter()
    if (descriptionInput.classList.contains('is-invalid') && !validateDescription()) {
      restoreField(descriptionInput)
    }
  })
}

function renderContext(request) {
  contextCard.classList.remove('d-none')
  contextTitle.textContent = request.title || '需求详情'
  contextLink.href = `./request-detail.html?id=${request.requestId}`
  contextMeta.textContent = [
    request.typeName,
    request.regionFullName || regionLabel(request),
    `发布于 ${formatDateTime(request.createTime)}`,
  ]
    .filter(Boolean)
    .join(' · ')
  if (request.description) {
    contextDescription.textContent = request.description
    contextDescription.classList.remove('d-none')
  } else {
    contextDescription.classList.add('d-none')
  }
}

async function handleSubmit(event) {
  event.preventDefault()
  hideAlert(formAlert)
  clearAllFields(form)

  if (state.uploading) {
    showAlert(formAlert, '附件正在上传，请等待上传完成后再提交。', 'warning')
    return
  }

  const descriptionError = validateDescription()
  if (descriptionError) {
    applyFieldErrors(form, { description: descriptionError })
    descriptionInput.focus()
    return
  }

  const confirmed = window.confirm(isEdit ? '确认保存对该响应的修改吗？' : '确认提交这条“我来也”响应吗？')
  if (!confirmed) return

  const payload = {
    description: descriptionInput.value.trim(),
    attachmentIds: state.attachments.map((item) => item.fileId),
  }

  state.submitting = true
  setButtonLoading(submitButton, true, { text: isEdit ? '保存中…' : '提交中…' })
  try {
    if (isEdit) {
      await updateResponse(state.responseId, payload)
    } else {
      await submitResponse({ requestId: state.requestId, ...payload })
    }
    window.location.replace('./my-responses.html')
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    const hasFieldErrors = applyFieldErrors(form, error?.details)
    if (!hasFieldErrors) showAlert(formAlert, error?.message || '提交失败，请稍后重试', 'danger')
  } finally {
    state.submitting = false
    setButtonLoading(submitButton, false)
    refreshControls()
  }
}

function blockForm(reason) {
  state.ready = false
  form.querySelectorAll('input, textarea, button').forEach((element) => {
    element.disabled = true
  })
  submitButton.classList.add('d-none')
  showAlert(formAlert, reason, 'warning')
}

function reasonForBlocked(detail) {
  if (detail.status === 1) return '该响应已被需求发布者接受，不能再修改。'
  if (detail.status === 2) return '该响应已被需求发布者拒绝，不能再修改。'
  if (detail.status === 3) return '该响应已取消，不能再修改。'
  return '当前响应暂不可修改。'
}

async function loadEditor() {
  try {
    if (isEdit) {
      const detail = await fetchResponseDetail(state.responseId)
      state.requestId = detail.requestId
      document.title = '修改响应 - 好帮手'
      pageTitle.textContent = '修改“我来也”响应'
      pageDesc.textContent = `响应编号 #${detail.responseId} · 提交于 ${formatDateTime(detail.createTime)}`
      submitLabel.textContent = '保存修改'
      cancelLink.href = './my-responses.html'

      descriptionInput.value = detail.description ?? ''
      updateCounter()
      state.attachments = (detail.attachments ?? []).map((file) => ({ ...file, temporary: false }))
      renderAttachments()
      renderContext(detail.request ?? {})

      if (!detail.canModify) {
        blockForm(reasonForBlocked(detail))
        return
      }
    } else {
      if (!state.requestId) {
        blockForm('缺少需求参数，请从“我来也”列表进入。')
        return
      }
      const request = await fetchRequestDetail(state.requestId)
      renderContext(request)
    }

    state.ready = true
    refreshControls()
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    if (error?.code === 40403) {
      blockForm('该响应不存在或已被删除。')
      return
    }
    if (error?.code === 40402) {
      blockForm('该需求不存在或已被删除。')
      return
    }
    blockForm(error?.message || '页面初始化失败，请刷新重试')
  }
}

rememberFieldHints(form)
bindDescriptionValidation()
bindDropZone(imageDrop, imageInput, 'IMAGE')
bindDropZone(videoDrop, videoInput, 'VIDEO')
form.addEventListener('submit', handleSubmit)
updateCounter()

const { userPromise } = initPage({ alertBox })
userPromise.then((user) => {
  if (user) loadEditor()
})
