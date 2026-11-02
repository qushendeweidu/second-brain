<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { githubCallback } from '@/api'

const route = useRoute()
const router = useRouter()
const state = ref<'pending' | 'ok' | 'fail'>('pending')
const message = ref('正在完成 GitHub 绑定…')

/** 读取回调 code/state 并提交后端绑定；失败时展示原因 */
onMounted(async () => {
  const code = typeof route.query.code === 'string' ? route.query.code : ''
  const st = typeof route.query.state === 'string' ? route.query.state : ''
  const denied = !!route.query.error
  // code/state 为一次性凭证：读取后立即从地址栏与历史记录中抹掉，防止刷新或后退时重复提交过期 code
  await router.replace({ path: route.path, query: {} })
  if (denied) {
    state.value = 'fail'
    message.value = '你已取消 GitHub 授权'
    return
  }
  if (!code || !st) {
    state.value = 'fail'
    message.value = '回调参数缺失，请重新发起绑定'
    return
  }
  try {
    await githubCallback({ code, state: st })
    state.value = 'ok'
    message.value = 'GitHub 绑定成功'
    setTimeout(() => router.replace('/me'), 1200)
  } catch (e) {
    state.value = 'fail'
    message.value = (e as Error).message || '绑定失败'
  }
})
</script>

<template>
  <main class="flex min-h-screen items-center justify-center px-6">
    <div class="w-full max-w-sm rounded-xl border border-line bg-card p-8 text-center">
      <Icon
        :icon="state === 'pending' ? 'lucide:loader-2' : state === 'ok' ? 'lucide:check-circle-2' : 'lucide:x-circle'"
        :class="['mx-auto size-10', state === 'pending' && 'animate-spin text-ink-mute', state === 'ok' && 'text-ok', state === 'fail' && 'text-danger']"
        aria-hidden="true"
      />
      <p class="mt-4 font-display text-lg" role="status">{{ message }}</p>
      <RouterLink v-if="state !== 'pending'" to="/me" class="btn btn-ghost mt-6 py-2 text-sm">返回个人中心</RouterLink>
    </div>
  </main>
</template>
