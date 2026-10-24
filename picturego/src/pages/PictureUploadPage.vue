<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import {
  listPictureTagCategory,
  uploadPicture,
  uploadPictureByBatch,
  uploadPictureByUrl,
} from '../api/picture'
import { useUserStore } from '../store/user'
import { formatFileSize } from '../utils/format'

interface PictureMetaForm {
  name: string
  introduction: string
  category: string | undefined
  tags: string[]
}

function createMetaForm(): PictureMetaForm {
  return {
    name: '',
    introduction: '',
    category: undefined,
    tags: [],
  }
}

function buildMetaPayload(meta: PictureMetaForm): PictureUploadRequest {
  const introduction = meta.introduction.trim()
  const tags = meta.tags.map((tag) => tag.trim()).filter(Boolean)
  return {
    name: meta.name.trim() || undefined,
    introduction: introduction || undefined,
    category: meta.category?.trim() || undefined,
    tags: tags.length > 0 ? tags : undefined,
  }
}

function resetMetaForm(meta: PictureMetaForm) {
  Object.assign(meta, createMetaForm())
}

const userStore = useUserStore()
const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])

async function fetchTagCategory() {
  try {
    const data = await listPictureTagCategory()
    categoryOptions.value = data.categories ?? []
    tagOptions.value = data.tags ?? []
  } catch (error) {
    console.error('获取分类标签失败', error)
  }
}

/** 已选择、待确认上传的图片 */
const selectedFile = ref<File | null>(null)
const previewUrl = ref('')
const uploading = ref(false)
const activeTab = ref('file')
const fileMeta = reactive(createMetaForm())

function clearSelection() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
  selectedFile.value = null
  resetMetaForm(fileMeta)
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
    await uploadPicture(selectedFile.value, buildMetaPayload(fileMeta))
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
const urlPreviewUrl = ref('')
const urlPreviewLoaded = ref(false)
const urlPreviewing = ref(false)
const urlUploading = ref(false)
const urlMeta = reactive(createMetaForm())
const canPreviewUrl = computed(() => !!imageUrl.value?.trim())
let urlPreviewToken = 0
let urlPreviewTimer: ReturnType<typeof setTimeout> | undefined

function clearPreviewTimer() {
  if (urlPreviewTimer) {
    clearTimeout(urlPreviewTimer)
    urlPreviewTimer = undefined
  }
}

function clearUrlSelection() {
  urlPreviewToken += 1
  clearPreviewTimer()
  imageUrl.value = ''
  urlPreviewUrl.value = ''
  urlPreviewLoaded.value = false
  urlPreviewing.value = false
  resetMetaForm(urlMeta)
}

function handleUrlPreview() {
  const url = imageUrl.value.trim()
  if (!url) {
    message.warning('请输入图片链接')
    return
  }
  if (!/^https?:\/\//i.test(url)) {
    message.warning('请输入 http:// 或 https:// 开头的图片链接')
    return
  }

  clearPreviewTimer()
  const token = ++urlPreviewToken
  urlPreviewUrl.value = ''
  urlPreviewLoaded.value = false
  urlPreviewing.value = true

  const image = new Image()
  image.referrerPolicy = 'no-referrer'
  image.onload = () => {
    if (token !== urlPreviewToken) return
    clearPreviewTimer()
    urlPreviewing.value = false
    urlPreviewLoaded.value = true
    urlPreviewUrl.value = url
    resetMetaForm(urlMeta)
  }
  image.onerror = () => {
    if (token !== urlPreviewToken) return
    clearPreviewTimer()
    urlPreviewing.value = false
    message.error('图片加载失败，请检查链接是否有效或图片是否允许外链访问')
  }
  urlPreviewTimer = setTimeout(() => {
    if (token !== urlPreviewToken) return
    urlPreviewing.value = false
    message.error('图片加载超时，请检查链接后重试')
  }, 15000)
  image.src = url
}

