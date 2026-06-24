<script setup>
import { ref, computed, onMounted, onUnmounted, watch, markRaw } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { toolRegistry, getToolList } from '../tools/registry'
import { getCustomPlugins, customPlugins, loadCustomPlugins } from '../tools/customRegistry'
import { clearToolState, clearAllToolState } from '../composables/useToolState'

// 网格配置
const GRID_COLS = 12
const GRID_COL_WIDTH = 80 // 每列约 80px（用于计算拖拽调整大小的格子数）
const GRID_ROW_HEIGHT = 100 // 每行 100px

// 布局缓存 key
const LAYOUT_KEY = 'dashboard-layout'
const ADD_BTN_KEY = 'dashboard-add-btn'

// 工具默认大小（colSpan × rowSpan）
const defaultSizes = {
  'json-formatter': { colSpan: 6, rowSpan: 3 },
  'base64-converter': { colSpan: 6, rowSpan: 3 },
  'timestamp-converter': { colSpan: 4, rowSpan: 2 },
  'text-editor': { colSpan: 4, rowSpan: 4 },
  'storage-converter': { colSpan: 4, rowSpan: 4 },
  'calculator': { colSpan: 4, rowSpan: 4 },
  'base-converter': { colSpan: 4, rowSpan: 4 },
  'ip-converter': { colSpan: 4, rowSpan: 4 },
  'diff-tool': { colSpan: 6, rowSpan: 4 },
  'text-compare': { colSpan: 6, rowSpan: 4 }
}

// 工具卡片列表
const cards = ref([])
const showToolPicker = ref(false)

// 工具选择弹窗：搜索和分类筛选
const toolSearch = ref('')
const toolCategoryFilter = ref('')

// 所有可用工具（内置 + 自定义）
const allAvailableTools = computed(() => [...getToolList(), ...getCustomPlugins('DASHBOARD')])

// 所有分类
const toolCategories = computed(() => {
  const cats = new Set(allAvailableTools.value.map(t => t.category))
  return Array.from(cats)
})

// 筛选后的工具
const filteredTools = computed(() => {
  let list = allAvailableTools.value
  if (toolCategoryFilter.value) {
    list = list.filter(t => t.category === toolCategoryFilter.value)
  }
  const kw = toolSearch.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(t => t.name.toLowerCase().includes(kw) || t.id.toLowerCase().includes(kw))
  }
  return list
})

// 调整大小状态
const resizing = ref(null)

// 拖拽排序状态
const dragId = ref(null)
const isDragging = ref(false) // 是否正在拖拽中（用于显示网格虚线和变暗效果）

// ---- "+" 按钮状态 ----
const addBtnPos = ref({ x: -1, y: -1 }) // -1 表示使用默认位置
const addBtnSize = ref(56) // 按钮大小
const addBtnDragging = ref(null) // 拖拽状态

// ---- 布局缓存 ----
const saveLayout = () => {
  try {
    const layout = cards.value.map((c) => ({
      id: c.id,
      toolId: c.toolId,
      title: c.title,
      colSpan: c.colSpan,
      rowSpan: c.rowSpan
    }))
    localStorage.setItem(LAYOUT_KEY, JSON.stringify(layout))
  } catch (e) {
    console.warn('保存仪表盘布局失败:', e)
  }
}

const loadLayout = () => {
  try {
    const saved = localStorage.getItem(LAYOUT_KEY)
    if (saved) {
      const layout = JSON.parse(saved)
      cards.value = layout
        .map((item) => {
          let tool = toolRegistry[item.toolId]
          if (!tool) {
            tool = customPlugins.value.find(p => p.id === item.toolId)
          }
          if (!tool) return null
          return {
            id: item.id,
            toolId: item.toolId,
            title: item.title,
            colSpan: item.colSpan,
            rowSpan: item.rowSpan,
            component: markRaw(tool.component)
          }
        })
        .filter(Boolean)
    }
  } catch (e) {
    console.warn('恢复仪表盘布局失败:', e)
  }
}

// 保存/恢复 "+" 按钮位置和大小
const saveAddBtn = () => {
  try {
    localStorage.setItem(ADD_BTN_KEY, JSON.stringify({
      pos: addBtnPos.value,
      size: addBtnSize.value
    }))
  } catch (e) {}
}

const loadAddBtn = () => {
  try {
    const saved = localStorage.getItem(ADD_BTN_KEY)
    if (saved) {
      const data = JSON.parse(saved)
      addBtnPos.value = data.pos || { x: -1, y: -1 }
      addBtnSize.value = data.size || 56
    }
  } catch (e) {}
}

