<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { EditOutlined, LoadingOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { uploadFile } from '../api/file'
import { DEFAULT_AVATAR_URL } from '../constants/avatar'

defineProps<{ value: string }>()

const emit = defineEmits<{
  'update:value': [value: string]
}>()

const uploading = ref(false)

async function beforeUpload(file: File) {
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片文件')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    message.error('图片大小不能超过 2MB')
    return false
  }
  uploading.value = true
  try {
    const url = await uploadFile(file)
    emit('update:value', url)
    message.success('头像上传成功')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '头像上传失败')
  } finally {
    uploading.value = false
  }
  return false
}
</script>

<template>
  <a-upload
    accept="image/*"
    :show-upload-list="false"
    :before-upload="beforeUpload"
  >
    <div class="avatar-upload">
      <img :src="value || DEFAULT_AVATAR_URL" alt="头像" class="avatar-img" />
      <div class="avatar-mask">
        <LoadingOutlined v-if="uploading" />
        <PlusOutlined v-else-if="!value" />
        <EditOutlined v-else />
        <span>{{ uploading ? '上传中' : value ? '更换头像' : '上传头像' }}</span>
      </div>
    </div>
  </a-upload>
</template>

<style scoped>
.avatar-upload {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e5e6eb;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-upload:hover .avatar-mask {
  opacity: 1;
}
</style>
