import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * v-permission 按钮级权限指令：不满足任一权限时移除元素。
 * 用法：v-permission="'user:add'" 或 v-permission="['user:add','user:update']"
 * ADMIN 角色视为拥有全部权限（后端管理接口按角色鉴权）。
 */
export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const store = useUserStore()
    if (store.isAdmin || store.hasPermission(binding.value)) return
    el.parentNode?.removeChild(el)
  },
}
