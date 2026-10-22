const MONTHS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
]

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

/**
 * 统一时间展示为 yyyy-MM-dd HH:mm:ss。
 * 兼容后端两种返回格式：
 * - ISO 字符串：2026-10-17T19:43:34.000+00:00
 * - Java Date.toString()：Thu Oct 22 14:09:53 HKT 2026（按本地时间还原）
 */
export function formatDateTime(value?: string | number | Date | null): string {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  let date: Date
  if (value instanceof Date) {
    date = value
  } else if (typeof value === 'number') {
    date = new Date(value)
  } else {
    const javaDate = value.match(
      /^\w{3} (\w{3}) (\d{1,2}) (\d{2}):(\d{2}):(\d{2}) \S+ (\d{4})$/,
    )
    if (javaDate) {
      const month = MONTHS.indexOf(javaDate[1])
      if (month === -1) {
        return value
      }
      date = new Date(
        Number(javaDate[6]),
        month,
        Number(javaDate[2]),
        Number(javaDate[3]),
        Number(javaDate[4]),
        Number(javaDate[5]),
      )
    } else {
      date = new Date(value)
    }
  }
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}
