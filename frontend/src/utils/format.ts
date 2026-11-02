import dayjs from 'dayjs'

/** 后端时间格式 */
export const TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss'

/**
 * 格式化后端时间字符串。
 * @param value 后端返回的 yyyy-MM-dd HH:mm:ss 字符串
 * @param fallback 空值占位
 * @returns 格式化文本
 */
export function formatTime(value: string | null | undefined, fallback = '—'): string {
  if (!value) return fallback
  const d = dayjs(value)
  return d.isValid() ? d.format(TIME_FORMAT) : value
}
