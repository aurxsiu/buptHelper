import 'bootstrap/dist/css/bootstrap.min.css'
import { Modal } from 'bootstrap'
import '../styles/base.css'
import '../styles/requests.css'

import {
  acceptResponse,
  deleteRequest,
  fetchRequestDetail,
  fetchRequestResponses,
  fetchUserProfile,
  rejectResponse,
} from '../api/requests.js'
import {
  createBadge,
  formatDateTime,
  regionLabel,
  requestStatusMeta,
  resolveAssetUrl,
  responseStatusMeta,
} from '../utils/display.js'
import { initPage, isUnauthorized, redirectToLogin } from '../utils/page.js'
import { hideAlert, setButtonLoading, showAlert } from '../utils/ui.js'

const params = new URLSearchParams(window.location.search)
const requestId = Number(params.get('id'))

const PAGE_WINDOW = 5
const RESPONSE_PAGE_SIZE = 5

const alertBox = document.querySelector('#page-alert')
const loadingCard = document.querySelector('#detail-loading')
const errorCard = document.querySelector('#detail-error')
const errorText = document.querySelector('#detail-error-text')
const content = document.querySelector('#detail-content')
const detailAlert = document.querySelector('#detail-alert')

const titleElement = document.querySelector('#request-title')
const statusSlot = document.querySelector('#request-status')
const metaElement = document.querySelector('#request-meta')
const statusBanner = document.querySelector('#status-banner')
const ownerActions = document.querySelector('#owner-actions')
const editButton = document.querySelector('#edit-button')
const deleteButton = document.querySelector('#delete-button')
const descriptionElement = document.querySelector('#request-description')
const publisherInfo = document.querySelector('#publisher-info')
const ownerHint = document.querySelector('#owner-hint')

const mediaSection = document.querySelector('#media-section')
const imageGallery = document.querySelector('#image-gallery')
const videoWrapper = document.querySelector('#video-wrapper')

const responderActions = document.querySelector('#responder-actions')
const responderHint = document.querySelector('#responder-hint')
const responderButtons = document.querySelector('#responder-buttons')

const responsesSection = document.querySelector('#responses-section')
const responsesSummary = document.querySelector('#responses-summary')
const responsesAlert = document.querySelector('#responses-alert')
const responsesList = document.querySelector('#responses-list')
const responsesEmpty = document.querySelector('#responses-empty')
const responsesPagination = document.querySelector('#responses-pagination')

const previewImage = document.querySelector('#image-preview-img')
const previewTitle = document.querySelector('#image-preview-title')
const userProfileAlert = document.querySelector('#user-profile-alert')
const userProfileList = document.querySelector('#user-profile-list')

const imagePreviewModal = Modal.getOrCreateInstance(
  document.querySelector('#image-preview-modal'),
)
const userProfileModal = Modal.getOrCreateInstance(
  document.querySelector('#user-profile-modal'),
)

const state = {
  detail: null,
  responsePage: 1,
  responsePageSize: RESPONSE_PAGE_SIZE,
}

function showLoading() {
  loadingCard.classList.remove('d-none')
  errorCard.classList.add('d-none')
  content.classList.add('d-none')
}

function showError(message) {
  loadingCard.classList.add('d-none')
  content.classList.add('d-none')
  errorCard.classList.remove('d-none')
  errorText.textContent = message
}

function showContent() {
  loadingCard.classList.add('d-none')
  errorCard.classList.add('d-none')
  content.classList.remove('d-none')
}

function appendDefinition(list, label, value, { isLink = false } = {}) {
  const term = document.createElement('dt')
  term.className = 'col-sm-3 text-body-secondary fw-normal'
  term.textContent = label

  const detail = document.createElement('dd')
  detail.className = 'col-sm-9 mb-2'
  if (isLink && value) {
    const link = document.createElement('a')
    link.href = `tel:${value}`
    link.textContent = value
    detail.append(link)
  } else {
    detail.textContent = String(value ?? '—')
  }

  list.append(term, detail)
}

function renderPublisher(publisher = {}) {
  publisherInfo.replaceChildren()
  appendDefinition(publisherInfo, '姓名', publisher.realName || publisher.username)
  appendDefinition(publisherInfo, '用户名', publisher.username)
  appendDefinition(publisherInfo, '手机号码', publisher.phone, { isLink: true })
  appendDefinition(publisherInfo, '用户简介', publisher.bio || '—')
  appendDefinition(publisherInfo, '注册时间', publisher.registerTime)
}

