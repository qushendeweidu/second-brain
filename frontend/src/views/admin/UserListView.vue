<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElAvatar,
  ElButton,
  ElCard,
  ElDatePicker,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
  vLoading,
} from 'element-plus'
import { ArrowDown, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { deleteUser, deleteUserRole, listUsers } from '@/api'
import type { UserQueryDTO, UserStatus, UserVO } from '@/types'
import { fileUrl } from '@/utils/request'
import { formatTime } from '@/utils/format'
import { notify } from '@/utils/notify'
import UserFormDialog from './components/UserFormDialog.vue'
import RoleDialog from './components/RoleDialog.vue'
import ControlDialog from './components/ControlDialog.vue'
import UserDetailDrawer from './components/UserDetailDrawer.vue'

/** 筛选表单（空值不提交） */
const filters = reactive({
  username: '',
  nickName: '',
  email: '',
  phone: '',
  bio: '',
  status: undefined as UserStatus | undefined,
  roles: [] as string[],
  permissions: [] as string[],
  createRange: null as [string, string] | null,
  updateRange: null as [string, string] | null,
})
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const rows = ref<UserVO[]>([])
const loading = ref(false)

const ROLE_OPTIONS = ['ADMIN', 'USER', 'GUEST']
const PERMISSION_OPTIONS = ['user:read', 'user:add', 'user:update', 'user:delete', 'note:add', 'note:update', 'note:delete']

// 弹窗状态
const current = ref<UserVO | null>(null)
const formOpen = ref(false)
const roleOpen = ref(false)
const controlOpen = ref(false)
const detailOpen = ref(false)

/**
 * 组装查询 DTO。
 * @returns UserQueryDTO
 */
function buildQuery(): UserQueryDTO {
  const q: UserQueryDTO = { pageDTO: { pageNum: page.pageNum, pageSize: page.pageSize } }
  ;(['username', 'nickName', 'email', 'phone', 'bio'] as const).forEach((k) => {
    const v = filters[k].trim()
    if (v) q[k] = v
  })
  if (filters.status !== undefined) q.status = filters.status
  if (filters.roles.length) q.roles = filters.roles
  if (filters.permissions.length) q.permissions = filters.permissions
  if (filters.createRange) [q.userCreateStartTime, q.userCreateEndTime] = filters.createRange
  if (filters.updateRange) [q.userUpdateStartTime, q.userUpdateEndTime] = filters.updateRange
  return q
}

/** 拉取列表 */
async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await listUsers(buildQuery())
    rows.value = res.records
    page.total = res.total
  } finally {
    loading.value = false
  }
}

/** 查询（回到第一页） */
function onSearch(): void {
  page.pageNum = 1
  load()
}

/** 重置筛选 */
function onReset(): void {
  Object.assign(filters, {
    username: '', nickName: '', email: '', phone: '', bio: '',
    status: undefined, roles: [], permissions: [], createRange: null, updateRange: null,
  })
  onSearch()
}

/**
 * 打开弹窗。
 * @param kind 弹窗类型
 * @param row 目标用户；新增时为 null
 */
function open(kind: 'form' | 'role' | 'control' | 'detail', row: UserVO | null): void {
  current.value = row
  if (kind === 'form') formOpen.value = true
  if (kind === 'role') roleOpen.value = true
  if (kind === 'control') controlOpen.value = true
  if (kind === 'detail') detailOpen.value = true
}

/**
 * 删除用户（二次确认）。
 * @param row 目标用户
 */
async function onDelete(row: UserVO): Promise<void> {
  const ok = await ElMessageBox.confirm(
    `将删除用户「${row.username}」及其角色、资料与登录态，操作不可恢复。`,
    '删除用户',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
  ).catch(() => false)
  if (!ok) return
  await deleteUser(row.id)
  notify('用户已删除', 'success')
  if (rows.value.length === 1 && page.pageNum > 1) page.pageNum--
  load()
}

/**
 * 删除角色权限（二次确认）。
 * @param row 目标用户
 */