// 监听变化自动保存
watch(cards, saveLayout, { deep: true })
watch([addBtnPos, addBtnSize], saveAddBtn, { deep: true })

// 添加工具
const addTool = (toolId) => {
  // 先从内置注册表查找，再从自定义插件查找
  let tool = toolRegistry[toolId]
  if (!tool) {
    tool = customPlugins.value.find(p => p.id === toolId)
  }
  if (!tool) return
  const id = `card-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`
  const size = defaultSizes[toolId] || { colSpan: 4, rowSpan: 2 }
  cards.value.push({
    id,
    toolId,
    title: tool.name,
    colSpan: size.colSpan,
    rowSpan: size.rowSpan,
    component: markRaw(tool.component)
  })
  showToolPicker.value = false
}

// 删除工具
const removeCard = (id) => {
  const card = cards.value.find((c) => c.id === id)
  if (card) clearToolState(id)
  cards.value = cards.value.filter((c) => c.id !== id)
}

// 一键清除
const clearDashboard = () => {
  if (cards.value.length === 0) {
    ElMessage.info('仪表盘已是空的')
    return
  }
  ElMessageBox.confirm('确定要清除仪表盘中所有工具吗？所有工具的输入内容也将被清除。', '清除确认', {
    confirmButtonText: '确定清除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      clearAllToolState()
      cards.value = []
      ElMessage.success('仪表盘已清空')
    })
    .catch(() => {})
}

// 窗口大小变化时，约束 "+" 按钮位置在可视区域内
const clampAddBtnPos = () => {
  if (addBtnPos.value.x < 0) return // 使用默认位置，无需约束
  const maxX = window.innerWidth - addBtnSize.value
  const maxY = window.innerHeight - addBtnSize.value
  addBtnPos.value = {
    x: Math.max(0, Math.min(maxX, addBtnPos.value.x)),
    y: Math.max(0, Math.min(maxY, addBtnPos.value.y))
  }
}

onMounted(async () => {
  // 刷新自定义插件列表，确保后台禁用的插件不再显示
  await loadCustomPlugins()
  loadLayout()
  loadAddBtn()
  window.addEventListener('resize', clampAddBtnPos)
})

// ---- 拖拽排序（仅标题栏可拖拽）----
const onDragStart = (e, id) => {
  dragId.value = id
  isDragging.value = true
  e.dataTransfer.effectAllowed = 'move'
  // 设置半透明的拖拽影像
  e.dataTransfer.setDragImage(e.target.closest('.tool-card'), 0, 0)
}

const onDragOver = (e) => {
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
}

const onDrop = (e, targetId) => {
  e.preventDefault()
  if (dragId.value && dragId.value !== targetId) {
    const fromIdx = cards.value.findIndex((c) => c.id === dragId.value)
    const toIdx = cards.value.findIndex((c) => c.id === targetId)
    if (fromIdx >= 0 && toIdx >= 0) {
      const temp = cards.value[fromIdx]
      cards.value[fromIdx] = cards.value[toIdx]
      cards.value[toIdx] = temp
    }
  }
  dragId.value = null
  isDragging.value = false
}

const onDragEnd = () => {
  dragId.value = null
  isDragging.value = false
}

// ---- 调整大小 ----
const startResize = (e, card) => {
  resizing.value = {
    cardId: card.id,
    startX: e.clientX,
    startY: e.clientY,
    origColSpan: card.colSpan,
    origRowSpan: card.rowSpan
  }
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
  e.preventDefault()
  e.stopPropagation()
}

const onMouseMove = (e) => {
  if (!resizing.value) return
  const dx = e.clientX - resizing.value.startX
  const dy = e.clientY - resizing.value.startY
  const deltaCol = Math.round(dx / GRID_COL_WIDTH)
  const deltaRow = Math.round(dy / GRID_ROW_HEIGHT)
  const card = cards.value.find((c) => c.id === resizing.value.cardId)
  if (card) {
    card.colSpan = Math.max(2, Math.min(GRID_COLS, resizing.value.origColSpan + deltaCol))
    card.rowSpan = Math.max(1, resizing.value.origRowSpan + deltaRow)
  }
}

const onMouseUp = () => {
  resizing.value = null
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
}

