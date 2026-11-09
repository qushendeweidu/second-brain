/** 后端统一响应结构 R<T> */
export interface R<T> {
  code: number
  msg: string
  data: T
}

/** 分页请求参数 */
export interface PageDTO {
  pageNum: number
  pageSize: number
}

/** 分页响应 PageResult<T> */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 登录 / 注册请求 */
export interface LoginDTO {
  username: string
  password: string
}

/** 登录响应：key 为请求头名，值为 token，可能只返回其中一个 */
export type TokenMap = Partial<Record<'AccessToken' | 'RefreshToken', string>>

/** 用户状态：1 正常，0 禁用 */
export type UserStatus = 0 | 1

/** 用户视图 UserVO */
export interface UserVO {
  id: number
  username: string
  nickName: string | null
  email: string | null
  phone: string | null
  status: UserStatus
  avatar: string | null
  bio: string | null
  roles: string[] | null
  permissions: string[] | null
  userCreateTime: string | null
  userUpdateTime: string | null
  roleCreateTime: string | null
  roleUpdateTime: string | null
}

/** 用户分页查询 UserDTO */
export interface UserQueryDTO {
  pageDTO: PageDTO
  username?: string
  nickName?: string
  email?: string
  phone?: string
  bio?: string
  status?: UserStatus
  roles?: string[]
  permissions?: string[]
  userCreateStartTime?: string
  userCreateEndTime?: string
  userUpdateStartTime?: string
  userUpdateEndTime?: string
}

/** 创建用户 UserCreateDTO */
export interface UserCreateDTO {
  username: string
  password: string
  nickName?: string
  email?: string
  phone?: string
  bio?: string
  status?: UserStatus
}

/** 更新用户 UserUpdateDTO */
export interface UserUpdateDTO {
  id: number
  username?: string
  password?: string
  nickName?: string
  email?: string
  phone?: string
  bio?: string
  status?: UserStatus
}

/** 角色权限 UserRoleVO */
export interface UserRoleVO {
  id: number
  userId: number
  roles: string[]
  permissions: string[]
  createTime: string
  updateTime: string
}

/** 更新角色权限 */
export interface UserRoleUpdateDTO {
  userId: number
  roles?: string[]
  permissions?: string[]
}

/** 个人资料 UserProfileVO */
export interface UserProfileVO {
  userId: number
  nickName: string | null
  email: string | null
  phone: string | null
  avatar: string | null
  bio: string | null
  createTime: string | null
  updateTime: string | null
}

/** 更新个人资料（form-urlencoded） */
export interface UserProfileUpdateDTO {
  userId: number
  nickName?: string
  email?: string
  phone?: string
  bio?: string
  avatar?: string
}

/** java.util.concurrent.TimeUnit 枚举 */
export type TimeUnit =
  | 'NANOSECONDS'
  | 'MICROSECONDS'
  | 'MILLISECONDS'
  | 'SECONDS'
  | 'MINUTES'
  | 'HOURS'
  | 'DAYS'

/** 封禁用户请求 */
export interface BlockUserDTO {
  userId: number
  blockedTime: number
  timeUnit: TimeUnit
  blocked: boolean
}

/** GitHub 回调请求 */
export interface GithubCallbackDTO {
  code: string
  state: string
}

/** 用户限流配置 UserRestrictVO（对应 user_restrict 表一行） */
export interface UserRestrictVO {
  id: number
  userId: number
  restrictRequest: number
  windowSecond: number
  timeUnit: TimeUnit
}

/** 设置/更新用户限流（JSON body） */
export interface RestrictUserDTO {
  userId: number
  restrictRequest: number
  windowSecond: number
  timeUnit: TimeUnit
}