async function onDeleteRole(row: UserVO): Promise<void> {
  const ok = await ElMessageBox.confirm(
    `删除后「${row.username}」将没有任何角色与权限，并被强制下线。`,
    '删除角色权限',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' },
  ).catch(() => false)
  if (!ok) return
  await deleteUserRole(row.id)
  notify('角色权限已删除', 'success')
  load()
}

/**
 * 处理"更多"下拉命令。
 * @param cmd 命令
 * @param row 目标用户
 */
function onCommand(cmd: string, row: UserVO): void {
  if (cmd === 'role') open('role', row)
  else if (cmd === 'control') open('control', row)
  else if (cmd === 'deleteRole') onDeleteRole(row)
  else if (cmd === 'delete') onDelete(row)
}

onMounted(load)
</script>

<template>
  <div class="user-list">
    <ElCard shadow="never" class="filter-card">
      <ElForm :model="filters" inline label-width="64px" class="filter-form" @submit.prevent="onSearch">
        <ElFormItem label="用户名"><ElInput v-model="filters.username" clearable placeholder="精确匹配" /></ElFormItem>
        <ElFormItem label="昵称"><ElInput v-model="filters.nickName" clearable placeholder="精确匹配" /></ElFormItem>
        <ElFormItem label="邮箱"><ElInput v-model="filters.email" clearable placeholder="精确匹配" /></ElFormItem>
        <ElFormItem label="电话"><ElInput v-model="filters.phone" clearable placeholder="精确匹配" /></ElFormItem>
        <ElFormItem label="简介"><ElInput v-model="filters.bio" clearable placeholder="模糊匹配" /></ElFormItem>
        <ElFormItem label="状态">
          <ElSelect v-model="filters.status" clearable placeholder="全部">
            <ElOption label="正常" :value="1" />
            <ElOption label="禁用" :value="0" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="角色">
          <ElSelect v-model="filters.roles" multiple collapse-tags filterable allow-create clearable placeholder="任一命中">
            <ElOption v-for="r in ROLE_OPTIONS" :key="r" :label="r" :value="r" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="权限">
          <ElSelect v-model="filters.permissions" multiple collapse-tags filterable allow-create clearable placeholder="任一命中">
            <ElOption v-for="p in PERMISSION_OPTIONS" :key="p" :label="p" :value="p" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="创建时间" class="range-item">
          <ElDatePicker
            v-model="filters.createRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始"
            end-placeholder="结束"
            :default-time="[new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]"
          />
        </ElFormItem>
        <ElFormItem label="更新时间" class="range-item">
          <ElDatePicker
            v-model="filters.updateRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始"
            end-placeholder="结束"
            :default-time="[new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]"
          />
        </ElFormItem>
        <ElFormItem class="action-item">
          <div class="btn-row">
            <ElButton type="primary" :icon="Search" native-type="submit">查询</ElButton>
            <ElButton :icon="Refresh" @click="onReset">重置</ElButton>
          </div>
        </ElFormItem>
      </ElForm>
    </ElCard>

    <ElCard shadow="never" class="table-card">
      <div class="toolbar">
        <span class="toolbar-title">共 {{ page.total }} 位用户</span>
        <div class="btn-row">
          <ElButton v-permission="'user:add'" type="primary" :icon="Plus" @click="open('form', null)">新增用户</ElButton>
        </div>
      </div>

      <div class="paginated-table">
        <ElTable v-loading="loading" :data="rows" row-key="id" stripe>
          <ElTableColumn prop="id" label="ID" width="72" />
          <ElTableColumn label="用户" min-width="200">
            <template #default="{ row }">
              <div class="user-cell">
                <ElAvatar :size="32" :src="fileUrl(row.avatar) || undefined">{{ (row.nickName || row.username).slice(0, 1) }}</ElAvatar>
                <div class="user-cell-text">
                  <div class="name">{{ row.nickName || '—' }}</div>
                  <div class="sub">@{{ row.username }}</div>
                </div>
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="联系方式" min-width="200">
            <template #default="{ row }">
              <div class="contact">{{ row.email || '—' }}</div>
              <div class="contact sub">{{ row.phone || '—' }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="84">
            <template #default="{ row }">
              <ElTag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="角色" min-width="150">
            <template #default="{ row }">
              <div class="tag-wrap">
                <ElTag v-for="r in row.roles ?? []" :key="r" size="small" type="warning">{{ r }}</ElTag>
                <span v-if="!row.roles?.length" class="sub">—</span>
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="创建时间" width="170">
            <template #default="{ row }"><span class="mono">{{ formatTime(row.userCreateTime) }}</span></template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="op-cell">
                <ElButton link type="primary" @click="open('detail', row as UserVO)">详情</ElButton>
                <ElButton v-permission="'user:update'" link type="primary" @click="open('form', row as UserVO)">编辑</ElButton>
                <ElDropdown trigger="click" @command="(cmd: string) => onCommand(cmd, row as UserVO)">
                  <ElButton link type="primary">更多<ArrowDown class="dd-icon" /></ElButton>
                  <template #dropdown>
                    <ElDropdownMenu>
                      <ElDropdownItem command="role">角色权限</ElDropdownItem>
                      <ElDropdownItem command="control">封禁 / 限流 / 下线</ElDropdownItem>
                      <ElDropdownItem command="deleteRole" divided>删除角色权限</ElDropdownItem>
                      <ElDropdownItem command="delete" class="danger-item">删除用户</ElDropdownItem>
                    </ElDropdownMenu>
                  </template>
                </ElDropdown>
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>

      <div class="paginated-footer">
        <ElPagination
          v-model:current-page="page.pageNum"
          v-model:page-size="page.pageSize"
          :total="page.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="load"
          @size-change="onSearch"
        />
      </div>
    </ElCard>

    <UserFormDialog v-model="formOpen" :user="current" @saved="load" />
    <RoleDialog v-model="roleOpen" :user="current" @saved="load" />
    <ControlDialog v-model="controlOpen" :user="current" @done="load" />
    <UserDetailDrawer v-model="detailOpen" :user-id="current?.id ?? null" />
  </div>
</template>

<style scoped>
.user-list { display: flex; flex-direction: column; gap: 16px; }
.filter-card, .table-card { border-color: #dbe3ef; background: #fbfcff; }
.filter-form :deep(.el-form-item) { margin-right: 16px; margin-bottom: 14px; }
.filter-form :deep(.el-input), .filter-form :deep(.el-select) { width: 180px; }
.filter-form .range-item :deep(.el-date-editor) { width: 360px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; gap: 12px; }
.toolbar-title { font-size: 14px; color: #4a5670; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-cell-text { min-width: 0; }
.name { color: #14213d; font-weight: 500; }
.sub { color: #7d8aa3; font-size: 12px; }
.contact { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mono { font-family: var(--font-mono); font-size: 12px; }
.tag-wrap { display: flex; flex-wrap: wrap; gap: 4px; }
.op-cell { display: flex; align-items: center; gap: 12px; }
.op-cell .el-button { margin: 0 !important; }
.dd-icon { width: 12px; height: 12px; margin-left: 2px; }
.paginated-footer { display: flex; justify-content: flex-end; margin-top: 16px; }
:global(.danger-item) { color: #b42318 !important; }

@media (max-width: 767px) {
  .filter-form :deep(.el-form-item) { width: 100%; margin-right: 0; }
  .filter-form :deep(.el-input), .filter-form :deep(.el-select),
  .filter-form .range-item :deep(.el-date-editor) { width: 100%; }
  .toolbar { flex-direction: column; align-items: stretch; }
  .paginated-table :deep(.el-table__header),
  .paginated-table :deep(.el-table__body) { min-width: 900px !important; width: auto !important; }
  .paginated-table :deep(.el-table__body-wrapper) { overflow-x: auto !important; }
  .paginated-table :deep(.cell) { white-space: nowrap !important; }
  .paginated-footer { flex-wrap: wrap; justify-content: flex-start; overflow-x: auto; }
}
</style>
