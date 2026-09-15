<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

interface NoteTag {
  id: number
  name: string
  content: string
}

const storageKey = 'webnote-notes'
const cookieName = 'webnote-notes'
const oldCookieName = 'webnote-content'
const tags = ref<NoteTag[]>([{ id: Date.now(), name: '我的笔记', content: '' }])
const activeTagId = ref(tags.value[0].id)
const fileInput = ref<HTMLInputElement | null>(null)
const importMessage = ref('')
const previewTagId = ref<number | null>(null)
let previewTimer: ReturnType<typeof setTimeout> | null = null
const activeTag = computed(() => tags.value.find((tag) => tag.id === activeTagId.value) ?? tags.value[0])

function readCookie(name: string) {
  const cookie = document.cookie.split('; ').find((item) => item.startsWith(`${name}=`))
  return cookie ? decodeURIComponent(cookie.substring(name.length + 1)) : null
}

function readSavedTags() {
  try {
    return localStorage.getItem(storageKey)
  } catch {
    return null
  }
}

function saveTags(value: NoteTag[]) {
  try {
    localStorage.setItem(storageKey, JSON.stringify(value))
  } catch {
    // Keep the editor usable when storage is unavailable or full.
  }
}

onMounted(() => {
  const savedTags = readSavedTags() ?? readCookie(cookieName)
  const savedOldNote = readCookie(oldCookieName)

  if (savedTags) {
    try {
      const parsedTags = JSON.parse(savedTags) as NoteTag[]
      if (parsedTags.length > 0) {
        tags.value = parsedTags
        activeTagId.value = parsedTags[0].id
        saveTags(parsedTags)
      }
    } catch {
      // Ignore invalid cookie data and keep the default note.
    }
  } else if (savedOldNote !== null) {
    tags.value[0].content = savedOldNote
  }
})

watch(tags, saveTags, { deep: true })

function getPreview(content: string) {
  const encoder = new TextEncoder()
  let byteLength = 0
  let preview = ''

  for (const character of content) {
    const characterLength = encoder.encode(character).length
    if (byteLength + characterLength > 100) {
      break
    }

    preview += character
    byteLength += characterLength
  }

  return preview + (preview.length < content.length ? '……' : '')
}

function showTagPreview(tagId: number) {
  if (previewTimer) {
    clearTimeout(previewTimer)
  }

  previewTagId.value = null
  previewTimer = setTimeout(() => {
    previewTagId.value = tagId
  }, 2000)
}

function hideTagPreview() {
  if (previewTimer) {
    clearTimeout(previewTimer)
    previewTimer = null
  }

  previewTagId.value = null
}

function addTag() {
  const newTag: NoteTag = {
    id: Date.now(),
    name: `新笔记 ${tags.value.length + 1}`,
    content: '',
  }
  tags.value.push(newTag)
  activeTagId.value = newTag.id
}

function removeTag(tagId: number) {
  if (tags.value.length === 1) {
    return
  }

  const tagToRemove = tags.value.find((tag) => tag.id === tagId)
  if (!tagToRemove || !window.confirm(`确定要删除“${tagToRemove.name}”吗？其中的内容也会被删除。`)) {
    return
  }

  const tagIndex = tags.value.findIndex((tag) => tag.id === tagId)
  tags.value = tags.value.filter((tag) => tag.id !== tagId)

  if (activeTagId.value === tagId) {
    activeTagId.value = tags.value[Math.max(0, tagIndex - 1)].id
  }
}

function exportNotes() {
  const backup = JSON.stringify({ version: 1, exportedAt: new Date().toISOString(), tags: tags.value }, null, 2)
  const downloadUrl = URL.createObjectURL(new Blob([backup], { type: 'application/json' }))
  const link = document.createElement('a')

  link.href = downloadUrl
  link.download = `webnote-${new Date().toISOString().slice(0, 10)}.json`
  link.click()
  URL.revokeObjectURL(downloadUrl)
}

function openImportPicker() {
  fileInput.value?.click()
}

function isValidTags(value: unknown): value is NoteTag[] {
  return Array.isArray(value) && value.length > 0 && value.every((tag) => {
    if (!tag || typeof tag !== 'object') {
      return false
    }

    const importedTag = tag as Partial<NoteTag>
    return typeof importedTag.id === 'number'
      && typeof importedTag.name === 'string'
      && typeof importedTag.content === 'string'
  })
}

async function importNotes(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''

  if (!file) {
    return
  }

  try {
    const importedData = JSON.parse(await file.text()) as unknown
    const importedTags = Array.isArray(importedData)
      ? importedData
      : importedData && typeof importedData === 'object' && 'tags' in importedData
        ? (importedData as { tags: unknown }).tags
        : null

    if (!isValidTags(importedTags)) {
      throw new Error('invalid notes')
    }

    tags.value = importedTags
    activeTagId.value = importedTags[0].id
    importMessage.value = `已导入 ${importedTags.length} 条笔记`
  } catch {
    importMessage.value = '导入失败，请选择有效的笔记 JSON 文件'
  }
}
</script>

