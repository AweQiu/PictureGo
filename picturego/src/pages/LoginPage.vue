<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')

const loginForm = reactive({
  account: '',
  password: '',
})

const registerForm = reactive({
  account: '',
  password: '',
  confirmPassword: '',
})

const loginLoading = ref(false)
const registerLoading = ref(false)

async function handleLogin() {
  if (!loginForm.account || !loginForm.password) {
    message.warning('请输入账号和密码')
    return
  }
  loginLoading.value = true
  const error = userStore.login(loginForm.account, loginForm.password)
  loginLoading.value = false
  if (error) {
    message.error(error)
    return
  }
  message.success('登录成功')
  router.replace({ name: 'home' })
}

async function handleRegister() {
  if (!registerForm.account || !registerForm.password) {
    message.warning('请输入账号和密码')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }
  registerLoading.value = true
  const error = userStore.register(registerForm.account, registerForm.password)
  registerLoading.value = false
  if (error) {
    message.error(error)
    return
  }
  message.success('注册成功，请登录')
  activeTab.value = 'login'
  loginForm.account = registerForm.account
  registerForm.account = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
}
</script>

<template>
  <div class="login-page">
    <a-card class="login-card" :bordered="false">
      <div class="brand">
        <img src="/picgo.png" alt="PicGo云图库" class="brand-logo" />
        <h1 class="brand-title">PicGo云图库</h1>
      </div>
      <a-tabs v-model:activeKey="activeTab" centered>
        <a-tab-pane key="login" tab="登录">
          <a-form layout="vertical" @submit.prevent="handleLogin">
            <a-form-item label="账号">
              <a-input
                v-model:value="loginForm.account"
                size="large"
                placeholder="请输入账号"
              >
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item label="密码">
              <a-input-password
                v-model:value="loginForm.password"
                size="large"
                placeholder="请输入密码"
                @press-enter="handleLogin"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>
            <a-button
              type="primary"
              size="large"
              block
              html-type="submit"
              :loading="loginLoading"
            >
              登录
            </a-button>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="register" tab="注册">
          <a-form layout="vertical" @submit.prevent="handleRegister">
            <a-form-item label="账号">
              <a-input
                v-model:value="registerForm.account"
                size="large"
                placeholder="请输入账号"
              >
                <template #prefix>
                  <UserOutlined />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item label="密码">
              <a-input-password
                v-model:value="registerForm.password"
                size="large"
                placeholder="请输入密码"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>
            <a-form-item label="确认密码">
              <a-input-password
                v-model:value="registerForm.confirmPassword"
                size="large"
                placeholder="请再次输入密码"
                @press-enter="handleRegister"
              >
                <template #prefix>
                  <LockOutlined />
                </template>
              </a-input-password>
            </a-form-item>
            <a-button
              type="primary"
              size="large"
              block
              html-type="submit"
              :loading="registerLoading"
            >
              注册
            </a-button>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #e0f2ff 0%, #f5f0ff 50%, #fff0f6 100%);
}

.login-card {
  width: 100%;
  max-width: 400px;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(31, 45, 61, 0.12);
}

.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 8px;
}

.brand-logo {
  width: 72px;
  height: 72px;
  object-fit: contain;
}

.brand-title {
  margin: 12px 0 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2d3d;
}
</style>
