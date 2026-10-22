<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  DeleteOutlined,
  EditOutlined,
  FileSearchOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { addUser, deleteUser, listUserVOByPage, updateUser } from '../api/user'
import { USER_ROLE, USER_ROLE_OPTIONS, USER_ROLE_TEXT } from '../constants/enums'
import { useUserStore } from '../store/user'
import { formatDateTime } from '../utils/format'
import AvatarUpload from '../components/AvatarUpload.vue'
import UserAvatar from '../components/UserAvatar.vue'

const userStore = useUserStore()

// ---------- 查询与分页 ----------
const queryForm = reactive({
  userAccount: '',
  userName: '',
  userRole: undefined as string | undefined,
})

const userList = ref<UserVO[]>([])
const loading = ref(false)
const total = ref(0)
const pagination = reactive({
  current: 1,
  pageSize: 10,
})

async function fetchData() {
  loading.value = true
  try {
    const page = await listUserVOByPage({
      current: pagination.current,
      pageSize: pagination.pageSize,
      userAccount: queryForm.userAccount || undefined,
      userName: queryForm.userName || undefined,
      userRole: queryForm.userRole || undefined,
    })
    userList.value = page.records ?? []
    total.value = Number(page.total ?? 0)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '查询用户失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  queryForm.userAccount = ''
  queryForm.userName = ''
  queryForm.userRole = undefined
  handleSearch()
}

function handleTableChange(pager: TablePaginationConfig) {
  pagination.current = pager.current ?? 1
  pagination.pageSize = pager.pageSize ?? 10
  fetchData()
}

onMounted(fetchData)

// ---------- 表格 ----------
const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 140, ellipsis: true },
  { title: '账号', dataIndex: 'userAccount', key: 'userAccount', width: 130, ellipsis: true },
  { title: '用户名', dataIndex: 'userName', key: 'userName', width: 130, ellipsis: true },
  { title: '头像', dataIndex: 'userAvatar', key: 'userAvatar', width: 80 },
  {
    title: '简介',
    dataIndex: 'userProfile',
    key: 'userProfile',
    width: 220,
    ellipsis: true,
  },
  { title: '角色', dataIndex: 'userRole', key: 'userRole', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 210, fixed: 'right' as const },
]

const roleColorMap: Record<string, string> = {
  [USER_ROLE.ADMIN]: 'blue',
  [USER_ROLE.USER]: 'default',
}

// ---------- 详情浮窗 ----------
const detailVisible = ref(false)
const detailUser = ref<UserVO | null>(null)

function openDetail(record: UserVO) {
  detailUser.value = record
  detailVisible.value = true
}

// ---------- 新增 / 编辑 ----------
const editVisible = ref(false)
const editLoading = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const editingId = ref<string | null>(null)
const editForm = reactive({
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: USER_ROLE.USER as string,
})

const dialogTitle = computed(() =>
  dialogMode.value === 'add' ? '新增用户' : '编辑用户',
)

function openAdd() {
  dialogMode.value = 'add'
  editingId.value = null
  Object.assign(editForm, {
    userAccount: '',
    userName: '',
    userAvatar: '',
    userProfile: '',
    userRole: USER_ROLE.USER,
  })
  editVisible.value = true
}

function openEdit(record: UserVO) {
  dialogMode.value = 'edit'
  editingId.value = record.id
  Object.assign(editForm, {
    userAccount: record.userAccount,
    userName: record.userName,
    userAvatar: record.userAvatar,
    userProfile: record.userProfile,
    userRole: record.userRole,
  })
  editVisible.value = true
}

async function handleDialogSubmit() {
  if (!editForm.userAccount.trim()) {
    message.warning('账号不能为空')
    return
  }
  if (!editForm.userRole) {
    message.warning('请选择角色')
    return
  }
  editLoading.value = true
  try {
    if (dialogMode.value === 'add') {
      await addUser({
        userAccount: editForm.userAccount.trim(),
        userName: editForm.userName.trim() || undefined,
        userAvatar: editForm.userAvatar || undefined,
        userProfile: editForm.userProfile.trim() || undefined,
        userRole: editForm.userRole,
      })
      message.success('新增用户成功')
    } else {
      await updateUser({
        id: editingId.value!,
        userAccount: editForm.userAccount.trim(),
        userName: editForm.userName.trim() || undefined,
        userAvatar: editForm.userAvatar || undefined,
        userProfile: editForm.userProfile.trim() || undefined,
        userRole: editForm.userRole,
      })
      message.success('用户信息已更新')
    }
    editVisible.value = false
    fetchData()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    editLoading.value = false
  }
}

// ---------- 删除 ----------
async function handleDelete(record: UserVO) {
  try {
    await deleteUser({ id: record.id })
    message.success(`已删除用户「${record.userName}」`)
    if (userList.value.length === 1 && pagination.current > 1) {
      pagination.current -= 1
    }
    fetchData()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '删除失败')
  }
}
</script>

