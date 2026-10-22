<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { DEFAULT_AVATAR_URL } from '../constants/avatar'

const props = withDefaults(
  defineProps<{
    src?: string | null
    size?: number
    shape?: 'circle' | 'square'
    alt?: string
  }>(),
  {
    src: undefined,
    size: 40,
    shape: 'circle',
    alt: '用户头像',
  },
)

/** 远程头像加载失败（如地址失效）时回退到默认头像 */
const loadFailed = ref(false)

watch(
  () => props.src,
  () => {
    loadFailed.value = false
  },
)

const avatarSrc = computed(() => {
  const value = props.src?.trim()
  return !value || loadFailed.value ? DEFAULT_AVATAR_URL : value
})

function handleLoadError() {
  loadFailed.value = true
  return false
}
</script>

<template>
  <a-avatar
    :src="avatarSrc"
    :size="size"
    :shape="shape"
    :alt="alt"
    :load-error="handleLoadError"
  />
</template>
