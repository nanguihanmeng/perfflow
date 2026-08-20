/**
 * Axios 实例封装
 * - 统一注入 Authorization: Bearer <accessToken>
 * - 同时校验 HTTP 状态码与 Result.code（API.md 0.3 节）
 * - 凭证过期（401 / 1103 / 1104）单飞刷新后重放原请求
 * - 二进制流（导出 / 打印）透传，不走 Result 解析
 */
import axios from 'axios'
import type { AxiosError, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { ApiError, ResultCode, type Result } from '@/types/result'
import type { LoginResp } from '@/types/dto'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/utils/token'

/** 声明式扩展 axios 配置（请求层透传标记） */
declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 为 true 时跳过全局错误 toast（由调用方自行处理） */
    silent?: boolean
  }
}

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const instance = axios.create({
  baseURL,
  timeout: 15000
})

/* ------------------------- 请求拦截器 ------------------------- */

instance.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

/* ------------------------- 刷新令牌（单飞） ------------------------- */

interface RetriableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

let isRefreshing = false
/** 等待刷新完成的请求回调队列 */
let pendingQueue: Array<(token: string) => void> = []

const refreshAccessToken = async (): Promise<string> => {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new ApiError('未登录', ResultCode.UNAUTHORIZED, 401)
  }
  // 使用独立 axios 发起，避免递归触发拦截器
  const res = await axios.post<Result<LoginResp>>(`${baseURL}/auth/refresh`, { refreshToken })
  const body = res.data
  if (body.code !== ResultCode.SUCCESS || !body.data) {
    throw new ApiError(body.message, body.code, res.status)
  }
  setTokens(body.data.accessToken, body.data.refreshToken)
  return body.data.accessToken
}

const isBinaryRequest = (config: AxiosRequestConfig): boolean =>
  config.responseType === 'blob' || config.responseType === 'arraybuffer' || config.responseType === 'text'

const redirectToLogin = (): void => {
  clearTokens()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

const redirectToForbidden = (): void => {
  if (window.location.pathname !== '/403') {
    window.location.href = '/403'
  }
}

/** 凭证失效：刷新后重放原请求 */
const retryAfterRefresh = async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
  const retryConfig = config as RetriableConfig
  retryConfig._retry = true

  // 无 refreshToken 时直接登出
  if (!getRefreshToken()) {
    redirectToLogin()
    throw new ApiError('请先登录', ResultCode.UNAUTHORIZED, 401)
  }

  // 已有刷新在进行中，排队等待
  if (isRefreshing) {
    const token = await new Promise<string>((resolve) => {
      pendingQueue.push(resolve)
    })
    retryConfig.headers.set('Authorization', `Bearer ${token}`)
    return instance.request(retryConfig)
  }

  isRefreshing = true
  try {
    const token = await refreshAccessToken()
    pendingQueue.forEach((resolve) => resolve(token))
    pendingQueue = []
    retryConfig.headers.set('Authorization', `Bearer ${token}`)
    return instance.request(retryConfig)
  } catch (refreshError) {
    pendingQueue = []
    redirectToLogin()
    throw refreshError instanceof ApiError
      ? refreshError
      : new ApiError('登录已过期，请重新登录', ResultCode.TOKEN_EXPIRED, 401)
  } finally {
    isRefreshing = false
  }
}

/* ------------------------- 响应拦截器 ------------------------- */

const handleResponseSuccess = (
  response: AxiosResponse
): AxiosResponse | Promise<AxiosResponse> => {
  // 二进制 / 文本流（导出 Excel、打印 HTML）直接透传
  if (isBinaryRequest(response.config)) {
    return response
  }

  const body = response.data as Result<unknown> | undefined
  if (body && typeof body.code === 'number' && body.code === ResultCode.SUCCESS) {
    return response
  }

  const code = body?.code ?? -1
  const message = body?.message ?? '请求失败'

  // 凭证失效（HTTP 200 兜底）
  if (code === ResultCode.TOKEN_INVALID || code === ResultCode.TOKEN_EXPIRED) {
    redirectToLogin()
    return Promise.reject(new ApiError(message, code, response.status))
  }

  // 业务规则错误（HTTP 200 + code 2xxx）
  if (!response.config.silent) {
    ElMessage.error(message)
  }
  return Promise.reject(new ApiError(message, code, response.status))
}

const handleResponseError = async (error: AxiosError): Promise<AxiosResponse> => {
  // 网络层错误（无响应）
  if (!error.response) {
    if (!error.config?.silent) {
      ElMessage.error('网络异常，请稍后重试')
    }
    throw new ApiError('网络异常', ResultCode.INTERNAL_ERROR, 0)
  }

  const { status, config, data } = error.response
  const body = data as Result<unknown> | undefined
  const code = body?.code ?? -1
  const message = body?.message ?? `请求失败(${status})`

  // 二进制请求出错（导出失败等）
  if (isBinaryRequest(config)) {
    if (!config.silent) {
      ElMessage.error(message)
    }
    throw new ApiError(message, code, status)
  }

  // 凭证无效 / 过期 → 刷新重放（仅一次）
  const tokenInvalid =
    status === 401 || code === ResultCode.TOKEN_INVALID || code === ResultCode.TOKEN_EXPIRED
  if (tokenInvalid && config && !(config as RetriableConfig)._retry) {
    return retryAfterRefresh(config)
  }

  // 权限不足：admin 访问业务接口（403 + 1007）跳 403 页，其余提示
  if (status === 403 || code === ResultCode.ADMIN_NO_BUSINESS_VISIBILITY) {
    if (!config.silent) {
      ElMessage.error(message)
    }
    redirectToForbidden()
    throw new ApiError(message, code, status)
  }
  if (code === ResultCode.FORBIDDEN) {
    if (!config.silent) {
      ElMessage.error(message)
    }
    throw new ApiError(message, code, status)
  }

  // 其余错误统一提示
  if (!config.silent) {
    ElMessage.error(message)
  }
  throw new ApiError(message, code, status)
}

instance.interceptors.response.use(handleResponseSuccess, handleResponseError)

/* ------------------------- 类型化请求方法 ------------------------- */
/** resolve 为 Result<T>（统一响应体） */
export const request = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<Result<T>> {
    return instance.get<Result<T>>(url, config).then((res) => res.data)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<Result<T>> {
    return instance.post<Result<T>>(url, data, config).then((res) => res.data)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<Result<T>> {
    return instance.put<Result<T>>(url, data, config).then((res) => res.data)
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<Result<T>> {
    return instance.delete<Result<T>>(url, config).then((res) => res.data)
  }
}

/** 导出 Excel：返回原始 AxiosResponse（Blob） */
export const getBlob = (url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<Blob>> =>
  instance.get<Blob>(url, { responseType: 'blob', timeout: 60000, ...config })

/** 打印 HTML：返回文本内容 */
export const getText = (url: string, config?: AxiosRequestConfig): Promise<string> =>
  instance.get(url, { responseType: 'text', ...config }).then((res) => res.data as string)