<template>
  <main class="note-page">
    <section class="note-card">
      <div class="editor-layout">
        <aside class="tag-sidebar">
          <p class="eyebrow">Personal Note</p>
          <h1>随手记</h1>
          <div class="tag-list" role="tablist" aria-label="笔记标签">
            <div v-for="tag in tags" :key="tag.id" class="tag-item" @mouseenter="showTagPreview(tag.id)"
              @mouseleave="hideTagPreview">
              <button class="tag-button" :class="{ active: tag.id === activeTagId }" type="button" role="tab"
                :aria-selected="tag.id === activeTagId" @click="activeTagId = tag.id">
                {{ tag.name }}
              </button>
              <div v-if="previewTagId === tag.id" class="tag-preview" role="tooltip">
                {{ getPreview(tag.content) || '暂无内容' }}
              </div>
            </div>
          </div>
          <input v-model="activeTag.name" class="tag-name" aria-label="笔记名称" />
          <div class="tag-actions">
            <button class="action-button add-button" type="button" @click="addTag">新增笔记</button>
            <button class="action-button delete-button" type="button" :disabled="tags.length === 1"
              @click="removeTag(activeTagId)">
              删除笔记
            </button>
            <button class="transfer-button" type="button" @click="exportNotes">导出笔记</button>
            <button class="transfer-button" type="button" @click="openImportPicker">导入笔记</button>
            <input ref="fileInput" class="file-input" type="file" accept=".json,application/json" aria-label="导入笔记文件"
              @change="importNotes" />
          </div>
        </aside>
        <section class="editor-area">
          <textarea v-model="activeTag.content" aria-label="笔记内容" placeholder="写下此刻的想法……" />
          <p class="status">每个笔记的内容会自动保存在当前浏览器中<span v-if="importMessage"> · {{ importMessage }}</span></p>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.note-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px 20px;
  box-sizing: border-box;
  background: #f4f0e8;
  color: #24312d;
}

.note-card {
  width: min(100%, 1360px);
  padding: clamp(28px, 6vw, 64px);
  box-sizing: border-box;
  border: 1px solid #d8d0c1;
  background: #fffdf8;
  box-shadow: 12px 12px 0 #d9e2d8;
}

.eyebrow {
  margin: 0 0 12px;
  color: #66816d;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

h1 {
  margin: 0 0 24px;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(36px, 7vw, 48px);
  font-weight: 400;
  letter-spacing: 0;
}

.editor-layout {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: clamp(24px, 5vw, 56px);
  min-height: min(68vh, 720px);
}

.tag-sidebar {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-right: 1px solid #d8d0c1;
  padding-right: 24px;
}

.tag-list {
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  position: relative;
}

.tag-preview {
  position: absolute;
  z-index: 2;
  top: calc(100% + 8px);
  left: 0;
  width: min(280px, calc(100vw - 64px));
  padding: 10px 12px;
  box-sizing: border-box;
  border: 1px solid #b8c5b9;
  background: #fffdf8;
  box-shadow: 4px 4px 0 #d9e2d8;
  color: #52665a;
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  pointer-events: none;
}

.tag-actions {
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  border-top: 1px solid #d8d0c1;
  padding-top: 16px;
  gap: 8px;
}

.tag-button,
.action-button {
  width: 100%;
  text-align: left;
}

button {
  padding: 9px 13px;
  border: 1px solid #b8c5b9;
  border-radius: 0;
  background: #fffdf8;
  color: #52665a;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
}

button:hover,
button.active {
  border-color: #66816d;
  background: #d9e2d8;
  color: #24312d;
}

.action-button {
  border-color: #66816d;
  background: #66816d;
  color: #fffdf8;
  text-align: center;
}

.action-button:hover {
  border-color: #52665a;
  background: #52665a;
  color: #fffdf8;
}

.delete-button {
  border-color: #c78d76;
  background: transparent;
  color: #a05e48;
}

.delete-button:hover {
  border-color: #a05e48;
  background: #f3ded5;
  color: #7d4534;
}

.transfer-button {
  border-color: #9ba79e;
  background: #eef2ed;
  color: #52665a;
  text-align: center;
}

.transfer-button:hover {
  border-color: #66816d;
  background: #d9e2d8;
  color: #24312d;
}

.file-input {
  display: none;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.tag-name {
  width: 100%;
  margin-bottom: 0;
  padding: 8px 0;
  border: 0;
  border-bottom: 1px solid #d8d0c1;
  outline: none;
  background: transparent;
  color: #52665a;
  font: inherit;
  font-size: 14px;
}

.tag-name:focus {
  border-color: #66816d;
}

.editor-area {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

textarea {
  display: block;
  width: 100%;
  min-height: 0;
  height: 100%;
  flex: 1;
  padding: 16px;
  box-sizing: border-box;
  resize: vertical;
  border: 1px solid #b8c5b9;
  border-radius: 0;
  outline: none;
  background: #f8faf5;
  color: inherit;
  font: inherit;
  font-size: 17px;
  line-height: 1.7;
}

textarea:focus {
  border-color: #66816d;
  box-shadow: 0 0 0 3px #d9e2d8;
}

.status {
  margin: 14px 0 0;
  color: #718079;
  font-size: 13px;
}

@media (max-width: 560px) {
  .editor-layout {
    display: block;
    min-height: 0;
  }

  .tag-sidebar {
    border-right: 0;
    border-bottom: 1px solid #d8d0c1;
    margin-bottom: 24px;
    padding: 0 0 20px;
  }

  .tag-list {
    flex-direction: row;
  }

  .tag-item {
    flex: 0 0 auto;
  }

  .tag-button {
    width: auto;
  }

  .tag-actions {
    margin-top: 18px;
    flex-direction: row;
  }

  .action-button {
    width: auto;
  }

  .tag-name {
    margin-top: 18px;
  }

  textarea {
    min-height: 55vh;
  }
}
</style>