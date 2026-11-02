<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElRadioButton,
  ElRadioGroup,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import { createUser, updateUser } from '@/api'
import type { UserStatus, UserVO } from '@/types'
import { notify } from '@/utils/notify'

/** 创建 / 编辑用户弹窗；user 为空时为创建模式 */
const props = defineProps<{ user: UserVO | null }>()
const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ saved: [] }>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const isEdit = computed(() => !!props.user)

const form = reactive({
  username: '',
  password: '',
  nickName: '',
  email: '',
  phone: '',
  bio: '',
  status: 1 as UserStatus,
})

const rules = computed<FormRules>(() => ({
  username: [
    { required: !isEdit.value, message: '请输入用户名', trigger: 'blur' },
    { max: 255, message: '不超过 255 字', trigger: 'blur' },
  ],
  password: [
    { required: !isEdit.value, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 255, message: '密码 6~255 位', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  nickName: [{ max: 255, message: '不超过 255 字', trigger: 'blur' }],
  phone: [{ max: 255, message: '不超过 255 字', trigger: 'blur' }],
  bio: [{ max: 255, message: '不超过 255 字', trigger: 'blur' }],
}))

// 打开时按模式重置表单
watch(visible, (v) => {
  if (!v) return
  const u = props.user
  form.username = u?.username ?? ''
  form.password = ''
  form.nickName = u?.nickName ?? ''
  form.email = u?.email ?? ''
  form.phone = u?.phone ?? ''
  form.bio = u?.bio ?? ''
  form.status = u?.status ?? 1
  formRef.value?.clearValidate()
})

/**
 * 去掉空字符串字段（后端对非空字段才更新）。
 * @param obj 原对象
 * @returns 精简后的对象
 */
function compact<T extends object>(obj: T): Partial<T> {
  return Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== '' && v !== null && v !== undefined)) as Partial<T>
}

/** 提交：创建或更新 */
async function onSubmit(): Promise<void> {
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  try {
    if (props.user) {
      const payload = compact({ ...form })
      // 用户名未变更时不提交，避免后端重复校验
      if (payload.username === props.user.username) delete payload.username
      await updateUser({ id: props.user.id, ...payload })
      notify('用户已更新', 'success')
    } else {
      const id = await createUser(compact({ ...form }) as typeof form)
      notify(`用户已创建（ID ${id}）`, 'success')
    }
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <ElDialog v-model="visible" :title="isEdit ? `编辑用户 · ${user?.username}` : '新增用户'" width="560px" destroy-on-close>
    <ElForm ref="formRef" :model="form" :rules="rules" label-width="72px" @submit.prevent>
      <ElFormItem label="用户名" prop="username">
        <ElInput v-model="form.username" maxlength="255" autocomplete="off" />
      </ElFormItem>
      <ElFormItem label="密码" prop="password">
        <ElInput
          v-model="form.password"
          type="password"
          show-password
          maxlength="255"
          autocomplete="new-password"
          :placeholder="isEdit ? '留空则不修改' : '6~255 位'"
        />
      </ElFormItem>
      <ElFormItem label="昵称" prop="nickName"><ElInput v-model="form.nickName" maxlength="255" /></ElFormItem>
      <ElFormItem label="邮箱" prop="email"><ElInput v-model="form.email" maxlength="255" /></ElFormItem>
      <ElFormItem label="电话" prop="phone"><ElInput v-model="form.phone" maxlength="255" /></ElFormItem>
      <ElFormItem label="简介" prop="bio">
        <ElInput v-model="form.bio" type="textarea" :rows="3" maxlength="255" show-word-limit />
      </ElFormItem>
      <ElFormItem label="状态">
        <ElRadioGroup v-model="form.status">
          <ElRadioButton :value="1">正常</ElRadioButton>
          <ElRadioButton :value="0">禁用</ElRadioButton>
        </ElRadioGroup>
      </ElFormItem>
    </ElForm>
    <template #footer>
      <div class="btn-row" style="justify-content: flex-end">
        <ElButton @click="visible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="onSubmit">保存</ElButton>
      </div>
    </template>
  </ElDialog>
</template>
