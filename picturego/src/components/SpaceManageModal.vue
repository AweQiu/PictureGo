<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { getSpaceVOById, listSpaceLevel, updateSpace } from '../api/space'
import { formatDateTime, formatFileSize } from '../utils/format'

const props = defineProps<{
  open: boolean
  space: SpaceVO | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: [space: SpaceVO]
}>()

const spaceData = ref<SpaceVO | null>(null)
const levelList = ref<SpaceLevel[]>([])
const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const spaceForm = reactive({
  spaceLevel: undefined as number | undefined,
  maxCount: undefined as number | undefined,
  maxSizeMb: undefined as number | undefined,
})

const levelText = computed(() => {
  if (!spaceData.value) return '—'
  return (
    levelList.value.find((level) => level.value === spaceData.value?.spaceLevel)?.text ??
    `级别 ${spaceData.value.spaceLevel}`
  )
})

const sizePercent = computed(() => {
  if (!spaceData.value?.maxSize) return 0
  return Math.min(100, Math.round((spaceData.value.totalSize / spaceData.value.maxSize) * 100))
})

const countPercent = computed(() => {
  if (!spaceData.value?.maxCount) return 0
  return Math.min(100, Math.round((spaceData.value.totalCount / spaceData.value.maxCount) * 100))
})

async function loadSpace() {
  if (!props.space) return
  loading.value = true
  editing.value = false
  spaceData.value = props.space
  try {
    const [detail, levels] = await Promise.all([
      getSpaceVOById(props.space.id),
      listSpaceLevel(),
    ])
    spaceData.value = detail
    levelList.value = levels ?? []
    emit('saved', detail)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '获取空间详情失败')
  } finally {
    loading.value = false
  }
}

function startEdit() {
  if (!spaceData.value) return
  Object.assign(spaceForm, {
    spaceLevel: spaceData.value.spaceLevel,
    maxCount: spaceData.value.maxCount,
    maxSizeMb: Number((spaceData.value.maxSize / 1024 / 1024).toFixed(2)),
  })
  editing.value = true
}

function handleLevelChange(value: number) {
  const level = levelList.value.find((item) => item.value === value)
  if (!level) return
  spaceForm.maxCount = level.maxCount
  spaceForm.maxSizeMb = Number((level.maxSize / 1024 / 1024).toFixed(2))
}

async function handleSave() {
  if (!spaceData.value) return
  if (spaceForm.spaceLevel === undefined) {
    message.warning('请选择空间级别')
    return
  }
  if (!Number.isInteger(spaceForm.maxCount) || (spaceForm.maxCount ?? 0) <= 0) {
    message.warning('数量额度必须为正整数')
    return
  }
  if (!Number.isFinite(spaceForm.maxSizeMb) || (spaceForm.maxSizeMb ?? 0) <= 0) {
    message.warning('存储额度必须大于 0')
    return
  }
  if (spaceForm.maxCount! < spaceData.value.totalCount) {
    message.warning(`数量额度不能低于当前已使用的 ${spaceData.value.totalCount} 张`)
    return
  }
  const maxSize = Math.round(spaceForm.maxSizeMb! * 1024 * 1024)
  if (maxSize < spaceData.value.totalSize) {
    message.warning(`存储额度不能低于当前已使用的 ${formatFileSize(spaceData.value.totalSize)}`)
    return
  }
  saving.value = true
  try {
    await updateSpace({
      id: spaceData.value.id,
      spaceName: spaceData.value.spaceName,
      spaceLevel: spaceForm.spaceLevel,
      maxCount: spaceForm.maxCount,
      maxSize,
    })
    message.success('空间信息已更新')
    await loadSpace()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存空间失败')
  } finally {
    saving.value = false
  }
}

watch(
  () => props.open,
  (visible) => {
    if (visible) loadSpace()
  },
  { immediate: true },
)
</script>

<template>
  <a-modal
    :open="open"
    :title="editing ? '编辑空间' : '空间详情'"
    width="600px"
    centered
    :mask-closable="!saving"
    @cancel="emit('update:open', false)"
  >
    <a-spin :spinning="loading">
      <template v-if="spaceData">
        <div v-if="!editing" class="space-detail">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="空间名称">
              {{ spaceData.spaceName || '未命名空间' }}
            </a-descriptions-item>
            <a-descriptions-item label="空间级别">
              <a-tag color="blue">{{ levelText }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="所属用户">
              {{ spaceData.userVO?.userName || spaceData.userVO?.userAccount || spaceData.userId }}
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">
              {{ formatDateTime(spaceData.createTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="编辑时间">
              {{ formatDateTime(spaceData.editTime) }}
            </a-descriptions-item>
          </a-descriptions>
          <div class="space-usage">
            <div class="usage-row">
              <span class="usage-label">存储额度</span>
              <a-progress :percent="sizePercent" size="small" class="usage-progress" />
              <span class="usage-text">
                {{ formatFileSize(spaceData.totalSize) }} / {{ formatFileSize(spaceData.maxSize) }}
              </span>
            </div>
            <div class="usage-row">
              <span class="usage-label">数量额度</span>
              <a-progress :percent="countPercent" size="small" class="usage-progress" />
              <span class="usage-text">{{ spaceData.totalCount }} / {{ spaceData.maxCount }} 张</span>
            </div>
          </div>
        </div>

        <a-form v-else layout="vertical" class="space-form">
          <a-form-item label="空间级别" required>
            <a-select
              v-model:value="spaceForm.spaceLevel"
              :options="levelList.map((level) => ({ value: level.value, label: level.text }))"
              @change="handleLevelChange"
            />
            <div class="form-hint">切换级别会填入该级别的默认额度，仍可手动调整</div>
          </a-form-item>
          <a-form-item label="数量额度" required>
            <a-input-number
              v-model:value="spaceForm.maxCount"
              :min="1"
              :precision="0"
              addon-after="张"
              class="quota-input"
            />
          </a-form-item>
          <a-form-item label="存储额度" required>
            <a-input-number
              v-model:value="spaceForm.maxSizeMb"
              :min="0.01"
              :precision="2"
              addon-after="MB"
              class="quota-input"
            />
          </a-form-item>
        </a-form>
      </template>
    </a-spin>

    <template #footer>
      <template v-if="editing">
        <a-button :disabled="saving" @click="editing = false">返回详情</a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">保存</a-button>
      </template>
      <template v-else>
        <a-button @click="emit('update:open', false)">关闭</a-button>
        <a-button type="primary" :disabled="loading || !spaceData" @click="startEdit">
          编辑空间
        </a-button>
      </template>
    </template>
  </a-modal>
</template>

<style scoped>
.space-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.space-usage {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.usage-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.usage-label {
  flex: 0 0 56px;
  color: #5e6d82;
}

.usage-progress {
  flex: 1;
  margin: 0;
}

.usage-text {
  flex: 0 0 140px;
  font-size: 12px;
  color: #8c8c8c;
  text-align: right;
  white-space: nowrap;
}

.quota-input {
  width: 100%;
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #8c8c8c;
}

@media (max-width: 600px) {
  .usage-row {
    flex-wrap: wrap;
  }

  .usage-progress {
    min-width: 160px;
  }

  .usage-text {
    flex-basis: 100%;
    text-align: left;
  }
}
</style>
