<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import {
  ROLE_OPTIONS,
  useUserStore,
  type EditUserPayload,
  type UserRecord,
} from '../store/user'
import AvatarUpload from '../components/AvatarUpload.vue'

const userStore = useUserStore()

const keyword = ref('')
const roleFilter = ref<string>('')

const filteredUsers = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return userStore.users.filter((user) => {
    const matchKeyword =
      !kw ||
      user.account.toLowerCase().includes(kw) ||
      user.username.toLowerCase().includes(kw)
    const matchRole = !roleFilter.value || user.role === roleFilter.value
    return matchKeyword && matchRole
  })
})

function resetFilter() {
  keyword.value = ''
  roleFilter.value = ''
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '账号', dataIndex: 'account', key: 'account' },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '头像', dataIndex: 'avatar', key: 'avatar', width: 90 },
  { title: '简介', dataIndex: 'bio', key: 'bio', ellipsis: true },
  { title: '角色', dataIndex: 'role', key: 'role', width: 120 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
]

const roleColorMap: Record<string, string> = {
  超级管理员: 'blue',
  VIP用户: 'gold',
  普通用户: 'default',
}

const editVisible = ref(false)
const editLoading = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const editingId = ref<number | null>(null)
const editForm = reactive<EditUserPayload>({
  account: '',
  username: '',
  avatar: '',
  bio: '',
  role: '普通用户',
})

const dialogTitle = computed(() =>
  dialogMode.value === 'add' ? '新增用户' : '编辑用户',
)

function openAdd() {
  dialogMode.value = 'add'
  editingId.value = null
  Object.assign(editForm, {
    account: '',
    username: '',
    avatar: '',
    bio: '',
    role: '普通用户',
  })
  editVisible.value = true
}

function openEdit(record: UserRecord) {
  dialogMode.value = 'edit'
  editingId.value = record.id
  Object.assign(editForm, {
    account: record.account,
    username: record.username,
    avatar: record.avatar,
    bio: record.bio,
    role: record.role,
  })
  editVisible.value = true
}

function handleDialogSubmit() {
  const payload: EditUserPayload = {
    account: editForm.account.trim(),
    username: editForm.username.trim(),
    avatar: editForm.avatar,
    bio: editForm.bio.trim(),
    role: editForm.role,
  }
  if (!payload.account) {
    message.warning('账号不能为空')
    return
  }
  if (!payload.role) {
    message.warning('请选择角色')
    return
  }
  if (dialogMode.value === 'edit' && !payload.username) {
    message.warning('用户名不能为空')
    return
  }
  editLoading.value = true
  const error =
    dialogMode.value === 'add'
      ? userStore.addUser(payload)
      : userStore.updateUser(editingId.value!, payload)
  editLoading.value = false
  if (error) {
    message.error(error)
    return
  }
  message.success(dialogMode.value === 'add' ? '新增用户成功' : '用户信息已更新')
  editVisible.value = false
}

function handleDelete(record: UserRecord) {
  const error = userStore.removeUser(record.id)
  if (error) {
    message.error(error)
    return
  }
  message.success(`已删除用户「${record.username}」`)
}
</script>

<template>
  <a-card :bordered="false" title="用户管理" class="user-manage-card">
    <div class="toolbar">
      <a-input
        v-model:value="keyword"
        class="toolbar-keyword"
        placeholder="搜索账号或用户名"
        allow-clear
      >
        <template #prefix>
          <SearchOutlined />
        </template>
      </a-input>
      <a-select
        v-model:value="roleFilter"
        class="toolbar-role"
        placeholder="按角色筛选"
        allow-clear
        :options="[
          { value: '', label: '全部角色' },
          ...ROLE_OPTIONS.map((role) => ({ value: role, label: role })),
        ]"
      />
      <a-button @click="resetFilter">重置</a-button>
      <a-button type="primary" class="toolbar-add" @click="openAdd">
        <template #icon><PlusOutlined /></template>
        新增用户
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="filteredUsers"
      :pagination="{
        pageSize: 5,
        showSizeChanger: true,
        pageSizeOptions: ['5', '10', '20'],
        showTotal: (total: number) => `共 ${total} 条`,
      }"
      :scroll="{ x: 1100 }"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'avatar'">
          <a-avatar :src="record.avatar" :size="44" shape="circle" />
        </template>
        <template v-else-if="column.key === 'role'">
          <a-tag :color="roleColorMap[record.role] ?? 'default'">
            {{ record.role }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'bio'">
          <span>{{ record.bio || '—' }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openEdit(record)">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
            <a-popconfirm
              title="确定删除该用户吗？"
              ok-text="确定"
              cancel-text="取消"
              :disabled="record.id === userStore.currentUserId"
              @confirm="handleDelete(record)"
            >
              <a-button
                type="link"
                size="small"
                danger
                :disabled="record.id === userStore.currentUserId"
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
        <AvatarUpload v-model:value="editForm.avatar" />
      </a-form-item>
      <a-form-item label="账号" required>
        <a-input v-model:value="editForm.account" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item label="用户名" :required="dialogMode === 'edit'">
        <a-input
          v-model:value="editForm.username"
          :placeholder="dialogMode === 'add' ? '选填，默认为账号' : '请输入用户名'"
        />
      </a-form-item>
      <a-form-item label="简介">
        <a-textarea
          v-model:value="editForm.bio"
          placeholder="请输入简介"
          :rows="3"
          :maxlength="100"
          show-count
        />
      </a-form-item>
      <a-form-item label="角色" required>
        <a-select
          v-model:value="editForm.role"
          :options="ROLE_OPTIONS.map((role) => ({ value: role, label: role }))"
        />
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

.toolbar-keyword {
  width: 260px;
}

.toolbar-role {
  width: 160px;
}

.toolbar-add {
  margin-left: auto;
}
</style>
