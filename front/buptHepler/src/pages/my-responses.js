import 'bootstrap/dist/css/bootstrap.min.css'
import { Modal } from 'bootstrap'
import '../styles/base.css'
import '../styles/requests.css'

import {
  cancelResponse,
  fetchMyResponses,
  fetchResponseDetail,
} from '../api/responses.js'
import {
  createBadge,
  formatDateTime,
  regionLabel,
  resolveAssetUrl,
  responseStatusMeta,
} from '../utils/display.js'
import { initPage, isUnauthorized, redirectToLogin } from '../utils/page.js'
import { hideAlert, setButtonLoading, showAlert } from '../utils/ui.js'

const alertBox = document.querySelector('#page-alert')
const listAlert = document.querySelector('#list-alert')
const tabs = document.querySelector('#status-tabs')
const list = document.querySelector('#response-list')
const emptyState = document.querySelector('#empty-state')
const emptyText = document.querySelector('#empty-text')
const pageSummary = document.querySelector('#page-summary')
const pagination = document.querySelector('#pagination')

const detailModalEl = document.querySelector('#response-detail-modal')
const detailModal = Modal.getOrCreateInstance(detailModalEl)
const detailAlert = document.querySelector('#response-detail-alert')
const detailContent = document.querySelector('#response-detail-content')

const state = {
  page: 1,
  pageSize: 5,
  status: '',
}

const PAGE_WINDOW = 5

function renderStatusTabs() {
  tabs.querySelectorAll('button[data-status]').forEach((button) => {
    const active = String(button.dataset.status) === String(state.status)
    button.classList.toggle('active', active)
  })
}

function setLoading() {
  list.replaceChildren()
  pageSummary.textContent = '正在加载…'
  emptyState.classList.add('d-none')
}

function createCard(response) {
  const request = response.request ?? {}
  const article = document.createElement('article')
  article.className = 'border rounded-3 p-3 mb-3'

  const header = document.createElement('div')
  header.className = 'd-flex flex-wrap align-items-start justify-content-between gap-2'

  const identity = document.createElement('div')
  identity.className = 'flex-grow-1'
  const requestLink = document.createElement('a')
  requestLink.className = 'fw-semibold link-dark link-offset-2'
  requestLink.href = `./request-detail.html?id=${response.requestId}`
  requestLink.textContent = request.title || `需求 #${response.requestId}`
  identity.append(requestLink)

  const meta = document.createElement('div')
  meta.className = 'small text-body-secondary mt-1'
  const parts = [request.typeName, regionLabel(request), `提交于 ${formatDateTime(response.createTime)}`]
  if (response.attachments?.length) parts.push(`${response.attachments.length} 个附件`)
  meta.textContent = parts.filter(Boolean).join(' · ')
  identity.append(meta)

  header.append(identity, createBadge(responseStatusMeta(response.status)))
  article.append(header)

  if (response.description) {
    const description = document.createElement('p')
    description.className = 'pre-line text-body-secondary mt-2 mb-2 small'
    description.textContent = response.description
    article.append(description)
  }

  const actions = document.createElement('div')
  actions.className = 'd-flex flex-wrap gap-2'

  const viewButton = document.createElement('button')
  viewButton.type = 'button'
  viewButton.className = 'btn btn-outline-secondary btn-sm'
  viewButton.textContent = '查看'
  viewButton.addEventListener('click', () => openDetail(response.responseId))
  actions.append(viewButton)

  if (response.canModify) {
    const editButton = document.createElement('button')
    editButton.type = 'button'
    editButton.className = 'btn btn-outline-dark btn-sm'
    editButton.textContent = '修改'
    editButton.addEventListener('click', () =>
      window.location.assign(`./response-edit.html?responseId=${response.responseId}`),
    )
    actions.append(editButton)
  }
  if (response.canDelete) {
    const cancelButton = document.createElement('button')
    cancelButton.type = 'button'
    cancelButton.className = 'btn btn-outline-danger btn-sm'
    cancelButton.textContent = '取消响应'
    cancelButton.addEventListener('click', (event) => handleCancel(response, event.currentTarget))
    actions.append(cancelButton)
  }

  article.append(actions)
  return article
}

async function handleCancel(response, button) {
  const requestTitle = response.request?.title || `需求 #${response.requestId}`
  if (!window.confirm(`确认取消对“${requestTitle}”的响应吗？取消后该响应状态将变为“已取消”。`)) return

  hideAlert(listAlert)
  setButtonLoading(button, true, { text: '取消中…' })
  try {
    await cancelResponse(response.responseId)
    if (list.querySelectorAll('article').length === 1 && state.page > 1) {
      state.page -= 1
    }
    showAlert(listAlert, '响应已取消。', 'success')
    await loadResponses()
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    showAlert(listAlert, error?.message || '取消失败，请稍后重试', 'danger')
    if (error?.code === 40904) await loadResponses()
  } finally {
    setButtonLoading(button, false)
  }
}

