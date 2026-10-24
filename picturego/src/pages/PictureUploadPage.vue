<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import { uploadPicture, uploadPictureByBatch, uploadPictureByUrl } from '../api/picture'
import { useUserStore } from '../store/user'
import { formatFileSize } from '../utils/format'

const userStore = useUserStore()

/** 已选择、待确认上传的图片 */
const selectedFile = ref<File | null>(null)
const previewUrl = ref('')
const uploading = ref(false)
const activeTab = ref('file')

function clearSelection() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
  selectedFile.value = null
}

/** 选择图片后只做本地预览，点击「确认上传」才真正调用上传接口 */
function handleSelect(file: File) {
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片文件')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  clearSelection()
  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
  return false
}

async function handleConfirm() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    // 与后端 /picture/upload 对齐：只提交文件，其余信息由后端解析
    await uploadPicture(selectedFile.value)
    message.success('图片上传成功')
    clearSelection()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '图片上传失败')
  } finally {
    uploading.value = false
  }
}

// ---------- URL 上传 ----------
const imageUrl = ref('')
const urlUploading = ref(false)
const canUrlUpload = computed(() => !!imageUrl.value?.trim())

async function handleUrlUpload() {
  const url = imageUrl.value?.trim()
  if (!url) {
    message.warning('请输入图片链接')
    return
  }
  if (!/^https?:\/\//i.test(url)) {
    message.warning('请输入 http:// 或 https:// 开头的图片链接')
    return
  }
  urlUploading.value = true
  try {
    // 与后端 /picture/upload/url 对齐：只提交 url，其余信息由后端解析
    await uploadPictureByUrl({ url })
    message.success('图片上传成功')
    imageUrl.value = ''
  } catch (error) {
    message.error(error instanceof Error ? error.message : '图片上传失败')
  } finally {
    urlUploading.value = false
  }
}

const batchSearchText = ref('')
const batchCount = ref(5)
const batchNamePrefix = ref('')
const batchUploading = ref(false)
const canBatchUpload = computed(
  () => !!batchSearchText.value.trim() && typeof batchCount.value === 'number' && batchCount.value >= 1,
)

async function handleBatchUpload() {
  const searchText = batchSearchText.value.trim()
  if (!searchText) {
    message.warning('请输入搜索关键词')
    return
  }
  if (!batchCount.value || batchCount.value < 1) {
    message.warning('请输入有效的图片数量')
    return
  }
  batchUploading.value = true
  try {
    const uploadedCount = await uploadPictureByBatch({
      searchText,
      count: batchCount.value,
      namePrefix: batchNamePrefix.value.trim() || undefined,
    })
    message.success(`批量上传完成，成功上传 ${uploadedCount} 张图片`)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '批量上传失败')
  } finally {
    batchUploading.value = false
  }
}

onBeforeUnmount(clearSelection)
</script>

<template>
  <a-card :bordered="false" title="上传图片" class="upload-card">
    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="file" tab="文件上传">
        <a-upload-dragger
          accept="image/*"
          :show-upload-list="false"
          :before-upload="handleSelect"
          :disabled="uploading"
          class="upload-dragger"
        >
          <template v-if="selectedFile">
            <img :src="previewUrl" :alt="selectedFile.name" class="upload-preview" />
            <p class="dragger-text">点击或拖拽可更换图片</p>
          </template>
          <template v-else>
            <p class="dragger-icon"><InboxOutlined /></p>
            <p class="dragger-text">点击或拖拽图片到此处</p>
            <p class="dragger-hint">支持 jpg / jpeg / gif / png / webp 等图片格式，大小不超过 5MB</p>
          </template>
        </a-upload-dragger>

        <div class="upload-footer">
          <div v-if="selectedFile" class="file-info">
            <span class="file-name">{{ selectedFile.name }}</span>
            <span>{{ formatFileSize(selectedFile.size) }}</span>
          </div>
          <span v-else class="file-hint">选择图片后可确认上传</span>
          <div class="file-actions">
            <a-button
              type="primary"
              :loading="uploading"
              :disabled="!selectedFile"
              @click="handleConfirm"
            >
              确认上传
            </a-button>
            <a-button :disabled="!selectedFile || uploading" @click="clearSelection">
              重新选择
            </a-button>
          </div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="url" tab="URL 上传">
        <div class="url-pane">
          <div class="url-row">
            <a-input
              v-model:value="imageUrl"
              placeholder="请输入 http:// 或 https:// 开头的图片链接"
              :disabled="urlUploading"
              allow-clear
              @press-enter="handleUrlUpload"
            />
            <a-button
              type="primary"
              :loading="urlUploading"
              :disabled="!canUrlUpload"
              @click="handleUrlUpload"
            >
              URL 上传
            </a-button>
          </div>
          <p class="url-hint">图片的名称、尺寸、格式等信息将由后端自动解析</p>
        </div>
      </a-tab-pane>

      <a-tab-pane v-if="userStore.isAdmin" key="batch" tab="批量上传">
        <a-form layout="vertical" class="batch-form">
          <a-alert
            type="info"
            show-icon
            message="根据关键词搜索并批量上传图片，图片信息将由后端自动解析"
            class="batch-tip"
          />
          <a-form-item label="搜索关键词" required>
            <a-input
              v-model:value="batchSearchText"
              placeholder="请输入图片搜索关键词，如：风景"
              :disabled="batchUploading"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="图片数量" required>
            <a-input-number
              v-model:value="batchCount"
              :min="1"
              :max="30"
              :precision="0"
              :disabled="batchUploading"
              class="batch-count"
            />
          </a-form-item>
          <a-form-item label="名称前缀">
            <a-input
              v-model:value="batchNamePrefix"
              placeholder="请输入图片名称前缀（可选）"
              :disabled="batchUploading"
              allow-clear
            />
          </a-form-item>
          <a-button
            type="primary"
            :loading="batchUploading"
            :disabled="!canBatchUpload"
            @click="handleBatchUpload"
          >
            批量上传
          </a-button>
        </a-form>
      </a-tab-pane>
    </a-tabs>
  </a-card>
</template>

<style scoped>
.upload-card {
  border-radius: 12px;
}

.upload-dragger :deep(.ant-upload-drag) {
  border-radius: 12px;
}

.upload-preview {
  display: block;
  max-width: 100%;
  max-height: 360px;
  margin: 0 auto 12px;
  object-fit: contain;
  border-radius: 8px;
}

.dragger-icon {
  margin-bottom: 8px;
  font-size: 42px;
  color: #1677ff;
}

.dragger-text {
  margin: 0;
  font-size: 15px;
  color: #1f2d3d;
}

.dragger-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #8c8c8c;
}

.upload-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}

.file-info {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #5e6d82;
}

.file-name {
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1f2d3d;
}

.file-hint {
  font-size: 13px;
  color: #bfbfbf;
}

.file-actions {
  display: flex;
  gap: 12px;
  margin-left: auto;
}

.url-pane {
  padding: 12px 0 8px;
}

.url-row {
  display: flex;
  gap: 12px;
}

.url-row :deep(.ant-input-affix-wrapper) {
  flex: 1;
}

.url-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #8c8c8c;
}

.batch-form {
  max-width: 560px;
  padding: 12px 0 8px;
}

.batch-tip {
  margin-bottom: 24px;
}

.batch-count {
  width: 100%;
}

@media (max-width: 600px) {
  .upload-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .file-actions {
    margin-left: 0;
  }

  .url-row {
    flex-direction: column;
  }
}
</style>
