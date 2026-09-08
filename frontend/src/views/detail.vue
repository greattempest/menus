<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const menu = ref(null)
const loading = ref(true)
const error = ref('')

const fieldMeta = [
  { key: 'id', label: 'ID' },
  { key: 'code', label: '编号' },
  { key: 'name', label: '名称' },
  { key: 'status', label: '状态' },
  { key: 'image', label: '图片' },
  { key: 'video', label: '视频' },
  { key: 'material', label: '食材' },
  { key: 'step', label: '步骤' },
  { key: 'energy', label: '能量/信息' },
  { key: 'tag', label: '标签' },
]

onMounted(async () => {
  const menuId = route.params.id

  try {
    const response = await fetch(`/api/menus/${menuId}`)
    if (!response.ok) {
      throw new Error(`请求失败：${response.status}`)
    }

    const data = await response.json()
    if (!data || typeof data !== 'object') {
      throw new Error('返回数据为空')
    }

    menu.value = data
  } catch (err) {
    error.value = err.message || '获取菜单详情失败'
  } finally {
    loading.value = false
  }
})

const goBack = () => {
  router.push('/')
}

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') {
    return '—'
  }

  return String(value)
}
</script>

<template>
  <div class="detail-shell">
    <div class="detail-panel">
      <button class="back-button" @click="goBack">返回列表</button>

      <div v-if="loading" class="status">加载中...</div>
      <div v-else-if="error" class="status error">{{ error }}</div>

      <div v-else-if="menu" class="detail-content">
        <div class="detail-header">
          <div class="detail-image-wrap">
            <img v-if="menu.image" :src="menu.image" :alt="menu.name || '菜单图片'" class="detail-image" />
            <div v-else class="detail-image-placeholder">{{ (menu.name || 'M').charAt(0).toUpperCase() }}</div>
          </div>

          <div class="detail-title-group">
            <div class="detail-name">{{ menu.name || '未命名菜单' }}</div>
            <div class="detail-subtitle">编号：{{ formatValue(menu.code) }}</div>
          </div>
        </div>

        <div class="detail-fields">
          <div v-for="field in fieldMeta" :key="field.key" class="detail-row">
            <div class="detail-label">{{ field.label }}</div>
            <div class="detail-value">
              <template v-if="field.key === 'image' && menu.image">
                <img :src="menu.image" :alt="menu.name || '菜单图片'" class="mini-image" />
              </template>
              <template v-else-if="field.key === 'video' && menu.video">
                <a :href="menu.video" target="_blank" rel="noopener noreferrer">查看视频</a>
              </template>
              <template v-else>
                {{ formatValue(menu[field.key]) }}
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
