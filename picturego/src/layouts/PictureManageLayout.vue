<script setup lang="ts">
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DatabaseOutlined, PictureOutlined } from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { key: 'picturePublic', label: '公共图库', icon: () => h(PictureOutlined) },
  { key: 'mySpace', label: '我的空间', icon: () => h(DatabaseOutlined) },
]

/** 左侧菜单选中项：公共图库 / 我的空间 */
const selectedKeys = computed(() => [
  route.name === 'mySpace' ? 'mySpace' : 'picturePublic',
])

function handleMenuClick({ key }: { key: string }) {
  if (route.name !== key) {
    router.push({ name: key })
  }
}
</script>

<template>
  <div class="manage-shell">
    <aside class="manage-sider">
      <div class="sider-title">图片管理</div>
      <a-menu
        mode="inline"
        class="sider-menu"
        :selected-keys="selectedKeys"
        :items="menuItems"
        @click="handleMenuClick"
      />
      <p class="sider-hint">
        公共图库：管理你发布到公共图库的图片；我的空间：管理私有空间内的图片。
      </p>
    </aside>
    <section class="manage-main">
      <router-view />
    </section>
  </div>
</template>

<style scoped>
.manage-shell {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.manage-sider {
  position: sticky;
  top: 88px;
  flex: 0 0 200px;
  width: 200px;
  padding: 8px 8px 12px;
  background: #fff;
  border-radius: 12px;
}

.sider-title {
  padding: 12px 12px 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
}

.sider-menu {
  border-inline-end: none;
}

.sider-hint {
  margin: 12px 12px 0;
  font-size: 12px;
  line-height: 18px;
  color: #8c8c8c;
}

.manage-main {
  flex: 1;
  min-width: 0;
}

@media (max-width: 900px) {
  .manage-shell {
    flex-direction: column;
  }

  .manage-sider {
    position: static;
    flex: none;
    width: 100%;
  }

  .sider-hint {
    display: none;
  }
}
</style>
