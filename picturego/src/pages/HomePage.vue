<script setup lang="ts">
import { useUserStore } from '../store/user'

const userStore = useUserStore()
</script>

<template>
  <div class="home-page">
    <a-card :bordered="false" class="welcome-card">
      <img src="/picgo.png" alt="PicGo云图库" class="welcome-logo" />
      <h1 class="welcome-title">PicGo云图库</h1>
      <p class="welcome-desc">
        你好，{{ userStore.currentUser?.username }}！欢迎使用 PicGo云图库，在这里管理你的图片。
      </p>
    </a-card>
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  justify-content: center;
}

.welcome-card {
  width: 100%;
  max-width: 720px;
  text-align: center;
  border-radius: 12px;
}

.welcome-logo {
  width: 160px;
  height: 160px;
  object-fit: contain;
}

.welcome-title {
  margin: 16px 0 8px;
  font-size: 32px;
  font-weight: 600;
  color: #1f2d3d;
}

.welcome-desc {
  margin: 0;
  font-size: 16px;
  color: #5e6d82;
}
</style>
