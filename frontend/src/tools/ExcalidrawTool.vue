<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload, Camera, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  instanceId: { type: String, required: true }
})

const iframeRef = ref(null)
const iframeKey = ref(0)
const ready = ref(false)
const sceneData = ref(null)

// Excalidraw 嵌入 URL（支持 embed 模式）
const iframeSrc = 'https://excalidraw.com/'

// postMessage 消息处理（Excalidraw 支持 scene 更新事件）
function handleMessage(event) {
  if (event.origin !== 'https://excalidraw.com') return
  const msg = event.data
  if (!msg) return

  // Excalidraw 发送场景更新
  if (msg.type === 'SCENE_UPDATE' || msg.type === 'excalidrawScene') {
    sceneData.value = msg.payload || msg.elements || msg
    ready.value = true
  }
}

function sendMsg(msg) {
  if (iframeRef.value && iframeRef.value.contentWindow) {
    iframeRef.value.contentWindow.postMessage(msg, 'https://excalidraw.com')
  }
}

// 导入 .excalidraw / .json 文件
const fileInput = ref(null)
function triggerImport() { fileInput.value?.click() }

function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (event) => {
    try {
      const data = JSON.parse(event.target.result)
      sceneData.value = data
      // 尝试通过 postMessage 导入场景
      sendMsg({ type: 'IMPORT', data })
      // 同时通过 URL hash 加载
      const encoded = encodeURIComponent(JSON.stringify(data))
      iframeRef.value.src = `https://excalidraw.com/#json=${encoded}`
      ElMessage.success(`已导入: ${file.name}`)
    } catch {
      ElMessage.error('文件格式不正确，请选择 .excalidraw 或 .json 文件')
    }
  }
  reader.onerror = () => ElMessage.error('文件读取失败')
  reader.readAsText(file)
  e.target.value = ''
}

// 导出为 .excalidraw (JSON) 文件
function exportData() {
  if (sceneData.value) {
    downloadJson(sceneData.value, `excalidraw-${Date.now()}.excalidraw`)
    ElMessage.success('已导出 .excalidraw 文件')
  } else {
    // 如果没有场景数据，提示用户在 Excalidraw 中操作
    ElMessage.info('请在 Excalidraw 中绘制内容后，使用导出菜单保存')
  }
}

// 保存为 PNG 图片
function saveAsImage() {
  // Excalidraw 没有标准的 postMessage 导出 API
  // 提示用户使用 Excalidraw 内置的导出功能
  ElMessage.info('请点击画布左上角菜单 → 导出 → PNG/SVG 进行保存图片')
}

function downloadJson(data, filename) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function reloadIframe() {
  ready.value = false
  iframeKey.value++
}

onMounted(() => {
  window.addEventListener('message', handleMessage)
})

onBeforeUnmount(() => {
  window.removeEventListener('message', handleMessage)
})
</script>

<template>
  <div class="excalidraw-tool">
    <div class="draw-toolbar">
      <div class="toolbar-info">
        <span class="tool-label">Excalidraw 手绘流程图</span>
        <el-tag size="small" type="warning">在线工具</el-tag>
      </div>
      <div class="toolbar-actions">
        <el-button size="small" :icon="Upload" @click="triggerImport">导入</el-button>
        <el-button size="small" :icon="Download" @click="exportData">导出</el-button>
        <el-button size="small" type="primary" :icon="Camera" @click="saveAsImage">保存图片</el-button>
        <el-button size="small" :icon="Refresh" @click="reloadIframe">刷新</el-button>
        <input ref="fileInput" type="file" accept=".excalidraw,.json,application/json" style="display:none" @change="handleFileChange" />
      </div>
    </div>
    <div class="draw-iframe-container">
      <iframe
        ref="iframeRef"
        :key="iframeKey"
        :src="iframeSrc"
        class="draw-iframe"
        frameborder="0"
        allowfullscreen
      />
    </div>
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

.draw-iframe-container {
  flex: 1;
  min-height: 0;
  position: relative;
}

.draw-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
