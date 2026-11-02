<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElAlert, ElButton, ElDialog, ElForm, ElFormItem, ElOption, ElSelect, vLoading } from 'element-plus'
import { getUserRole, updateUserRole } from '@/api'
import type { UserVO } from '@/types'
import { notify } from '@/utils/notify'

/** 角色权限编辑弹窗 */
const props = defineProps<{ user: UserVO | null }>()
const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ saved: [] }>()

/** 常用候选项（来自种子数据），select 允许输入自定义值 */
const ROLE_OPTIONS = ['ADMIN', 'USER', 'GUEST']
const PERMISSION_OPTIONS = ['user:read', 'user:add', 'user:update', 'user:delete', 'note:add', 'note:update', 'note:delete']

const roles = ref<string[]>([])
const permissions = ref<string[]>([])
const loading = ref(false)
const saving = ref(false)
const noRecord = ref(false)

// 打开时拉取最新角色权限
watch(visible, async (v) => {
  if (!v || !props.user) return
  loading.value = true
  noRecord.value = false
  try {
    const data = await getUserRole(props.user.id)
    roles.value = [...(data.roles ?? [])]
    permissions.value = [...(data.permissions ?? [])]
  } catch {
    // 40104：该用户暂无角色记录
    noRecord.value = true
    roles.value = []
    permissions.value = []
  } finally {
    loading.value = false
  }
})

/** 保存角色权限（后端会强制该用户重新登录） */
async function onSubmit(): Promise<void> {
  if (!props.user) return
  if (!roles.value.length && !permissions.value.length) {
    notify('角色与权限不能同时为空', 'error')
    return
  }
  saving.value = true
  try {
    await updateUserRole({
      userId: props.user.id,
      roles: roles.value.length ? roles.value : undefined,
      permissions: permissions.value.length ? permissions.value : undefined,
    })
    notify('权限已更新，该用户需要重新登录', 'success')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <ElDialog v-model="visible" :title="`角色权限 · ${user?.username ?? ''}`" width="520px">
    <div v-loading="loading">
      <ElAlert
        v-if="noRecord"
        type="warning"
        :closable="false"
        show-icon
        title="该用户暂无角色记录"
        style="margin-bottom: 16px"
      />
      <ElForm label-width="56px" @submit.prevent>
        <ElFormItem label="角色">
          <ElSelect v-model="roles" multiple filterable allow-create default-first-option placeholder="选择或输入角色" style="width: 100%">
            <ElOption v-for="r in ROLE_OPTIONS" :key="r" :label="r" :value="r" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="权限">
          <ElSelect v-model="permissions" multiple filterable allow-create default-first-option placeholder="选择或输入权限" style="width: 100%">
            <ElOption v-for="p in PERMISSION_OPTIONS" :key="p" :label="p" :value="p" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <ElAlert type="info" :closable="false" title="保存后该用户的登录态将被清除，需重新登录才能获得新权限。" />
    </div>
    <template #footer>
      <div class="btn-row" style="justify-content: flex-end">
        <ElButton @click="visible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" :disabled="loading" @click="onSubmit">保存</ElButton>
      </div>
    </template>
  </ElDialog>
</template>
