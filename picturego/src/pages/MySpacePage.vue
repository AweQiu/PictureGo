<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { CloudUploadOutlined, DeleteOutlined, EditOutlined, SearchOutlined } from '@ant-design/icons-vue'
import {
  deletePicture,
  editPicture,
  listPictureTagCategory,
  listPictureVOByPage,
  uploadPicture,
  uploadPictureByUrl,
} from '../api/picture'
import { getSpaceVOById, listSpaceLevel, listSpaceVOByPage } from '../api/space'
import { useUserStore } from '../store/user'
import { formatFileSize } from '../utils/format'

const router = useRouter()
const userStore = useUserStore()

// ---------- 空间信息 ----------
const space = ref<SpaceVO | null>(null)
const spaceLoading = ref(false)
const levelList = ref<SpaceLevel[]>([])

const levelText = computed(() => {
  if (!space.value) return ''
  return levelList.value.find((level) => level.value === space.value?.spaceLevel)?.text ?? ''
})

const sizePercent = computed(() => {
  if (!space.value?.maxSize) return 0
  return Math.min(100, Math.round((space.value.totalSize / space.value.maxSize) * 100))
})

const countPercent = computed(() => {
  if (!space.value?.maxCount) return 0
  return Math.min(100, Math.round((space.value.totalCount / space.value.maxCount) * 100))
})

async function fetchMySpace() {
  const userId = userStore.loginUser?.id
  if (!userId) return
  spaceLoading.value = true
  try {
    const page = await listSpaceVOByPage({ userId, current: 1, pageSize: 1 })
    const own = page.records?.[0]
    if (!own) {
      space.value = null
      return
    }
    // 通过 /space/get/vo 获取空间完整信息
    space.value = await getSpaceVOById(own.id)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '获取私人空间失败')
  } finally {
    spaceLoading.value = false
  }
}

async function fetchLevels() {
  try {
    levelList.value = (await listSpaceLevel()) ?? []
  } catch (error) {
    console.error('获取空间级别失败', error)
  }
}

// ---------- 空间内图片 ----------
const pictureList = ref<PictureVO[]>([])
const listLoading = ref(false)
const total = ref(0)
const pagination = reactive({ current: 1, pageSize: 12 })
const searchText = ref('')

async function fetchPictures() {
  if (!space.value) return
  listLoading.value = true
  try {
    const page = await listPictureVOByPage({
      current: pagination.current,
      pageSize: pagination.pageSize,
      spaceId: space.value.id,
      searchText: searchText.value.trim() || undefined,
    })
    pictureList.value = page.records ?? []
    total.value = Number(page.total ?? 0)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载图片失败')
  } finally {
    listLoading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchPictures()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchPictures()
}

function openDetail(picture: PictureVO) {
  router.push({ name: 'pictureDetail', params: { id: picture.id } })
}

// ---------- 编辑图片 ----------
const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])
const editVisible = ref(false)
const editLoading = ref(false)
const editingId = ref<string | null>(null)
const editingPicture = ref<PictureVO | null>(null)
const editForm = reactive({
  name: '',
  introduction: '',
  category: undefined as string | undefined,
  tags: [] as string[],
})

async function fetchTagCategory() {
  try {
    const data = await listPictureTagCategory()
    categoryOptions.value = data.categories ?? []
    tagOptions.value = data.tags ?? []
  } catch (error) {
    console.error('获取分类标签失败', error)
  }
}

function openEdit(picture: PictureVO) {
  editingId.value = picture.id
  editingPicture.value = picture
  Object.assign(editForm, {
    name: picture.name ?? '',
    introduction: picture.introduction ?? '',
    category: picture.category || undefined,
    tags: [...(picture.tags ?? [])],
  })
  editVisible.value = true
}

async function handleEditSubmit() {
  if (!editingId.value) return
  editLoading.value = true
  try {
    // 先执行暂存的重新上传（请求附带图片 id，后端会沿用原图所在空间）
    if (stagedFile.value) {
      await uploadPicture(stagedFile.value, { id: editingId.value })
    } else if (stagedUrl.value) {
      await uploadPictureByUrl({ id: editingId.value, url: stagedUrl.value })
    }
    // 再保存图片信息，表单中的名称等字段覆盖重传时后端解析出的默认值
    await editPicture({
      id: editingId.value,
      name: editForm.name.trim() || undefined,
      introduction: editForm.introduction.trim(),
      category: editForm.category?.trim(),
      tags: editForm.tags.map((tag) => tag.trim()).filter(Boolean),
    })
    message.success('图片信息已更新')
    editVisible.value = false
    fetchPictures()
    fetchMySpace()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    editLoading.value = false
  }
}

