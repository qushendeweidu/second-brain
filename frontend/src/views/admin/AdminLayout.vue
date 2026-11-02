<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import 'element-plus/dist/index.css'
import { ElButton, ElConfigProvider, ElMenu, ElMenuItem, ElIcon } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { Expand, SwitchButton, User, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import logo from '@/assets/logo.png'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
/** 移动端侧边栏抽屉开关 */
const drawerOpen = ref(false)

// 切换路由自动收起移动端抽屉
watch(() => route.path, () => (drawerOpen.value = false))

/** 退出登录并跳转登录页 */
function onLogout(): void {
  store.logout()
  router.replace('/login')
}
</script>

<template>
  <ElConfigProvider :locale="zhCn">
    <div class="admin-shell">
      <div v-if="drawerOpen" class="admin-mask" aria-hidden="true" @click="drawerOpen = false"></div>
      <aside class="admin-aside" :class="{ open: drawerOpen }">
        <div class="admin-brand">
          <img :src="logo" alt="大众商城" width="34" height="34" class="logo" />
          <span>大众商城</span>
          <small>管理后台</small>
        </div>
        <ElMenu :default-active="route.path" router class="admin-menu">
          <ElMenuItem index="/admin/users">
            <ElIcon><User /></ElIcon>
            <span>用户管理</span>
          </ElMenuItem>
        </ElMenu>
      </aside>

      <div class="admin-main">
        <header class="admin-header">
          <div class="btn-row">
            <ElButton class="menu-toggle" :icon="Expand" text aria-label="打开菜单" @click="drawerOpen = true" />
            <h1 class="admin-title">{{ route.meta.title }}</h1>
          </div>
          <div class="btn-row">
            <ElButton :icon="UserFilled" text @click="router.push('/me')">个人中心</ElButton>
            <ElButton :icon="SwitchButton" text @click="onLogout">退出</ElButton>
          </div>
        </header>
        <main class="admin-content">
          <RouterView />
        </main>
      </div>
    </div>
  </ElConfigProvider>
</template>

<style>
/* ElementPlus 主题色对齐全站赭石强调色 */
.admin-shell {
  --el-color-primary: #0c50c0;
  --el-color-primary-light-3: #5585d3;
  --el-color-primary-light-5: #85a7e0;
  --el-color-primary-light-7: #b6caee;
  --el-color-primary-light-8: #cedcf2;
  --el-color-primary-light-9: #e7eef9;
  --el-color-primary-dark-2: #093f99;
  --el-border-radius-base: 6px;
  min-height: 100vh;
  display: flex;
  background: #f3f6fb;
}
.btn-row { display: flex; align-items: center; gap: 8px; }
.btn-row .el-button { margin: 0 !important; }
.admin-aside {
  position: fixed; inset: 0 auto 0 0; width: 220px; z-index: 30;
  background: #14213d; color: #f3f6fb; display: flex; flex-direction: column;
  transition: transform 0.2s ease;
}
.admin-brand { display: flex; align-items: center; gap: 10px; height: 64px; padding: 0 22px; font-family: var(--font-display); font-size: 17px; }
.admin-brand .logo { width: 34px; height: 34px; border-radius: 50%; background: #fbfcff; flex-shrink: 0; }
.admin-brand small { font-family: var(--font-mono); font-size: 11px; color: rgba(243, 246, 251, 0.45); }
.admin-menu.el-menu {
  --el-menu-bg-color: transparent; --el-menu-text-color: rgba(243, 246, 251, 0.7);
  --el-menu-hover-bg-color: rgba(243, 246, 251, 0.06); --el-menu-active-color: #f3f6fb;
  border-right: 0;
}
.admin-menu .el-menu-item.is-active { background: rgba(12, 80, 192, 0.55); }
.admin-main { flex: 1; min-width: 0; margin-left: 220px; display: flex; flex-direction: column; }
.admin-header {
  height: 64px; padding: 0 28px; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #dbe3ef; background: #fbfcff;
}
.admin-title { font-family: var(--font-display); font-size: 20px; color: #14213d; }
.admin-content { padding: 24px 28px; }
.menu-toggle { display: none !important; }
.admin-mask { display: none; }

@media (max-width: 767px) {
  .admin-aside { transform: translateX(-100%); }
  .admin-aside.open { transform: translateX(0); }
  .admin-mask { display: block; position: fixed; inset: 0; z-index: 25; background: rgba(20, 33, 61, 0.4); }
  .admin-main { margin-left: 0; }
  .admin-header { padding: 0 12px; }
  .admin-content { padding: 16px 12px; }
  .menu-toggle { display: inline-flex !important; }
  .el-dialog { width: 94vw !important; margin: 5vh auto !important; }
  .el-message-box { width: 88vw !important; }
  .el-drawer { width: 100% !important; }
}
</style>