async function handleUrlUpload() {
  if (!urlPreviewLoaded.value || !urlPreviewUrl.value) {
    message.warning('请先确认图片链接并加载成功')
    return
  }
  urlUploading.value = true
  try {
    await uploadPictureByUrl({
      ...buildMetaPayload(urlMeta),
      url: urlPreviewUrl.value,
    })
    message.success('图片上传成功')
    clearUrlSelection()
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

onMounted(fetchTagCategory)
onBeforeUnmount(() => {
  clearSelection()
  clearUrlSelection()
})
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

        <a-form v-if="selectedFile" layout="vertical" class="meta-form">
          <div class="meta-grid">
            <a-form-item label="图片名称">
              <a-input
                v-model:value="fileMeta.name"
                placeholder="选填，留空则由后端解析"
                :disabled="uploading"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="图片分类">
              <a-auto-complete
                v-model:value="fileMeta.category"
                placeholder="选填，可选择或输入分类"
                :options="categoryOptions.map((item) => ({ value: item }))"
                :disabled="uploading"
                allow-clear
              />
            </a-form-item>
          </div>
          <a-form-item label="图片简介">
            <a-textarea
              v-model:value="fileMeta.introduction"
              placeholder="选填，最多 800 字"
              :rows="3"
              :maxlength="800"
              show-count
              :disabled="uploading"
            />
          </a-form-item>
          <a-form-item label="图片标签">
            <a-select
              v-model:value="fileMeta.tags"
              mode="tags"
              placeholder="选填，可选择或输入标签"
              :options="tagOptions.map((item) => ({ value: item, label: item }))"
              :disabled="uploading"
              allow-clear
            />
          </a-form-item>
          <p class="meta-hint">以上信息均为选填，未填写的内容将由后端自动解析</p>
        </a-form>

        <div class="upload-footer">
          <div v-if="selectedFile" class="file-info">
            <span class="file-name">{{ selectedFile.name }}</span>
            <span>{{ formatFileSize(selectedFile.size) }}</span>
          </div>
          <span v-else class="file-hint">选择图片后可填写图片信息并上传</span>
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
              :disabled="urlPreviewing || urlPreviewLoaded || urlUploading"
              allow-clear
              @press-enter="handleUrlPreview"
            />
            <a-button
              v-if="!urlPreviewLoaded"
              type="primary"
              :loading="urlPreviewing"
              :disabled="!canPreviewUrl"
              @click="handleUrlPreview"
            >
              确认
            </a-button>
          </div>
          <p v-if="!urlPreviewLoaded" class="url-hint">
            点击「确认」后先加载图片预览，加载成功再填写图片信息并上传
          </p>

          <div v-if="urlPreviewing" class="url-loading">
            <a-spin size="small" />
            <span>正在加载图片...</span>
          </div>

          <div v-if="urlPreviewLoaded" class="url-preview-area">
            <img
              :src="urlPreviewUrl"
              :alt="urlMeta.name || 'URL 图片预览'"
              class="url-preview"
              referrerpolicy="no-referrer"
            />

            <a-form layout="vertical" class="meta-form">
              <div class="meta-grid">
                <a-form-item label="图片名称">
                  <a-input
                    v-model:value="urlMeta.name"
                    placeholder="选填，留空则由后端解析"
                    :disabled="urlUploading"
                    allow-clear
                  />
                </a-form-item>
                <a-form-item label="图片分类">
                  <a-auto-complete
                    v-model:value="urlMeta.category"
                    placeholder="选填，可选择或输入分类"
                    :options="categoryOptions.map((item) => ({ value: item }))"
                    :disabled="urlUploading"
                    allow-clear
                  />
                </a-form-item>
              </div>
              <a-form-item label="图片简介">
                <a-textarea
                  v-model:value="urlMeta.introduction"
                  placeholder="选填，最多 800 字"
                  :rows="3"
                  :maxlength="800"
                  show-count
                  :disabled="urlUploading"
                />
              </a-form-item>
              <a-form-item label="图片标签">
                <a-select
                  v-model:value="urlMeta.tags"
                  mode="tags"
                  placeholder="选填，可选择或输入标签"
                  :options="tagOptions.map((item) => ({ value: item, label: item }))"
                  :disabled="urlUploading"
                  allow-clear
                />
              </a-form-item>
              <p class="meta-hint">以上信息均为选填，未填写的内容将由后端自动解析</p>
            </a-form>

            <div class="upload-footer">
              <span class="file-hint">图片已加载，可填写信息后确认上传</span>
              <div class="file-actions">
                <a-button
                  type="primary"
                  :loading="urlUploading"
                  :disabled="urlPreviewing"
                  @click="handleUrlUpload"
                >
                  确认上传
                </a-button>
                <a-button :disabled="urlUploading" @click="clearUrlSelection">
                  重新选择
                </a-button>
              </div>
            </div>
          </div>
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

.url-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 180px;
  font-size: 13px;
  color: #8c8c8c;
}

.url-preview-area {
  margin-top: 20px;
}

.url-preview {
  display: block;
  max-width: 100%;
  max-height: 360px;
  margin: 0 auto 20px;
  object-fit: contain;
  border-radius: 8px;
}

.meta-form {
  margin-top: 20px;
  padding: 20px 20px 4px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.meta-hint {
  margin: 0 0 16px;
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

  .meta-form {
    padding: 16px 16px 0;
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
