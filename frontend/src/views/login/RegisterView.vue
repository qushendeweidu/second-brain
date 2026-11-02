<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { Label } from 'radix-vue'
import AuthShell from '@/components/AuthShell.vue'
import { register } from '@/api'
import { notify } from '@/utils/notify'

const router = useRouter()
const form = reactive({ username: '', password: '', confirm: '' })
const loading = ref(false)
const error = ref('')

/**
 * 校验表单。
 * @returns 错误文案，通过返回空串
 */
function validate(): string {
  if (!form.username.trim()) return '请输入用户名'
  if (form.password.length < 6) return '密码至少 6 位'
  if (form.password !== form.confirm) return '两次输入的密码不一致'
  return ''
}

/** 提交注册：成功后跳登录页（后端注册不自动登录） */
async function onSubmit(): Promise<void> {
  error.value = validate()
  if (error.value) return
  loading.value = true
  try {
    await register({ username: form.username.trim(), password: form.password })
    notify('注册成功，请登录', 'success')
    await router.replace('/login')
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell title="创建账户" subtitle="注册一个大众商城账户">
    <form class="space-y-5" novalidate @submit.prevent="onSubmit">
      <div class="space-y-1.5">
        <Label for="r-username" class="text-sm font-medium text-ink">用户名</Label>
        <input id="r-username" v-model="form.username" class="field" autocomplete="username" maxlength="255" />
      </div>
      <div class="space-y-1.5">
        <Label for="r-password" class="text-sm font-medium text-ink">密码</Label>
        <input
          id="r-password"
          v-model="form.password"
          type="password"
          class="field"
          autocomplete="new-password"
          maxlength="255"
          aria-describedby="pwd-hint"
        />
        <p id="pwd-hint" class="text-xs text-ink-mute">至少 6 位</p>
      </div>
      <div class="space-y-1.5">
        <Label for="r-confirm" class="text-sm font-medium text-ink">确认密码</Label>
        <input id="r-confirm" v-model="form.confirm" type="password" class="field" autocomplete="new-password" maxlength="255" />
      </div>

      <p v-if="error" class="flex items-center gap-2 text-sm text-danger" role="alert">
        <Icon icon="lucide:alert-circle" class="size-4 shrink-0" aria-hidden="true" />{{ error }}
      </p>

      <button type="submit" class="btn btn-primary w-full" :disabled="loading">
        <Icon v-if="loading" icon="lucide:loader-2" class="size-4 animate-spin" aria-hidden="true" />
        {{ loading ? '提交中…' : '注册' }}
      </button>
    </form>
    <p class="mt-6 text-sm text-ink-soft">
      已有账户？
      <RouterLink to="/login" class="font-medium text-accent underline-offset-4 hover:underline">去登录</RouterLink>
    </p>
  </AuthShell>
</template>
