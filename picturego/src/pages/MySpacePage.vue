<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, SearchOutlined } from '@ant-design/icons-vue'
import {
  deletePicture,
  editPicture,
  listPictureTagCategory,
  listPictureVOByPage,
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
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    editLoading.value = false
  }
}

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
        <div v-if="pictureList.length > 0" class="picture-grid">
          <div v-for="picture in pictureList" :key="picture.id" class="picture-item">
            <div class="picture-cover" role="button" tabindex="0" @click="openDetail(picture)" @keydown.enter="openDetail(picture)">
              <img :src="picture.thumbnailUrl || picture.url" :alt="picture.name" loading="lazy" />
            </div>
            <div class="picture-meta">
              <span class="picture-name" :title="picture.name">{{ picture.name }}</span>
              <div class="picture-actions">
                <a-button type="link" size="small" @click="openEdit(picture)">
                  <template #icon><EditOutlined /></template>
                  编辑
                </a-button>
                <a-button type="link" size="small" danger @click="confirmDelete(picture)">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </div>
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
      <a-empty description="你还没有私人空间，可联系管理员创建" />
    </a-card>
  </a-spin>

  <a-modal
    v-model:open="editVisible"
    title="编辑图片"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="editLoading"
    centered
    @ok="handleEditSubmit"
  >
    <a-form layout="vertical">
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

.picture-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.picture-item {
  overflow: hidden;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fff;
}

.picture-cover {
  aspect-ratio: 4 / 3;
  background: #f2f4f7;
  cursor: pointer;
}

.picture-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.picture-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
}

.picture-name {
  flex: 1;
  overflow: hidden;
  font-size: 13px;
  color: #1f2d3d;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.picture-actions {
  display: flex;
  flex-shrink: 0;
}

.picture-empty {
  margin: 60px 0;
}

.list-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
