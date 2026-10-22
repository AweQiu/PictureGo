<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CloudUploadOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
} from '@ant-design/icons-vue'
import {
  deletePicture,
  getPictureVOById,
  listPictureTagCategory,
  updatePicture,
  uploadPicture,
} from '../api/picture'
import UserAvatar from '../components/UserAvatar.vue'
import { useUserStore } from '../store/user'
import { formatDateTime, formatFileSize } from '../utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const pictureId = String(route.params.id ?? '')
const picture = ref<PictureVO | null>(null)
const loading = ref(false)
const loadFailed = ref(false)

/** 编辑 / 删除仅管理员可见 */
const canManage = computed(() => userStore.isAdmin)

/** 替换图片文件：管理员或图片作者本人可操作（与后端权限一致） */
const canReupload = computed(
  () =>
    canManage.value ||
    (!!picture.value && picture.value.userId === userStore.loginUser?.id),
)

async function fetchDetail() {
  loading.value = true
  loadFailed.value = false
  try {
    picture.value = await getPictureVOById(pictureId)
  } catch (error) {
    loadFailed.value = true
    message.error(error instanceof Error ? error.message : '加载图片详情失败')
  } finally {
    loading.value = false
  }
}

// ---------- 下载 ----------
const downloading = ref(false)

async function handleDownload() {
  const target = picture.value
  if (!target) return
  downloading.value = true
  try {
    const response = await fetch(target.url)
    if (!response.ok) throw new Error('下载失败')
    const blob = await response.blob()
    const ext = target.picFormat ? `.${target.picFormat.toLowerCase()}` : ''
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `${target.name || 'picture'}${ext}`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(link.href)
    message.success('开始下载')
  } catch {
    // 图片源跨域等原因无法直接下载时，退化为新窗口打开原图
    window.open(target.url, '_blank')
    message.info('已在新窗口打开原图，可右键另存为')
  } finally {
    downloading.value = false
  }
}

// ---------- 重新上传（替换图片文件） ----------
const reuploadVisible = ref(false)
const reuploading = ref(false)

async function handleReupload(file: File) {
  const target = picture.value
  if (!target) return false
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片文件')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  reuploading.value = true
  try {
    await uploadPicture(file, target.id)
    message.success('图片已重新上传')
    reuploadVisible.value = false
    fetchDetail()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '重新上传失败')
  } finally {
    reuploading.value = false
  }
  return false
}

// ---------- 编辑（管理员） ----------
const editVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  name: '',
  introduction: '',
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

function openEdit() {
  const target = picture.value
  if (!target) return
  Object.assign(editForm, {
    name: target.name ?? '',
    introduction: target.introduction ?? '',
    category: target.category || undefined,
    tags: target.tags ?? [],
  })
  editVisible.value = true
}

async function handleEditSubmit() {
  const target = picture.value
  if (!target) return
  if (!editForm.name.trim()) {
    message.warning('图片名称不能为空')
    return
  }
  editLoading.value = true
  try {
    await updatePicture({
      id: target.id,
      name: editForm.name.trim(),
      introduction: editForm.introduction.trim() || undefined,
      category: editForm.category || undefined,
      tags: editForm.tags,
    })
    message.success('图片信息已更新')
    editVisible.value = false
    fetchDetail()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    editLoading.value = false
  }
}

// ---------- 删除（管理员） ----------
const deleteLoading = ref(false)

async function handleDelete() {
  const target = picture.value
  if (!target) return
  deleteLoading.value = true
  try {
    await deletePicture({ id: target.id })
    message.success(`已删除图片「${target.name}」`)
    router.back()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '删除失败')
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  fetchDetail()
  fetchTagCategory()
})
</script>

