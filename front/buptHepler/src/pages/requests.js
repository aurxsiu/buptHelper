import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import '../styles/base.css'
import '../styles/requests.css'

import { deleteRequest, fetchRequestTypes, fetchRequests, fetchRegions } from '../api/requests.js'
import {
  createBadge,
  fillRegionOptions,
  fillTypeOptions,
  formatDateTime,
  regionLabel,
  requestStatusMeta,
} from '../utils/display.js'
import { initPage, isUnauthorized, redirectToLogin } from '../utils/page.js'
import { hideAlert, setButtonLoading, showAlert } from '../utils/ui.js'

const params = new URLSearchParams(window.location.search)

const alertBox = document.querySelector('#page-alert')
const listAlert = document.querySelector('#list-alert')
const filterForm = document.querySelector('#filter-form')
const typeSelect = document.querySelector('#type-filter')
const regionSelect = document.querySelector('#region-filter')
const keywordInput = document.querySelector('#keyword')
const tbody = document.querySelector('#request-tbody')
const tableWrapper = document.querySelector('#request-table-wrapper')
const emptyState = document.querySelector('#empty-state')
const emptyText = document.querySelector('#empty-text')
const pageSummary = document.querySelector('#page-summary')
const pagination = document.querySelector('#pagination')
const pageSizeSelect = document.querySelector('#page-size')

const state = {
  page: 1,
  pageSize: Number(pageSizeSelect.value) || 10,
  status: '',
  typeCode: '',
  regionId: '',
  keyword: '',
}

function setLoadingRow() {
  const row = document.createElement('tr')
  const cell = document.createElement('td')
  cell.colSpan = 7
  cell.className = 'text-center text-body-secondary py-4'
  cell.textContent = '正在加载…'
  row.append(cell)
  tbody.replaceChildren(row)
  tableWrapper.classList.remove('d-none')
  emptyState.classList.add('d-none')
  pageSummary.textContent = '正在加载…'
}

function createCell(className = '') {
  const cell = document.createElement('td')
  if (className) cell.className = className
  return cell
}

function createActionButton(label, variant, { disabled = false, title = '', onClick } = {}) {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = `btn btn-${variant} btn-sm`
  button.textContent = label
  if (title) button.title = title
  button.disabled = disabled
  if (!disabled && onClick) button.addEventListener('click', onClick)
  return button
}

function modifyHint(item) {
  if (item.canModify) return ''
  if (Number(item.status) === -1) return '已取消的需求不能修改'
  if (item.responseCount > 0) return '已有“我来也”响应，不能修改'
  return '当前需求不能修改'
}

function deleteHint(item) {
  if (item.canDelete) return ''
  if (Number(item.status) === -1) return '该需求已取消'
  if (item.responseCount > 0) return '已有“我来也”响应，不能删除'
  return '当前需求不能删除'
}

function responseSummary(item) {
  if (!item.responseCount) return '—'
  const pending = item.pendingResponseCount ?? 0
  const accepted = item.acceptedResponseCount ?? 0
  const parts = [`共 ${item.responseCount} 条`]
  if (pending > 0) parts.push(`待接受 ${pending}`)
  if (accepted > 0) parts.push(`已接受 ${accepted}`)
  return parts.join(' · ')
}

async function handleDelete(item, button) {
  const confirmed = window.confirm(
    `确认删除“${item.title}”吗？删除后该需求将标记为已取消，且不可恢复。`,
  )
  if (!confirmed) return

  setButtonLoading(button, true)
  hideAlert(listAlert)
  try {
    await deleteRequest(item.requestId)
    if (tbody.querySelectorAll('tr[data-request-id]').length === 1 && state.page > 1) {
      state.page -= 1
    }
    showAlert(listAlert, '需求已删除。', 'success')
    await loadRequests()
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    showAlert(listAlert, error?.message || '删除失败，请稍后重试', 'danger')
  } finally {
    setButtonLoading(button, false)
  }
}

function createRow(item) {
  const row = document.createElement('tr')
  row.dataset.requestId = String(item.requestId)

  const titleCell = createCell('request-title-cell')
  const titleLink = document.createElement('a')
  titleLink.className = 'fw-semibold link-dark link-offset-2 text-truncate d-block'
  titleLink.href = `./request-detail.html?id=${item.requestId}`
  titleLink.textContent = item.title
  titleCell.append(titleLink)

  const mediaParts = []
  if (item.imageCount > 0) mediaParts.push(`图片 ${item.imageCount}`)
  if (item.videoCount > 0) mediaParts.push('视频 1')
  if (mediaParts.length > 0) {
    const hint = document.createElement('span')
    hint.className = 'small text-body-secondary'
    hint.textContent = mediaParts.join(' · ')
    titleCell.append(hint)
  }

  const typeCell = createCell()
  typeCell.textContent = item.typeName || '—'

  const regionCell = createCell()
  regionCell.textContent = regionLabel(item)

  const statusCell = createCell()
  statusCell.append(createBadge(requestStatusMeta(item.status)))

  const responseCell = createCell('small text-body-secondary')
  responseCell.textContent = responseSummary(item)

  const timeCell = createCell('small text-body-secondary')
  timeCell.textContent = formatDateTime(item.createTime)

  const actionCell = createCell('text-end')
  const actions = document.createElement('div')
  actions.className = 'd-inline-flex flex-wrap justify-content-end gap-1'
  actions.append(
    createActionButton('查看', 'outline-secondary', {
      onClick: () => window.location.assign(`./request-detail.html?id=${item.requestId}`),
    }),
    createActionButton('修改', 'outline-dark', {
      disabled: !item.canModify,
      title: modifyHint(item),
      onClick: () => window.location.assign(`./request-edit.html?id=${item.requestId}`),
    }),
    createActionButton('删除', 'outline-danger', {
      disabled: !item.canDelete,
      title: deleteHint(item),
      onClick: (event) => handleDelete(item, event.currentTarget),
    }),
  )
  actionCell.append(actions)

  row.append(titleCell, typeCell, regionCell, statusCell, responseCell, timeCell, actionCell)
  return row
}

