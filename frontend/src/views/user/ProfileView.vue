<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import {
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogOverlay,
  AlertDialogPortal,
  AlertDialogRoot,
  AlertDialogTitle,
  AlertDialogTrigger,
  AvatarFallback,
  AvatarImage,
  AvatarRoot,
  Label,
} from 'radix-vue'
import { githubAuthorize, githubUnbind, updateProfile, uploadAvatar } from '@/api'
import BrandMark from '@/components/BrandMark.vue'
import { useUserStore } from '@/stores/user'
import { fileUrl } from '@/utils/request'
import { notify } from '@/utils/notify'
import { formatTime } from '@/utils/format'

const store = useUserStore()
const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const ghLoading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const form = reactive({ nickName: '', email: '', phone: '', bio: '' })
const errors = reactive<Record<string, string>>({})

const profile = computed(() => store.profile)
const initial = computed(() => (profile.value?.nickName || 'U').slice(0, 1).toUpperCase())

/** 用资料填充表单 */
function fillForm(): void {
  form.nickName = profile.value?.nickName ?? ''
  form.email = profile.value?.email ?? ''
  form.phone = profile.value?.phone ?? ''
  form.bio = profile.value?.bio ?? ''
}

/** 加载本人资料 */
async function load(): Promise<void> {
  loading.value = true
  try {
    await store.fetchProfile()
    fillForm()
  } finally {
    loading.value = false
  }
}

/**
 * 表单校验（长度与邮箱格式）。
 * @returns 是否通过
 */
function validate(): boolean {
  Object.keys(errors).forEach((k) => delete errors[k])
  if (form.nickName.length > 255) errors.nickName = '昵称不超过 255 字'
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = '邮箱格式不正确'
  if (form.phone.length > 255) errors.phone = '电话不超过 255 字'
  if (form.bio.length > 255) errors.bio = '简介不超过 255 字'
  return Object.keys(errors).length === 0
}

/** 保存资料（后端仅更新非空字段） */
async function onSave(): Promise<void> {
  if (!validate()) return
  saving.value = true
  try {
    await updateProfile({ userId: store.userId, ...form })
    notify('资料已保存', 'success')
    // 后端只更新非空字段，本地按同样规则合并，省去一次重新拉取
    if (store.profile) {
      ;(['nickName', 'email', 'phone', 'bio'] as const).forEach((k) => {
        if (form[k]) store.profile![k] = form[k]
      })
    }
    fillForm()
  } finally {
    saving.value = false
  }
}

/**
 * 选择头像后上传（限制图片类型与 5MB）。
 * @param e input change 事件
 */
async function onAvatarChange(e: Event): Promise<void> {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) return notify('请选择图片文件', 'error')
  if (file.size > 5 * 1024 * 1024) return notify('图片不能超过 5MB', 'error')
  uploading.value = true
  try {
    // 接口已返回新头像地址，直接回填，省去一次 /user/profile/myself 请求
    const url = await uploadAvatar(store.userId, file)
    if (store.profile) store.profile.avatar = url
    notify('头像已更新', 'success')
  } finally {
    uploading.value = false
  }
}

/** 跳转 GitHub 授权页 */
async function onBindGithub(): Promise<void> {
  ghLoading.value = true
  try {
    const url = await githubAuthorize()
    window.location.href = url
  } catch (e) {
    notify((e as Error).message, 'error')
    ghLoading.value = false
  }
}

/** 解绑 GitHub */
async function onUnbindGithub(): Promise<void> {
  ghLoading.value = true
  try {
    await githubUnbind()
    notify('已解绑 GitHub', 'success')
  } finally {
    ghLoading.value = false
  }
}