function addPageItem(ul, label, targetPage, { disabled = false, active = false } = {}) {
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
      state.page = targetPage
      loadResponses()
    })
  }
  item.append(button)
  ul.append(item)
}

function renderPagination(result) {
  const totalPages = result.totalPages ?? 0
  pagination.replaceChildren()
  if (totalPages <= 1) {
    pagination.classList.add('d-none')
    return
  }
  const ul = document.createElement('ul')
  ul.className = 'pagination pagination-sm mb-0'
  addPageItem(ul, '上一页', result.page - 1, { disabled: !result.hasPrev })
  let start = Math.max(1, result.page - Math.floor(PAGE_WINDOW / 2))
  const end = Math.min(totalPages, start + PAGE_WINDOW - 1)
  start = Math.max(1, end - PAGE_WINDOW + 1)
  for (let index = start; index <= end; index += 1) {
    addPageItem(ul, String(index), index, { active: index === result.page })
  }
  addPageItem(ul, '下一页', result.page + 1, { disabled: !result.hasNext })
  pagination.append(ul)
  pagination.classList.remove('d-none')
}

function renderResult(result) {
  const items = result?.list ?? []
  list.replaceChildren(...items.map(createCard))
  emptyState.classList.toggle('d-none', items.length > 0)
  emptyText.textContent = state.status === ''
    ? '你还没有提交过“我来也”响应。'
    : '没有符合当前状态的响应。'
  pageSummary.textContent = `共 ${result.total} 条`
  renderPagination(result)
}

async function loadResponses() {
  setLoading()
  try {
    const result = await fetchMyResponses({
      status: state.status || undefined,
      page: state.page,
      pageSize: state.pageSize,
    })
    renderResult(result)
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    list.replaceChildren()
    emptyState.classList.add('d-none')
    pageSummary.textContent = '加载失败'
    pagination.replaceChildren()
    pagination.classList.add('d-none')
    showAlert(listAlert, error?.message || '响应列表加载失败，请稍后重试', 'danger')
  }
}

// ---------- 详情弹窗 ----------

function createImageThumb(file) {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = 'gallery-item'
  const image = document.createElement('img')
  image.src = resolveAssetUrl(file.url)
  image.alt = file.fileName || '响应图片'
  image.loading = 'lazy'
  button.append(image)
  return button
}

function renderAttachments(attachments = []) {
  const images = attachments.filter((file) => file.mediaType !== 'VIDEO')
  const videos = attachments.filter((file) => file.mediaType === 'VIDEO')
  const wrapper = document.createElement('div')

  if (images.length > 0) {
    const gallery = document.createElement('div')
    gallery.className = 'gallery mb-3'
    gallery.replaceChildren(...images.map(createImageThumb))
    wrapper.append(gallery)
  }
  videos.forEach((file) => {
    const video = document.createElement('video')
    video.src = resolveAssetUrl(file.url)
    video.controls = true
    video.preload = 'metadata'
    video.className = 'w-100 rounded mb-3'
    wrapper.append(video)
  })
  return wrapper
}

async function openDetail(responseId) {
  hideAlert(detailAlert)
  detailContent.textContent = '正在加载…'
  detailModal.show()

  try {
    const detail = await fetchResponseDetail(responseId)
    const request = detail.request ?? {}

    detailContent.replaceChildren()
    const badge = createBadge(responseStatusMeta(detail.status))
    const requestLink = document.createElement('a')
    requestLink.className = 'fw-semibold link-dark link-offset-2'
    requestLink.href = `./request-detail.html?id=${detail.requestId}`
    requestLink.textContent = request.title || `需求 #${detail.requestId}`

    const head = document.createElement('div')
    head.className = 'd-flex flex-wrap align-items-center justify-content-between gap-2 mb-2'
    head.append(requestLink, badge)
    detailContent.append(head)

    const meta = document.createElement('div')
    meta.className = 'small text-body-secondary mb-3'
    meta.textContent = [request.typeName, regionLabel(request), `提交于 ${formatDateTime(detail.createTime)}`]
      .filter(Boolean)
      .join(' · ')
    detailContent.append(meta)

    if (detail.description) {
      const description = document.createElement('p')
      description.className = 'pre-line mb-3'
      description.textContent = detail.description
      detailContent.append(description)
    }

    detailContent.append(renderAttachments(detail.attachments ?? []))
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    detailContent.textContent = ''
    showAlert(detailAlert, error?.message || '响应详情加载失败，请稍后重试', 'danger')
  }
}

tabs.addEventListener('click', (event) => {
  const button = event.target.closest('button[data-status]')
  if (!button) return
  state.status = button.dataset.status
  state.page = 1
  renderStatusTabs()
  hideAlert(listAlert)
  loadResponses()
})

const { userPromise } = initPage({ alertBox })

async function boot() {
  const user = await userPromise
  if (!user) return
  await loadResponses()
}

boot()
