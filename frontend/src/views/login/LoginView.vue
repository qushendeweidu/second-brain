<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { Label } from 'radix-vue'
import AuthShell from '@/components/AuthShell.vue'
import { useUserStore } from '@/stores/user'
import { notify } from '@/utils/notify'

const store = useUserStore()
const route = useRoute()
const router = useRouter()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const showPwd = ref(false)
const error = ref('')

/**
 * 提交登录：成功后按 redirect 回跳，管理员默认进管理后台。
 */
async function onSubmit(): Promise<void> {
  error.value = ''
  if (!form.username.trim() || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    await store.login({ username: form.username.trim(), password: form.password })
    notify('登录成功', 'success')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    // 仅允许站内相对路径，防止开放重定向
    const safe = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : ''
    await router.replace(safe || (store.isAdmin ? '/admin' : '/me'))
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell title="欢迎回来" subtitle="登录你的大众商城账户">
    <form class="space-y-5" novalidate @submit.prevent="onSubmit">
      <div class="space-y-1.5">
        <Label for="username" class="text-sm font-medium text-ink">用户名</Label>
        <input id="username" v-model="form.username" class="field" autocomplete="username" maxlength="255" />
      </div>
      <div class="space-y-1.5">
        <Label for="password" class="text-sm font-medium text-ink">密码</Label>
        <div class="relative">
          <input
            id="password"
            v-model="form.password"
            :type="showPwd ? 'text' : 'password'"
            class="field pr-11"
            autocomplete="current-password"
            maxlength="255"
          />
          <button
            type="button"
            class="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-ink-mute hover:text-ink outline-none focus-visible:text-accent"
            :aria-label="showPwd ? '隐藏密码' : '显示密码'"
            @click="showPwd = !showPwd"
          >
            <Icon :icon="showPwd ? 'lucide:eye-off' : 'lucide:eye'" class="size-4" aria-hidden="true" />
          </button>
        </div>
      </div>

      <p v-if="error" class="flex items-center gap-2 text-sm text-danger" role="alert">
        <Icon icon="lucide:alert-circle" class="size-4 shrink-0" aria-hidden="true" />{{ error }}
      </p>

      <button type="submit" class="btn btn-primary w-full" :disabled="loading">
        <Icon v-if="loading" icon="lucide:loader-2" class="size-4 animate-spin" aria-hidden="true" />
        {{ loading ? '登录中…' : '登录' }}
      </button>
    </form>
    <p class="mt-6 text-sm text-ink-soft">
      还没有账户？
      <RouterLink to="/register" class="font-medium text-accent underline-offset-4 hover:underline">注册</RouterLink>
    </p>
  </AuthShell>
</template>