/** 退出登录 */
function onLogout(): void {
  store.logout()
  router.replace('/login')
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen">
    <header class="border-b border-line bg-card/70">
      <div class="mx-auto flex h-16 max-w-5xl items-center justify-between px-6">
        <BrandMark />

        <nav class="flex items-center gap-2">
          <RouterLink v-if="store.isAdmin" to="/admin" class="btn btn-ghost py-2 text-sm">
            <Icon icon="lucide:layout-dashboard" class="size-4" aria-hidden="true" />管理后台
          </RouterLink>
          <button type="button" class="btn btn-ghost py-2 text-sm" @click="onLogout">
            <Icon icon="lucide:log-out" class="size-4" aria-hidden="true" />退出
          </button>
        </nav>
      </div>
    </header>

    <main class="mx-auto max-w-5xl px-6 py-10">
      <div v-if="loading" class="flex items-center gap-2 text-ink-soft">
        <Icon icon="lucide:loader-2" class="size-4 animate-spin" aria-hidden="true" />加载中…
      </div>

      <div v-else class="grid grid-cols-[280px_1fr] gap-8 max-md:grid-cols-1">
        <!-- 左栏：身份卡 -->
        <aside class="space-y-6">
          <section class="rounded-xl border border-line bg-card p-6">
            <div class="flex flex-col items-center text-center">
              <AvatarRoot class="inline-flex size-24 items-center justify-center overflow-hidden rounded-full border border-line bg-accent-soft">
                <AvatarImage v-if="profile?.avatar" :src="fileUrl(profile.avatar)" alt="头像" class="size-full object-cover" />
                <AvatarFallback class="font-display text-3xl text-accent-strong">{{ initial }}</AvatarFallback>
              </AvatarRoot>
              <h1 class="mt-4 font-display text-xl">{{ store.displayName }}</h1>
              <p class="mt-1 font-mono text-xs text-ink-mute">ID {{ store.userId }}</p>
              <div class="mt-3 flex flex-wrap justify-center gap-1.5">
                <span
                  v-for="r in store.roles"
                  :key="r"
                  class="rounded-full border border-line px-2.5 py-0.5 font-mono text-[11px] text-ink-soft"
                >{{ r }}</span>
              </div>
              <input ref="fileInput" type="file" accept="image/*" class="sr-only" tabindex="-1" @change="onAvatarChange" />
              <button type="button" class="btn btn-ghost mt-5 w-full py-2 text-sm" :disabled="uploading" @click="fileInput?.click()">
                <Icon :icon="uploading ? 'lucide:loader-2' : 'lucide:image-up'" :class="['size-4', uploading && 'animate-spin']" aria-hidden="true" />
                {{ uploading ? '上传中…' : '更换头像' }}
              </button>
            </div>
            <dl class="mt-6 space-y-2 border-t border-line pt-4 text-xs text-ink-mute">
              <div class="flex justify-between gap-2"><dt>创建于</dt><dd class="font-mono">{{ formatTime(profile?.createTime) }}</dd></div>
              <div class="flex justify-between gap-2"><dt>更新于</dt><dd class="font-mono">{{ formatTime(profile?.updateTime) }}</dd></div>
            </dl>
          </section>

          <!-- GitHub 绑定 -->
          <section class="rounded-xl border border-line bg-card p-6">
            <h2 class="flex items-center gap-2 text-sm font-semibold">
              <Icon icon="lucide:github" class="size-4" aria-hidden="true" />GitHub 身份
            </h2>
            <p class="mt-2 text-xs leading-relaxed text-ink-soft">一个账户只能绑定一个 GitHub，一个 GitHub 也只能绑定一个账户。</p>
            <div class="mt-4 flex flex-col gap-2">
              <button type="button" class="btn btn-primary w-full py-2 text-sm" :disabled="ghLoading" @click="onBindGithub">
                <Icon icon="lucide:link" class="size-4" aria-hidden="true" />绑定 GitHub
              </button>
              <AlertDialogRoot>
                <AlertDialogTrigger class="btn btn-danger w-full py-2 text-sm" :disabled="ghLoading">
                  <Icon icon="lucide:unlink" class="size-4" aria-hidden="true" />解除绑定
                </AlertDialogTrigger>
                <AlertDialogPortal>
                  <AlertDialogOverlay class="fixed inset-0 z-50 bg-ink/40" />
                  <AlertDialogContent class="fixed left-1/2 top-1/2 z-50 w-[min(92vw,420px)] -translate-x-1/2 -translate-y-1/2 rounded-xl border border-line bg-card p-6 shadow-xl outline-none">
                    <AlertDialogTitle class="font-display text-lg">解除 GitHub 绑定？</AlertDialogTitle>
                    <AlertDialogDescription class="mt-2 text-sm text-ink-soft">解绑后可以重新绑定其他 GitHub 账户。</AlertDialogDescription>
                    <div class="mt-6 flex items-center justify-end gap-2">
                      <AlertDialogCancel class="btn btn-ghost py-2 text-sm">取消</AlertDialogCancel>
                      <AlertDialogAction class="btn btn-danger py-2 text-sm" @click="onUnbindGithub">确认解绑</AlertDialogAction>
                    </div>
                  </AlertDialogContent>
                </AlertDialogPortal>
              </AlertDialogRoot>
            </div>
          </section>
        </aside>

        <!-- 右栏：资料编辑 -->
        <section class="rounded-xl border border-line bg-card p-8 max-md:p-5">
          <h2 class="font-display text-2xl">个人资料</h2>
          <p class="mt-1 text-sm text-ink-soft">留空的字段不会被修改。</p>
          <form class="mt-8 grid grid-cols-2 gap-x-6 gap-y-5 max-md:grid-cols-1" novalidate @submit.prevent="onSave">
            <div class="space-y-1.5">
              <Label for="p-nick" class="text-sm font-medium">昵称</Label>
              <input id="p-nick" v-model="form.nickName" class="field" maxlength="255" />
              <p v-if="errors.nickName" class="text-xs text-danger">{{ errors.nickName }}</p>
            </div>
            <div class="space-y-1.5">
              <Label for="p-phone" class="text-sm font-medium">电话</Label>
              <input id="p-phone" v-model="form.phone" class="field" inputmode="tel" autocomplete="tel" maxlength="255" />
              <p v-if="errors.phone" class="text-xs text-danger">{{ errors.phone }}</p>
            </div>
            <div class="col-span-2 space-y-1.5 max-md:col-span-1">
              <Label for="p-email" class="text-sm font-medium">邮箱</Label>
              <input id="p-email" v-model="form.email" type="email" class="field" autocomplete="email" maxlength="255" />
              <p v-if="errors.email" class="text-xs text-danger">{{ errors.email }}</p>
            </div>
            <div class="col-span-2 space-y-1.5 max-md:col-span-1">
              <Label for="p-bio" class="text-sm font-medium">简介</Label>
              <textarea id="p-bio" v-model="form.bio" rows="4" class="field resize-y" maxlength="255"></textarea>
              <p class="flex justify-between text-xs text-ink-mute">
                <span class="text-danger">{{ errors.bio }}</span><span class="font-mono">{{ form.bio.length }}/255</span>
              </p>
            </div>
            <div class="col-span-2 flex items-center justify-end gap-2 border-t border-line pt-5 max-md:col-span-1">
              <button type="button" class="btn btn-ghost" :disabled="saving" @click="fillForm">重置</button>
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <Icon v-if="saving" icon="lucide:loader-2" class="size-4 animate-spin" aria-hidden="true" />保存
              </button>
            </div>
          </form>
        </section>
      </div>
    </main>
  </div>
</template>
