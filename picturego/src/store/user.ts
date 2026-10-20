import { defineStore } from 'pinia'
import { generateAvatar } from '../utils/avatar'

export interface UserRecord {
  id: number
  account: string
  password: string
  username: string
  avatar: string
  bio: string
  role: string
  createdAt: string
  updatedAt: string
}

export interface ProfilePayload {
  account: string
  username: string
  avatar: string
  bio: string
}

export interface EditUserPayload extends ProfilePayload {
  role: string
}

export const ROLE_OPTIONS = ['超级管理员', 'VIP用户', '普通用户']

const USERS_KEY = 'picgo_users'
const CURRENT_ID_KEY = 'picgo_current_user_id'

function now(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}

function makeUser(partial: Partial<UserRecord> & { account: string }): UserRecord {
  const time = now()
  return {
    id: partial.id ?? 0,
    account: partial.account,
    password: partial.password ?? '123456',
    username: partial.username ?? partial.account,
    avatar: partial.avatar || generateAvatar(partial.username ?? partial.account),
    bio: partial.bio ?? '',
    role: partial.role ?? '普通用户',
    createdAt: partial.createdAt ?? time,
    updatedAt: partial.updatedAt ?? time,
  }
}

function seedUsers(): UserRecord[] {
  return [
    makeUser({
      id: 1,
      account: 'admin',
      username: '管理员',
      role: '超级管理员',
      bio: '系统内置管理员账号',
      createdAt: '2026-01-01 10:00:00',
      updatedAt: '2026-01-01 10:00:00',
    }),
    makeUser({
      id: 2,
      account: 'zhangsan',
      username: '张三',
      bio: '风光摄影爱好者',
      createdAt: '2026-03-15 14:30:00',
      updatedAt: '2026-04-02 09:10:00',
    }),
    makeUser({
      id: 3,
      account: 'lisi',
      username: '李四',
      role: 'VIP用户',
      bio: '人像摄影师',
      createdAt: '2026-05-20 09:12:00',
      updatedAt: '2026-06-11 18:40:00',
    }),
    makeUser({
      id: 4,
      account: 'wangwu',
      username: '王五',
      bio: '记录生活的随手拍',
      createdAt: '2026-07-08 16:45:00',
      updatedAt: '2026-07-08 16:45:00',
    }),
    makeUser({
      id: 5,
      account: 'zhaoliu',
      username: '赵六',
      role: 'VIP用户',
      bio: '旅行与街拍',
      createdAt: '2026-09-01 11:20:00',
      updatedAt: '2026-09-28 08:05:00',
    }),
  ]
}

function loadUsers(): UserRecord[] {
  try {
    const raw = localStorage.getItem(USERS_KEY)
    if (raw) {
      const list = JSON.parse(raw) as UserRecord[]
      if (Array.isArray(list) && list.length > 0) {
        return list
      }
    }
  } catch {
    // 数据损坏时回退到演示数据
  }
  const seeded = seedUsers()
  localStorage.setItem(USERS_KEY, JSON.stringify(seeded))
  return seeded
}

function loadCurrentId(users: UserRecord[]): number | null {
  const raw = localStorage.getItem(CURRENT_ID_KEY)
  if (!raw) return null
  const id = Number(raw)
  return users.some((user) => user.id === id) ? id : null
}

export const useUserStore = defineStore('user', {
  state: () => {
    const users = loadUsers()
    return {
      users,
      currentUserId: loadCurrentId(users) as number | null,
    }
  },
  getters: {
    currentUser: (state): UserRecord | null =>
      state.users.find((user) => user.id === state.currentUserId) ?? null,
    isLogin: (state): boolean =>
      state.users.some((user) => user.id === state.currentUserId),
  },
  actions: {
    persist() {
      localStorage.setItem(USERS_KEY, JSON.stringify(this.users))
      if (this.currentUserId === null) {
        localStorage.removeItem(CURRENT_ID_KEY)
      } else {
        localStorage.setItem(CURRENT_ID_KEY, String(this.currentUserId))
      }
    },
    nextId(): number {
      return this.users.reduce((max, user) => Math.max(max, user.id), 0) + 1
    },
    hasAccount(account: string, excludeId?: number): boolean {
      return this.users.some(
        (user) => user.account === account && user.id !== excludeId,
      )
    },
    login(account: string, password: string): string | null {
      const matched = this.users.find(
        (user) => user.account === account && user.password === password,
      )
      if (!matched) {
        return '账号或密码错误'
      }
      this.currentUserId = matched.id
      this.persist()
      return null
    },
    register(account: string, password: string): string | null {
      if (this.hasAccount(account)) {
        return '该账号已被注册'
      }
      this.users.push(makeUser({ id: this.nextId(), account, password }))
      this.persist()
      return null
    },
    logout() {
      this.currentUserId = null
      this.persist()
    },
    /** 修改个人信息（账号、用户名、头像、简介） */
    updateProfile(payload: ProfilePayload): string | null {
      const user = this.currentUser
      if (!user) {
        return '用户不存在'
      }
      if (this.hasAccount(payload.account, user.id)) {
        return '该账号已被占用'
      }
      Object.assign(user, payload, { updatedAt: now() })
      this.persist()
      return null
    },
    /** 用户管理页新增用户 */
    addUser(payload: EditUserPayload): string | null {
      if (this.hasAccount(payload.account)) {
        return '该账号已被占用'
      }
      const username = payload.username || payload.account
      this.users.push(
        makeUser({
          id: this.nextId(),
          account: payload.account,
          username,
          avatar: payload.avatar,
          bio: payload.bio,
          role: payload.role,
        }),
      )
      this.persist()
      return null
    },
    /** 用户管理页编辑用户 */
    updateUser(id: number, payload: EditUserPayload): string | null {
      const user = this.users.find((item) => item.id === id)
      if (!user) {
        return '用户不存在'
      }
      if (this.hasAccount(payload.account, id)) {
        return '该账号已被占用'
      }
      Object.assign(user, payload, { updatedAt: now() })
      this.persist()
      return null
    },
    removeUser(id: number): string | null {
      if (id === this.currentUserId) {
        return '不能删除当前登录用户'
      }
      const index = this.users.findIndex((user) => user.id === id)
      if (index === -1) {
        return '用户不存在'
      }
      this.users.splice(index, 1)
      this.persist()
      return null
    },
    verifyPassword(password: string): boolean {
      return this.currentUser?.password === password
    },
    changePassword(newPassword: string): string | null {
      const user = this.currentUser
      if (!user) {
        return '用户不存在'
      }
      user.password = newPassword
      user.updatedAt = now()
      this.persist()
      return null
    },
  },
})
