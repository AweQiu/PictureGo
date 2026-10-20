<script setup lang="ts">
import { message } from 'ant-design-vue'
import { EditOutlined, PlusOutlined } from '@ant-design/icons-vue'

defineProps<{ value: string }>()

const emit = defineEmits<{
  'update:value': [value: string]
}>()

function beforeUpload(file: File) {
  if (!file.type.startsWith('image/')) {
    message.error('只能上传图片文件')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    message.error('图片大小不能超过 2MB')
    return false
  }
  const reader = new FileReader()
  reader.onload = () => emit('update:value', reader.result as string)
  reader.readAsDataURL(file)
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
      <img v-if="value" :src="value" alt="头像" class="avatar-img" />
      <div v-else class="avatar-placeholder">
        <PlusOutlined />
        <span>上传头像</span>
      </div>
      <div class="avatar-mask">
        <EditOutlined />
        <span>更换头像</span>
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

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 12px;
  color: #8c8c8c;
  background: #fafafa;
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
