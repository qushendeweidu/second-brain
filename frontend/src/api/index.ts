import { request } from '@/utils/request'
import type {
  BlockUserDTO,
  GithubCallbackDTO,
  LoginDTO,
  PageResult,
  TimeUnit,
  TokenMap,
  UserCreateDTO,
  UserProfileUpdateDTO,
  UserProfileVO,
  UserQueryDTO,
  UserRoleUpdateDTO,
  UserRoleVO,
  UserUpdateDTO,
  UserVO,
} from '@/types'

/* ============ 认证 ============ */

/**
 * 登录。不携带旧 token，确保后端同时签发长短 token。
 * @param data 用户名与密码
 * @returns 以请求头名为 key 的 token 对象
 */
export const login = (data: LoginDTO) =>
  request<TokenMap>({ url: '/user/login', method: 'post', data, skipAuth: true })

/**
 * 注册（成功不自动登录）。
 * @param data 用户名与密码
 * @returns null
 */
export const register = (data: LoginDTO) =>
  request<null>({ url: '/user/register', method: 'post', data, skipAuth: true })

/* ============ 个人资料 ============ */

/**
 * 获取本人资料。
 * @returns UserProfileVO
 */
export const getMyProfile = () => request<UserProfileVO>({ url: '/user/profile/myself', method: 'get' })

/**
 * 更新资料（后端无 @RequestBody，按 form-urlencoded 提交，空值字段不提交）。
 * @param dto 资料字段，userId 必填
 * @returns 是否成功
 */
export const updateProfile = (dto: UserProfileUpdateDTO) => {
  const form = new URLSearchParams()
  Object.entries(dto).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') form.append(k, String(v))
  })
  return request<boolean>({
    url: '/user/updateUserProfile',
    method: 'patch',
    data: form,
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
}

/**
 * 上传头像（文件字段名 multipartFile，userId 走 query）。
 * @param userId 目标用户 id
 * @param file 图片文件
 * @returns 头像 URL（/file/xxx）
 */
export const uploadAvatar = (userId: number, file: File) => {
  const fd = new FormData()
  fd.append('multipartFile', file)
  return request<string>({ url: '/user/saveUserAvatar', method: 'post', params: { userId }, data: fd })
}

/**
 * 管理员查看指定用户资料。
 * @param userId 用户 id
 * @returns UserProfileVO
 */
export const getUserProfile = (userId: number) =>
  request<UserProfileVO>({ url: `/user/profile/${userId}`, method: 'get' })

/* ============ 用户管理（ADMIN） ============ */

/**
 * 用户分页列表（POST + 嵌套 pageDTO）。
 * @param dto 筛选条件
 * @returns 分页结果
 */
export const listUsers = (dto: UserQueryDTO) =>
  request<PageResult<UserVO>>({ url: '/user/list', method: 'post', data: dto })

/**
 * 用户详情。
 * @param id 用户 id
 * @returns UserVO
 */
export const getUser = (id: number) => request<UserVO>({ url: `/user/${id}`, method: 'get' })

/**
 * 创建用户。
 * @param dto 创建参数
 * @returns 新用户 id
 */
export const createUser = (dto: UserCreateDTO) => request<number>({ url: '/user/create', method: 'post', data: dto })

/**
 * 更新用户（含资料字段）。
 * @param dto 更新参数，id 必填
 * @returns 是否成功
 */
export const updateUser = (dto: UserUpdateDTO) => request<boolean>({ url: '/user/update', method: 'patch', data: dto })

/**
 * 删除用户（级联删除角色、资料与登录态）。
 * @param id 用户 id
 * @returns null
 */
export const deleteUser = (id: number) => request<null>({ url: `/user/${id}`, method: 'delete' })

/* ============ 角色权限（ADMIN） ============ */

/**
 * 获取用户角色权限（无记录时后端返回 40104，调用方静默处理）。
 * @param userId 用户 id
 * @returns UserRoleVO
 */
export const getUserRole = (userId: number) =>
  request<UserRoleVO>({ url: `/user/role/${userId}`, method: 'get', silent: true })

/**
 * 更新角色权限（成功后该用户被强制重登）。
 * @param dto userId + roles/permissions
 * @returns 是否成功
 */
export const updateUserRole = (dto: UserRoleUpdateDTO) =>
  request<boolean>({ url: '/user/role', method: 'patch', data: dto })

/**
 * 删除角色权限。
 * @param userId 用户 id
 * @returns null
 */
export const deleteUserRole = (userId: number) => request<null>({ url: `/user/role/${userId}`, method: 'delete' })

/* ============ 账户管控（ADMIN） ============ */

/**
 * 封禁用户（临时 / 永久）。
 * @param dto 封禁参数
 * @returns null
 */
export const blockUser = (dto: BlockUserDTO) => request<null>({ url: '/user/blockedUser', method: 'post', data: dto })

/**
 * 删除用户登录态（强制下线 / 解除临时封禁）。
 * @param userId 用户 id
 * @returns null
 */
export const deleteUserSecurity = (userId: number) =>
  request<null>({ url: `/user/deleteUserSecurity/${userId}`, method: 'delete' })

/**
 * 对用户限流（参数均为 query）。
 * @param userId 用户 id
 * @param restrictId 限流配置 id
 * @param timeUnit 时间单位
 * @returns null
 */
export const restrictUser = (userId: number, restrictId: number, timeUnit: TimeUnit) =>
  request<null>({ url: '/user/restrictUserToken', method: 'post', params: { userId, restrictId, timeUnit } })

/* ============ GitHub 绑定 ============
 * 后端三个接口均从 SecurityContext 取当前用户（不再解析请求头 token），
 * 但仍经过 JWT 过滤器，故请求拦截器照常携带双 token。
 */

/**
 * 获取 GitHub 授权地址（裸字符串返回）。
 * @returns 授权 URL
 */
export const githubAuthorize = () =>
  request<string>({ url: '/auth/github/authorize', method: 'get', raw: true, responseType: 'text' }).then((body) => {
    // responseType=text 时错误 R 结构为字符串，尝试解析以给出后端文案
    const text = String(body ?? '').trim()
    if (text.startsWith('{')) {
      const r = JSON.parse(text) as { code: number; msg: string }
      throw new Error(r.msg || '获取授权地址失败')
    }
    if (!/^https?:\/\//.test(text)) throw new Error('获取授权地址失败')
    return text
  })

/**
 * GitHub 回调绑定（空 body 返回）。
 * @param dto code 与 state
 * @returns 无
 */
export const githubCallback = (dto: GithubCallbackDTO) =>
  request<void>({ url: '/auth/github/callback', method: 'post', data: dto, raw: true })

/**
 * 解绑 GitHub（空 body 返回）。
 * @returns 无
 */
export const githubUnbind = () => request<void>({ url: '/auth/github/unbind', method: 'delete', raw: true })
