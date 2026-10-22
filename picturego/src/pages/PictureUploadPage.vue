<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import {
  listPictureTagCategory,
  updatePicture,
  uploadPicture,
} from '../api/picture'
import { formatFileSize } from '../utils/format'

/** 上传成功后后端返回的图片（含解析出的格式、尺寸等信息） */
const uploaded = ref<PictureVO | null>(null)
const uploading = ref(false)
const saving = ref(false)

/** 可选的图片信息，全部选填 */
const form = reactive({
  introduction: '',
  category: undefined as string | undefined,
  tags: [] as string[],
})

const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

const hasMetadata = computed(
  () => Boolean(form.introduction.trim() || form.category || form.tags.length > 0),
)

async function fetchTagCategory() {
  try {
    const data = await listPictureTagCategory()
    categoryOptions.value = data.categories ?? []
    tagOptions.value = data.tags ?? []
  } catch (error) {
    console.error('获取分类标签失败', error)
  }
}

function resetForm() {
  uploaded.value = null
  form.introduction = ''
  form.category = undefined
  form.tags = []
}

async function handleUpload(file: File) {
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片文件')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  uploading.value = true
  try {
    // 已有上传结果时带上 id，重新上传即替换同一张图片的文件
    const picture = await uploadPicture(file, uploaded.value?.id)
    uploaded.value = picture
    form.introduction = picture.introduction ?? ''
    form.category = picture.category || undefined
    form.tags = picture.tags ?? []
    message.success('图片上传成功，可按需完善图片信息')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '图片上传失败')
  } finally {
    uploading.value = false
  }
  return false
}

async function handleSubmit() {
  const picture = uploaded.value
  if (!picture) {
    message.warning('请先上传图片')
    return
  }
  saving.value = true
  try {
    if (hasMetadata.value) {
      await updatePicture({
        id: picture.id,
        introduction: form.introduction.trim(),
        category: form.category,
        tags: form.tags,
      })
    }
    message.success('图片上传成功')
    resetForm()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存图片信息失败')
  } finally {
    saving.value = false
  }
}

onMounted(fetchTagCategory)
</script>

<template>
  <a-card :bordered="false" title="上传图片" class="upload-card">
    <a-upload-dragger
      accept="image/*"
      :show-upload-list="false"
      :before-upload="handleUpload"
      :disabled="uploading"
      class="upload-dragger"
    >
      <a-spin :spinning="uploading" tip="上传中...">
        <template v-if="uploaded">
          <img :src="uploaded.url" :alt="uploaded.name" class="upload-preview" />
          <p class="dragger-text">图片已上传，点击或拖拽可替换图片</p>
        </template>
        <template v-else>
          <p class="dragger-icon"><InboxOutlined /></p>
          <p class="dragger-text">点击或拖拽图片到此处上传</p>
          <p class="dragger-hint">支持 jpg / jpeg / gif / png / webp 等图片格式，大小不超过 5MB</p>
        </template>
      </a-spin>
    </a-upload-dragger>

    <div class="parsed-info">
      <template v-if="uploaded">
        <span class="parsed-label">已解析</span>
        <span>名称：{{ uploaded.name || '—' }}</span>
        <span>格式：{{ uploaded.picFormat || '—' }}</span>
        <span>尺寸：{{ uploaded.picWidth }} × {{ uploaded.picHeight }}</span>
        <span>大小：{{ formatFileSize(uploaded.picSize) }}</span>
      </template>
      <span v-else class="parsed-hint">上传成功后自动解析图片信息</span>
    </div>

    <a-form layout="vertical" class="info-form">
      <a-form-item label="简介">
        <a-textarea
          v-model:value="form.introduction"
          placeholder="选填，介绍一下这张图片"
          :rows="3"
          :maxlength="200"
          show-count
          :disabled="!uploaded"
        />
      </a-form-item>
      <a-form-item label="分类">
        <a-select
          v-model:value="form.category"
          placeholder="选填，选择图片分类"
          allow-clear
          show-search
          :disabled="!uploaded"
          :options="categoryOptions.map((item) => ({ value: item, label: item }))"
        />
      </a-form-item>
      <a-form-item label="标签">
        <a-select
          v-model:value="form.tags"
          placeholder="选填，选择图片标签"
          mode="multiple"
          allow-clear
          :disabled="!uploaded"
          :options="tagOptions.map((item) => ({ value: item, label: item }))"
        />
      </a-form-item>
      <a-form-item>
        <div class="form-actions">
          <a-button
            type="primary"
            :loading="saving"
            :disabled="!uploaded"
            @click="handleSubmit"
          >
            完成上传
          </a-button>
          <a-button :disabled="!uploaded" @click="resetForm">重新选择</a-button>
        </div>
      </a-form-item>
    </a-form>
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
  max-height: 320px;
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

.parsed-info {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  margin: 16px 0 20px;
  font-size: 13px;
  color: #5e6d82;
}

.parsed-label {
  color: #8c8c8c;
}

.parsed-hint {
  color: #bfbfbf;
}

.info-form {
  max-width: 640px;
}

.form-actions {
  display: flex;
  gap: 12px;
}
</style>
