<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import { uploadPicture } from '../api/picture'
import { formatFileSize } from '../utils/format'

/** 已选择、待确认上传的图片 */
const selectedFile = ref<File | null>(null)
const previewUrl = ref('')
const uploading = ref(false)

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

onBeforeUnmount(clearSelection)
</script>

<template>
  <a-card :bordered="false" title="上传图片" class="upload-card">
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

@media (max-width: 600px) {
  .upload-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .file-actions {
    margin-left: 0;
  }
}
</style>
