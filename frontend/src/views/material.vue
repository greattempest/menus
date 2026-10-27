<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { auth } from '../auth'

const router = useRouter()
const materials = ref([])
const columns = ref([])
const primaryKey = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const searchQuery = ref('')
const page = ref(1)
const pageSize = 10
const dialogOpen = ref(false)
const editingMaterial = ref(null)
const formValues = ref({})

const canCreate = computed(() => auth.hasPermission('menu:create'))
const canUpdate = computed(() => auth.hasPermission('menu:update'))
const canDelete = computed(() => auth.hasPermission('menu:delete'))
const displayColumns = computed(() =>
  columns.value.filter((column) => !['id', 'delete_flag'].includes(column.name.toLowerCase())),
)
const editableColumns = computed(() => columns.value.filter((column) => {
  const name = column.name.toLowerCase()
  return name !== 'id' && name !== 'delete_flag'
    && !column.autoIncrement && (!editingMaterial.value || !column.primaryKey)
}))

const filteredMaterials = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) {
    return materials.value
  }

  return materials.value.filter((material) =>
    displayColumns.value.some(({ name }) => {
      const value = material[name]
      return value !== null && value !== undefined && String(value).toLowerCase().includes(query)
    }),
  )
})

const getColumnLabel = (columnName) => ({
  name: '名称',
  ener: '热量',
  prot: '蛋白质',
  carb: '碳水',
  fat: '脂肪',
}[columnName.toLowerCase()] ?? columnName)

const totalPages = computed(() => Math.max(1, Math.ceil(filteredMaterials.value.length / pageSize)))

const currentMaterials = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredMaterials.value.slice(start, start + pageSize)
})

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') {
    return '—'
  }

  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}

const fetchMaterials = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await fetch('/api/materials', {
      headers: { Accept: 'application/json' },
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`获取食材失败（HTTP ${response.status}）`)
    }

    const data = await response.json()
    if (!Array.isArray(data.rows) || !Array.isArray(data.columns)) {
      throw new Error('返回数据格式不正确')
    }

    materials.value = data.rows
    columns.value = data.columns
    primaryKey.value = data.primaryKey || ''
    page.value = Math.min(page.value, totalPages.value)
  } catch (err) {
    error.value = err.message || '获取食材失败'
  } finally {
    loading.value = false
  }
}

const getErrorMessage = async (response, fallback) => {
  try {
    const body = await response.json()
    return body.message || body.error || fallback
  } catch {
    return fallback
  }
}

const openCreateDialog = () => {
  editingMaterial.value = null
  formValues.value = Object.fromEntries(
    editableColumns.value.map((column) => [column.name, column.autoIncrement ? null : '']),
  )
  dialogOpen.value = true
  error.value = ''
}

const openEditDialog = (material) => {
  editingMaterial.value = material
  formValues.value = Object.fromEntries(
    editableColumns.value.map((column) => [column.name, material[column.name] ?? '']),
  )
  dialogOpen.value = true
  error.value = ''
}

const closeDialog = () => {
  if (!saving.value) {
    dialogOpen.value = false
  }
}

const buildPayload = () => Object.fromEntries(
  editableColumns.value.map((column) => {
    const value = formValues.value[column.name]
    if (value === '' && column.nullable) {
      return [column.name, null]
    }
    if (value !== '' && /^(tinyint|smallint|mediumint|int|integer|bigint|decimal|numeric|float|double|real)$/i.test(column.type)) {
      return [column.name, Number(value)]
    }
    return [column.name, value]
  }),
)

const saveMaterial = async () => {
  if (saving.value) {
    return
  }

  saving.value = true
  error.value = ''
  try {
    const isEditing = Boolean(editingMaterial.value)
    const id = isEditing ? editingMaterial.value[primaryKey.value] : null
    const url = isEditing ? `/api/materials/${encodeURIComponent(id)}` : '/api/materials'
    const response = await fetch(url, {
      method: isEditing ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      credentials: 'include',
      body: JSON.stringify(buildPayload()),
    })
    if (!response.ok) {
      throw new Error(await getErrorMessage(response, `保存失败（HTTP ${response.status}）`))
    }

    dialogOpen.value = false
    await fetchMaterials()
  } catch (err) {
    error.value = err.message || '保存食材失败'
  } finally {
    saving.value = false
  }
}

