<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const menu = ref(null)
const loading = ref(true)
const uploading = ref(false)
const error = ref('')

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') {
    return '—'
  }

  return String(value)
}

const openVideo = () => {
  if (menu.value?.video) {
    window.open(menu.value.video, '_blank', 'noopener,noreferrer')
  }
}

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

const uploadImage = async (event) => {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  if (!file.type.startsWith('image/')) {
    error.value = '请选择图片文件'
    event.target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    error.value = '图片大小不能超过 5MB'
    event.target.value = ''
    return
  }

  uploading.value = true
  error.value = ''
  imagePreview.value = URL.createObjectURL(file)

  try {
    const body = new FormData()
    body.append('file', file)
    const response = await fetch('/api/uploads', { method: 'POST', body })
    if (!response.ok) {
      throw new Error(await getResponseError(response))
    }

    const result = await response.json()
    form.value.image = result.url
  } catch (err) {
    imagePreview.value = ''
    error.value = err.message || '图片上传失败'
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

const goBack = () => {
  router.push('/')
}
</script>

<template>
  <div class="card-detail-shell">
    <div class="card-detail-panel">
      <div class="card-toolbar">
        <button class="back-button" @click="goBack">返回列表</button>
      </div>

      <div v-if="loading" class="status">加载中...</div>
      <div v-else-if="error" class="status error">{{ error }}</div>

      <div v-else-if="menu" class="card-detail-layout">
        <section class="hero-card">
          <div class="hero-image-wrap">
            <img v-if="menu.image" :src="menu.image" :alt="menu.name || '菜单图片'" class="hero-image" />
            <div v-else class="hero-placeholder">{{ (menu.name || 'M').charAt(0).toUpperCase() }}</div>
          </div>

          <div class="hero-content">
            <div class="eyebrow">菜单详情</div>
            <h1>{{ menu.name || '未命名菜单' }}</h1>
            <div class="meta-row">
              <span class="tag-pill">编号：{{ formatValue(menu.code) }}</span>
              <span class="tag-pill status-pill">状态：{{ formatValue(menu.status) }}</span>
            </div>

            <div class="summary-grid">
              <div class="summary-item">
                <span class="summary-label">图片</span>
                <span class="summary-value">{{ menu.image ? '已上传' : '无图片' }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">视频</span>
                <span class="summary-value">{{ menu.video ? '已附加' : '无视频' }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">标签</span>
                <span class="summary-value">{{ formatValue(menu.tag) }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">能量</span>
                <span class="summary-value">{{ formatValue(menu.energy) }}</span>
              </div>
            </div>

            <div class="hero-actions">
              <button class="primary-button" @click="openVideo" :disabled="!menu.video">播放视频</button>
            </div>
          </div>
        </section>

        <section class="info-grid">
          <article class="info-card wide">
            <div class="card-title">食材信息</div>
            <div class="card-body-text">{{ formatValue(menu.material) }}</div>
          </article>

          <article class="info-card wide">
            <div class="card-title">制作步骤</div>
            <div class="card-body-text whitespace-pre">{{ formatValue(menu.step) }}</div>
          </article>

          <article class="info-card">
            <div class="card-title">标签</div>
            <div class="card-body-text">{{ formatValue(menu.tag) }}</div>
          </article>

          <article class="info-card">
            <div class="card-title">视频链接</div>
            <div class="card-body-text">
              <a v-if="menu.video" :href="menu.video" target="_blank" rel="noopener noreferrer">打开链接</a>
              <span v-else>—</span>
            </div>
          </article>
        </section>
      </div>
    </div>
  </div>
</template>
