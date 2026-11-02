<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInputNumber,
  ElOption,
  ElSelect,
  ElSwitch,
  ElTabPane,
  ElTabs,
} from 'element-plus'
import { blockUser, deleteUserSecurity, restrictUser } from '@/api'
import type { TimeUnit, UserVO } from '@/types'
import { notify } from '@/utils/notify'

/** 账户管控弹窗：封禁 / 限流 / 强制下线 */
const props = defineProps<{ user: UserVO | null }>()
const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ done: [] }>()

const UNIT_OPTIONS: { label: string; value: TimeUnit }[] = [
  { label: '秒', value: 'SECONDS' },
  { label: '分钟', value: 'MINUTES' },
  { label: '小时', value: 'HOURS' },
  { label: '天', value: 'DAYS' },
]

const tab = ref<'block' | 'restrict' | 'kick'>('block')
const busy = ref(false)
const block = reactive({ blockedTime: 30, timeUnit: 'MINUTES' as TimeUnit, blocked: false })
const restrict = reactive({ restrictId: 1, timeUnit: 'SECONDS' as TimeUnit })

watch(visible, (v) => {
  if (!v) return
  tab.value = 'block'
  Object.assign(block, { blockedTime: 30, timeUnit: 'MINUTES', blocked: false })
  Object.assign(restrict, { restrictId: 1, timeUnit: 'SECONDS' })
})

/**
 * 执行异步操作并统一处理 loading / 成功提示。
 * @param fn 实际调用
 * @param ok 成功文案
 */
async function run(fn: () => Promise<unknown>, ok: string): Promise<void> {
  busy.value = true
  try {
    await fn()
    notify(ok, 'success')
    visible.value = false
    emit('done')
  } finally {
    busy.value = false
  }
}

/** 提交封禁 */
function onBlock(): Promise<void> {
  const u = props.user!
  return run(
    () => blockUser({ userId: u.id, ...block }),
    block.blocked ? '已永久封禁该用户' : '已临时封禁该用户',
  )
}

/** 提交限流 */
function onRestrict(): Promise<void> {
  const u = props.user!
  return run(() => restrictUser(u.id, restrict.restrictId, restrict.timeUnit), '已对该用户启用限流')
}

/** 强制下线 / 解除临时封禁 */
function onKick(): Promise<void> {
  const u = props.user!
  return run(() => deleteUserSecurity(u.id), '已清除该用户登录态')
}
</script>

<template>
  <ElDialog v-model="visible" :title="`账户管控 · ${user?.username ?? ''}`" width="520px">
    <ElTabs v-model="tab">
      <ElTabPane label="封禁" name="block">
        <ElForm label-width="84px" @submit.prevent>
          <ElFormItem label="封禁时长">
            <div class="btn-row" style="width: 100%">
              <ElInputNumber v-model="block.blockedTime" :min="0" :step="1" step-strictly controls-position="right" />
              <ElSelect v-model="block.timeUnit" style="width: 110px">
                <ElOption v-for="o in UNIT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </ElSelect>
            </div>
          </ElFormItem>
          <ElFormItem label="永久封禁">
            <ElSwitch v-model="block.blocked" />
          </ElFormItem>
        </ElForm>
        <ElAlert
          :type="block.blocked ? 'error' : 'info'"
          :closable="false"
          :title="block.blocked
            ? '永久封禁会把用户状态置为禁用并清除登录态；解封需在编辑中把状态改回“正常”，再执行“强制下线”清理残留。'
            : '临时封禁到期自动解除；期间该用户的所有请求将被拒绝。'"
        />
        <div class="btn-row" style="justify-content: flex-end; margin-top: 20px">
          <ElButton type="danger" :loading="busy" @click="onBlock">确认封禁</ElButton>
        </div>
      </ElTabPane>

      <ElTabPane label="限流" name="restrict">
        <ElForm label-width="84px" @submit.prevent>
          <ElFormItem label="限流配置">
            <ElInputNumber v-model="restrict.restrictId" :min="1" :step="1" step-strictly controls-position="right" />
          </ElFormItem>
          <ElFormItem label="时间单位">
            <ElSelect v-model="restrict.timeUnit" style="width: 140px">
              <ElOption v-for="o in UNIT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
            </ElSelect>
          </ElFormItem>
        </ElForm>
        <ElAlert type="info" :closable="false" title="配置 ID 对应 user_restrict 表，默认 1 号配置为 100 次 / 60 秒。" />
        <div class="btn-row" style="justify-content: flex-end; margin-top: 20px">
          <ElButton type="warning" :loading="busy" @click="onRestrict">启用限流</ElButton>
        </div>
      </ElTabPane>

      <ElTabPane label="强制下线" name="kick">
        <ElAlert
          type="warning"
          :closable="false"
          title="清除该用户的登录态，已签发的 token 立即失效。也可用于提前解除临时封禁。"
        />
        <div class="btn-row" style="justify-content: flex-end; margin-top: 20px">
          <ElButton type="warning" :loading="busy" @click="onKick">清除登录态</ElButton>
        </div>
      </ElTabPane>
    </ElTabs>
  </ElDialog>
</template>
