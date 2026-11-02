<script setup lang="ts">
import { Icon } from '@iconify/vue'
import logo from '@/assets/logo.png'
import BrandMark from '@/components/BrandMark.vue'

/**
 * 登录 / 注册共用的双栏外壳：左侧品牌区（移动端隐藏），右侧表单插槽。
 * @prop title 表单标题
 * @prop subtitle 表单副标题
 */
defineProps<{ title: string; subtitle: string }>()
</script>

<template>
  <main class="grid min-h-screen grid-cols-[1.1fr_1fr] max-md:grid-cols-1">
    <section class="flex flex-col justify-between border-r border-line bg-accent-soft px-14 py-12 max-md:hidden">
      <BrandMark :size="40" with-en />
      <div class="max-w-md">
        <img :src="logo" alt="" aria-hidden="true" width="168" height="168" class="rounded-full bg-card shadow-[0_10px_30px_-12px_rgba(12,80,192,0.45)]" />
        <p class="mt-10 font-display text-[38px] font-semibold leading-[1.3] text-ink">
          好物不贵，<br />人人都能逛。
        </p>
        <p class="mt-5 flex items-center gap-2 text-[15px] text-ink-soft">
          <Icon icon="lucide:heart" class="size-4 shrink-0 text-heart" aria-hidden="true" />
          欢迎来到大众商城
        </p>
      </div>
      <p class="text-xs tracking-[0.2em] text-ink-mute">DAZHONG MALL</p>
    </section>

    <section class="flex items-center justify-center px-6 py-12">
      <div class="w-full max-w-sm">
        <BrandMark class="mb-8 md:hidden" />
        <h1 class="font-display text-3xl font-semibold text-ink">{{ title }}</h1>
        <p class="mt-2 text-sm text-ink-soft">{{ subtitle }}</p>
        <div class="mt-8">
          <slot />
        </div>
      </div>
    </section>
  </main>
</template>
