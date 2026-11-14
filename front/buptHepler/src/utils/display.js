const REQUEST_STATUS_META = {
  0: { text: '已发布', badge: 'text-bg-success' },
  '-1': { text: '已取消', badge: 'text-bg-secondary' },
}

const RESPONSE_STATUS_META = {
  0: { text: '待接受', badge: 'text-bg-warning' },
  1: { text: '已接受', badge: 'text-bg-success' },
  2: { text: '已拒绝', badge: 'text-bg-danger' },
  3: { text: '已取消', badge: 'text-bg-secondary' },
}

const UNKNOWN_META = { text: '未知状态', badge: 'text-bg-light' }

export function requestStatusMeta(status) {
  return REQUEST_STATUS_META[String(status)] ?? UNKNOWN_META
}

export function responseStatusMeta(status) {
  return RESPONSE_STATUS_META[String(status)] ?? UNKNOWN_META
}

export function createBadge(meta) {
  const badge = document.createElement('span')
  badge.className = `badge ${meta.badge}`
  badge.textContent = meta.text
  return badge
}

export function formatDateTime(value) {
  return value ? String(value) : '—'
}

export function formatFileSize(bytes) {
  const size = Number(bytes)
  if (!Number.isFinite(size) || size <= 0) return '—'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

/** 后端返回的附件地址形如 /api/files/1/content，同源直接使用即可 */
export function resolveAssetUrl(url) {
  if (!url) return ''
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function appendOption(select, value, text) {
  const option = document.createElement('option')
  option.value = value
  option.textContent = text
  select.append(option)
}

/** 渲染帮忙类型下拉框，placeholder 传 null 表示不需要空选项 */
export function fillTypeOptions(select, types = [], { placeholder = '全部帮忙类型', value = '' } = {}) {
  if (!select) return
  select.replaceChildren()
  if (placeholder !== null) appendOption(select, '', placeholder)
  types.forEach((type) => {
    appendOption(select, String(type.typeCode ?? type.code), type.typeName ?? type.name ?? '')
  })
  select.value = String(value ?? '')
}

/** 渲染按省份分组的地域下拉框 */
export function fillRegionOptions(select, regions = [], { placeholder = '全部地域', value = '' } = {}) {
  if (!select) return
  const fragment = document.createDocumentFragment()
  if (placeholder !== null) {
    const option = document.createElement('option')
    option.value = ''
    option.textContent = placeholder
    fragment.append(option)
  }

  const groups = new Map()
  regions.forEach((region) => {
    const province = region.province || '其他'
    if (!groups.has(province)) groups.set(province, [])
    groups.get(province).push(region)
  })

  groups.forEach((items, province) => {
    const group = document.createElement('optgroup')
    group.label = province
    items.forEach((region) => {
      const option = document.createElement('option')
      option.value = String(region.regionId)
      option.textContent =
        region.displayName || [region.city, region.regionName].filter(Boolean).join(' · ') || province
      group.append(option)
    })
    fragment.append(group)
  })

  select.replaceChildren(fragment)
  select.value = String(value ?? '')
}

/** 列表中的需求地域展示：优先使用后端拼接的全称 */
export function regionLabel(item) {
  return item.regionFullName || [item.city, item.regionName].filter(Boolean).join(' · ') || '—'
}
