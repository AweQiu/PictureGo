<script setup lang="ts">
import { computed, ref } from 'vue'
import { useUserStore } from '../store/user'
import PictureManagePage from './PictureManagePage.vue'
import PictureMyPublicPanel from '../components/PictureMyPublicPanel.vue'

const userStore = useUserStore()

const isAdmin = computed(() => userStore.isAdmin)

/** 管理员可在「我发布的」和「全站图片」之间切换，普通用户只有「我发布的」 */
const activeTab = ref('mine')
</script>

<template>
  <a-card :bordered="false" class="public-manage-card">
    <a-tabs v-if="isAdmin" v-model:activeKey="activeTab" class="manage-tabs">
      <a-tab-pane key="mine" tab="我发布的">
        <PictureMyPublicPanel />
      </a-tab-pane>
      <a-tab-pane key="all" tab="全站图片">
        <PictureManagePage />
      </a-tab-pane>
    </a-tabs>
    <PictureMyPublicPanel v-else />
  </a-card>
</template>

<style scoped>
.public-manage-card {
  border-radius: 12px;
}

.manage-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}
</style>
