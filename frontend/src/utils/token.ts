import type { TokenMap } from '@/types'

/** 长 token（RefreshToken 头，30 天）存储键，持久化在 localStorage */
const REFRESH_KEY = 'sb_refresh_token'
/** 短 token（Authorization 头，1 小时）存储键，存 sessionStorage，关闭标签页即失效 */
const SHORT_KEY = 'sb_short_token'

/** 长 token payload 中前端关心的字段 */
export interface TokenClaims {
  sub: string
  roles: string[]
  permissions: string[]
  exp?: number
}

/**
 * 读取本地双 token。
 * @returns 以请求头名为 key 的 token 对象，缺失的 key 为 undefined
 */
export function getTokens(): TokenMap {
  return {
    RefreshToken: localStorage.getItem(REFRESH_KEY) ?? undefined,
    Authorization: sessionStorage.getItem(SHORT_KEY) ?? undefined,
  }
}

/**
 * 写入 token；只覆盖 map 中存在的 key（后端可能只返回其中一个）。
 * @param map 登录响应 data 或 { Authorization: 短token }
 */
export function setTokens(map: TokenMap): void {
  if (map.RefreshToken) localStorage.setItem(REFRESH_KEY, map.RefreshToken)
  if (map.Authorization) sessionStorage.setItem(SHORT_KEY, map.Authorization)
}

/** 清空本地双 token（退出登录 / token 失效时调用） */
export function clearTokens(): void {
  localStorage.removeItem(REFRESH_KEY)
  sessionStorage.removeItem(SHORT_KEY)
}

/**
 * 解析 JWT payload（仅 base64url 解码，不校验签名，签名由后端校验）。
 * @param token JWT 字符串
 * @returns payload 对象；解析失败返回 null
 */
/** 已解码 payload 缓存：token 不变就不重复 base64 + JSON 解析（每个请求都会检查短 token 过期） */
const payloadCache = new Map<string, unknown>()

export function decodeJwt<T = Record<string, unknown>>(token: string): T | null {
  if (payloadCache.has(token)) return payloadCache.get(token) as T | null
  const payload = parseJwt<T>(token)
  // 只保留最近的少量 token（长、短各一，外加换取过渡期），防止无限增长
  if (payloadCache.size >= 4) payloadCache.clear()
  payloadCache.set(token, payload)
  return payload
}

/**
 * 实际解码 JWT payload。
 * @param token JWT 字符串
 * @returns payload 对象；解析失败返回 null
 */
function parseJwt<T>(token: string): T | null {
  try {
    const part = token.split('.')[1]
    if (!part) return null
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0))
    return JSON.parse(new TextDecoder().decode(bytes)) as T
  } catch {
    return null
  }
}

/**
 * 判断 JWT 是否已过期（预留 30 秒缓冲）。
 * @param token JWT 字符串
 * @returns true 表示已过期或无法解析
 */
export function isExpired(token: string | undefined): boolean {
  if (!token) return true
  const payload = decodeJwt<{ exp?: number }>(token)
  if (!payload?.exp) return false
  return payload.exp * 1000 - 30_000 < Date.now()
}

/**
 * 从长 token 解析用户 id、角色与权限。
 * @returns claims；无长 token 或解析失败返回 null
 */
export function getClaims(): TokenClaims | null {
  const { RefreshToken } = getTokens()
  if (!RefreshToken) return null
  const payload = decodeJwt<Partial<TokenClaims>>(RefreshToken)
  if (!payload?.sub) return null
  return {
    sub: payload.sub,
    roles: Array.isArray(payload.roles) ? payload.roles : [],
    permissions: Array.isArray(payload.permissions) ? payload.permissions : [],
    exp: payload.exp,
  }
}