function openImagePreview(url, fileName) {
  previewImage.src = url
  previewImage.alt = fileName || '需求图片预览'
  previewTitle.textContent = fileName || '图片预览'
  imagePreviewModal.show()
}

function createImageThumb(file, index) {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = 'gallery-item'

  const image = document.createElement('img')
  image.src = resolveAssetUrl(file.url)
  image.alt = file.fileName || `需求图片 ${index + 1}`
  image.loading = 'lazy'

  button.append(image)
  button.addEventListener('click', () => openImagePreview(image.src, file.fileName))
  return button
}

function renderMedia(attachments = []) {
  const images = attachments.filter((file) => file.mediaType !== 'VIDEO')
  const videos = attachments.filter((file) => file.mediaType === 'VIDEO')

  mediaSection.classList.toggle('d-none', attachments.length === 0)

  imageGallery.classList.toggle('d-none', images.length === 0)
  imageGallery.replaceChildren(...images.map(createImageThumb))

  videoWrapper.classList.toggle('d-none', videos.length === 0)
  videoWrapper.replaceChildren(
    ...videos.map((file) => {
      const video = document.createElement('video')
      video.src = resolveAssetUrl(file.url)
      video.controls = true
      video.preload = 'metadata'
      video.className = 'w-100 rounded'
      return video
    }),
  )
}

function renderMeta(detail) {
  const parts = [
    detail.typeName || '未分类',
    regionLabel(detail),
    `发布于 ${formatDateTime(detail.createTime)}`,
    `最近修改 ${formatDateTime(detail.updateTime)}`,
    `响应 ${detail.responseCount ?? 0} 条`,
  ]
  metaElement.replaceChildren(
    ...parts.map((text) => {
      const span = document.createElement('span')
      span.textContent = text
      return span
    }),
  )
}

function renderOwnerArea(detail) {
  const mine = Boolean(detail.mine)
  ownerActions.classList.toggle('d-none', !mine)
  renderResponderArea(detail)

  if (!mine) {
    ownerHint.textContent = '只有需求发布者可以查看响应并接受或拒绝。'
    responsesSection.classList.add('d-none')
    return
  }

  editButton.href = `./request-edit.html?id=${detail.requestId}`
  editButton.classList.toggle('d-none', !detail.canModify)
  deleteButton.classList.toggle('d-none', !detail.canDelete)

  if (Number(detail.status) === -1) {
    ownerHint.textContent = '该需求已取消，不能再修改或删除。'
  } else if (detail.responseCount > 0) {
    ownerHint.textContent = '该需求已有“我来也”响应，因此不能修改或删除，但可以查看响应者信息并确认。'
  } else {
    ownerHint.textContent = '提示：一旦有用户提交“我来也”响应，该需求将不能修改或删除。'
  }

  responsesSection.classList.remove('d-none')
}

function renderResponderArea(detail) {
  const mine = Boolean(detail.mine)
  responderActions.classList.toggle('d-none', mine || Number(detail.status) === -1)
  if (mine || Number(detail.status) === -1) return

  responderButtons.replaceChildren()

  if (detail.myResponseId) {
    responderHint.textContent = '你已对该需求提交过响应，可前往“我的响应”查看或修改。'
    const viewButton = document.createElement('a')
    viewButton.className = 'btn btn-outline-success btn-sm'
    viewButton.href = `./my-responses.html?requestId=${detail.requestId}`
    viewButton.textContent = '查看我的响应'
    responderButtons.append(viewButton)
    return
  }

  responderHint.textContent = '如果你能提供帮助，点击“我来也”提交你的介绍信息。'
  const respondButton = document.createElement('a')
  respondButton.className = 'btn btn-dark btn-sm'
  respondButton.href = `./response-edit.html?requestId=${detail.requestId}`
  respondButton.textContent = '我来也'
  responderButtons.append(respondButton)
}

function renderDetail(detail) {
  document.title = `${detail.title} - 请帮忙 - 好帮手`
  titleElement.textContent = detail.title
  statusSlot.replaceChildren(createBadge(requestStatusMeta(detail.status)))
  renderMeta(detail)
  renderPublisher(detail.publisher)
  descriptionElement.textContent = detail.description || '—'
  renderMedia(detail.attachments ?? [])

  statusBanner.className = 'alert d-none mt-3 mb-0'
  statusBanner.textContent = ''
  if (Number(detail.status) === -1) {
    statusBanner.className = 'alert alert-secondary mt-3 mb-0'
    statusBanner.textContent = '该需求已取消，不能再被修改或删除。'
  }

  renderOwnerArea(detail)
}