// ---- "+" 按钮拖拽 ----
const startAddBtnDrag = (e) => {
  // 计算初始位置（如果未自定义位置，使用当前默认位置）
  const btn = e.currentTarget
  const rect = btn.getBoundingClientRect()
  const startX = addBtnPos.value.x >= 0 ? addBtnPos.value.x : rect.left
  const startY = addBtnPos.value.y >= 0 ? addBtnPos.value.y : rect.top

  addBtnDragging.value = {
    startX,
    startY,
    mouseStartX: e.clientX,
    mouseStartY: e.clientY,
    moved: false
  }

  // 初始化位置
  if (addBtnPos.value.x < 0) {
    addBtnPos.value = { x: rect.left, y: rect.top }
  }

  document.addEventListener('mousemove', onAddBtnMouseMove)
  document.addEventListener('mouseup', onAddBtnMouseUp)
  e.preventDefault()
  e.stopPropagation()
}

const onAddBtnMouseMove = (e) => {
  if (!addBtnDragging.value) return
  const dx = e.clientX - addBtnDragging.value.mouseStartX
  const dy = e.clientY - addBtnDragging.value.mouseStartY

  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
    addBtnDragging.value.moved = true
  }

  addBtnPos.value = {
    x: Math.max(0, Math.min(window.innerWidth - addBtnSize.value, addBtnDragging.value.startX + dx)),
    y: Math.max(0, Math.min(window.innerHeight - addBtnSize.value, addBtnDragging.value.startY + dy))
  }
}

const onAddBtnMouseUp = (e) => {
  if (addBtnDragging.value && !addBtnDragging.value.moved) {
    // 没有移动，视为点击 → 打开工具选择
    showToolPicker.value = true
  }
  addBtnDragging.value = null
  document.removeEventListener('mousemove', onAddBtnMouseMove)
  document.removeEventListener('mouseup', onAddBtnMouseUp)
}

// "+" 按钮滚轮缩放
const onAddBtnWheel = (e) => {
  e.preventDefault()
  const delta = e.deltaY > 0 ? -4 : 4
  addBtnSize.value = Math.max(32, Math.min(80, addBtnSize.value + delta))
}

onUnmounted(() => {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
  document.removeEventListener('mousemove', onAddBtnMouseMove)
  document.removeEventListener('mouseup', onAddBtnMouseUp)
  window.removeEventListener('resize', clampAddBtnPos)
})
</script>

<template>
  <div class="dashboard" :class="{ 'drag-active': isDragging }">
    <!-- 空状态 -->
    <div v-if="cards.length === 0" class="empty-state">
      <el-empty description="点击右下角 + 添加工具到仪表盘">
        <el-button type="primary" @click="showToolPicker = true">添加工具</el-button>
      </el-empty>
    </div>

    <!-- 顶部操作栏 -->
    <div v-if="cards.length > 0" class="dash-toolbar">
      <span class="dash-count">共 {{ cards.length }} 个工具</span>
      <div class="toolbar-actions">
        <el-button type="primary" size="small" :icon="Plus" @click="showToolPicker = true">添加工具</el-button>
        <el-button type="danger" size="small" plain @click="clearDashboard">一键清除</el-button>
      </div>
    </div>

    <!-- 网格容器 -->
    <div v-if="cards.length > 0" class="grid-container">
      <div
        v-for="card in cards"
        :key="card.id"
        class="tool-card"
        :class="{
          dragging: dragId === card.id,
          'drag-dimmed': isDragging && dragId !== card.id
        }"
        :style="{
          gridColumn: `span ${card.colSpan}`,
          gridRow: `span ${card.rowSpan}`
        }"
        @dragover="onDragOver"
        @drop="onDrop($event, card.id)"
      >
        <!-- 标题栏（仅标题栏可拖拽） -->
        <div
          class="card-header"
          draggable="true"
          @dragstart="onDragStart($event, card.id)"
          @dragend="onDragEnd"
        >
          <span class="card-title">{{ card.title }}</span>
          <span class="card-size">{{ card.colSpan }}×{{ card.rowSpan }}</span>
          <button class="card-close" @click="removeCard(card.id)">×</button>
        </div>
        <!-- 工具内容 -->
        <div class="card-body">
          <component :is="card.component" :instanceId="card.id" :plugin-id="card.toolId" />
        </div>
        <!-- 右下角调整大小 -->
        <div class="resize-handle" @mousedown="startResize($event, card)" />
      </div>
    </div>

    <!-- 浮动添加按钮（可拖拽移动、滚轮缩放） -->
    <div
      class="add-button"
      :style="{
        width: addBtnSize + 'px',
        height: addBtnSize + 'px',
        fontSize: (addBtnSize * 0.5) + 'px',
        left: addBtnPos.x >= 0 ? addBtnPos.x + 'px' : 'auto',
        right: addBtnPos.x >= 0 ? 'auto' : '32px',
        bottom: addBtnPos.x >= 0 ? 'auto' : '32px',
        top: addBtnPos.x >= 0 ? addBtnPos.y + 'px' : 'auto'
      }"
      @mousedown="startAddBtnDrag"
      @wheel="onAddBtnWheel"
    >+</div>

    <!-- 工具选择弹窗 -->
    <el-dialog v-model="showToolPicker" title="选择工具" width="680px" top="6vh">
      <!-- 搜索和筛选 -->
      <div class="tool-picker-toolbar">
        <el-input
          v-model="toolSearch"
          placeholder="搜索工具名称或 ID"
          clearable
          :prefix-icon="Search"
          class="tool-search-input"
        />
        <el-select
          v-model="toolCategoryFilter"
          placeholder="全部分类"
          clearable
          class="tool-category-select"
        >
          <el-option
            v-for="cat in toolCategories"
            :key="cat"
            :label="cat"
            :value="cat"
          />
        </el-select>
        <span class="tool-picker-count">共 {{ filteredTools.length }} 个工具</span>
      </div>

      <!-- 工具网格 -->
      <div class="tool-picker-grid">
        <div
          v-for="tool in filteredTools"
          :key="tool.id"
          class="tool-picker-card"
          @click="addTool(tool.id)"
        >
          <div class="tool-picker-info">
            <span class="tool-picker-name">{{ tool.name }}</span>
            <span class="tool-picker-category">{{ tool.category }}</span>
          </div>
          <span class="tool-picker-size">
            {{ defaultSizes[tool.id]?.colSpan || 4 }}×{{ defaultSizes[tool.id]?.rowSpan || 2 }}
          </span>
        </div>
        <div v-if="filteredTools.length === 0" class="tool-picker-empty">
          未找到匹配的工具
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.dashboard {
  width: 100%;
  height: 100%;
  overflow: auto;
  background-color: #f0f2f5;
  position: relative;
}

