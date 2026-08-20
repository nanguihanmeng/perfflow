/**
 * Token 管理（localStorage）
 */
const ACCESS_TOKEN_KEY = 'perfflow_access_token'
const REFRESH_TOKEN_KEY = 'perfflow_refresh_token'

export const getAccessToken = (): string => localStorage.getItem(ACCESS_TOKEN_KEY) ?? ''

export const getRefreshToken = (): string => localStorage.getItem(REFRESH_TOKEN_KEY) ?? ''

export const setTokens = (accessToken: string, refreshToken: string): void => {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export const clearTokens = (): void => {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export const hasToken = (): boolean => getAccessToken() !== ''
