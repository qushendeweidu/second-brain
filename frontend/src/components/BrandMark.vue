<script setup lang="ts">
import logo from '@/assets/logo.png'

/**
 * 大众商城品牌标识：圆形 Logo + 中文名（可选英文名）。
 * @prop size Logo 直径（px），默认 36
 * @prop withEn 是否显示英文名 DAZHONG MALL
 */
withDefaults(defineProps<{ size?: number; withEn?: boolean }>(), { size: 36, withEn: false })
</script>

<template>
  <div class="flex items-center gap-2.5">
    <img
      :src="logo"
      alt="大众商城"
      :width="size"
      :height="size"
      class="shrink-0 rounded-full bg-card object-cover"
    />
    <div class="leading-tight">
      <div class="font-display text-lg font-semibold tracking-wide text-ink">大众商城</div>
      <div v-if="withEn" class="text-[10px] font-medium tracking-[0.2em] text-accent">DAZHONG MALL</div>
    </div>
  </div>
</template>
