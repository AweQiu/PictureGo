<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  DownOutlined,
  HomeOutlined,
  LogoutOutlined,
  PictureOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { useUserStore } from '../store/user'
import AvatarUpload from '../components/AvatarUpload.vue'
import UserAvatar from '../components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const selectedKeys = computed(() => [route.name as string])

const menuItems = computed(() => {
  const items = [{ key: 'home', label: '首页', icon: () => h(HomeOutlined) }]
  if (userStore.isAdmin) {
    items.push(
      { key: 'pictureManage', label: '图片管理', icon: () => h(PictureOutlined) },
      { key: 'userManage', label: '用户管理', icon: () => h(TeamOutlined) },
    )
  }
  return items
})

function handleMenuClick({ key }: { key: string }) {
  if (key !== route.name) {
    router.push({ name: key })
  }
}

// ---------- 个人信息 ----------
const profileVisible = ref(false)
const profileLoading = ref(false)
const profileForm = reactive({
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
})

function openProfile() {
  const user = userStore.loginUser
  if (!user) return
  Object.assign(profileForm, {
    userAccount: user.userAccount,
    userName: user.userName,
    userAvatar: user.userAvatar,
    userProfile: user.userProfile,
  })
  profileVisible.value = true
}

async function handleProfileSubmit() {
  if (!profileForm.userAccount.trim() || !profileForm.userName.trim()) {
    message.warning('账号和用户名不能为空')
    return
  }
  profileLoading.value = true
  try {
    await userStore.updateProfile({
      userAccount: profileForm.userAccount.trim(),
      userName: profileForm.userName.trim(),
      userAvatar: profileForm.userAvatar || undefined,
      userProfile: profileForm.userProfile.trim() || undefined,
    })
    message.success('个人信息已更新')
    profileVisible.value = false
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    profileLoading.value = false
  }
}

// ---------- 用户下拉菜单 ----------
function handleUserMenuClick({ key }: { key: string }) {
  if (key === 'profile') {
    openProfile()
  } else if (key === 'logout') {
    Modal.confirm({
      title: '退出登录',
      content: '确定要退出登录吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        await userStore.logout()
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
          <UserAvatar :src="userStore.loginUser?.userAvatar" :size="36" />
          <span class="nickname">{{ userStore.displayName }}</span>
          <DownOutlined class="down-icon" />
        </div>
        <template #overlay>
          <a-menu @click="handleUserMenuClick">
            <a-menu-item key="profile">
              <UserOutlined />
              个人信息
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
        <AvatarUpload v-model:value="profileForm.userAvatar" />
      </a-form-item>
      <a-form-item label="账号" required>
        <a-input v-model:value="profileForm.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item label="用户名" required>
        <a-input v-model:value="profileForm.userName" placeholder="请输入用户名" />
      </a-form-item>
      <a-form-item label="简介">
        <a-textarea
          v-model:value="profileForm.userProfile"
          placeholder="介绍一下自己吧"
          :rows="3"
          :maxlength="100"
          show-count
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
</style>
