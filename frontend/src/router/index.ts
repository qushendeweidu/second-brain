import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getTokens } from '@/utils/token'
import { refreshShortToken } from '@/utils/request'
import { notify } from '@/utils/notify'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 无需登录 */
    public?: boolean
    /** 仅游客可见（已登录访问时跳走） */
    guestOnly?: boolean
    /** 需要的角色 */
    role?: string
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/me' },
    {
      path: '/login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录', public: true, guestOnly: true },
    },
    {
      path: '/register',
      component: () => import('@/views/login/RegisterView.vue'),
      meta: { title: '注册', public: true, guestOnly: true },
    },
    {
      path: '/me',
      component: () => import('@/views/user/ProfileView.vue'),
      meta: { title: '个人中心' },
    },
    {
      path: '/oauth/github/callback',
      component: () => import('@/views/user/GithubCallbackView.vue'),
      meta: { title: 'GitHub 绑定' },
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { role: 'ADMIN' },
      redirect: '/admin/users',
      children: [
        {
          path: 'users',
          component: () => import('@/views/admin/UserListView.vue'),
          meta: { title: '用户管理', role: 'ADMIN' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/me' },
  ],
})

/** 路由守卫：未登录跳登录页；角色不符跳个人中心 */
router.beforeEach(async (to) => {
  const store = useUserStore()
  const { RefreshToken, AccessToken } = getTokens()
  // 冷启动（新标签页/会话结束）只有长 token 时，先换短 token，否则解析不到角色权限
  if (RefreshToken && !AccessToken) {
    try {
      await refreshShortToken()
    } catch {
      // 换取失败不中断跳转，后续请求会触发重新登录
    }
  }
  store.syncClaims()
  if (to.meta.guestOnly && store.isLogin) return '/me'
  if (!to.meta.public && !store.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.role && !store.roles.includes(to.meta.role)) {
    notify('无权限访问该页面', 'error')
    return '/me'
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 大众商城` : '大众商城'
})

export default router