function createActionButton(text, variant, onClick) {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = `btn btn-${variant} btn-sm`

  const spinner = document.createElement('span')
  spinner.className = 'spinner-border spinner-border-sm me-1 d-none'
  spinner.setAttribute('data-loading-spinner', '')
  spinner.setAttribute('aria-hidden', 'true')

  const label = document.createElement('span')
  label.setAttribute('data-loading-text', '')
  label.textContent = text

  button.append(spinner, label)
  button.addEventListener('click', () => onClick(button))
  return button
}

function responderName(response) {
  return response.responder?.realName || response.responder?.username || '该用户'
}

async function showUserProfile(userId, button) {
  if (!userId) return

  hideAlert(userProfileAlert)
  setButtonLoading(button, true, { text: '加载中…' })
  try {
    const profile = await fetchUserProfile(userId)
    userProfileList.replaceChildren()
    appendDefinition(userProfileList, '姓名', profile.realName || profile.username)
    appendDefinition(userProfileList, '用户名', profile.username)
    appendDefinition(userProfileList, '用户类型', profile.userTypeName || profile.userType)
    appendDefinition(userProfileList, '手机号码', profile.phone, { isLink: true })
    appendDefinition(userProfileList, '用户简介', profile.bio || '—')
    appendDefinition(userProfileList, '注册时间', profile.registerTime)
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    showAlert(userProfileAlert, error?.message || '用户信息加载失败，请稍后重试', 'danger')
  } finally {
    setButtonLoading(button, false)
    userProfileModal.show()
  }
}

function createResponseCard(response) {
  const card = document.createElement('article')
  card.className = 'border rounded-3 p-3 mb-3'

  const header = document.createElement('div')
  header.className = 'd-flex flex-wrap align-items-start justify-content-between gap-2'

  const identity = document.createElement('div')
  const name = document.createElement('div')
  name.className = 'fw-semibold'
  name.textContent = responderName(response)
  if (response.responder?.username) {
    const username = document.createElement('span')
    username.className = 'small text-body-secondary ms-2'
    username.textContent = `@${response.responder.username}`
    name.append(username)
  }

  const time = document.createElement('div')
  time.className = 'small text-body-secondary'
  time.textContent = `提交于 ${formatDateTime(response.createTime)}`

  identity.append(name, time)
  header.append(identity, createBadge(responseStatusMeta(response.status)))
  card.append(header)

  if (response.description) {
    const description = document.createElement('p')
    description.className = 'pre-line mt-2 mb-2'
    description.textContent = response.description
    card.append(description)
  }

  const attachments = response.attachments ?? []
  const images = attachments.filter((file) => file.mediaType !== 'VIDEO')
  if (images.length > 0) {
    const gallery = document.createElement('div')
    gallery.className = 'gallery mt-2'
    gallery.replaceChildren(...images.map(createImageThumb))
    card.append(gallery)
  }

  attachments
    .filter((file) => file.mediaType === 'VIDEO')
    .forEach((file) => {
      const video = document.createElement('video')
      video.src = resolveAssetUrl(file.url)
      video.controls = true
      video.preload = 'metadata'
      video.className = 'w-100 rounded mt-2'
      card.append(video)
    })

  const actions = document.createElement('div')
  actions.className = 'd-flex flex-wrap gap-2 mt-3'
  actions.append(
    createActionButton('查看用户信息', 'outline-secondary', (button) =>
      showUserProfile(response.responder?.userId, button),
    ),
  )
  if (response.canAccept) {
    actions.append(
      createActionButton('接受响应', 'dark', (button) => handleResponseAction(response, button, 'accept')),
    )
  }
  if (response.canReject) {
    actions.append(
      createActionButton('拒绝响应', 'outline-danger', (button) =>
        handleResponseAction(response, button, 'reject'),
      ),
    )
  }
  card.append(actions)
  return card
}

