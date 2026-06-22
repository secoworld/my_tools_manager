<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload, Delete, ZoomIn, ZoomOut, FullScreen } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'
import { useZoomFullscreen } from '../composables/useZoomFullscreen'
import LineNumberTextarea from '../components/LineNumberTextarea.vue'

const props = defineProps({
  instanceId: { type: String, required: true }
})

const title = ref('无标题笔记')
const content = ref('')
const showLineNumbers = ref(false)

useToolState(props.instanceId, { title, content, showLineNumbers })

const editorZoom = useZoomFullscreen(14)

const charCount = computed(() => content.value.length)
const lineCount = computed(() => content.value.split('\n').length)

const downloadTxt = () => {
  if (!content.value.trim() && !title.value.trim()) { ElMessage.warning('没有内容可下载'); return }
  const blob = new Blob([content.value], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${title.value || '笔记'}.txt`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已下载')
}

const fileInput = ref(null)
const triggerUpload = () => { fileInput.value?.click() }
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (event) => {
    content.value = event.target.result
    title.value = file.name.replace(/\.txt$/i, '')
    ElMessage.success(`已加载: ${file.name}`)
  }
  reader.onerror = () => ElMessage.error('文件读取失败')
  reader.readAsText(file, 'UTF-8')
  e.target.value = ''
}

const clearAll = () => { content.value = ''; ElMessage.info('已清空') }
</script>

<template>
  <div class="text-editor">
    <!-- 标题栏 -->
    <div class="editor-header">
      <input v-model="title" class="title-input" placeholder="输入笔记标题..." />
      <div class="editor-actions">
        <el-button size="small" :icon="Download" @click="downloadTxt">下载</el-button>
        <el-button size="small" :icon="Upload" @click="triggerUpload">导入</el-button>
        <el-button size="small" type="danger" plain :icon="Delete" @click="clearAll">清空</el-button>
        <input ref="fileInput" type="file" accept=".txt,text/plain" style="display:none" @change="handleFileChange" />
      </div>
    </div>

    <!-- 缩放/全屏控制栏 -->
    <div class="zoom-bar">
      <div class="zoom-actions">
        <el-button size="small" :icon="ZoomOut" @click="editorZoom.zoomOut()" />
        <span class="font-size-display">{{ editorZoom.fontSize.value }}px</span>
        <el-button size="small" :icon="ZoomIn" @click="editorZoom.zoomIn()" />
        <el-divider direction="vertical" />
        <el-checkbox v-model="showLineNumbers" size="small">显示行号</el-checkbox>
      </div>
      <el-button size="small" :icon="FullScreen" @click="editorZoom.toggleFullscreen()">放大编辑</el-button>
    </div>

    <!-- 编辑区 -->
    <div class="editor-container">
      <LineNumberTextarea
        v-model="content"
        :show-line-numbers="showLineNumbers"
        :font-size="editorZoom.fontSize.value"
        placeholder="在此记录你的内容..."
      />
    </div>

    <!-- 状态栏 -->
    <div class="editor-status">
      <span>字符: {{ charCount }}</span>
      <span>行数: {{ lineCount }}</span>
      <span class="auto-save-hint">自动保存</span>
    </div>

    <!-- 放大编辑弹窗 -->
    <el-dialog
      v-model="editorZoom.isFullscreen.value"
      title="放大编辑"
      width="85%"
      top="5vh"
      :close-on-click-modal="false"
      class="text-editor-dialog"
    >
      <div class="dialog-toolbar">
        <input v-model="title" class="dialog-title-input" placeholder="笔记标题..." />
        <div class="dialog-controls">
          <el-button size="small" :icon="Download" @click="downloadTxt">下载</el-button>
          <el-divider direction="vertical" />
          <el-button size="small" :icon="ZoomOut" @click="editorZoom.zoomOut()" />
          <span class="font-size-display">{{ editorZoom.fontSize.value }}px</span>
          <el-button size="small" :icon="ZoomIn" @click="editorZoom.zoomIn()" />
          <el-button size="small" @click="editorZoom.resetZoom()">重置</el-button>
          <el-divider direction="vertical" />
          <el-checkbox v-model="showLineNumbers" size="small">显示行号</el-checkbox>
        </div>
      </div>
      <div class="dialog-editor-container">
        <LineNumberTextarea
          v-model="content"
          :show-line-numbers="showLineNumbers"
          :font-size="editorZoom.fontSize.value"
          placeholder="在此记录你的内容..."
        />
      </div>
      <div class="dialog-status">
        <span>字符: {{ charCount }}</span>
        <span>行数: {{ lineCount }}</span>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.text-editor {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.title-input {
  flex: 1;
  min-width: 120px;
  border: none;
  border-bottom: 2px solid #e4e7ed;
  background: transparent;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding: 6px 4px;
  outline: none;
  transition: border-color 0.3s;
}

.title-input:focus { border-bottom-color: #409eff; }

.editor-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.zoom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.zoom-actions { display: flex; align-items: center; gap: 4px; }
.font-size-display { font-size: 12px; color: #909399; min-width: 36px; text-align: center; }

/* 编辑区容器（包裹 LineNumberTextarea） */
.editor-container {
  flex: 1;
  min-height: 0;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.3s;
}

.editor-container:focus-within {
  border-color: #409eff;
}

.editor-container :deep(.line-number-textarea) {
  height: 100%;
  font-family: 'Consolas', 'Monaco', 'Microsoft YaHei', monospace;
  line-height: 1.7;
}

.editor-container :deep(.text-area) {
  font-family: 'Consolas', 'Monaco', 'Microsoft YaHei', monospace;
  line-height: 1.7;
}

.editor-container :deep(.line-numbers) {
  font-family: 'Consolas', 'Monaco', 'Microsoft YaHei', monospace;
  line-height: 1.7;
}

.editor-status {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.auto-save-hint { margin-left: auto; color: #67c23a; }

/* 弹窗样式 */
.dialog-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 16px;
}

.dialog-title-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  outline: none;
  border-bottom: 2px solid #e4e7ed;
  padding: 4px 8px;
}

.dialog-title-input:focus { border-bottom-color: #409eff; }

.dialog-controls { display: flex; align-items: center; gap: 8px; flex-shrink: 0; flex-wrap: wrap; }

.dialog-editor-container {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  height: 65vh;
}

.dialog-editor-container :deep(.line-number-textarea) {
  height: 100%;
  font-family: 'Consolas', 'Monaco', 'Microsoft YaHei', monospace;
  line-height: 1.8;
}

.dialog-editor-container :deep(.text-area) {
  font-family: 'Consolas', 'Monaco', 'Microsoft YaHei', monospace;
  line-height: 1.8;
}

.dialog-editor-container :deep(.line-numbers) {
  font-family: 'Consolas', 'Monaco', 'Microsoft YaHei', monospace;
  line-height: 1.8;
}

.dialog-status {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
