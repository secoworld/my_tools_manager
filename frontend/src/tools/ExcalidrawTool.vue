<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload, Camera, Refresh } from '@element-plus/icons-vue'
import { createElement } from 'react'
import { createRoot } from 'react-dom/client'
import { Excalidraw, exportToBlob, serializeAsJSON } from '@excalidraw/excalidraw'

const props = defineProps({
  instanceId: { type: String, required: true }
})

const containerRef = ref(null)
let reactRoot = null
let excalidrawApi = null

onMounted(() => {
  if (!containerRef.value) return
  reactRoot = createRoot(containerRef.value)
  reactRoot.render(
    createElement(Excalidraw, {
      excalidrawAPI: (api) => { excalidrawApi = api },
      initialData: {
        elements: [],
        appState: { gridSize: null, viewBackgroundColor: '#ffffff' },
        scrollToContent: true
      },
      UIOptions: {
        canvasActions: {
          loadScene: false,
          saveToActiveFile: false,
          export: { saveFileToDisk: false },
          toggleTheme: false
        }
      }
    })
  )
})

onBeforeUnmount(() => {
  if (reactRoot) {
    reactRoot.unmount()
    reactRoot = null
  }
  excalidrawApi = null
})

// 导入 .excalidraw / .json 文件
const fileInput = ref(null)
function triggerImport() { fileInput.value?.click() }

async function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  try {
    const text = await file.text()
    const data = JSON.parse(text)
    if (excalidrawApi && data.elements) {
      excalidrawApi.updateScene({ elements: data.elements })
      excalidrawApi.scrollToContent()
      ElMessage.success(`已导入: ${file.name}`)
    } else {
      ElMessage.warning('文件格式不正确，未找到 elements 数据')
    }
  } catch (err) {
    ElMessage.error('导入失败: ' + err.message)
  }
  e.target.value = ''
}

// 导出为 .excalidraw (JSON) 文件
function exportData() {
  if (!excalidrawApi) { ElMessage.warning('编辑器未就绪'); return }
  const elements = excalidrawApi.getSceneElements()
  if (!elements || elements.length === 0) {
    ElMessage.warning('画布为空，请先绘制内容')
    return
  }
  const appState = excalidrawApi.getAppState()
  const files = excalidrawApi.getFiles()
  const json = serializeAsJSON(elements, appState, files, 'local')
  const blob = new Blob([json], { type: 'application/json;charset=utf-8' })
  downloadBlob(blob, `excalidraw-${Date.now()}.excalidraw`)
  ElMessage.success('已导出')
}

// 保存为 PNG 图片
async function saveAsImage() {
  if (!excalidrawApi) { ElMessage.warning('编辑器未就绪'); return }
  const elements = excalidrawApi.getSceneElements()
  if (!elements || elements.length === 0) {
    ElMessage.warning('画布为空')
    return
  }
  try {
    const blob = await exportToBlob({
      elements,
      appState: { ...excalidrawApi.getAppState(), exportBackground: true },
      files: excalidrawApi.getFiles(),
      mimeType: 'image/png'
    })
    downloadBlob(blob, `excalidraw-${Date.now()}.png`)
    ElMessage.success('图片已保存')
  } catch (err) {
    ElMessage.error('导出图片失败: ' + err.message)
  }
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function refreshEditor() {
  if (excalidrawApi) {
    excalidrawApi.refresh()
    ElMessage.success('已刷新')
  }
}
</script>

<template>
  <div class="excalidraw-tool">
    <div class="draw-toolbar">
      <div class="toolbar-info">
        <span class="tool-label">Excalidraw 手绘流程图</span>
        <el-tag size="small" type="success">离线</el-tag>
      </div>
      <div class="toolbar-actions">
        <el-button size="small" :icon="Upload" @click="triggerImport">导入</el-button>
        <el-button size="small" :icon="Download" @click="exportData">导出</el-button>
        <el-button size="small" type="primary" :icon="Camera" @click="saveAsImage">保存图片</el-button>
        <el-button size="small" :icon="Refresh" @click="refreshEditor">刷新</el-button>
        <input ref="fileInput" type="file" accept=".excalidraw,.json,application/json" style="display:none" @change="handleFileChange" />
      </div>
    </div>
    <div ref="containerRef" class="excalidraw-container" />
  </div>
</template>

<style scoped>
.excalidraw-tool {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.draw-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-label {
  font-weight: 600;
  color: #303133;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.excalidraw-container {
  flex: 1;
  min-height: 0;
  height: 100%;
  width: 100%;
}

.excalidraw-container :deep(.excalidraw) {
  height: 100%;
}
</style>
