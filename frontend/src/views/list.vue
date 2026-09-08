<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const menus = ref([])
const loading = ref(true)
const error = ref('')
const page = ref(1)
const searchQuery = ref('')
const showActionMenu = ref(false)
const pageSize = 9

const filteredMenus = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) {
    return menus.value
  }

  return menus.value.filter((menu) => {
    const name = String(menu?.name ?? '').toLowerCase()
    const material = String(menu?.material ?? '').toLowerCase()
    const tag = String(menu?.tag ?? '').toLowerCase()
    return name.includes(query) || material.includes(query) || tag.includes(query)
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredMenus.value.length / pageSize)))

const currentMenus = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredMenus.value.slice(start, start + pageSize)
})

const goPrev = () => {
  if (page.value > 1) {
    page.value -= 1
  }
}

const goNext = () => {
  if (page.value < totalPages.value) {
    page.value += 1
  }
}

const fetchMenus = async (keyword = '') => {
  loading.value = true
  error.value = ''

  try {
    const url = keyword ? `/api/menus?keyword=${encodeURIComponent(keyword)}` : '/api/menus'
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error(`请求失败：${response.status}`)
    }

    const data = await response.json()
    if (!Array.isArray(data)) {
      throw new Error('返回数据不是菜单列表')
    }

    menus.value = data
    page.value = 1
  } catch (err) {
    error.value = err.message || '获取菜单失败'
  } finally {
    loading.value = false
  }
}

const onSearchInput = async () => {
  await fetchMenus(searchQuery.value)
}

const goToAdd = () => {
  showActionMenu.value = false
  router.push({ name: 'Add' })
}

const goToDetail = (menu) => {
  const id = menu?.id ?? menu?.code
  if (id === undefined || id === null || id === '') {
    return
  }

  router.push({ name: 'Detail', params: { id } })
}

const goToEdit = (menu) => {
  const id = menu?.id ?? menu?.code
  if (id === undefined || id === null || id === '') {
    return
  }

  router.push({ name: 'Edit', params: { id } })
}

const goToCardDetail = (menu) => {
  const id = menu?.id ?? menu?.code
  if (id === undefined || id === null || id === '') {
    return
  }

  router.push({ name: 'DetailCard', params: { id } })
}

const deleteMenu = async (menu) => {
  const id = menu?.id ?? menu?.code
  if (id === undefined || id === null || id === '') {
    return
  }

  if (!window.confirm(`确认删除“${menu.name || '这道食谱'}”吗？`)) {
    return
  }

  try {
    const response = await fetch(`/api/menus/${encodeURIComponent(id)}`, { method: 'DELETE' })
    if (!response.ok) {
      throw new Error(`删除失败：${response.status}`)
    }

    await fetchMenus(searchQuery.value)
  } catch (err) {
    error.value = err.message || '删除食谱失败'
  }
}

watch(
  () => searchQuery.value,
  () => {
    if (page.value > totalPages.value) {
      page.value = totalPages.value
    }
  }
)

onMounted(() => {
  fetchMenus()
})
</script>

<template>
  <div class="app-shell">
    <div class="panel">
      <header class="header">
        <div>
          <h1>美食指北</h1>
          <h4>且食嗟来之饭</h4>
        </div>
        <div class="action-menu-wrap">
          <button class="action-menu-button" type="button" aria-label="打开操作菜单" :aria-expanded="showActionMenu"
            @click="showActionMenu = !showActionMenu">
            <span></span>
            <span></span>
            <span></span>
          </button>
          <div v-if="showActionMenu" class="action-menu">
            <button class="action-menu-item" type="button" @click="goToAdd">
              <span class="action-menu-icon">+</span>
              <span>新增食谱</span>
            </button>
          </div>
        </div>
      </header>

      <div class="search-box-wrap">
        <input v-model="searchQuery" class="search-box" type="text" placeholder="搜索" @input="onSearchInput" />
      </div>

      <div v-if="loading" class="status">加载中...</div>
      <div v-else-if="error" class="status error">{{ error }}</div>

      <div v-else class="content">
        <div v-if="currentMenus.length === 0" class="status">没有找到相关食谱</div>
        <div v-else class="menu-grid">
          <article v-for="menu in currentMenus" :key="menu.id ?? menu.code ?? menu.name" class="menu-card"
            @click="goToCardDetail(menu)">
            <div class="image-area">
              <img v-if="menu.image" :src="menu.image" :alt="menu.name || '美食图片'" />
              <div v-else class="image-placeholder">{{ (menu.name || 'M').charAt(0).toUpperCase() }}</div>
            </div>
            <div class="card-body">
              <div class="menu-name">{{ menu.name || '黑暗料理' }}</div>
              <div class="card-actions">
                <!--button class="mini-action" @click.stop="goToDetail(menu)">原版</button-->
                <button class="mini-action accent" @click.stop="goToCardDetail(menu)">详情</button>
                <button class="mini-action" @click.stop="goToEdit(menu)">编辑</button>
                <button class="mini-action danger" @click.stop="deleteMenu(menu)">删除</button>
              </div>
            </div>
          </article>
        </div>

        <div v-if="currentMenus.length > 0" class="pagination">
          <button class="page-button" :disabled="page === 1" @click="goPrev">上一页</button>
          <span class="page-text">{{ page }} / {{ totalPages }}</span>
          <button class="page-button" :disabled="page >= totalPages" @click="goNext">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>