// ---------- 重新上传（先暂存新图片，点击保存时才真正上传） ----------
const reuploadTab = ref<'file' | 'url'>('file')
const stagedFile = ref<File | null>(null)
const stagedFileUrl = ref('')
const stagedUrl = ref('')
const urlInput = ref('')
const urlChecking = ref(false)
let urlCheckToken = 0
let urlCheckTimer: ReturnType<typeof setTimeout> | null = null

const stagedPreviewUrl = computed(() => stagedFileUrl.value || stagedUrl.value)
const hasStaged = computed(() => !!stagedPreviewUrl.value)

function clearStaged() {
  if (stagedFileUrl.value) {
    URL.revokeObjectURL(stagedFileUrl.value)
  }
  stagedFile.value = null
  stagedFileUrl.value = ''
  stagedUrl.value = ''
  urlInput.value = ''
  urlChecking.value = false
  urlCheckToken += 1
  if (urlCheckTimer) {
    clearTimeout(urlCheckTimer)
    urlCheckTimer = null
  }
}

function handleStageFile(file: File) {
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片文件')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  clearStaged()
  stagedFile.value = file
  stagedFileUrl.value = URL.createObjectURL(file)
  message.success('已选择新图片，点击保存后生效')
  return false
}

function handleStageUrl() {
  const url = urlInput.value.trim()
  if (!url) {
    message.warning('请输入图片链接')
    return
  }
  if (!/^https?:\/\//i.test(url)) {
    message.warning('请输入 http:// 或 https:// 开头的图片链接')
    return
  }
  const token = ++urlCheckToken
  if (urlCheckTimer) {
    clearTimeout(urlCheckTimer)
    urlCheckTimer = null
  }
  urlChecking.value = true
  const image = new Image()
  image.referrerPolicy = 'no-referrer'
  image.onload = () => {
    if (token !== urlCheckToken) return
    if (urlCheckTimer) {
      clearTimeout(urlCheckTimer)
      urlCheckTimer = null
    }
    urlChecking.value = false
    const keepInput = url
    clearStaged()
    urlInput.value = keepInput
    stagedUrl.value = keepInput
    message.success('图片链接已就绪，点击保存后生效')
  }
  image.onerror = () => {
    if (token !== urlCheckToken) return
    urlChecking.value = false
    message.error('图片加载失败，请检查链接是否有效或图片是否允许外链访问')
  }
  urlCheckTimer = setTimeout(() => {
    if (token !== urlCheckToken) return
    urlChecking.value = false
    message.error('图片加载超时，请检查链接后重试')
  }, 15000)
  image.src = url
}

watch(editVisible, (visible) => {
  if (!visible) clearStaged()
})

onBeforeUnmount(clearStaged)