const deleteMaterial = async (material) => {
  const id = material[primaryKey.value]
  if (id === undefined || id === null || !window.confirm('确定删除这条食材记录吗？')) {
    return
  }

  error.value = ''
  try {
    const response = await fetch(`/api/materials/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(await getErrorMessage(response, `删除失败（HTTP ${response.status}）`))
    }

    await fetchMaterials()
  } catch (err) {
    error.value = err.message || '删除食材失败'
  }
}

const goBack = () => {
  router.push({ name: 'List' })
}

onMounted(fetchMaterials)
</script>

<template>
  <div class="app-shell">
    <section class="panel material-panel">
      <header class="material-header">
        <div>
          <p class="material-eyebrow">食材管理</p>
          <h1>食材列表</h1>
          <p class="material-description">共 {{ filteredMaterials.length }} 项食材</p>
        </div>
        <div class="material-header-actions">
          <button v-if="canCreate" class="material-primary-button" type="button" @click="openCreateDialog">
            新增食材
          </button>
          <button class="material-back-button" type="button" @click="goBack">返回食谱</button>
        </div>
      </header>

      <div class="search-box-wrap">
        <input
          v-model="searchQuery"
          class="search-box"
          type="search"
          placeholder="搜索食材"
          aria-label="搜索食材"
          @input="page = 1"
        />
      </div>

      <div v-if="loading" class="status">加载中...</div>
      <div v-else-if="error && !dialogOpen" class="status error">{{ error }}</div>
      <div v-else-if="columns.length === 0" class="status">食材表没有可展示的字段</div>
      <template v-else>
        <div class="material-table-wrap">
          <table class="material-table">
            <thead>
              <tr>
                <th v-for="column in displayColumns" :key="column.name">{{ getColumnLabel(column.name) }}</th>
                <th v-if="(canUpdate || canDelete) && primaryKey">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(material, index) in currentMaterials" :key="material[primaryKey] ?? index">
                <td v-for="column in displayColumns" :key="column.name">
                  {{ formatValue(material[column.name]) }}
                </td>
                <td v-if="(canUpdate || canDelete) && primaryKey" class="material-row-actions">
                  <button v-if="canUpdate" class="material-text-button" type="button" @click="openEditDialog(material)">
                    编辑
                  </button>
                  <button v-if="canDelete" class="material-text-button danger" type="button" @click="deleteMaterial(material)">
                    删除
                  </button>
                </td>
              </tr>
              <tr v-if="currentMaterials.length === 0">
                <td :colspan="displayColumns.length + ((canUpdate || canDelete) && primaryKey ? 1 : 0)" class="material-empty">
                  {{ materials.length === 0 ? '暂无食材数据' : '没有找到相关食材' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <nav v-if="filteredMaterials.length > 0" class="material-pagination" aria-label="食材列表分页">
          <button :disabled="page === 1" type="button" @click="page -= 1">上一页</button>
          <span>{{ page }} / {{ totalPages }}</span>
          <button :disabled="page >= totalPages" type="button" @click="page += 1">下一页</button>
        </nav>
      </template>
    </section>

    <div v-if="dialogOpen" class="material-dialog-backdrop" @click.self="closeDialog">
      <section class="material-dialog" role="dialog" aria-modal="true" :aria-labelledby="'material-dialog-title'">
        <header class="material-dialog-header">
          <h2 id="material-dialog-title">{{ editingMaterial ? '编辑食材' : '新增食材' }}</h2>
          <button class="material-close-button" type="button" aria-label="关闭" @click="closeDialog">×</button>
        </header>
        <form @submit.prevent="saveMaterial">
          <div class="material-form-fields">
            <label v-for="column in editableColumns" :key="column.name" class="material-form-field">
              <span>{{ getColumnLabel(column.name) }}<strong v-if="!column.nullable"> *</strong></span>
              <textarea
                v-if="/text|json|blob/i.test(column.type)"
                v-model="formValues[column.name]"
                :required="!column.nullable"
                rows="3"
              />
              <input
                v-else
                v-model="formValues[column.name]"
                :type="/^(tinyint|smallint|mediumint|int|integer|bigint|decimal|numeric|float|double|real)$/i.test(column.type) ? 'number' : 'text'"
                :step="/^(decimal|numeric|float|double|real)$/i.test(column.type) ? 'any' : undefined"
                :required="!column.nullable"
              />
            </label>
          </div>
          <p v-if="error" class="material-form-error">{{ error }}</p>
          <footer class="material-dialog-actions">
            <button class="material-back-button" type="button" :disabled="saving" @click="closeDialog">取消</button>
            <button class="material-primary-button" type="submit" :disabled="saving || editableColumns.length === 0">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.material-panel {
  min-width: 0;
}

.material-header,
.material-header-actions,
.material-dialog-header,
.material-dialog-actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.material-header {
  margin-bottom: 24px;
}

.material-header-actions {
  align-items: center;
}

.material-eyebrow {
  margin: 0 0 8px;
  color: #6366f1;
  font-size: 0.85rem;
  font-weight: 700;
}

.material-description {
  margin: 8px 0 0;
  color: #64748b;
}

.material-back-button,
.material-primary-button,
.material-pagination button {
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #fff;
  color: #374151;
  padding: 9px 14px;
  cursor: pointer;
}

.material-primary-button {
  border-color: #4f46e5;
  background: #4f46e5;
  color: #fff;
}

.material-back-button:hover,
.material-pagination button:not(:disabled):hover {
  border-color: #818cf8;
  color: #4f46e5;
}

.material-primary-button:hover {
  background: #4338ca;
}

.material-primary-button:disabled,
.material-back-button:disabled,
.material-pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.material-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.material-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.material-table th,
.material-table td {
  min-width: 120px;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  vertical-align: top;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.material-table th {
  background: #f8fafc;
  color: #475569;
  font-size: 0.875rem;
  font-weight: 700;
}

.material-table tbody tr:last-child td {
  border-bottom: 0;
}

.material-row-actions {
  white-space: nowrap;
}

.material-text-button,
.material-close-button {
  border: 0;
  background: transparent;
  color: #4f46e5;
  cursor: pointer;
  padding: 4px 8px;
}

.material-text-button.danger {
  color: #dc2626;
}

.material-empty {
  color: #64748b;
  text-align: center;
}

.material-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
  color: #64748b;
}

.material-dialog-backdrop {
  position: fixed;
  z-index: 10;
  inset: 0;
  display: grid;
  place-items: center;
  overflow-y: auto;
  padding: 24px;
  background: rgba(15, 23, 42, 0.45);
}

.material-dialog {
  width: min(100%, 600px);
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  padding: 24px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.2);
}

.material-dialog-header {
  align-items: center;
  margin-bottom: 20px;
}

.material-dialog-header h2 {
  margin: 0;
  font-size: 1.25rem;
}

.material-close-button {
  color: #64748b;
  font-size: 1.5rem;
}

.material-form-fields {
  display: grid;
  gap: 16px;
}

.material-form-field {
  display: grid;
  gap: 7px;
  color: #374151;
  font-size: 0.9rem;
  font-weight: 600;
}

.material-form-field strong {
  color: #dc2626;
}

.material-form-field input,
.material-form-field textarea {
  width: 100%;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #f8fafc;
  color: #111827;
  padding: 10px 12px;
  font: inherit;
}

.material-form-field textarea {
  resize: vertical;
}

.material-form-error {
  margin: 16px 0 0;
  color: #b91c1c;
}

.material-dialog-actions {
  justify-content: flex-end;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .material-header {
    align-items: stretch;
    flex-direction: column;
  }

  .material-header-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .material-back-button,
  .material-primary-button {
    align-self: flex-start;
  }
}
</style>
