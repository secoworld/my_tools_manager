<script setup>
import { ref, onMounted, onUnmounted, watch, markRaw } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { toolRegistry, getToolList } from '../tools/registry'
import { clearToolState, clearAllToolState } from '../composables/useToolState'

// 网格配置
const GRID_COLS = 12
const GRID_COL_WIDTH = 80 // 每列约 80px（用于计算拖拽调整大小的格子数）
const GRID_ROW_HEIGHT = 100 // 每行 100px

// 布局缓存 key
const LAYOUT_KEY = 'dashboard-layout'

// 工具默认大小（colSpan × rowSpan）
const defaultSizes = {
  'json-formatter': { colSpan: 6, rowSpan: 3 },
  'base64-converter': { colSpan: 6, rowSpan: 3 },
  'timestamp-converter': { colSpan: 4, rowSpan: 2 },
  'text-editor': { colSpan: 4, rowSpan: 4 }
}

// 工具卡片列表
const cards = ref([])
const showToolPicker = ref(false)

// 调整大小状态
const resizing = ref(null)

// 拖拽排序状态
const dragId = ref(null)

// ---- 布局缓存 ----
// 保存卡片布局到 localStorage
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

// 从 localStorage 恢复卡片布局
const loadLayout = () => {
  try {
    const saved = localStorage.getItem(LAYOUT_KEY)
    if (saved) {
      const layout = JSON.parse(saved)
      cards.value = layout
        .map((item) => {
          const tool = toolRegistry[item.toolId]
          if (!tool) return null // 工具已移除，跳过
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

// 监听卡片变化，自动保存布局
watch(cards, saveLayout, { deep: true })

// 添加工具
const addTool = (toolId) => {
  const tool = toolRegistry[toolId]
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

// 删除工具（同时清除其状态缓存）
const removeCard = (id) => {
  const card = cards.value.find((c) => c.id === id)
  if (card) clearToolState(id)
  cards.value = cards.value.filter((c) => c.id !== id)
}

// 一键清除仪表盘
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
      // 清除所有工具状态缓存
      clearAllToolState()
      cards.value = []
      ElMessage.success('仪表盘已清空')
    })
    .catch(() => {})
}

// 页面加载时恢复布局
onMounted(() => {
  loadLayout()
})

// ---- 拖拽排序（HTML5 Drag API，交换卡片顺序）----
const onDragStart = (e, id) => {
  dragId.value = id
  e.dataTransfer.effectAllowed = 'move'
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
}
const onDragEnd = () => {
  dragId.value = null
}

// ---- 调整大小（鼠标拖拽右下角，按格子单位调整）----
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

onUnmounted(() => {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
})
</script>

<template>
  <div class="dashboard">
    <!-- 空状态 -->
    <div v-if="cards.length === 0" class="empty-state">
      <el-empty description="点击右下角 + 添加工具到仪表盘">
        <el-button type="primary" @click="showToolPicker = true">添加工具</el-button>
      </el-empty>
    </div>

    <!-- 顶部操作栏 -->
    <div v-if="cards.length > 0" class="dash-toolbar">
      <span class="dash-count">共 {{ cards.length }} 个工具</span>
      <el-button type="danger" size="small" plain @click="clearDashboard">一键清除</el-button>
    </div>

    <!-- 网格容器 -->
    <div v-if="cards.length > 0" class="grid-container">
      <div
        v-for="card in cards"
        :key="card.id"
        class="tool-card"
        :class="{ dragging: dragId === card.id }"
        :style="{
          gridColumn: `span ${card.colSpan}`,
          gridRow: `span ${card.rowSpan}`
        }"
        draggable="true"
        @dragstart="onDragStart($event, card.id)"
        @dragover="onDragOver"
        @drop="onDrop($event, card.id)"
        @dragend="onDragEnd"
      >
        <!-- 标题栏（拖拽移动） -->
        <div class="card-header">
          <span class="card-title">{{ card.title }}</span>
          <span class="card-size">{{ card.colSpan }}×{{ card.rowSpan }}</span>
          <button class="card-close" @click="removeCard(card.id)">×</button>
        </div>
        <!-- 工具内容 -->
        <div class="card-body">
          <component :is="card.component" :instanceId="card.id" />
        </div>
        <!-- 右下角调整大小 -->
        <div class="resize-handle" @mousedown="startResize($event, card)" />
      </div>
    </div>

    <!-- 浮动添加按钮 -->
    <div class="add-button" @click="showToolPicker = true">+</div>

    <!-- 工具选择弹窗 -->
    <el-dialog v-model="showToolPicker" title="选择工具" width="420px">
      <div class="tool-picker-list">
        <div
          v-for="tool in getToolList()"
          :key="tool.id"
          class="tool-picker-item"
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
  padding: 12px 16px 0 16px;
}

.dash-count {
  font-size: 13px;
  color: #909399;
}

/* 网格布局：12 列，每行 100px，dense 瀑布流填充 */
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
  transition: opacity 0.2s;
}

.tool-card.dragging {
  opacity: 0.4;
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

.add-button {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  z-index: 100;
  transition: transform 0.2s;
  user-select: none;
}

.add-button:hover {
  transform: scale(1.1);
}

.tool-picker-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tool-picker-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.tool-picker-item:hover {
  border-color: #409eff;
  background-color: #ecf5ff;
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