// ---------- 删除图片 ----------
function confirmDelete(picture: PictureVO) {
  Modal.confirm({
    title: '删除图片',
    content: `确定删除图片「${picture.name}」吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deletePicture({ id: picture.id })
        message.success('图片已删除')
        if (pictureList.value.length === 1 && pagination.current > 1) {
          pagination.current -= 1
        }
        fetchPictures()
        fetchMySpace()
      } catch (error) {
        message.error(error instanceof Error ? error.message : '删除失败')
      }
    },
  })
}

onMounted(async () => {
  fetchLevels()
  fetchTagCategory()
  await fetchMySpace()
  fetchPictures()
})
</script>

<template>
  <a-spin :spinning="spaceLoading">
    <a-card v-if="space" :bordered="false" class="space-card">
      <div class="space-header">
        <div class="space-title">
          <span class="space-name">{{ space.spaceName || '我的空间' }}</span>
          <a-tag v-if="levelText" color="blue">{{ levelText }}</a-tag>
        </div>
        <a-button type="primary" @click="router.push({ name: 'pictureUpload' })">
          上传图片
        </a-button>
      </div>
      <div class="space-usage">
        <div class="usage-item">
          <span class="usage-label">容量</span>
          <a-progress :percent="sizePercent" size="small" class="usage-bar" />
          <span class="usage-text">
            {{ formatFileSize(space.totalSize) }} / {{ formatFileSize(space.maxSize) }}
          </span>
        </div>
        <div class="usage-item">
          <span class="usage-label">数量</span>
          <a-progress :percent="countPercent" size="small" class="usage-bar" />
          <span class="usage-text">{{ space.totalCount }} / {{ space.maxCount }} 张</span>
        </div>
      </div>
    </a-card>

    <a-card v-if="space" :bordered="false" class="picture-card">
      <div class="list-toolbar">
        <a-input
          v-model:value="searchText"
          class="toolbar-search"
          placeholder="搜索空间内图片名称 / 简介"
          allow-clear
          @press-enter="handleSearch"
        >
          <template #prefix>
            <SearchOutlined />
          </template>
        </a-input>
        <a-button type="primary" @click="handleSearch">搜索</a-button>
      </div>

      <a-spin :spinning="listLoading">
        <div v-if="pictureList.length > 0" class="gallery-grid">
          <div v-for="picture in pictureList" :key="picture.id" class="gallery-card">
            <div
              class="gallery-cover"
              role="button"
              tabindex="0"
              @click="openDetail(picture)"
              @keydown.enter="openDetail(picture)"
            >
              <img :src="picture.thumbnailUrl || picture.url" :alt="picture.name" loading="lazy" />
              <div class="gallery-overlay">
                <span class="gallery-name" :title="picture.name">{{ picture.name }}</span>
                <div v-if="picture.tags?.length" class="gallery-tags">
                  <span v-for="tag in picture.tags" :key="tag" class="tag-chip">{{ tag }}</span>
                </div>
              </div>
            </div>
            <div class="gallery-actions">
              <a-button type="text" size="small" class="action-btn" @click.stop="openEdit(picture)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button
                type="text"
                size="small"
                danger
                class="action-btn"
                @click.stop="confirmDelete(picture)"
              >
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </div>
          </div>
        </div>
        <a-empty v-else-if="!listLoading" class="picture-empty" description="空间内暂无图片" />
      </a-spin>

      <div v-if="total > 0" class="list-pagination">
        <a-pagination
          :current="pagination.current"
          :page-size="pagination.pageSize"
          :total="total"
          show-quick-jumper
          @change="handlePageChange"
        />
      </div>
    </a-card>

    <a-card v-else-if="!spaceLoading" :bordered="false" class="space-card">
      <a-empty description="你还没有私人空间">
        <a-button type="primary" @click="router.push({ name: 'spaceCreate' })">
          创建空间
        </a-button>
      </a-empty>
    </a-card>
  </a-spin>

  <a-modal
    v-model:open="editVisible"
    title="编辑图片"
    ok-text="保存"
    cancel-text="取消"
    width="880px"
    :confirm-loading="editLoading"
    centered
    @ok="handleEditSubmit"
  >
    <div class="edit-body">
      <div v-if="editingPicture" class="edit-preview">
        <a-image
          :src="stagedPreviewUrl || editingPicture.thumbnailUrl || editingPicture.url"
          :preview="{ src: stagedPreviewUrl || editingPicture.url }"
          :alt="editingPicture.name"
          class="edit-image"
        />
        <p v-if="hasStaged" class="preview-hint staged-hint">
          新图片将在保存后替换当前图片
          <a-button type="link" size="small" class="staged-undo" @click="clearStaged">撤销</a-button>
        </p>
        <p v-else class="preview-hint">点击图片查看大图</p>
        <a-tabs v-model:activeKey="reuploadTab" size="small" class="reupload-tabs">
          <a-tab-pane key="file" tab="文件重传">
            <a-upload-dragger
              accept="image/*"
              :show-upload-list="false"
              :before-upload="handleStageFile"
              :disabled="editLoading"
              class="reupload-dragger"
            >
              <p class="reupload-icon"><CloudUploadOutlined /></p>
              <p class="reupload-text">点击或拖拽选择新图片</p>
              <p class="reupload-hint">不超过 5MB，保存后替换当前图片</p>
            </a-upload-dragger>
          </a-tab-pane>
          <a-tab-pane key="url" tab="URL 重传">
            <div class="reupload-url">
              <a-input
                v-model:value="urlInput"
                placeholder="输入 http:// 或 https:// 开头的图片链接"
                allow-clear
                @press-enter="handleStageUrl"
              />
              <a-button :loading="urlChecking" :disabled="editLoading" @click="handleStageUrl">
                加载
              </a-button>
            </div>
            <p class="reupload-hint url-hint">链接加载成功后，保存时替换当前图片</p>
          </a-tab-pane>
        </a-tabs>
      </div>
      <a-form layout="vertical" class="edit-form-area">
      <a-form-item label="图片名称">
        <a-input v-model:value="editForm.name" placeholder="选填，留空则保持默认名称" allow-clear />
      </a-form-item>
      <a-form-item label="图片分类">
        <a-auto-complete
          v-model:value="editForm.category"
          placeholder="选填，可选择或输入分类"
          :options="categoryOptions.map((item) => ({ value: item }))"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="图片简介">
        <a-textarea
          v-model:value="editForm.introduction"
          placeholder="选填，最多 800 字"
          :rows="3"
          :maxlength="800"
          show-count
        />
      </a-form-item>
      <a-form-item label="图片标签">
        <a-select
          v-model:value="editForm.tags"
          mode="tags"
          placeholder="选填，可选择或输入标签"
          :options="tagOptions.map((item) => ({ value: item, label: item }))"
          allow-clear
        />
      </a-form-item>
      </a-form>
    </div>
  </a-modal>
</template>

<style scoped>
.space-card,
.picture-card {
  border-radius: 12px;
}

.picture-card {
  margin-top: 16px;
}

.space-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.space-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.space-name {
  overflow: hidden;
  font-size: 18px;
  font-weight: 600;
  color: #1f2d3d;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.space-usage {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 48px;
  margin-top: 16px;
}

.usage-item {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 10px;
  min-width: 260px;
}

.usage-label {
  flex-shrink: 0;
  font-size: 13px;
  color: #5e6d82;
}

.usage-bar {
  flex: 1;
  margin: 0;
}

.usage-text {
  flex-shrink: 0;
  font-size: 12px;
  color: #8c8c8c;
  white-space: nowrap;
}

.list-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-search {
  max-width: 360px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

/* 与首页一致的图片卡片 */
.gallery-card {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.gallery-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.gallery-card:focus-within {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.gallery-cover {
  position: relative;
  aspect-ratio: 3 / 4;
  background: #f2f4f7;
  cursor: pointer;
}

.gallery-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.gallery-card:hover .gallery-cover img {
  transform: scale(1.04);
}

.gallery-overlay {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 32px 10px 10px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.55) 100%);
  pointer-events: none;
  transition: padding-bottom 0.25s ease;
}

/* 悬停时为底部上浮的操作按钮让出空间，避免遮挡名称/标签 */
.gallery-card:hover .gallery-overlay,
.gallery-card:focus-within .gallery-overlay {
  padding-bottom: 52px;
}

.gallery-name {
  overflow: hidden;
  font-size: 13px;
  line-height: 1.3;
  color: #fff;
  text-overflow: ellipsis;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.2s;
}

.gallery-card:hover .gallery-name,
.gallery-card:focus-within .gallery-name {
  opacity: 1;
}

.gallery-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-chip {
  padding: 0 7px;
  font-size: 11px;
  line-height: 18px;
  color: #fff;
  background: rgba(255, 255, 255, 0.24);
  border-radius: 999px;
  backdrop-filter: blur(4px);
}

/* 编辑/删除：默认沉到卡片底部之外，鼠标移入时从底部上浮，移出下沉隐藏 */
.gallery-actions {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  justify-content: flex-end;
  gap: 2px;
  padding: 6px 8px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.72) 55%);
  opacity: 0;
  transform: translateY(100%);
  transition:
    transform 0.25s ease,
    opacity 0.2s ease;
}

.gallery-card:hover .gallery-actions,
.gallery-card:focus-within .gallery-actions {
  opacity: 1;
  transform: translateY(0);
}

.gallery-actions .action-btn {
  color: #fff;
}

.gallery-actions .action-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
}

.gallery-actions .action-btn.ant-btn-dangerous {
  color: #ffa39e;
}

.gallery-actions .action-btn.ant-btn-dangerous:hover {
  color: #ffccc7;
  background: rgba(255, 77, 79, 0.3);
}

/* 触屏设备没有 hover，直接展示操作按钮和名称 */
@media (hover: none) {
  .gallery-overlay {
    padding-bottom: 52px;
  }

  .gallery-name,
  .gallery-actions {
    opacity: 1;
  }

  .gallery-actions {
    transform: translateY(0);
  }
}

.picture-empty {
  margin: 60px 0;
}

.list-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.edit-body {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.edit-preview {
  flex: 0 0 320px;
  min-width: 0;
}

.edit-image {
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  background: #f2f4f7;
  border-radius: 8px;
}

.edit-image :deep(img) {
  max-height: 320px;
  object-fit: contain;
}

.preview-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #bfbfbf;
  text-align: center;
}

.staged-hint {
  color: #fa8c16;
}

.staged-undo {
  padding: 0;
  height: auto;
  font-size: 12px;
}

.reupload-tabs {
  margin-top: 10px;
}

.reupload-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}

.reupload-dragger {
  display: block;
}

.reupload-dragger :deep(.ant-upload-drag) {
  padding: 10px 0;
}

.reupload-icon {
  margin: 0;
  font-size: 22px;
  color: #1677ff;
}

.reupload-text {
  margin: 4px 0 0;
  font-size: 12px;
  color: #5e6d82;
}

.reupload-hint {
  margin: 2px 0 0;
  font-size: 12px;
  color: #bfbfbf;
}

.reupload-url {
  display: flex;
  gap: 8px;
}

.url-hint {
  margin-top: 6px;
}

.edit-form-area {
  flex: 1;
  min-width: 0;
}

@media (max-width: 768px) {
  .edit-body {
    flex-direction: column;
  }

  .edit-preview {
    flex: none;
    width: 100%;
  }
}
</style>
