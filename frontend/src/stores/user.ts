import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as api from '@/api'
import type { LoginDTO, UserProfileVO } from '@/types'
import { clearTokens, getClaims, setTokens, type TokenClaims } from '@/utils/token'

/** 用户状态：登录态、角色权限（取自长 token claim）与本人资料 */
export const useUserStore = defineStore('user', () => {
  const claims = ref<TokenClaims | null>(getClaims())
  const profile = ref<UserProfileVO | null>(null)

  const isLogin = computed(() => !!claims.value)
  const userId = computed(() => (claims.value ? Number(claims.value.sub) : 0))
  const roles = computed(() => claims.value?.roles ?? [])
  const permissions = computed(() => claims.value?.permissions ?? [])
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  const displayName = computed(() => profile.value?.nickName || `用户 ${userId.value}`)

  /**
   * 判断是否拥有任一权限。
   * @param perm 单个或多个权限码
   * @returns 是否命中
   */
  function hasPermission(perm: string | string[]): boolean {
    const list = Array.isArray(perm) ? perm : [perm]
    return list.some((p) => permissions.value.includes(p))
  }

  /**
   * 登录：清理旧 token 后请求，存储返回的 token map 并解析 claims。
   * @param dto 用户名与密码
   */
  async function login(dto: LoginDTO): Promise<void> {
    clearTokens()
    const map = await api.login(dto)
    setTokens(map)
    claims.value = getClaims()
    if (!claims.value) throw new Error('登录凭证解析失败')
  }

  /**
   * 拉取本人资料。
   * @returns 资料对象
   */
  async function fetchProfile(): Promise<UserProfileVO> {
    profile.value = await api.getMyProfile()
    return profile.value
  }

  /** 退出登录：清 token 与内存状态 */
  function logout(): void {
    clearTokens()
    claims.value = null
    profile.value = null
  }

  return {
    claims,
    profile,
    isLogin,
    userId,
    roles,
    permissions,
    isAdmin,
    displayName,
    hasPermission,
    login,
    fetchProfile,
    logout,
  }
})
