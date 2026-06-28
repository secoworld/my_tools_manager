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

// Drawnix 嵌入 URL
const iframeSrc = 'https://www.drawnix.com/'

function handleMessage(event) {
  const msg = event.data
  if (!msg || typeof msg !== 'object') return
  // Drawnix 场景更新事件（如果有）
  if (msg.type === 'SCENE_UPDATE' || msg.type === 'drawnixScene') {
    sceneData.value = msg.payload || msg
    ready.value = true
  }
}

function sendMsg(msg) {
  if (iframeRef.value && iframeRef.value.contentWindow) {
    iframeRef.value.contentWindow.postMessage(msg, '*')
  }
}

// 导入 .json / .drawnix 文件
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
      sendMsg({ type: 'IMPORT', data })
      ElMessage.success(`已导入: ${file.name}`)
    } catch {
      ElMessage.error('文件格式不正确，请选择 .json 文件')
    }
  }
  reader.onerror = () => ElMessage.error('文件读取失败')
  reader.readAsText(file)
  e.target.value = ''
}

// 导出为 JSON 文件
function exportData() {
  if (sceneData.value) {
    downloadJson(sceneData.value, `drawnix-${Date.now()}.json`)
    ElMessage.success('已导出文件')
  } else {
    ElMessage.info('请先在画布上操作后，使用工具内置导出功能保存')
  }
}

// 保存为图片
function saveAsImage() {
  ElMessage.info('请使用 Drawnix 内置的导出功能保存为图片')
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
  <div class="drawnix-tool">
    <div class="draw-toolbar">
      <div class="toolbar-info">
        <span class="tool-label">Drawnix 思维导图</span>
        <el-tag size="small" type="warning">在线工具</el-tag>
      </div>
      <div class="toolbar-actions">
        <el-button size="small" :icon="Upload" @click="triggerImport">导入</el-button>
        <el-button size="small" :icon="Download" @click="exportData">导出</el-button>
        <el-button size="small" type="primary" :icon="Camera" @click="saveAsImage">保存图片</el-button>
        <el-button size="small" :icon="Refresh" @click="reloadIframe">刷新</el-button>
        <input ref="fileInput" type="file" accept=".json,.drawnix,application/json" style="display:none" @change="handleFileChange" />
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
.drawnix-tool {
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
