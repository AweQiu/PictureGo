<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  DownOutlined,
  HomeOutlined,
  KeyOutlined,
  LogoutOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { useUserStore, type ProfilePayload } from '../store/user'
import AvatarUpload from '../components/AvatarUpload.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const selectedKeys = computed(() => [route.name as string])

const menuItems = [
  { key: 'home', label: '首页', icon: () => h(HomeOutlined) },
  { key: 'userManage', label: '用户管理', icon: () => h(TeamOutlined) },
]

function handleMenuClick({ key }: { key: string }) {
  if (key !== route.name) {
    router.push({ name: key })
  }
}

// ---------- 个人信息 ----------
const profileVisible = ref(false)
const profileLoading = ref(false)
const profileForm = reactive<ProfilePayload>({
  account: '',
  username: '',
  avatar: '',
  bio: '',
})

function openProfile() {
  const user = userStore.currentUser
  if (!user) return
  Object.assign(profileForm, {
    account: user.account,
    username: user.username,
    avatar: user.avatar,
    bio: user.bio,
  })
  profileVisible.value = true
}

function handleProfileSubmit() {
  if (!profileForm.account.trim() || !profileForm.username.trim()) {
    message.warning('账号和用户名不能为空')
    return
  }
  profileLoading.value = true
  const error = userStore.updateProfile({
    account: profileForm.account.trim(),
    username: profileForm.username.trim(),
    avatar: profileForm.avatar,
    bio: profileForm.bio.trim(),
  })
  profileLoading.value = false
  if (error) {
    message.error(error)
    return
  }
  message.success('个人信息已更新')
  profileVisible.value = false
}

// ---------- 修改密码 ----------
const passwordVisible = ref(false)
const passwordLoading = ref(false)
const passwordVerified = ref(false)
const passwordForm = reactive({
  current: '',
  next: '',
  confirm: '',
})

function resetPasswordModal() {
  passwordVerified.value = false
  passwordForm.current = ''
  passwordForm.next = ''
  passwordForm.confirm = ''
}

function openChangePassword() {
  resetPasswordModal()
  passwordVisible.value = true
}

function handleVerifyCurrent() {
  if (!passwordForm.current) {
    message.warning('请输入当前密码')
    return
  }
  if (!userStore.verifyPassword(passwordForm.current)) {
    message.error('当前密码错误')
    return
  }
  passwordVerified.value = true
}

function handlePasswordSubmit() {
  if (!passwordForm.next) {
    message.warning('请输入新密码')
    return
  }
  if (passwordForm.next === passwordForm.current) {
    message.warning('新密码不能与当前密码相同')
    return
  }
  if (passwordForm.next !== passwordForm.confirm) {
    message.warning('两次输入的新密码不一致')
    return
  }
  passwordLoading.value = true
  const error = userStore.changePassword(passwordForm.next)
  passwordLoading.value = false
  if (error) {
    message.error(error)
    return
  }
  message.success('密码修改成功')
  passwordVisible.value = false
}

// ---------- 用户下拉菜单 ----------
function handleUserMenuClick({ key }: { key: string }) {
  if (key === 'profile') {
    openProfile()
  } else if (key === 'changePassword') {
    openChangePassword()
  } else if (key === 'logout') {
    Modal.confirm({
      title: '退出登录',
      content: '确定要退出登录吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => {
        userStore.logout()
        message.success('已退出登录')
        router.replace({ name: 'login' })
      },
    })
  }
}
</script>

<template>
  <a-layout class="basic-layout">
    <a-layout-header class="header">
      <div class="header-left" @click="router.push({ name: 'home' })">
        <img src="/picgo.png" alt="logo" class="logo" />
        <span class="title">PicGo云图库</span>
      </div>
      <a-menu
        class="header-menu"
        mode="horizontal"
        :selected-keys="selectedKeys"
        :items="menuItems"
        @click="handleMenuClick"
      />
      <a-dropdown placement="bottomRight" :trigger="['click']">
        <div class="user-info">
          <a-avatar :src="userStore.currentUser?.avatar" :size="36" />
          <span class="nickname">{{ userStore.currentUser?.username }}</span>
          <DownOutlined class="down-icon" />
        </div>
        <template #overlay>
          <a-menu @click="handleUserMenuClick">
            <a-menu-item key="profile">
              <UserOutlined />
              个人信息
            </a-menu-item>
            <a-menu-item key="changePassword">
              <KeyOutlined />
              修改密码
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item key="logout">
              <LogoutOutlined />
              退出登录
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </a-layout-header>
    <a-layout-content class="content">
      <router-view />
    </a-layout-content>
  </a-layout>

  <a-modal
    v-model:open="profileVisible"
    title="个人信息"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="profileLoading"
    centered
    @ok="handleProfileSubmit"
  >
    <a-form layout="vertical">
      <a-form-item label="头像">
        <AvatarUpload v-model:value="profileForm.avatar" />
      </a-form-item>
      <a-form-item label="账号" required>
        <a-input v-model:value="profileForm.account" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item label="用户名" required>
        <a-input v-model:value="profileForm.username" placeholder="请输入用户名" />
      </a-form-item>
      <a-form-item label="简介">
        <a-textarea
          v-model:value="profileForm.bio"
          placeholder="介绍一下自己吧"
          :rows="3"
          :maxlength="100"
          show-count
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="passwordVisible"
    title="修改密码"
    :ok-text="passwordVerified ? '提交' : '下一步'"
    cancel-text="取消"
    :confirm-loading="passwordLoading"
    centered
    @ok="passwordVerified ? handlePasswordSubmit() : handleVerifyCurrent()"
    @cancel="resetPasswordModal"
  >
    <a-form v-if="!passwordVerified" layout="vertical" @submit.prevent="handleVerifyCurrent">
      <a-alert
        message="为保障账号安全，请先输入当前密码进行校验"
        type="info"
        show-icon
        class="password-tip"
      />
      <a-form-item label="当前密码" required>
        <a-input-password
          v-model:value="passwordForm.current"
          placeholder="请输入当前密码"
          @press-enter="handleVerifyCurrent"
        />
      </a-form-item>
    </a-form>
    <a-form v-else layout="vertical" @submit.prevent="handlePasswordSubmit">
      <a-form-item label="新密码" required>
        <a-input-password
          v-model:value="passwordForm.next"
          placeholder="请输入新密码"
        />
      </a-form-item>
      <a-form-item label="确认新密码" required>
        <a-input-password
          v-model:value="passwordForm.confirm"
          placeholder="请再次输入新密码"
          @press-enter="handlePasswordSubmit"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 10;
  line-height: normal;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2d3d;
  white-space: nowrap;
}

.header-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none;
  line-height: 62px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.nickname {
  font-size: 14px;
  color: #1f2d3d;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.down-icon {
  font-size: 12px;
  color: #8c8c8c;
}

.content {
  padding: 24px;
  background: #f5f7fa;
}

.password-tip {
  margin-bottom: 16px;
}
</style>