const PAGE_WINDOW = 5

function addPageItem(list, label, targetPage, { disabled = false, active = false } = {}) {
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
      loadRequests()
    })
  }

  item.append(button)
  list.append(item)
}

function renderPagination(result) {
  const totalPages = result.totalPages ?? 0
  pagination.replaceChildren()

  if (totalPages <= 1) {
    pagination.classList.add('d-none')
    return
  }

  const list = document.createElement('ul')
  list.className = 'pagination pagination-sm mb-0'

  addPageItem(list, '上一页', result.page - 1, { disabled: !result.hasPrev })

  let start = Math.max(1, result.page - Math.floor(PAGE_WINDOW / 2))
  const end = Math.min(totalPages, start + PAGE_WINDOW - 1)
  start = Math.max(1, end - PAGE_WINDOW + 1)

  for (let index = start; index <= end; index += 1) {
    addPageItem(list, String(index), index, { active: index === result.page })
  }

  addPageItem(list, '下一页', result.page + 1, { disabled: !result.hasNext })

  pagination.append(list)
  pagination.classList.remove('d-none')
}

function renderResult(result) {
  const list = result?.list ?? []
  state.pageSize = result?.pageSize ?? state.pageSize

  if (list.length === 0) {
    tbody.replaceChildren()
    tableWrapper.classList.add('d-none')
    emptyState.classList.remove('d-none')
    emptyText.textContent = hasFilters()
      ? '没有符合当前筛选条件的“请帮忙”需求。'
      : '你还没有发布过“请帮忙”需求。'
    pageSummary.textContent = '共 0 条'
    pagination.replaceChildren()
    pagination.classList.add('d-none')
    return
  }

  emptyState.classList.add('d-none')
  tableWrapper.classList.remove('d-none')
  tbody.replaceChildren(...list.map(createRow))

  pageSummary.textContent = `共 ${result.total} 条，第 ${result.page} / ${result.totalPages} 页`
  renderPagination(result)
}

function hasFilters() {
  return Boolean(state.status || state.typeCode || state.regionId || state.keyword)
}

async function loadRequests() {
  setLoadingRow()
  try {
    const result = await fetchRequests({
      scope: 'mine',
      status: state.status,
      typeCode: state.typeCode,
      regionId: state.regionId,
      keyword: state.keyword,
      page: state.page,
      pageSize: state.pageSize,
    })
    renderResult(result)
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    tbody.replaceChildren()
    emptyState.classList.add('d-none')
    tableWrapper.classList.remove('d-none')
    pageSummary.textContent = '加载失败'
    pagination.replaceChildren()
    pagination.classList.add('d-none')
    showAlert(listAlert, error?.message || '需求列表加载失败，请稍后重试', 'danger')
  }
}

async function loadDictionaries() {
  const [types, regions] = await Promise.all([fetchRequestTypes(), fetchRegions()])
  fillTypeOptions(typeSelect, types, { placeholder: '全部类型', value: state.typeCode })
  fillRegionOptions(regionSelect, regions, { placeholder: '全部地域', value: state.regionId })
}

filterForm.addEventListener('submit', (event) => {
  event.preventDefault()
  state.status = filterForm.elements.namedItem('status').value
  state.typeCode = typeSelect.value
  state.regionId = regionSelect.value
  state.keyword = keywordInput.value.trim()
  state.page = 1
  hideAlert(listAlert)
  loadRequests()
})

filterForm.addEventListener('reset', (event) => {
  event.preventDefault()
  filterForm.elements.namedItem('status').value = ''
  typeSelect.value = ''
  regionSelect.value = ''
  keywordInput.value = ''
  state.status = ''
  state.typeCode = ''
  state.regionId = ''
  state.keyword = ''
  state.page = 1
  hideAlert(listAlert)
  loadRequests()
})

pageSizeSelect.addEventListener('change', () => {
  state.pageSize = Number(pageSizeSelect.value) || 10
  state.page = 1
  loadRequests()
})

const { userPromise } = initPage({ alertBox })

async function boot() {
  const user = await userPromise
  if (!user) return

  try {
    await loadDictionaries()
  } catch (error) {
    if (isUnauthorized(error)) {
      redirectToLogin('?logout=1')
      return
    }
    showAlert(listAlert, error?.message || '帮忙类型与地域加载失败，请刷新重试', 'danger')
  }

  await loadRequests()

  if (params.get('deleted') === '1') {
    showAlert(listAlert, '需求已删除。', 'success')
  }
}

boot()
