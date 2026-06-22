<script setup>
import { computed } from 'vue'
import { Document, Lock, Clock, EditPen, Coin, Grid, Histogram, Connection, DocumentCopy, Files, Tools } from '@element-plus/icons-vue'
import { useWindowManagerStore } from '../stores/windowManager'
import { getToolList } from '../tools/registry'
import { getCustomPlugins } from '../tools/customRegistry'

const windowManager = useWindowManagerStore()

// 图标映射
const iconMap = {
  Document,
  Lock,
  Clock,
  EditPen,
  Coin,
  Grid,
  Histogram,
  Connection,
  DocumentCopy,
  Files,
  Tools
}

// 按 category 分组的工具列表（内置 + 自定义）
const groupedTools = computed(() => {
  const tools = [...getToolList(), ...getCustomPlugins('WORKSPACE')]
  const groups = {}
  tools.forEach((tool) => {
    if (!groups[tool.category]) {
      groups[tool.category] = []
    }
    groups[tool.category].push(tool)
  })
  return groups
})

// 点击工具打开窗口
const handleToolClick = (toolId) => {
  windowManager.openTool(toolId)
}
</script>

<template>
  <div class="sidebar">
    <div class="tools-section">
      <div class="section-title">工具列表</div>
      <div class="tools-list">
        <div v-for="(tools, category) in groupedTools" :key="category" class="tool-group">
          <div class="group-title">{{ category }}</div>
          <div
            v-for="tool in tools"
            :key="tool.id"
            class="tool-item"
            @click="handleToolClick(tool.id)"
          >
            <el-icon class="tool-icon">
              <component :is="iconMap[tool.icon]" v-if="iconMap[tool.icon]" />
            </el-icon>
            <span class="tool-name">{{ tool.name }}</span>
            <el-tag v-if="tool.needBackend" size="small" type="warning" effect="plain">后端</el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  width: 240px;
  height: 100%;
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tools-section {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.section-title {
  padding: 8px 20px;
  font-size: 12px;
  color: #909399;
  font-weight: 600;
}

.tool-group {
  margin-bottom: 8px;
}

.group-title {
  padding: 6px 20px;
  font-size: 12px;
  color: #c0c4cc;
}

.tool-item {
  display: flex;
  align-items: center;
  padding: 8px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
  gap: 8px;
}

.tool-item:hover {
  background-color: #f5f7fa;
}

.tool-icon {
  font-size: 16px;
  color: #606266;
}

.tool-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
}
</style>
