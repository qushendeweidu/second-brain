<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElAvatar, ElDescriptions, ElDescriptionsItem, ElDrawer, ElEmpty, ElTag, vLoading } from 'element-plus'
import { getUser } from '@/api'
import type { UserVO } from '@/types'
import { fileUrl } from '@/utils/request'
import { formatTime } from '@/utils/format'

/** 用户详情抽屉，打开时按 id 拉取最新数据 */
const props = defineProps<{ userId: number | null }>()
const visible = defineModel<boolean>({ required: true })

const detail = ref<UserVO | null>(null)
const loading = ref(false)

watch(visible, async (v) => {
  if (!v || !props.userId) return
  loading.value = true
  detail.value = null
  try {
    detail.value = await getUser(props.userId)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <ElDrawer v-model="visible" title="用户详情" size="480px">
    <div v-loading="loading" style="min-height: 200px">
      <template v-if="detail">
        <div class="detail-head">
          <ElAvatar :size="56" :src="fileUrl(detail.avatar) || undefined">{{ (detail.nickName || detail.username).slice(0, 1) }}</ElAvatar>
          <div>
            <div class="detail-name">{{ detail.nickName || detail.username }}</div>
            <div class="detail-sub">@{{ detail.username }} · ID {{ detail.id }}</div>
          </div>
        </div>
        <ElDescriptions :column="1" border>
          <ElDescriptionsItem label="状态">
            <ElTag :type="detail.status === 1 ? 'success' : 'danger'">{{ detail.status === 1 ? '正常' : '禁用' }}</ElTag>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="邮箱">{{ detail.email || '—' }}</ElDescriptionsItem>
          <ElDescriptionsItem label="电话">{{ detail.phone || '—' }}</ElDescriptionsItem>
          <ElDescriptionsItem label="简介">{{ detail.bio || '—' }}</ElDescriptionsItem>
          <ElDescriptionsItem label="角色">
            <div class="tag-wrap">
              <ElTag v-for="r in detail.roles ?? []" :key="r" type="warning">{{ r }}</ElTag>
              <span v-if="!detail.roles?.length">—</span>
            </div>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="权限">
            <div class="tag-wrap">
              <ElTag v-for="p in detail.permissions ?? []" :key="p" type="info">{{ p }}</ElTag>
              <span v-if="!detail.permissions?.length">—</span>
            </div>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="用户创建">{{ formatTime(detail.userCreateTime) }}</ElDescriptionsItem>
          <ElDescriptionsItem label="用户更新">{{ formatTime(detail.userUpdateTime) }}</ElDescriptionsItem>
          <ElDescriptionsItem label="角色创建">{{ formatTime(detail.roleCreateTime) }}</ElDescriptionsItem>
          <ElDescriptionsItem label="角色更新">{{ formatTime(detail.roleUpdateTime) }}</ElDescriptionsItem>
        </ElDescriptions>
      </template>
      <ElEmpty v-else-if="!loading" description="未获取到用户信息" />
    </div>
  </ElDrawer>
</template>

<style scoped>
.detail-head { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.detail-name { font-family: var(--font-display); font-size: 18px; color: #14213d; }
.detail-sub { font-family: var(--font-mono); font-size: 12px; color: #7d8aa3; margin-top: 2px; }
.tag-wrap { display: flex; flex-wrap: wrap; gap: 6px; }
</style>
