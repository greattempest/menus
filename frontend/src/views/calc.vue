<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const materials = ref([])
const loading = ref(true)
const error = ref('')
const rows = ref([{ materialId: '', weight: '' }])
const nutrientFields = [
  { key: 'prot', label: '蛋白质' },
  { key: 'carb', label: '碳水' },
  { key: 'fat', label: '脂肪' },
]

const selectedRows = computed(() => rows.value
  .map((row) => {
    const material = materials.value.find((item) => String(item.id) === String(row.materialId))
    const weight = Number(row.weight)
    const energyPer100g = Number(material?.ener)
    const hasEnergy = material && material.ener !== null && material.ener !== ''
    const nutrients = Object.fromEntries(nutrientFields.map(({ key }) => {
      const value = material?.[key]
      const per100g = Number(value)
      const hasValue = value !== null && value !== undefined && value !== ''
        && Number.isFinite(per100g)

      return [key, {
        amount: hasValue && Number.isFinite(weight) && weight > 0 ? per100g * weight / 100 : 0,
        hasValue,
      }]
    }))

    return {
      ...row,
      material,
      weight,
      energyPer100g,
      nutrients,
      calories: material && hasEnergy && Number.isFinite(weight) && weight > 0
        && Number.isFinite(energyPer100g)
        ? energyPer100g * weight / 100
        : 0,
    }
  }))

const totalWeight = computed(() => selectedRows.value.reduce((sum, row) => {
  return row.material && Number.isFinite(row.weight) && row.weight > 0 ? sum + row.weight : sum
}, 0))

const totalCalories = computed(() => selectedRows.value.reduce((sum, row) => {
  return row.material ? sum + row.calories : sum
}, 0))

const nutrientTotals = computed(() => Object.fromEntries(nutrientFields.map(({ key }) => {
  const rowsWithWeight = selectedRows.value.filter((row) =>
    row.material && Number.isFinite(row.weight) && row.weight > 0,
  )

  return [key, {
    amount: rowsWithWeight.reduce((sum, row) => sum + row.nutrients[key].amount, 0),
    incomplete: rowsWithWeight.some((row) => !row.nutrients[key].hasValue),
  }]
})))

const hasInvalidEnergy = computed(() => selectedRows.value.some((row) => {
  return row.material && (row.material.ener === null || row.material.ener === ''
    || !Number.isFinite(Number(row.material.ener))
  )
}))

const formatNumber = (value) => new Intl.NumberFormat('zh-CN', {
  maximumFractionDigits: 2,
}).format(value)

const addIngredient = () => {
  rows.value.push({ materialId: '', weight: '' })
}

const removeIngredient = (index) => {
  rows.value.splice(index, 1)
  if (rows.value.length === 0) {
    addIngredient()
  }
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
    if (!Array.isArray(data.rows)) {
      throw new Error('返回数据格式不正确')
    }

    materials.value = data.rows
  } catch (err) {
    error.value = err.message || '获取食材失败'
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push({ name: 'List' })
}

onMounted(fetchMaterials)
</script>

