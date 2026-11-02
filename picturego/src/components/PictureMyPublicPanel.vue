<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CloudUploadOutlined,
  DeleteOutlined,
  EditOutlined,
  FileSearchOutlined,
  InfoCircleOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import {
  deletePicture,
  editPicture,
  listPictureTagCategory,
  listPictureVOByPage,
  uploadPicture,
} from '../api/picture'
import { useUserStore } from '../store/user'
import { formatDateTime, formatFileSize } from '../utils/format'

const router = useRouter()
const userStore = useUserStore()

const pictureList = ref<PictureVO[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({ current: 1, pageSize: 12 })

const queryForm = reactive({
  searchText: '',
  category: undefined as string | undefined,
  tags: [] as string[],
})

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

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  queryForm.searchText = ''
  queryForm.category = undefined
  queryForm.tags = []
  handleSearch()
}

function handleTableChange(pager: TablePaginationConfig) {
  pagination.current = pager.current ?? 1
  pagination.pageSize = pager.pageSize ?? 12
  fetchData()
}

/** 查询当前用户发布在公共图库的图片（后端强制只返回审核通过的公共图片） */
async function fetchData() {
  const userId = userStore.loginUser?.id
  if (!userId) return
  loading.value = true
  try {
    const page = await listPictureVOByPage({
      userId,
      current: pagination.current,
      pageSize: pagination.pageSize,
      searchText: queryForm.searchText.trim() || undefined,
      category: queryForm.category || undefined,
      tags: queryForm.tags.length > 0 ? queryForm.tags : undefined,
    })
    pictureList.value = page.records ?? []
    total.value = Number(page.total ?? 0)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载图片失败')
  } finally {
    loading.value = false
  }
}

// ---------- 编辑图片信息 ----------
const editVisible = ref(false)
const editLoading = ref(false)
const editingId = ref<string | null>(null)
const editForm = reactive({
  name: '',
  introduction: '',
  category: undefined as string | undefined,
  tags: [] as string[],
})

function openEdit(record: PictureVO) {
  editingId.value = record.id
  Object.assign(editForm, {
    name: record.name ?? '',
    introduction: record.introduction ?? '',
    category: record.category || undefined,
    tags: [...(record.tags ?? [])],
  })
  editVisible.value = true
}

async function handleEditSubmit() {
  if (!editingId.value) return
  const name = editForm.name.trim()
  if (!name) {
    message.warning('请输入图片名称')
    return
  }
  editLoading.value = true
  try {
    await editPicture({
      id: editingId.value,
      name,
      introduction: editForm.introduction.trim(),
      category: editForm.category?.trim(),
      tags: editForm.tags.map((tag) => tag.trim()).filter(Boolean),
    })
    message.success('图片信息已更新')
    editVisible.value = false
    fetchData()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    editLoading.value = false
  }
}

// ---------- 重新上传（替换图片文件，沿用原图所在空间） ----------
const reuploadingId = ref<string | null>(null)

function handleReupload(file: File, record: PictureVO) {
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片文件')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  void doReupload(file, record)
  return false
}

/** 为表格每一行生成一个携带当前记录的 before-upload 处理函数 */
function createReuploadHandler(record: PictureVO) {
  return (file: File) => handleReupload(file, record)
}

async function doReupload(file: File, record: PictureVO) {
  reuploadingId.value = record.id
  try {
    await uploadPicture(file, { id: record.id })
    message.success(`图片「${record.name}」已重新上传`)
    fetchData()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '重新上传失败')
  } finally {
    reuploadingId.value = null
  }
}

// ---------- 删除 ----------
async function handleDelete(record: PictureVO) {
  try {
    await deletePicture({ id: record.id })
    message.success(`已删除图片「${record.name}」`)
    if (pictureList.value.length === 1 && pagination.current > 1) {
      pagination.current -= 1
    }
    fetchData()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '删除失败')
  }
}

// ---------- 表格 ----------
const columns = [
  { title: '图片', dataIndex: 'url', key: 'url', width: 90 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 160, ellipsis: true },
  {
    title: '简介',
    dataIndex: 'introduction',
    key: 'introduction',
    width: 180,
    ellipsis: true,
  },
  { title: '分类', dataIndex: 'category', key: 'category', width: 100, ellipsis: true },
  { title: '标签', dataIndex: 'tags', key: 'tags', width: 150 },
  { title: '图片信息', key: 'picInfo', width: 130 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 320, fixed: 'right' as const },
]

onMounted(() => {
  fetchTagCategory()
  fetchData()
})
</script>

