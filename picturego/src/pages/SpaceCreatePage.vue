<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getUserById } from '../api/user'
import { addSpace, listSpaceLevel } from '../api/space'
import { useUserStore } from '../store/user'
import { formatFileSize } from '../utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 管理员模式：带 userId 参数，为指定用户创建；否则为当前登录用户自建 */
const adminUserId = route.query.userId as string | undefined
const isAdminMode = computed(() => !!adminUserId)

const targetUser = ref<User | null>(null)
const levelList = ref<SpaceLevel[]>([])
const pageLoading = ref(false)
const submitting = ref(false)

const createForm = reactive({
  spaceName: '',
  // 普通用户固定为普通空间（级别选择置灰），管理员可选
  spaceLevel: (isAdminMode.value ? undefined : 0) as number | undefined,
})

/** 自建模式下展示用的当前用户名 */
const selfName = computed(
  () => userStore.loginUser?.userName || userStore.loginUser?.userAccount || '',
)

async function fetchData() {
  if (isAdminMode.value && !userStore.isAdmin) {
    message.warning('没有权限为其他用户创建空间')
    router.replace({ name: 'mySpace' })
    return
  }
  pageLoading.value = true
  try {
    const requests: [Promise<User | null>, Promise<SpaceLevel[]>] = [
      isAdminMode.value ? getUserById(adminUserId!) : Promise.resolve(null),
      listSpaceLevel(),
    ]
    const [user, levels] = await Promise.all(requests)
    targetUser.value = user
    levelList.value = levels ?? []
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载数据失败')
  } finally {
    pageLoading.value = false
  }
}

async function handleSubmit() {
  if (isAdminMode.value && !targetUser.value) return
  submitting.value = true
  try {
    await addSpace({
      userId: isAdminMode.value ? targetUser.value!.id : undefined,
      spaceName: createForm.spaceName.trim() || undefined,
      spaceLevel: createForm.spaceLevel,
    })
    message.success(
      isAdminMode.value
        ? `已为用户「${targetUser.value!.userName || targetUser.value!.userAccount}」创建空间`
        : '私人空间创建成功',
    )
    router.replace({ name: isAdminMode.value ? 'userManage' : 'mySpace' })
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
        type="info"
        show-icon
        class="target-tip"
        :message="
          isAdminMode && targetUser
            ? `为用户「${targetUser.userName || targetUser.userAccount}」（ID：${targetUser.id}）创建私人空间`
            : `为「${selfName}」创建私人空间`
        "
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
            :placeholder="isAdminMode ? '选填，默认普通空间' : '普通空间'"
            :disabled="!isAdminMode"
            :allow-clear="isAdminMode"
            :options="
              levelList.map((level) => ({
                value: level.value,
                label: `${level.text}（${level.maxCount} 张 / ${formatFileSize(level.maxSize)}）`,
              }))
            "
          />
          <div v-if="!isAdminMode" class="level-hint">
            个人自建空间默认为普通空间，更高级别请联系管理员调整
          </div>
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

.level-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #8c8c8c;
}

.level-table {
  margin-top: 12px;
}
</style>
