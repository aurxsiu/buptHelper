import { API_BASE_URL, ApiError, request } from './request.js'
import { getToken } from '../utils/storage.js'

/** 附件上传规则，与 doc/接口设计-请帮忙模块.md 1.6 保持一致 */
export const IMAGE_MAX_COUNT = 6
export const VIDEO_MAX_COUNT = 1
export const IMAGE_MAX_SIZE = 5 * 1024 * 1024
export const VIDEO_MAX_SIZE = 100 * 1024 * 1024

const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const VIDEO_TYPES = ['video/mp4', 'video/webm']

/** 上传单个附件，返回后端 FileVO */
export function uploadFile(file, { onProgress, signal } = {}) {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', file)

    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${API_BASE_URL}/files`)
    xhr.responseType = 'text'

    const token = getToken()
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`)

    xhr.upload.addEventListener('progress', (event) => {
      if (!event.lengthComputable || typeof onProgress !== 'function') return
      onProgress(Math.round((event.loaded / event.total) * 100))
    })

    xhr.addEventListener('load', () => {
      let payload = null
      try {
        payload = xhr.responseText ? JSON.parse(xhr.responseText) : null
      } catch {
        payload = null
      }

      if (xhr.status >= 200 && xhr.status < 300 && payload?.code === 0) {
        resolve(payload.data)
        return
      }
      reject(
        new ApiError(payload?.message || `文件上传失败（${xhr.status}）`, {
          code: payload?.code ?? xhr.status * 100,
          status: xhr.status,
          details: payload?.data ?? null,
        }),
      )
    })
    xhr.addEventListener('error', () => reject(new ApiError('网络异常，文件上传失败', { code: -1 })))
    xhr.addEventListener('abort', () => reject(new ApiError('文件上传已取消', { code: -3 })))

    if (signal) {
      if (signal.aborted) {
        xhr.abort()
        return
      }
      signal.addEventListener('abort', () => xhr.abort(), { once: true })
    }

    xhr.send(formData)
  })
}

/** 删除上传后未提交的临时文件（服务器仅允许删除未被需求引用的文件） */
export function removeUploadedFile(fileId) {
  return request(`/files/${fileId}`, { method: 'DELETE' })
}

/** 校验附件类型与大小，返回空字符串表示通过 */
export function validateUploadFile(file, mediaType) {
  const allowedTypes = mediaType === 'VIDEO' ? VIDEO_TYPES : IMAGE_TYPES
  const maxSize = mediaType === 'VIDEO' ? VIDEO_MAX_SIZE : IMAGE_MAX_SIZE
  const label = mediaType === 'VIDEO' ? '视频' : '图片'

  if (!allowedTypes.includes(file.type)) {
    return `仅支持 ${mediaType === 'VIDEO' ? 'MP4 / WebM' : 'JPG / PNG / WebP / GIF'} 格式的${label}`
  }
  if (file.size > maxSize) {
    return `${label}大小不能超过 ${maxSize / 1024 / 1024}MB`
  }
  return ''
}