<template>
  <a-card :bordered="false" title="用户管理" class="user-manage-card">
    <div class="toolbar">
      <a-input
        v-model:value="queryForm.userAccount"
        class="toolbar-item"
        placeholder="搜索账号"
        allow-clear
        @press-enter="handleSearch"
      >
        <template #prefix>
          <SearchOutlined />
        </template>
      </a-input>
      <a-input
        v-model:value="queryForm.userName"
        class="toolbar-item"
        placeholder="搜索用户名"
        allow-clear
        @press-enter="handleSearch"
      >
        <template #prefix>
          <SearchOutlined />
        </template>
      </a-input>
      <a-select
        v-model:value="queryForm.userRole"
        class="toolbar-role"
        placeholder="按角色筛选"
        allow-clear
        :options="USER_ROLE_OPTIONS"
      />
      <a-button type="primary" @click="handleSearch">搜索</a-button>
      <a-button @click="handleReset">重置</a-button>
      <a-button type="primary" class="toolbar-add" @click="openAdd">
        <template #icon><PlusOutlined /></template>
        新增用户
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="userList"
      :loading="loading"
      :pagination="{
        current: pagination.current,
        pageSize: pagination.pageSize,
        total,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '50'],
        showTotal: (count: number) => `共 ${count} 条`,
      }"
      :scroll="{ x: 1180 }"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'userAvatar'">
          <UserAvatar :src="record.userAvatar" :size="40" />
        </template>
        <template v-else-if="column.key === 'userRole'">
          <a-tag :color="roleColorMap[record.userRole] ?? 'default'">
            {{ USER_ROLE_TEXT[record.userRole] ?? record.userRole }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'userProfile'">
          <span>{{ record.userProfile || '—' }}</span>
        </template>
        <template v-else-if="column.key === 'createTime'">
          <span>{{ formatDateTime(record.createTime) }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space :size="0">
            <a-button type="link" size="small" @click="openDetail(record)">
              <template #icon><FileSearchOutlined /></template>
              详情
            </a-button>
            <a-button type="link" size="small" @click="openEdit(record)">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
            <a-popconfirm
              title="确定删除该用户吗？"
              ok-text="确定"
              cancel-text="取消"
              :disabled="record.id === userStore.loginUser?.id"
              @confirm="handleDelete(record)"
            >
              <a-button
                type="link"
                size="small"
                danger
                :disabled="record.id === userStore.loginUser?.id"
              >
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
    v-model:open="detailVisible"
    title="用户详情"
    :footer="null"
    width="480px"
    centered
  >
    <div v-if="detailUser" class="detail-body">
      <div class="detail-avatar">
        <UserAvatar :src="detailUser.userAvatar" :size="72" />
        <div class="detail-name">{{ detailUser.userName || detailUser.userAccount }}</div>
        <a-tag :color="roleColorMap[detailUser.userRole] ?? 'default'">
          {{ USER_ROLE_TEXT[detailUser.userRole] ?? detailUser.userRole }}
        </a-tag>
      </div>
      <a-descriptions :column="1" size="small" bordered>
        <a-descriptions-item label="用户 ID">{{ detailUser.id }}</a-descriptions-item>
        <a-descriptions-item label="账号">{{ detailUser.userAccount }}</a-descriptions-item>
        <a-descriptions-item label="用户名">
          {{ detailUser.userName || '—' }}
        </a-descriptions-item>
        <a-descriptions-item label="简介">
          {{ detailUser.userProfile || '—' }}
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ formatDateTime(detailUser.createTime) }}
        </a-descriptions-item>
      </a-descriptions>
    </div>
  </a-modal>

  <a-modal
    v-model:open="editVisible"
    :title="dialogTitle"
    ok-text="保存"
    cancel-text="取消"
    :confirm-loading="editLoading"
    centered
    @ok="handleDialogSubmit"
  >
    <a-form layout="vertical" class="edit-form">
      <a-form-item label="头像">
        <AvatarUpload v-model:value="editForm.userAvatar" />
      </a-form-item>
      <a-form-item label="账号" required>
        <a-input v-model:value="editForm.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item label="用户名" :required="dialogMode === 'edit'">
        <a-input
          v-model:value="editForm.userName"
          :placeholder="dialogMode === 'add' ? '选填，默认为账号' : '请输入用户名'"
        />
      </a-form-item>
      <a-form-item label="简介">
        <a-textarea
          v-model:value="editForm.userProfile"
          placeholder="请输入简介"
          :rows="3"
          :maxlength="100"
          show-count
        />
      </a-form-item>
      <a-form-item label="角色" required>
        <a-select v-model:value="editForm.userRole" :options="USER_ROLE_OPTIONS" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.user-manage-card {
  border-radius: 12px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-item {
  width: 200px;
}

.toolbar-role {
  width: 160px;
}

.toolbar-add {
  margin-left: auto;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.detail-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}
</style>
