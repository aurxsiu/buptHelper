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
import {
  createRequest,
  fetchRequestDetail,
  fetchRequestTypes,
  fetchRegions,
  updateRequest,
} from '../api/requests.js'
import { fillRegionOptions, fillTypeOptions, formatDateTime, resolveAssetUrl } from '../utils/display.js'
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

const TITLE_MAX = 50
const DESCRIPTION_MAX = 1000

const params = new URLSearchParams(window.location.search)
const rawId = Number(params.get('id'))
const isEdit = Number.isInteger(rawId) && rawId > 0

const alertBox = document.querySelector('#page-alert')
const formAlert = document.querySelector('#form-alert')
const pageTitle = document.querySelector('#page-title')
const pageDesc = document.querySelector('#page-desc')
const form = document.querySelector('#request-form')
const titleInput = form.elements.namedItem('title')
const typeSelect = form.elements.namedItem('typeCode')
const regionSelect = form.elements.namedItem('regionId')
const descriptionInput = form.elements.namedItem('description')
const submitButton = document.querySelector('#request-submit')
const submitLabel = submitButton.querySelector('[data-loading-text]')
const cancelLink = document.querySelector('#cancel-link')
const titleCounter = document.querySelector('#title-counter')
const descriptionCounter = document.querySelector('#description-counter')

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
  requestId: isEdit ? rawId : null,
  attachments: [],
  uploading: false,
  submitting: false,
  ready: false,
}

function updateCounters() {
  titleCounter.textContent = `${titleInput.value.trim().length}/${TITLE_MAX}`
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
  uploadProgress.querySelector('.progress')?.setAttribute('aria-valuenow', '0')
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
    image.alt = file.fileName || '需求图片'
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
      // 临时文件清理失败不影响页面操作，服务端可定期清理
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

function validateTitle() {
  const value = titleInput.value.trim()
  if (!value) return '请输入需求主题'
  if (value.length > TITLE_MAX) return `需求主题不能超过 ${TITLE_MAX} 个字符`
  return ''
}

function validateType() {
  return typeSelect.value ? '' : '请选择帮忙类型'
}

function validateRegion() {
  return regionSelect.value ? '' : '请选择地域'
}

function validateDescription() {
  const value = descriptionInput.value.trim()
  if (!value) return '请输入需求描述'
  if (value.length > DESCRIPTION_MAX) return `需求描述不能超过 ${DESCRIPTION_MAX} 个字符`
  return ''
}

function bindFieldValidation(input, validator) {
  input.addEventListener('blur', () => {
    const message = validator()
    if (message) setFieldError(input, message)
    else restoreField(input)
  })
  input.addEventListener('input', () => {
    if (input === titleInput || input === descriptionInput) updateCounters()
    if (input.classList.contains('is-invalid') && !validator()) restoreField(input)
  })
}

async function handleSubmit(event) {
  event.preventDefault()
  hideAlert(formAlert)
  clearAllFields(form)

  if (state.uploading) {
    showAlert(formAlert, '附件正在上传，请等待上传完成后再提交。', 'warning')
    return
  }

  const errors = {
    title: validateTitle(),
    typeCode: validateType(),
    regionId: validateRegion(),
    description: validateDescription(),
  }
  const invalidFields = Object.keys(errors).filter((name) => errors[name])
  if (invalidFields.length > 0) {
    applyFieldErrors(form, errors)
    form.elements.namedItem(invalidFields[0])?.focus()
    return
  }

  const confirmed = window.confirm(
    isEdit ? '确认保存对该“请帮忙”需求的修改吗？' : '确认发布这条“请帮忙”需求吗？',
  )
  if (!confirmed) return

  const payload = {
    title: titleInput.value.trim(),
    typeCode: typeSelect.value,
    regionId: Number(regionSelect.value),
    description: descriptionInput.value.trim(),
    attachmentIds: state.attachments.map((item) => item.fileId),
  }

  state.submitting = true
  setButtonLoading(submitButton, true, { text: isEdit ? '保存中…' : '发布中…' })
  try {
    const detail = isEdit
      ? await updateRequest(state.requestId, payload)
      : await createRequest(payload)
    const flag = isEdit ? 'saved' : 'created'
    window.location.replace(`./request-detail.html?id=${detail.requestId}&${flag}=1`)
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
  form.querySelectorAll('input, select, textarea, button').forEach((element) => {
    element.disabled = true
  })
  submitButton.classList.add('d-none')
  showAlert(formAlert, reason, 'warning')
}

function reasonForBlocked(detail) {
  if (!detail.mine) return '只能修改自己发布的需求，你只能查看该需求详情。'
  if (Number(detail.status) === -1) return '该需求已取消，不能再修改。'
  if (detail.responseCount > 0) return '该需求已有“我来也”响应，不能再修改。'
  return '当前需求暂不可修改。'
}

function applyDetail(detail) {
  document.title = '修改需求 - 好帮手'
  pageTitle.textContent = '修改请帮忙需求'
  pageDesc.textContent = `需求编号 #${detail.requestId} · 创建于 ${formatDateTime(detail.createTime)}`
  submitLabel.textContent = '保存修改'
  cancelLink.href = `./request-detail.html?id=${detail.requestId}`

  titleInput.value = detail.title ?? ''
  descriptionInput.value = detail.description ?? ''
  updateCounters()

  state.attachments = (detail.attachments ?? []).map((file) => ({ ...file, temporary: false }))
  renderAttachments()

  if (!detail.canModify) {
    blockForm(reasonForBlocked(detail))
    return
  }

  state.ready = true
  refreshControls()
}

async function loadEditor() {
  const detailPromise = isEdit ? fetchRequestDetail(state.requestId) : Promise.resolve(null)

  try {
    const [types, regions, detail] = await Promise.all([
      fetchRequestTypes(),
      fetchRegions(),
      detailPromise,
    ])

    fillTypeOptions(typeSelect, types, {
      placeholder: '请选择帮忙类型',
      value: detail?.typeCode ?? '',
    })
    fillRegionOptions(regionSelect, regions, {
      placeholder: '请选择地域',
      value: detail?.regionId ?? '',
    })

    if (detail) {
      applyDetail(detail)
      return
    }

    state.ready = true
    refreshControls()
    updateCounters()
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
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
bindFieldValidation(titleInput, validateTitle)
bindFieldValidation(descriptionInput, validateDescription)
;[typeSelect, regionSelect].forEach((select) => {
  select.addEventListener('change', () => {
    if (select.classList.contains('is-invalid')) restoreField(select)
  })
})
bindDropZone(imageDrop, imageInput, 'IMAGE')
bindDropZone(videoDrop, videoInput, 'VIDEO')
form.addEventListener('submit', handleSubmit)
updateCounters()

const { userPromise } = initPage({ alertBox })
userPromise.then((user) => {
  if (user) loadEditor()
})