<template>
  <div class="app-shell">
    <section class="panel calculator-panel">
      <header class="calculator-header">
        <div>
          <p class="calculator-eyebrow">菜肴工具</p>
          <h1>菜肴热量计算器</h1>
          <p class="calculator-description">按食材每 100 克的热量，输入实际使用重量计算。</p>
        </div>
        <button class="calculator-back-button" type="button" @click="goBack">返回食谱</button>
      </header>

      <div v-if="loading" class="status">正在加载食材...</div>
      <div v-else-if="error" class="status error">{{ error }}</div>
      <div v-else-if="materials.length === 0" class="status">暂无可用食材，请先在食材管理中添加食材。</div>
      <template v-else>
        <div class="calculator-ingredients">
          <div class="calculator-ingredients-heading">
            <h2>食材与重量</h2>
            <button class="calculator-add-button" type="button" @click="addIngredient">＋ 添加食材</button>
          </div>

          <div v-for="(row, index) in rows" :key="index" class="ingredient-row">
            <label class="ingredient-field">
              <span>食材</span>
              <select v-model="row.materialId">
                <option value="">请选择食材</option>
                <option
                  v-for="material in materials"
                  :key="material.id"
                  :value="String(material.id)"
                >
                  {{ material.name }}{{ material.ener !== null && material.ener !== '' ? `（${material.ener} 千卡/100克）` : '（未填写热量）' }}
                </option>
              </select>
            </label>
            <label class="ingredient-field">
              <span>重量（克）</span>
              <input
                v-model="row.weight"
                type="number"
                min="0"
                step="any"
                placeholder="输入克数"
              />
            </label>
            <div class="ingredient-calories">
              <span>该项热量</span>
              <strong>{{ formatNumber(selectedRows[index]?.calories ?? 0) }} 千卡</strong>
            </div>
            <button
              class="calculator-remove-button"
              type="button"
              :aria-label="`移除第 ${index + 1} 项食材`"
              @click="removeIngredient(index)"
            >
              移除
            </button>
          </div>
        </div>

        <section class="calculator-result" aria-live="polite">
          <p>总重量 <strong>{{ formatNumber(totalWeight) }} 克</strong></p>
          <p v-for="nutrient in nutrientFields" :key="nutrient.key">
            {{ nutrient.label }}
            <strong>
              {{ formatNumber(nutrientTotals[nutrient.key].amount) }} 克<span
                v-if="nutrientTotals[nutrient.key].incomplete"
                class="incomplete-marker"
                :aria-label="`${nutrient.label}数据不完整`"
              >*</span>
            </strong>
          </p>
          <p>成品总热量 <strong>{{ formatNumber(totalCalories) }} 千卡</strong></p>
        </section>
        <p v-if="nutrientFields.some(({ key }) => nutrientTotals[key].incomplete)" class="calculator-note">
          带 <span class="incomplete-marker">*</span> 的营养素总量仅统计有数据的食材。
        </p>
        <p v-if="hasInvalidEnergy" class="calculator-note">
          有食材未填写有效热量，该项暂按 0 千卡计算，请先完善食材热量数据。
        </p>
        <p class="calculator-footnote">
          计算方式：各食材热量 × 使用重量 ÷ 100 后求和；未计入烹饪过程中的水分变化。
        </p>
      </template>
    </section>
  </div>
</template>

<style scoped>
.calculator-panel {
  min-width: 0;
}

.calculator-header,
.calculator-ingredients-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.calculator-header {
  margin-bottom: 28px;
}

.calculator-eyebrow {
  margin: 0 0 8px;
  color: #6366f1;
  font-size: 0.85rem;
  font-weight: 700;
}

.calculator-description,
.calculator-footnote,
.calculator-note {
  color: #64748b;
}

.calculator-description {
  margin: 8px 0 0;
}

.calculator-back-button,
.calculator-add-button,
.calculator-remove-button {
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #fff;
  color: #374151;
  padding: 9px 14px;
  cursor: pointer;
}

.calculator-back-button:hover,
.calculator-add-button:hover {
  border-color: #818cf8;
  color: #4f46e5;
}

.calculator-add-button {
  color: #4f46e5;
}

.calculator-ingredients-heading {
  align-items: center;
  margin-bottom: 16px;
}

.calculator-ingredients-heading h2 {
  margin: 0;
  font-size: 1.1rem;
}

.ingredient-row {
  display: grid;
  grid-template-columns: minmax(180px, 2fr) minmax(120px, 1fr) minmax(130px, 1fr) auto;
  align-items: end;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid #e5e7eb;
}

.ingredient-field {
  display: grid;
  gap: 7px;
  color: #374151;
  font-size: 0.9rem;
  font-weight: 600;
}

.ingredient-field input,
.ingredient-field select {
  width: 100%;
  min-height: 42px;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #f8fafc;
  color: #111827;
  padding: 9px 12px;
  font: inherit;
}

.ingredient-calories {
  display: grid;
  gap: 7px;
  color: #64748b;
  font-size: 0.85rem;
}

.ingredient-calories strong {
  color: #111827;
  font-size: 1rem;
}

.calculator-remove-button {
  color: #dc2626;
}

.calculator-result {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px 24px;
  margin-top: 24px;
  padding: 20px;
  border: 1px solid #c7d2fe;
  border-radius: 14px;
  background: #eef2ff;
}

.calculator-result p {
  margin: 0;
  color: #475569;
}

.calculator-result strong {
  margin-left: 8px;
  color: #3730a3;
  font-size: 1.25rem;
}

.incomplete-marker {
  color: #dc2626;
}

.calculator-note {
  margin: 14px 0 0;
  color: #b45309;
  font-size: 0.9rem;
}

.calculator-footnote {
  margin: 18px 0 0;
  font-size: 0.85rem;
}

@media (max-width: 760px) {
  .calculator-header {
    align-items: stretch;
    flex-direction: column;
  }

  .calculator-back-button {
    align-self: flex-start;
  }

  .ingredient-row {
    grid-template-columns: 1fr 1fr;
  }

  .ingredient-field:first-child {
    grid-column: span 2;
  }
}

@media (max-width: 480px) {
  .ingredient-row {
    grid-template-columns: 1fr;
  }

  .ingredient-field:first-child {
    grid-column: auto;
  }
}
</style>
