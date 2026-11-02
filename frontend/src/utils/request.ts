import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import type { R } from '@/types'
import { clearTokens, getTokens, isExpired, setTokens } from './token'
import { notify } from './notify'

/** 需清 token 并跳转登录的业务码 */
const RELOGIN_CODES = new Set([401, 40101, 40102])

/** 业务错误，携带后端 code 便于页面按码处理 */
export class BizError extends Error {
  code: number
  /**
   * @param code 后端业务码
   * @param msg 后端提示文案
   */
  constructor(code: number, msg: string) {
    super(msg)
    this.code = code
  }
}

/** 扩展配置：raw=true 时不按 R 解包（GitHub 接口返回裸字符串/空 body）；silent=true 时不弹错误提示 */
declare module 'axios' {
  interface AxiosRequestConfig {
    raw?: boolean
    silent?: boolean
    skipAuth?: boolean
  }
}

/** Axios 实例，baseURL 默认走 Vite /api 代理 */
const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
})

let refreshing: Promise<string> | null = null

/**
 * 用长 token 换取新短 token（并发请求共享同一次换取）。
 * @returns 新的短 token
 */
export function refreshShortToken(): Promise<string> {
  if (!refreshing) {
    const { RefreshToken } = getTokens()
    refreshing = axios
      .post<R<string>>(`${instance.defaults.baseURL}/user/get_short_token`, null, {
        headers: RefreshToken ? { RefreshToken } : {},
      })
      .then((res) => {
        if (res.data?.code !== 200 || !res.data.data) {
          throw new BizError(res.data?.code ?? 401, res.data?.msg ?? '登录已失效')
        }
        setTokens({ Authorization: res.data.data })
        return res.data.data
      })
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

/** 登录失效处理：清 token，跳转登录页并带回跳地址 */
function handleRelogin(msg: string): void {
  clearTokens()
  if (location.pathname.startsWith('/login')) return
  notify(msg || '登录已失效，请重新登录', 'error')
  const redirect = encodeURIComponent(location.pathname + location.search)
  location.replace(`/login?redirect=${redirect}`)
}

/**
 * 统一处理业务错误：失效类跳登录，其余弹提示。
 * @param code 业务码
 * @param msg 文案
 * @param config 请求配置（silent 时不提示）
 */
function rejectBiz(code: number, msg: string, config?: AxiosRequestConfig): Promise<never> {
  if (RELOGIN_CODES.has(code)) handleRelogin(msg)
  else if (!config?.silent) notify(msg || '请求失败', 'error')
  return Promise.reject(new BizError(code, msg))
}

instance.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  if (config.skipAuth) return config
  let { Authorization, RefreshToken } = getTokens()
  // 有长 token 但短 token 缺失/过期 → 先换取短 token，避免请求必然 401
  if (RefreshToken && isExpired(Authorization)) {
    try {
      Authorization = await refreshShortToken()
    } catch (e) {
      const err = e as BizError
      handleRelogin(err.message)
      throw err
    }
  }
  if (Authorization) config.headers.set('Authorization', Authorization)
  if (RefreshToken) config.headers.set('RefreshToken', RefreshToken)
  return config
})

instance.interceptors.response.use(
  // 拦截器返回解包后的 data，而非 AxiosResponse，故显式放宽返回类型
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (res): any => {
    const body = res.data as R<unknown> | string | undefined
    const isR = typeof body === 'object' && body !== null && 'code' in body
    if (res.config.raw) {
      // 裸返回接口：失败时后端仍可能返回 R 错误结构
      if (isR && (body as R<unknown>).code !== 200) {
        return rejectBiz((body as R<unknown>).code, (body as R<unknown>).msg, res.config)
      }
      return body
    }
    if (!isR) return rejectBiz(50001, '响应格式异常', res.config)
    const r = body as R<unknown>
    if (r.code === 200) return r.data
    return rejectBiz(r.code, r.msg, res.config)
  },
  (error: AxiosError<R<unknown>>) => {
    const body = error.response?.data
    if (body && typeof body === 'object' && 'code' in body) {
      return rejectBiz(body.code, body.msg, error.config)
    }
    if (error.response?.status === 401) return rejectBiz(401, '登录已失效', error.config)
    if (error.response?.status === 403) return rejectBiz(40003, '禁止访问', error.config)
    const msg = error.code === 'ECONNABORTED' ? '请求超时' : '网络异常，请稍后再试'
    return rejectBiz(error.response?.status ?? 0, msg, error.config)
  },
)

/**
 * 发起请求，返回解包后的 data。
 * @param config Axios 请求配置
 * @returns R.data（raw 模式下为原始响应体）
 */
export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return instance.request(config) as unknown as Promise<T>
}

/**
 * 把后端返回的相对文件路径（/file/xxx）转为可访问地址；http 开头原样返回。
 * @param url 后端返回路径
 * @returns 浏览器可直接使用的地址，空值返回空串
 */
export function fileUrl(url: string | null | undefined): string {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  // 后端头像字段存的是 MinIO 对象名（如 2026/11/01/xxx.jpg），需补 /file/ 前缀走后端静态代理
  const path = url.replace(/^\/+/, '')
  const withPrefix = path.startsWith('file/') ? path : `file/${path}`
  return `${instance.defaults.baseURL}/${withPrefix}`
}
