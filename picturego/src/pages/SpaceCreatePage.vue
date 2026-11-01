<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getUserById } from '../api/user'
import { addSpace, listSpaceLevel } from '../api/space'
import { formatFileSize } from '../utils/format'

const route = useRoute()
const router = useRouter()

const targetUser = ref<User | null>(null)
const levelList = ref<SpaceLevel[]>([])
const pageLoading = ref(false)
const submitting = ref(false)

const createForm = reactive({
  spaceName: '',
  spaceLevel: undefined as number | undefined,
})

async function fetchData() {
  const userId = route.query.userId as string | undefined
  if (!userId) {
    message.warning('缺少目标用户，请从用户管理页进入')
    router.replace({ name: 'userManage' })
    return
  }
  pageLoading.value = true
  try {
    const [user, levels] = await Promise.all([getUserById(userId), listSpaceLevel()])
    targetUser.value = user
    levelList.value = levels ?? []
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载数据失败')
  } finally {
    pageLoading.value = false
  }
}

async function handleSubmit() {
  if (!targetUser.value) return
  submitting.value = true
  try {
    await addSpace({
      userId: targetUser.value.id,
      spaceName: createForm.spaceName.trim() || undefined,
      spaceLevel: createForm.spaceLevel,
    })
    message.success(`已为用户「${targetUser.value.userName || targetUser.value.userAccount}」创建空间`)
    router.replace({ name: 'userManage' })
  } catch (error) {
    message.error(error instanceof Error ? error.message : '创建空间失败')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <a-spin :spinning="pageLoading">
    <a-card :bordered="false" title="创建空间" class="space-create-card">
      <a-alert
        v-if="targetUser"
        type="info"
        show-icon
        class="target-tip"
        :message="`为用户「${targetUser.userName || targetUser.userAccount}」（ID：${targetUser.id}）创建私人空间`"
      />

      <a-form layout="vertical" class="create-form">
        <a-form-item label="空间名称">
          <a-input
            v-model:value="createForm.spaceName"
            placeholder="选填，留空则使用默认名称"
            :maxlength="30"
            show-count
            allow-clear
          />
        </a-form-item>
        <a-form-item label="空间级别">
          <a-select
            v-model:value="createForm.spaceLevel"
            placeholder="选填，默认普通版"
            allow-clear
            :options="
              levelList.map((level) => ({
                value: level.value,
                label: `${level.text}（${level.maxCount} 张 / ${formatFileSize(level.maxSize)}）`,
              }))
            "
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" :loading="submitting" @click="handleSubmit">
              确认创建
            </a-button>
            <a-button :disabled="submitting" @click="router.back()">返回</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-divider orientation="left" orientation-margin="0">空间级别说明</a-divider>
      <a-table
        :columns="[
          { title: '级别', dataIndex: 'text', key: 'text', width: 120 },
          { title: '图片数量上限', dataIndex: 'maxCount', key: 'maxCount', width: 140 },
          { title: '容量上限', dataIndex: 'maxSize', key: 'maxSize' },
        ]"
        :data-source="levelList"
        :pagination="false"
        row-key="value"
        size="small"
        class="level-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'maxCount'">{{ record.maxCount }} 张</template>
          <template v-else-if="column.key === 'maxSize'">
            {{ formatFileSize(record.maxSize) }}
          </template>
        </template>
      </a-table>
    </a-card>
  </a-spin>
</template>

<style scoped>
.space-create-card {
  max-width: 720px;
  margin: 0 auto;
  border-radius: 12px;
}

.target-tip {
  margin-bottom: 20px;
}

.create-form {
  max-width: 480px;
}

.level-table {
  margin-top: 12px;
}
</style>
