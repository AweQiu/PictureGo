const AVATAR_COLORS = [
  '#1677ff',
  '#722ed1',
  '#eb2f96',
  '#f5222d',
  '#fa8c16',
  '#52c41a',
  '#13c2c2',
  '#2f54eb',
]

/** 根据名字生成首字母 SVG 头像（data URI，可离线使用） */
export function generateAvatar(name: string): string {
  let hash = 0
  for (const ch of name) {
    hash = (hash * 31 + (ch.codePointAt(0) ?? 0)) >>> 0
  }
  const color = AVATAR_COLORS[hash % AVATAR_COLORS.length]
  const initial = (name.trim().charAt(0) || 'U').toUpperCase()
  const svg =
    `<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96">` +
    `<rect width="96" height="96" rx="48" fill="${color}"/>` +
    `<text x="48" y="63" font-size="44" font-family="sans-serif" fill="#ffffff" text-anchor="middle">${initial}</text>` +
    `</svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}
