<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const menu = ref(null)
const loading = ref(true)
const error = ref('')

const getVideoSource = (value) => {
  if (!value) {
    return { type: 'unsupported', url: '' }
  }

  try {
    const parsed = new URL(value)
    const hostname = parsed.hostname.toLowerCase()
    const youtubeId = parsed.searchParams.get('v')
      || parsed.pathname.match(/\/(?:embed|shorts)\/([^/?]+)/)?.[1]
      || (hostname === 'youtu.be' ? parsed.pathname.slice(1).split('/')[0] : '')

    if (youtubeId && ['youtube.com', 'www.youtube.com', 'm.youtube.com', 'youtu.be'].includes(hostname)) {
      return { type: 'iframe', url: `https://www.youtube.com/embed/${encodeURIComponent(youtubeId)}` }
    }

    const bilibiliId = parsed.pathname.match(/\/video\/(BV[\w]+|av\d+)/i)?.[1]
    if (bilibiliId && ['bilibili.com', 'www.bilibili.com', 'm.bilibili.com'].includes(hostname)) {
      const parameter = bilibiliId.toLowerCase().startsWith('av') ? 'aid' : 'bvid'
      return { type: 'iframe', url: `https://player.bilibili.com/player.html?${parameter}=${encodeURIComponent(bilibiliId)}&page=1` }
    }

    if (/\.(mp4|webm|ogg)(?:\?.*)?$/i.test(parsed.pathname)) {
      return { type: 'video', url: parsed.href }
    }
  } catch {
    return { type: 'unsupported', url: value }
  }

  return { type: 'unsupported', url: value }
}

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') {
    return '—'
  }

  return String(value)
}

const videoSource = () => getVideoSource(menu.value?.video)

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
              <a v-if="menu.video && videoSource().type === 'unsupported'" class="primary-button" :href="menu.video"
                target="_blank" rel="noopener noreferrer">打开视频</a>
            </div>
          </div>
        </section>

        <section v-if="menu.video" class="video-section">
          <div class="card-title">在线视频</div>
          <div v-if="videoSource().type === 'iframe'" class="video-frame-wrap">
            <iframe :src="videoSource().url" title="菜单在线视频" class="video-frame"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowfullscreen></iframe>
          </div>
          <video v-else-if="videoSource().type === 'video'" class="video-player" controls preload="metadata">
            <source :src="videoSource().url" />
            当前浏览器不支持视频播放。
          </video>
          <p v-else class="video-fallback">该网站不支持直接嵌入播放，请点击“打开视频”在新窗口观看。</p>
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