async function handleResponseAction(response, button, action) {
  const isAccept = action === 'accept'
  const message = isAccept
    ? `确认接受“${responderName(response)}”的“我来也”响应吗？接受后该需求将确认由 TA 提供帮助。`
    : `确认拒绝“${responderName(response)}”的“我来也”响应吗？`
  if (!window.confirm(message)) return

  hideAlert(responsesAlert)
  setButtonLoading(button, true, { text: '处理中…' })
  try {
    if (isAccept) await acceptResponse(response.responseId)
    else await rejectResponse(response.responseId)
    await loadDetail()
    showAlert(detailAlert, isAccept ? '已接受该响应。' : '已拒绝该响应。', 'success')
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    showAlert(responsesAlert, error?.message || '操作失败，请稍后重试', 'danger')
    if (error?.code === 40904 || error?.code === 40905) await loadResponses()
  } finally {
    setButtonLoading(button, false)
  }
}

function addResponsePageItem(list, label, targetPage, { disabled = false, active = false } = {}) {
  const item = document.createElement('li')
  item.className = `page-item${disabled ? ' disabled' : ''}${active ? ' active' : ''}`

  const button = document.createElement('button')
  button.type = 'button'
  button.className = 'page-link'
  button.textContent = label
  if (active) button.setAttribute('aria-current', 'page')
  button.disabled = disabled
  if (!disabled && !active) {
    button.addEventListener('click', () => {
      state.responsePage = targetPage
      loadResponses()
    })
  }

  item.append(button)
  list.append(item)
}

function renderResponses(result) {
  const list = result?.list ?? []
  responsesSummary.textContent = `共 ${result?.total ?? 0} 条`
  responsesEmpty.classList.toggle('d-none', list.length > 0)
  responsesList.replaceChildren(...list.map(createResponseCard))

  const totalPages = result?.totalPages ?? 0
  responsesPagination.replaceChildren()
  responsesPagination.classList.toggle('d-none', totalPages <= 1)
  if (totalPages <= 1) return

  const pageList = document.createElement('ul')
  pageList.className = 'pagination pagination-sm mb-0'

  addResponsePageItem(pageList, '上一页', result.page - 1, { disabled: !result.hasPrev })

  let start = Math.max(1, result.page - Math.floor(PAGE_WINDOW / 2))
  const end = Math.min(totalPages, start + PAGE_WINDOW - 1)
  start = Math.max(1, end - PAGE_WINDOW + 1)

  for (let index = start; index <= end; index += 1) {
    addResponsePageItem(pageList, String(index), index, { active: index === result.page })
  }

  addResponsePageItem(pageList, '下一页', result.page + 1, { disabled: !result.hasNext })
  responsesPagination.append(pageList)
}

async function loadResponses() {
  responsesSummary.textContent = '正在加载…'
  try {
    const result = await fetchRequestResponses(requestId, {
      page: state.responsePage,
      pageSize: state.responsePageSize,
    })
    renderResponses(result)
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    responsesSummary.textContent = ''
    showAlert(responsesAlert, error?.message || '响应信息加载失败，请稍后重试', 'danger')
  }
}

async function loadDetail() {
  try {
    const detail = await fetchRequestDetail(requestId)
    state.detail = detail
    renderDetail(detail)
    showContent()
    if (detail.mine) await loadResponses()
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    showError(
      error?.code === 40402
        ? '该需求不存在或已被删除。'
        : error?.message || '需求详情加载失败，请稍后重试',
    )
  }
}

async function handleDelete() {
  if (!state.detail) return
  const confirmed = window.confirm(
    `确认删除“${state.detail.title}”吗？删除后该需求将标记为已取消，且不可恢复。`,
  )
  if (!confirmed) return

  hideAlert(detailAlert)
  setButtonLoading(deleteButton, true, { text: '删除中…' })
  try {
    await deleteRequest(state.detail.requestId)
    window.location.replace('./requests.html?deleted=1')
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    showAlert(detailAlert, error?.message || '删除失败，请稍后重试', 'danger')
    if ([40902, 40903, 40906].includes(error?.code)) await loadDetail()
  } finally {
    setButtonLoading(deleteButton, false)
  }
}

deleteButton.addEventListener('click', handleDelete)

const { userPromise } = initPage({ alertBox })

async function boot() {
  const user = await userPromise
  if (!user) return

  if (!Number.isInteger(requestId) || requestId <= 0) {
    showError('需求标识无效，请从需求列表进入。')
    return
  }

  showLoading()
  await loadDetail()

  if (params.get('created') === '1') {
    showAlert(detailAlert, '“请帮忙”需求发布成功。', 'success')
  } else if (params.get('saved') === '1') {
    showAlert(detailAlert, '修改已保存。', 'success')
  }
}

boot()
