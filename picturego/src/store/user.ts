import { defineStore } from 'pinia'
import { getLoginUser, updateUser, userLogin, userLogout, userRegister } from '../api/user'
import { ApiError, NOT_LOGIN_CODE } from '../api/request'
import { USER_ROLE } from '../constants/enums'

export const useUserStore = defineStore('user', {
  state: () => ({
    loginUser: null as LoginUserVO | null,
    /** 是否已尝试过后端拉取登录态（避免路由守卫重复请求） */
    hasFetched: false,
  }),
  getters: {
    isLogin: (state): boolean => state.loginUser !== null,
    isAdmin(): boolean {
      return this.loginUser?.userRole === USER_ROLE.ADMIN
    },
    /** 展示名：用户名缺省时回退到账号 */
    displayName(): string {
      return this.loginUser?.userName || this.loginUser?.userAccount || ''
    },
  },
  actions: {
    /** 拉取当前登录用户（未登录时静默置空） */
    async fetchLoginUser() {
      try {
        this.loginUser = await getLoginUser()
      } catch (error) {
        if (!(error instanceof ApiError && error.code === NOT_LOGIN_CODE)) {
          console.error('获取登录用户失败', error)
        }
        this.loginUser = null
      } finally {
        this.hasFetched = true
      }
    },
    async login(payload: UserLoginRequest) {
      this.loginUser = await userLogin(payload)
      this.hasFetched = true
    },
    async register(payload: UserRegisterRequest) {
      await userRegister(payload)
    },
    async logout() {
      try {
        await userLogout()
      } catch (error) {
        // 会话可能已失效（后端重启/超时），此时仍按已退出处理
        console.warn('退出登录请求失败，按本地退出处理', error)
      } finally {
        this.loginUser = null
        this.hasFetched = true
      }
    },
    /** 修改个人信息（同步后端并更新本地登录态） */
    async updateProfile(payload: UserUpdateRequest) {
      const user = this.loginUser
      if (!user) {
        throw new ApiError(-1, '用户未登录')
      }
      await updateUser({ ...payload, id: user.id })
      this.loginUser = {
        ...user,
        userAccount: payload.userAccount ?? user.userAccount,
        userName: payload.userName ?? user.userName,
        userAvatar: payload.userAvatar ?? user.userAvatar,
        userProfile: payload.userProfile ?? user.userProfile,
      }
    },
  },
})
