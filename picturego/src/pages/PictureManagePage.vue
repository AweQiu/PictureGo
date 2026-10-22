<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  AuditOutlined,
  DeleteOutlined,
  EditOutlined,
  FileSearchOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import {
  deletePicture,
  listPictureByPage,
  listPictureTagCategory,
  reviewPicture,
  updatePicture,
} from '../api/picture'
import { getUserVOById } from '../api/user'
import {
  PIC_REVIEW_STATUS,
  PIC_REVIEW_STATUS_COLOR,
  PIC_REVIEW_STATUS_OPTIONS,
  PIC_REVIEW_STATUS_TEXT,
} from '../constants/enums'
import { formatDateTime, formatFileSize } from '../utils/format'

const router = useRouter()

// ---------- 查询与分页 ----------
const queryForm = reactive({
  searchText: '',
  category: undefined as string | undefined,
  tags: [] as string[],
  reviewStatus: undefined as number | undefined,
  userId: undefined as number | undefined,
})

const pictureList = ref<Picture[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({
  current: 1,
  pageSize: 10,
})

/** 分类 / 标签候选值 */
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

/** 审核人 ID -> 展示名（后端 Picture 只返回 reviewerId，这里按需查出用户名） */
const reviewerNames = reactive<Record<string, string>>({})

async function resolveReviewerNames(records: Picture[]) {
  const ids = Array.from(
    new Set(records.map((record) => record.reviewerId).filter(Boolean)),
  )
  await Promise.all(
    ids.map(async (id) => {
      if (id in reviewerNames) return
      try {
        const user = await getUserVOById(id)
        reviewerNames[id] = user.userName || user.userAccount || id
      } catch {
        reviewerNames[id] = ''
      }
    }),
  )
}

async function fetchData() {
  loading.value = true
  try {
    const page = await listPictureByPage({
      current: pagination.current,
      pageSize: pagination.pageSize,
      searchText: queryForm.searchText || undefined,
      category: queryForm.category || undefined,
      tags: queryForm.tags.length > 0 ? queryForm.tags : undefined,
      reviewStatus: queryForm.reviewStatus,
      userId: queryForm.userId,
    })
    const records = page.records ?? []
    pictureList.value = records
    total.value = Number(page.total ?? 0)
    await resolveReviewerNames(records)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '查询图片失败')
  } finally {
    loading.value = false
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
  queryForm.reviewStatus = undefined
  queryForm.userId = undefined
  handleSearch()
}

function handleTableChange(pager: TablePaginationConfig) {
  pagination.current = pager.current ?? 1
  pagination.pageSize = pager.pageSize ?? 10
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchTagCategory()
})

