import { reactive } from 'vue'

/** 提示类型 */
export type NoticeType = 'success' | 'error' | 'info'

/** 单条提示 */
export interface Notice {
  id: number
  type: NoticeType
  text: string
}

/** 全局提示队列（由 App.vue 中的 NoticeHost 渲染），与具体 UI 库解耦，请求层与两端页面共用 */
export const notices = reactive<Notice[]>([])

let seq = 0

/**
 * 推送一条全局提示，3.5 秒后自动移除；同文案 1 秒内不重复弹出。
 * @param text 提示文案
 * @param type 提示类型，默认 info
 */
export function notify(text: string, type: NoticeType = 'info'): void {
  if (notices.some((n) => n.text === text)) return
  const id = ++seq
  notices.push({ id, type, text })
  setTimeout(() => dismiss(id), 3500)
}

/**
 * 移除指定提示。
 * @param id 提示 id
 */
export function dismiss(id: number): void {
  const idx = notices.findIndex((n) => n.id === id)
  if (idx >= 0) notices.splice(idx, 1)
}
