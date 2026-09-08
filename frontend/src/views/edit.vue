<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const form = ref({
  name: '',
  code: '',
  material: '',
  tag: '',
  image: '',
  step: '',
  energy: '',
})
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const imagePreview = ref('')
const error = ref('')

const getMenuId = () => route.params.id

const request = async (url, options = {}) => {
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), 10000)

  try {
    return await fetch(url, {
      ...options,
      signal: controller.signal,
    })
  } catch (err) {
    if (err.name === 'AbortError') {
      throw new Error('请求超时，请确认后端服务已启动')
    }

    throw new Error('无法连接后端服务，请确认 Spring Boot 正在运行（端口 2026）')
  } finally {
    window.clearTimeout(timeoutId)
  }
}

const getResponseError = async (response, fallback) => {
  try {
    const body = await response.json()
    return body.message || body.error || fallback
  } catch {
    return fallback
  }
}

const loadMenu = async () => {
  try {
    const response = await request(`/api/menus/${encodeURIComponent(getMenuId())}`)
    if (!response.ok) {
      throw new Error(await getResponseError(response, `加载失败（HTTP ${response.status}）`))
    }

    const menu = await response.json()
    Object.keys(form.value).forEach((key) => {
      form.value[key] = String(menu?.[key] ?? '')
    })
  } catch (err) {
    error.value = err.message || '加载食谱失败'
  } finally {
    loading.value = false
  }
}

const buildPayload = () => Object.fromEntries(
  Object.entries(form.value).map(([key, value]) => [key, value.trim()]),
)

const goBack = () => {
  router.push({ name: 'List' })
}

const submitForm = async () => {
  if (saving.value) {
    return
  }

  const payload = buildPayload()
  if (!payload.name) {
    error.value = '请输入食谱名称'
    return
  }

  saving.value = true
  error.value = ''

  try {
    const response = await request(`/api/menus/${encodeURIComponent(getMenuId())}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(payload),
    })

    if (!response.ok) {
      throw new Error(await getResponseError(response, `保存失败（HTTP ${response.status}）`))
    }

    router.push({ name: 'List' })
  } catch (err) {
    error.value = err.message || '保存食谱失败'
  } finally {
    saving.value = false
  }
}

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

onMounted(loadMenu)
</script>

<template>
  <div class="app-shell">
    <div class="panel add-panel">
      <header class="page-toolbar">
        <div>
          <p class="eyebrow">EDIT RECIPE</p>
          <h1>编辑食谱</h1>
        </div>
        <button class="secondary-button" type="button" @click="goBack">返回列表</button>
      </header>

      <div v-if="loading" class="status">加载中...</div>
      <div v-else-if="error" class="status error form-error">{{ error }}</div>

      <form v-else class="recipe-form" @submit.prevent="submitForm">
        <label class="form-field form-field-wide">
          <span>食谱名称 <strong>*</strong></span>
          <input v-model="form.name" type="text" placeholder="例如：番茄炒蛋" required />
        </label>

        <label class="form-field form-field-wide">
          <span>代号 <strong>*</strong></span>
          <input v-model="form.code" type="text" required />
        </label>

        <label class="form-field">
          <span>食材</span>
          <textarea v-model="form.material" rows="4" placeholder="填写主要食材"></textarea>
        </label>

        <label class="form-field">
          <span>标签</span>
          <input v-model="form.tag" type="text" placeholder="例如：家常菜、快手菜" />
        </label>

        <label class="form-field form-field-wide">
          <span>上传图片</span>
          <input type="file" accept="image/*" :disabled="uploading" @change="uploadImage" />
          <small v-if="uploading">图片上传中...</small>
          <img v-if="imagePreview || form.image" class="image-preview" :src="imagePreview || form.image" alt="食谱预览" />
        </label>

        <label class="form-field form-field-wide">
          <span>制作步骤</span>
          <textarea v-model="form.step" rows="6" placeholder="按步骤填写制作方法"></textarea>
        </label>

        <label class="form-field">
          <span>能量信息</span>
          <input v-model="form.energy" type="text" placeholder="例如：350 kcal" />
        </label>

        <div v-if="error" class="status error form-error">{{ error }}</div>

        <div class="form-actions">
          <button class="secondary-button" type="button" @click="goBack">取消</button>
          <button class="primary-button" type="submit" :disabled="saving">
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