/* 拖拽激活时显示网格虚线背景 */
.dashboard.drag-active {
  background-color: #e8eaf0;
  background-image:
    linear-gradient(to right, rgba(64, 158, 255, 0.15) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(64, 158, 255, 0.15) 1px, transparent 1px);
  background-size: 80px 100px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

/* 顶部操作栏 */
.dash-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f0f2f5;
}

.dash-count {
  font-size: 13px;
  color: #909399;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

/* 网格布局 */
.grid-container {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  grid-auto-rows: 100px;
  grid-auto-flow: dense;
  gap: 12px;
  padding: 16px;
}

.tool-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
  position: relative;
  transition: opacity 0.2s, filter 0.2s;
}

/* 正在拖拽的卡片：透明度不要太低 */
.tool-card.dragging {
  opacity: 0.75;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.3);
  border-color: #409eff;
}

/* 拖拽时其他卡片变暗 */
.tool-card.drag-dimmed {
  opacity: 0.35;
  filter: grayscale(0.5);
}

.card-header {
  height: 38px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 8px;
  cursor: move;
  user-select: none;
  flex-shrink: 0;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.card-size {
  font-size: 11px;
  color: #909399;
  background: #fff;
  padding: 2px 8px;
  border-radius: 10px;
  border: 1px solid #e4e7ed;
}

.card-close {
  border: none;
  background: none;
  font-size: 18px;
  color: #909399;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}

.card-close:hover {
  color: #f56c6c;
}

.card-body {
  flex: 1;
  overflow: auto;
  min-height: 0;
}

.resize-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 16px;
  height: 16px;
  cursor: nwse-resize;
  background: linear-gradient(135deg, transparent 50%, #c0c4cc 50%);
  z-index: 10;
}

/* 浮动添加按钮 */
.add-button {
  position: fixed;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  z-index: 1000;
  transition: transform 0.15s, box-shadow 0.15s;
  user-select: none;
}

.add-button:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.5);
}

.add-button:active {
  cursor: grabbing;
  transform: scale(0.95);
}

.tool-picker-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.tool-search-input {
  flex: 1;
}

.tool-category-select {
  width: 160px;
}

.tool-picker-count {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.tool-picker-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  max-height: 60vh;
  overflow-y: auto;
}

.tool-picker-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.tool-picker-card:hover {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.tool-picker-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px 0;
  color: #c0c4cc;
  font-size: 14px;
}

.tool-picker-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tool-picker-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.tool-picker-category {
  font-size: 12px;
  color: #909399;
}

.tool-picker-size {
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 4px 10px;
  border-radius: 10px;
}
</style>