<template>
  <div class="my-public-panel">
    <p class="panel-hint">
      <InfoCircleOutlined />
      <span>这里管理你发布到公共图库的图片，目前仅展示审核通过的图片。</span>
    </p>

    <div class="toolbar">
      <a-input
        v-model:value="queryForm.searchText"
        class="toolbar-keyword"
        placeholder="搜索图片名称 / 简介"
        allow-clear
        @press-enter="handleSearch"
      >
        <template #prefix>
          <SearchOutlined />
        </template>
      </a-input>
      <a-select
        v-model:value="queryForm.category"
        class="toolbar-select"
        placeholder="按分类筛选"
        allow-clear
        :options="categoryOptions.map((item) => ({ value: item, label: item }))"
      />
      <a-select
        v-model:value="queryForm.tags"
        class="toolbar-tags"
        placeholder="按标签筛选"
        mode="multiple"
        allow-clear
        :max-tag-count="1"
        :options="tagOptions.map((item) => ({ value: item, label: item }))"
      />
      <a-button type="primary" @click="handleSearch">搜索</a-button>
      <a-button @click="handleReset">重置</a-button>
      <a-button class="toolbar-refresh" @click="fetchData">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="pictureList"
      :loading="loading"
      :pagination="{
        current: pagination.current,
        pageSize: pagination.pageSize,
        total,
        showSizeChanger: true,
        pageSizeOptions: ['12', '16', '20'],
        showTotal: (count: number) => `共 ${count} 条`,
      }"
      :scroll="{ x: 1300 }"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'url'">
          <img
            :src="record.thumbnailUrl || record.url"
            :alt="record.name"
            loading="lazy"
            class="pic-thumb"
          />
        </template>
        <template v-else-if="column.key === 'tags'">
          <template v-if="record.tags?.length">
            <a-tag v-for="tag in record.tags" :key="tag">{{ tag }}</a-tag>
          </template>
          <span v-else>—</span>
        </template>
        <template v-else-if="column.key === 'picInfo'">
          <div class="pic-info">
            <div>{{ record.picWidth }} × {{ record.picHeight }}</div>
            <div class="pic-info-sub">
              {{ formatFileSize(record.picSize) }} / {{ record.picFormat || '—' }}
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'createTime'">
          <span>{{ formatDateTime(record.createTime) }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space :size="0">
            <a-button
              type="link"
              size="small"
              @click="router.push({ name: 'pictureDetail', params: { id: record.id } })"
            >
              <template #icon><FileSearchOutlined /></template>
              详情
            </a-button>
            <a-button type="link" size="small" @click="openEdit(record)">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
            <a-upload
              :show-upload-list="false"
              :before-upload="createReuploadHandler(record)"
              :disabled="reuploadingId === record.id"
            >
              <a-button
                type="link"
                size="small"
                :loading="reuploadingId === record.id"
              >
                <template #icon><CloudUploadOutlined /></template>
                重新上传
              </a-button>
            </a-upload>
            <a-popconfirm
              title="确定删除该图片吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(record)"
            >
              <a-button type="link" size="small" danger>
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>

  <a-modal
    v-model:open="editVisible"
    title="编辑图片信息"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="editLoading"
    centered
    @ok="handleEditSubmit"
  >
    <a-form layout="vertical">
      <a-form-item label="名称" required>
        <a-input v-model:value="editForm.name" placeholder="请输入图片名称" />
      </a-form-item>
      <a-form-item label="简介">
        <a-textarea
          v-model:value="editForm.introduction"
          placeholder="请输入图片简介"
          :rows="3"
          :maxlength="200"
          show-count
        />
      </a-form-item>
      <a-form-item label="分类">
        <a-select
          v-model:value="editForm.category"
          placeholder="选择或输入分类"
          allow-clear
          show-search
          :options="categoryOptions.map((item) => ({ value: item, label: item }))"
        />
      </a-form-item>
      <a-form-item label="标签">
        <a-select
          v-model:value="editForm.tags"
          placeholder="选择或输入标签"
          mode="tags"
          allow-clear
          :options="tagOptions.map((item) => ({ value: item, label: item }))"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.panel-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 12px;
  font-size: 12px;
  color: #8c8c8c;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-keyword {
  width: 220px;
}

.toolbar-select {
  width: 150px;
}

.toolbar-tags {
  width: 200px;
}

.toolbar-refresh {
  margin-left: auto;
}

.pic-thumb {
  width: 72px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
}

.pic-info {
  font-size: 13px;
  line-height: 1.6;
}

.pic-info-sub {
  color: #8c8c8c;
}
</style>
