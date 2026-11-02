<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { dismiss, notices } from '@/utils/notify'

/** 提示类型对应的图标与色彩 */
const style = {
  success: { icon: 'lucide:check-circle-2', cls: 'text-ok' },
  error: { icon: 'lucide:alert-circle', cls: 'text-danger' },
  info: { icon: 'lucide:info', cls: 'text-ink-soft' },
} as const
</script>

<template>
  <!-- 顶部居中的提示栈，pointer-events 仅作用于提示本身，不遮挡页面点击 -->
  <div
    class="pointer-events-none fixed inset-x-0 top-4 z-[3000] flex flex-col items-center gap-2 px-4"
    role="status"
    aria-live="polite"
  >
    <TransitionGroup
      enter-from-class="opacity-0 -translate-y-2"
      leave-to-class="opacity-0"
      enter-active-class="transition duration-200"
      leave-active-class="transition duration-150"
    >
      <div
        v-for="n in notices"
        :key="n.id"
        class="pointer-events-auto flex max-w-md items-center gap-2.5 rounded-lg border border-line bg-card px-4 py-2.5 text-sm text-ink shadow-[0_6px_24px_-8px_rgba(31,29,26,0.25)]"
      >
        <Icon :icon="style[n.type].icon" :class="['size-4 shrink-0', style[n.type].cls]" aria-hidden="true" />
        <span class="min-w-0 flex-1">{{ n.text }}</span>
        <button
          type="button"
          class="shrink-0 rounded p-0.5 text-ink-mute hover:text-ink focus-visible:ring-2 focus-visible:ring-accent/40 outline-none"
          aria-label="关闭提示"
          @click="dismiss(n.id)"
        >
          <Icon icon="lucide:x" class="size-3.5" aria-hidden="true" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>