<template>
  <a-spin :spinning="loading">
    <div v-if="picture" class="detail-page">
      <section class="preview-card">
        <h3 class="card-title">图片预览</h3>
        <div class="preview-body">
          <a-image :src="picture.url" :alt="picture.name" class="preview-image" />
        </div>
      </section>

      <section class="info-card">
        <h3 class="card-title">图片信息</h3>
        <ul class="info-list">
          <li class="info-item">
            <span class="info-label">作者</span>
            <span class="info-value author">
              <UserAvatar :src="picture.userVO?.userAvatar" :size="24" />
              <span>{{ picture.userVO?.userName || picture.userVO?.userAccount || '—' }}</span>
            </span>
          </li>
          <li class="info-item">
            <span class="info-label">名称</span>
            <span class="info-value">{{ picture.name || '—' }}</span>
          </li>
          <li class="info-item">
            <span class="info-label">简介</span>
            <span class="info-value">{{ picture.introduction || '—' }}</span>
          </li>
          <li class="info-item">
            <span class="info-label">分类</span>
            <span class="info-value">{{ picture.category || '—' }}</span>
          </li>
          <li class="info-item">
            <span class="info-label">标签</span>
            <span class="info-value">
              <template v-if="picture.tags?.length">
                <a-tag v-for="tag in picture.tags" :key="tag">{{ tag }}</a-tag>
              </template>
              <template v-else>—</template>
            </span>
          </li>
          <li class="info-item">
            <span class="info-label">格式</span>
            <span class="info-value">{{ picture.picFormat || '—' }}</span>
          </li>
          <li class="info-item">
            <span class="info-label">宽度</span>
            <span class="info-value">{{ picture.picWidth || '—' }}</span>
          </li>
          <li class="info-item">
            <span class="info-label">高度</span>
            <span class="info-value">{{ picture.picHeight || '—' }}</span>
          </li>
          <li class="info-item">
            <span class="info-label">宽高比</span>
            <span class="info-value">{{ picture.picScale || '—' }}</span>
          </li>
          <li class="info-item">
            <span class="info-label">大小</span>
            <span class="info-value">{{ formatFileSize(picture.picSize) }}</span>
          </li>
          <li class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatDateTime(picture.createTime) }}</span>
          </li>
        </ul>

        <div class="action-row">
          <a-button
            type="primary"
            :loading="downloading"
            @click="handleDownload"
          >
            <template #icon><DownloadOutlined /></template>
            免费下载
          </a-button>
          <template v-if="canManage">
            <a-button @click="openEdit">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
            <a-popconfirm
              title="确定删除该图片吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete"
            >
              <a-button danger :loading="deleteLoading">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-popconfirm>
          </template>
          <a-button v-if="canReupload" @click="reuploadVisible = true">
            <template #icon><CloudUploadOutlined /></template>
            重新上传
          </a-button>
        </div>
      </section>
    </div>
    <a-result
      v-else-if="loadFailed && !loading"
      status="404"
      title="图片不存在或加载失败"
    >
      <template #extra>
        <a-button type="primary" @click="router.push({ name: 'home' })">返回首页</a-button>
      </template>
    </a-result>
  </a-spin>

  <a-modal
    v-model:open="reuploadVisible"
    title="重新上传"
    :footer="null"
    centered
  >
    <a-upload-dragger
      accept="image/*"
      :show-upload-list="false"
      :before-upload="handleReupload"
      :disabled="reuploading"
    >
      <a-spin :spinning="reuploading" tip="上传中...">
        <p class="reupload-icon"><CloudUploadOutlined /></p>
        <p class="reupload-text">点击或拖拽图片替换当前文件</p>
        <p class="reupload-hint">支持 jpg / png / webp 等图片格式，大小不超过 5MB</p>
      </a-spin>
    </a-upload-dragger>
  </a-modal>

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
          placeholder="选择分类"
          allow-clear
          show-search
          :options="categoryOptions.map((item) => ({ value: item, label: item }))"
        />
      </a-form-item>
      <a-form-item label="标签">
        <a-select
          v-model:value="editForm.tags"
          placeholder="选择标签"
          mode="multiple"
          allow-clear
          :options="tagOptions.map((item) => ({ value: item, label: item }))"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.detail-page {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.preview-card,
.info-card {
  padding: 20px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.preview-card {
  flex: 0 0 60%;
  min-width: 0;
}

.info-card {
  flex: 1;
  min-width: 360px;
}

.card-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}

.preview-body {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  background: #f7f8fa;
  border-radius: 8px;
  overflow: hidden;
}

.preview-image {
  display: block;
}

.preview-body :deep(.ant-image) {
  display: block;
  max-width: 100%;
}

.preview-body :deep(img) {
  display: block;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: 62vh;
  object-fit: contain;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.info-item {
  display: flex;
  gap: 12px;
  font-size: 14px;
  line-height: 24px;
}

.info-label {
  flex-shrink: 0;
  width: 70px;
  color: #5e6d82;
  white-space: nowrap;
}

.info-label::after {
  content: '：';
}

.info-value {
  flex: 1;
  min-width: 0;
  color: #1f2d3d;
  word-break: break-all;
}

.info-value.author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.reupload-icon {
  margin-bottom: 8px;
  font-size: 38px;
  color: #1677ff;
}

.reupload-text {
  margin: 0;
  font-size: 14px;
  color: #1f2d3d;
}

.reupload-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #8c8c8c;
}

@media (max-width: 900px) {
  .detail-page {
    flex-direction: column;
  }

  .preview-card {
    flex: 0 0 auto;
    width: 100%;
  }

  .info-card {
    width: 100%;
  }
}
</style>