// ---------- 工具 ----------
function parseTags(tags: string | string[]): string[] {
  if (Array.isArray(tags)) return tags
  try {
    const parsed = JSON.parse(tags)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

// ---------- 表格 ----------
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 130, ellipsis: true },
  { title: '图片', dataIndex: 'url', key: 'url', width: 90 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 150, ellipsis: true },
  {
    title: '简介',
    dataIndex: 'introduction',
    key: 'introduction',
    width: 170,
    ellipsis: true,
  },
  { title: '分类', dataIndex: 'category', key: 'category', width: 90, ellipsis: true },
  { title: '标签', dataIndex: 'tags', key: 'tags', width: 130 },
  { title: '图片信息', key: 'picInfo', width: 120 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 120, ellipsis: true },
  { title: '审核结果', key: 'review', width: 220 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '编辑时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 250, fixed: 'right' as const },
]

// ---------- 编辑 ----------
const editVisible = ref(false)
const editLoading = ref(false)
const editingId = ref<string | null>(null)
const editForm = reactive({
  name: '',
  introduction: '',
  category: undefined as string | undefined,
  tags: [] as string[],
})

function openEdit(record: Picture) {
  editingId.value = record.id
  Object.assign(editForm, {
    name: record.name,
    introduction: record.introduction,
    category: record.category || undefined,
    tags: parseTags(record.tags),
  })
  editVisible.value = true
}

async function handleEditSubmit() {
  if (!editForm.name.trim()) {
    message.warning('图片名称不能为空')
    return
  }
  editLoading.value = true
  try {
    await updatePicture({
      id: editingId.value!,
      name: editForm.name.trim(),
      introduction: editForm.introduction.trim() || undefined,
      category: editForm.category || undefined,
      tags: editForm.tags,
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

// ---------- 审核 ----------
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewForm = reactive({
  id: '' as string,
  name: '',
  reviewStatus: PIC_REVIEW_STATUS.PASS as number,
  reviewMessage: '',
})

function openReview(record: Picture) {
  Object.assign(reviewForm, {
    id: record.id,
    name: record.name,
    reviewStatus: PIC_REVIEW_STATUS.PASS,
    reviewMessage: '',
  })
  reviewVisible.value = true
}

const reviewNeedMessage = computed(
  () => reviewForm.reviewStatus === PIC_REVIEW_STATUS.REJECT,
)

async function handleReviewSubmit() {
  if (reviewNeedMessage.value && !reviewForm.reviewMessage.trim()) {
    message.warning('拒绝时请填写审核意见')
    return
  }
  reviewLoading.value = true
  try {
    await reviewPicture({
      id: reviewForm.id,
      reviewStatus: reviewForm.reviewStatus,
      reviewMessage: reviewForm.reviewMessage.trim() || undefined,
    })
    message.success('审核完成')
    reviewVisible.value = false
    fetchData()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '审核失败')
  } finally {
    reviewLoading.value = false
  }
}

// ---------- 删除 ----------
async function handleDelete(record: Picture) {
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
</script>

<template>
  <a-card :bordered="false" title="图片管理" class="picture-manage-card">
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
      <a-select
        v-model:value="queryForm.reviewStatus"
        class="toolbar-select"
        placeholder="按审核状态筛选"
        allow-clear
        :options="PIC_REVIEW_STATUS_OPTIONS"
      />
      <a-input-number
        v-model:value="queryForm.userId"
        class="toolbar-user-id"
        placeholder="用户ID"
        :min="1"
        :controls="false"
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
        pageSizeOptions: ['10', '20', '50'],
        showTotal: (count: number) => `共 ${count} 条`,
      }"
      :scroll="{ x: 1810 }"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'url'">
          <a-image :src="record.url" :width="72" :height="50" style="object-fit: cover" />
        </template>
        <template v-else-if="column.key === 'category'">
          <span>{{ record.category || '—' }}</span>
        </template>
        <template v-else-if="column.key === 'tags'">
          <template v-if="parseTags(record.tags).length > 0">
            <a-tag v-for="tag in parseTags(record.tags)" :key="tag">{{ tag }}</a-tag>
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
        <template v-else-if="column.key === 'review'">
          <a-tag :color="PIC_REVIEW_STATUS_COLOR[record.reviewStatus] ?? 'default'">
            {{ PIC_REVIEW_STATUS_TEXT[record.reviewStatus] ?? record.reviewStatus }}
          </a-tag>
          <div v-if="record.reviewerId" class="review-meta">
            审核人：{{ reviewerNames[record.reviewerId] || record.reviewerId }}
          </div>
          <div v-if="record.reviewTime" class="review-meta">
            审核时间：{{ formatDateTime(record.reviewTime) }}
          </div>
          <div
            v-if="record.reviewMessage"
            class="review-message"
            :class="{ 'is-reject': record.reviewStatus === PIC_REVIEW_STATUS.REJECT }"
            :title="record.reviewMessage"
          >
            {{ record.reviewMessage }}
          </div>
        </template>
        <template v-else-if="column.key === 'createTime'">
          <span>{{ formatDateTime(record.createTime) }}</span>
        </template>
        <template v-else-if="column.key === 'updateTime'">
          <span>{{ formatDateTime(record.updateTime) }}</span>
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
            <a-button
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS.PASS"
              type="link"
              size="small"
              @click="openReview(record)"
            >
              <template #icon><AuditOutlined /></template>
              审核
            </a-button>
            <a-button type="link" size="small" @click="openEdit(record)">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
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
  </a-card>

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

  <a-modal
    v-model:open="reviewVisible"
    :title="`审核图片「${reviewForm.name}」`"
    ok-text="提交"
    cancel-text="取消"
    :confirm-loading="reviewLoading"
    centered
    @ok="handleReviewSubmit"
  >
    <a-form layout="vertical">
      <a-form-item label="审核结果" required>
        <a-radio-group v-model:value="reviewForm.reviewStatus">
          <a-radio :value="PIC_REVIEW_STATUS.PASS">通过</a-radio>
          <a-radio :value="PIC_REVIEW_STATUS.REJECT">拒绝</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="审核意见" :required="reviewNeedMessage">
        <a-textarea
          v-model:value="reviewForm.reviewMessage"
          :placeholder="reviewNeedMessage ? '请填写拒绝原因' : '选填'"
          :rows="3"
          :maxlength="200"
          show-count
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.picture-manage-card {
  border-radius: 12px;
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

.toolbar-user-id {
  width: 110px;
}

.toolbar-refresh {
  margin-left: auto;
}

.pic-info {
  font-size: 13px;
  line-height: 1.6;
}

.pic-info-sub {
  color: #8c8c8c;
}

.review-meta {
  margin-top: 4px;
  font-size: 12px;
  line-height: 18px;
  color: #8c8c8c;
  white-space: nowrap;
}

.review-message {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-message.is-reject {
  color: #cf1322;
}
</style>
