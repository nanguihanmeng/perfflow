/**
 * 文件下载工具（导出 Excel 等二进制流）
 */
import type { AxiosResponse } from 'axios'

/** 从 Content-Disposition 解析文件名，兼容 filename*=UTF-8'' 与 filename="..." */
const parseFilename = (contentDisposition: string | undefined, fallback: string): string => {
  if (!contentDisposition) {
    return fallback
  }
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match) {
    return decodeURIComponent(utf8Match[1].trim())
  }
  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return plainMatch ? plainMatch[1].trim() : fallback
}

/** 触发浏览器下载 Blob 响应 */
export const downloadBlob = (response: AxiosResponse<Blob>, fallbackName = 'download.xlsx'): void => {
  const filename = parseFilename(response.headers['content-disposition'], fallbackName)
  const blob = new Blob([response.data])
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
