<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { listPictureTagCategory, listPictureVOByPage } from '../api/picture'

const router = useRouter()

const pictureList = ref<PictureVO[]>([])
const loading = ref(false)
const total = ref(0)

const pagination = reactive({
  current: 1,
  pageSize: 12,
})

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

async function fetchData() {
  loading.value = true
  try {
    const page = await listPictureVOByPage({
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

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleFilterChange() {
  pagination.current = 1
  fetchData()
}

/** 点击分类标签：再次点击已选中的分类即取消筛选 */
function toggleCategory(category?: string) {
  queryForm.category = category
  handleFilterChange()
}

/** 点击标签：在筛选条件中加入 / 移除该标签 */
function toggleTag(tag: string) {
  const index = queryForm.tags.indexOf(tag)
  if (index >= 0) {
    queryForm.tags.splice(index, 1)
  } else {
    queryForm.tags.push(tag)
  }
  handleFilterChange()
}

function handleReset() {
  queryForm.searchText = ''
  queryForm.category = undefined
  queryForm.tags = []
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function openDetail(picture: PictureVO) {
  router.push({ name: 'pictureDetail', params: { id: picture.id } })
}

onMounted(() => {
  fetchData()
  fetchTagCategory()
})
</script>

<template>
  <div class="gallery-page">
    <section class="gallery-toolbar">
      <div class="search-row">
        <a-input
          v-model:value="queryForm.searchText"
          class="search-input"
          size="large"
          placeholder="搜索图片名称 / 简介"
          allow-clear
          @press-enter="handleSearch"
        >
          <template #prefix>
            <SearchOutlined />
          </template>
        </a-input>
        <a-button type="primary" size="large" @click="handleSearch">搜索</a-button>
      </div>
      <div class="filter-line">
        <span class="filter-label">分类</span>
        <div class="filter-items">
          <a-tag
            class="filter-chip"
            :class="{ 'is-active': !queryForm.category }"
            @click="toggleCategory(undefined)"
          >
            全部
          </a-tag>
          <a-tag
            v-for="item in categoryOptions"
            :key="item"
            class="filter-chip"
            :class="{ 'is-active': queryForm.category === item }"
            @click="toggleCategory(item)"
          >
            {{ item }}
          </a-tag>
        </div>
        <span class="result-count">共 {{ total }} 张图片</span>
      </div>

      <div class="filter-line">
        <span class="filter-label">标签</span>
        <div class="filter-items">
          <a-tag
            v-for="tag in tagOptions"
            :key="tag"
            class="filter-chip"
            :class="{ 'is-active': queryForm.tags.includes(tag) }"
            @click="toggleTag(tag)"
          >
            {{ tag }}
          </a-tag>
          <span v-if="tagOptions.length === 0" class="filter-empty">暂无标签</span>
        </div>
        <div class="filter-tools">
          <a-select
            v-model:value="queryForm.tags"
            class="filter-tags"
            mode="multiple"
            placeholder="按标签筛选"
            allow-clear
            :max-tag-count="1"
            :options="tagOptions.map((item) => ({ value: item, label: item }))"
            @change="handleFilterChange"
          />
          <a-button type="link" @click="handleReset">重置</a-button>
        </div>
      </div>
    </section>

    <a-spin :spinning="loading">
      <div class="gallery-body">
        <div v-if="pictureList.length > 0" class="gallery-grid">
          <div
            v-for="picture in pictureList"
            :key="picture.id"
            class="gallery-card"
            role="button"
            tabindex="0"
            @click="openDetail(picture)"
            @keydown.enter="openDetail(picture)"
          >
            <div class="gallery-cover">
              <img :src="picture.url" :alt="picture.name" loading="lazy" />
              <div class="gallery-overlay">
                <span class="gallery-name" :title="picture.name">{{ picture.name }}</span>
                <div v-if="picture.tags?.length" class="gallery-tags">
                  <span v-for="tag in picture.tags" :key="tag" class="tag-chip">{{ tag }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <a-empty v-else-if="!loading" class="gallery-empty" description="暂无图片" />
      </div>
    </a-spin>

    <div v-if="total > pagination.pageSize" class="gallery-pagination">
      <a-pagination
        :current="pagination.current"
        :page-size="pagination.pageSize"
        :total="total"
        show-quick-jumper
        :page-size-options="['12', '24', '48']"
        @change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.gallery-page {
  width: 100%;
}

.gallery-toolbar {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.search-row {
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
}

.search-input {
  flex: 1;
}

.filter-line {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.filter-label {
  flex-shrink: 0;
  width: 40px;
  font-size: 13px;
  line-height: 26px;
  color: #5e6d82;
}

.filter-items {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.filter-chip {
  margin-inline-end: 0;
  padding: 1px 12px;
  font-size: 13px;
  line-height: 22px;
  border-radius: 999px;
  cursor: pointer;
  user-select: none;
  transition:
    color 0.2s,
    background 0.2s,
    border-color 0.2s;
}

.filter-chip.is-active {
  color: #1677ff;
  background: #e6f4ff;
  border-color: #91caff;
}

.filter-empty {
  font-size: 13px;
  line-height: 26px;
  color: #bfbfbf;
}

.filter-tools {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8px;
}

.filter-tags {
  width: 260px;
}

.result-count {
  flex-shrink: 0;
  font-size: 13px;
  line-height: 26px;
  color: #8c8c8c;
  white-space: nowrap;
}

.gallery-body {
  min-height: 320px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.gallery-card {
  overflow: hidden;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.gallery-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.gallery-card:focus-visible {
  outline: 2px solid #1677ff;
  outline-offset: 2px;
}

.gallery-cover {
  position: relative;
  aspect-ratio: 3 / 4;
  background: #f2f4f7;
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

.gallery-card:hover .gallery-name {
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

.gallery-empty {
  margin-top: 80px;
}

.gallery-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
