/** 用户角色 */
export const USER_ROLE = {
  USER: 'user',
  ADMIN: 'admin',
} as const

export const USER_ROLE_OPTIONS = [
  { value: USER_ROLE.USER, label: '普通用户' },
  { value: USER_ROLE.ADMIN, label: '管理员' },
]

export const USER_ROLE_TEXT: Record<string, string> = {
  [USER_ROLE.USER]: '普通用户',
  [USER_ROLE.ADMIN]: '管理员',
}

/** 图片审核状态 */
export const PIC_REVIEW_STATUS = {
  REVIEWING: 0,
  PASS: 1,
  REJECT: 2,
} as const

export const PIC_REVIEW_STATUS_OPTIONS = [
  { value: PIC_REVIEW_STATUS.REVIEWING, label: '待审核' },
  { value: PIC_REVIEW_STATUS.PASS, label: '通过' },
  { value: PIC_REVIEW_STATUS.REJECT, label: '拒绝' },
]

export const PIC_REVIEW_STATUS_TEXT: Record<number, string> = {
  [PIC_REVIEW_STATUS.REVIEWING]: '待审核',
  [PIC_REVIEW_STATUS.PASS]: '通过',
  [PIC_REVIEW_STATUS.REJECT]: '拒绝',
}

export const PIC_REVIEW_STATUS_COLOR: Record<number, string> = {
  [PIC_REVIEW_STATUS.REVIEWING]: 'orange',
  [PIC_REVIEW_STATUS.PASS]: 'green',
  [PIC_REVIEW_STATUS.REJECT]: 'red',
}
