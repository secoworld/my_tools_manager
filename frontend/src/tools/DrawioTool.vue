<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload, Camera, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  instanceId: { type: String, required: true }
})

const iframeRef = ref(null)
const iframeSrc = 'https://embed.diagrams.net/?embed=1&proto=json&saveAndExit=1&noSaveBtn=0&noExitBtn=1&lang=zh'
const ready = ref(false)
const lastDiagramXml = ref('')

// postMessage 消息处理
// 注意：drawio 在 proto=json 模式下通过 postMessage 发送 JSON 字符串（非对象）
function handleMessage(event) {
  let msg = event.data
  if (!msg) return
  if (typeof msg === 'string') {
    try { msg = JSON.parse(msg) } catch (e) { return }
  }
  if (typeof msg !== 'object') return

  switch (msg.event) {
    case 'init':
      // drawio iframe 初始化完成，发送空图表让它进入可编辑状态
      ready.value = true
      sendMsg({
        action: 'load',
        xml: '<mxGraphModel><root><mxCell id="0"/><mxCell id="1" parent="0"/></root></mxGraphModel>',
        autosave: 0
      })
      break
    case 'save':
      // 用户点击保存，收到 XML 数据
      lastDiagramXml.value = msg.data
      break
    case 'export':
      // 导出图片结果
      if (msg.data) {
        downloadDataUrl(msg.data, `drawio-${Date.now()}.png`)
        ElMessage.success('图片已保存')
      }
      break
    case 'exit':
      // 用户退出编辑（我们禁用了退出按钮，这不会触发）
      break
  }
}

function sendMsg(msg) {
  if (iframeRef.value && iframeRef.value.contentWindow) {
    // proto=json 协议要求发送 JSON 字符串
    iframeRef.value.contentWindow.postMessage(JSON.stringify(msg), '*')
  }
}

// 导入 .drawio / .xml 文件
const fileInput = ref(null)
function triggerImport() { fileInput.value?.click() }

function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (event) => {
    const xml = event.target.result
    lastDiagramXml.value = xml
    sendMsg({ action: 'load', xml, autosave: 1 })
    ElMessage.success(`已导入: ${file.name}`)
  }
  reader.onerror = () => ElMessage.error('文件读取失败')
  reader.readAsText(file)
  e.target.value = ''
}

// 导出为 .drawio (XML) 文件
function exportXml() {
  // 请求 drawio 发送当前 XML 数据
  sendMsg({ action: 'save' })
  setTimeout(() => {
    if (lastDiagramXml.value) {
      const blob = new Blob([lastDiagramXml.value], { type: 'application/xml;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `drawio-${Date.now()}.drawio`
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('已导出 .drawio 文件')
    } else {
      ElMessage.warning('请先在画布上操作后重试')
    }
  }, 500)
}

// 保存为 PNG 图片
function saveAsImage() {
  sendMsg({ action: 'export', format: 'png', spin: '正在导出图片...' })
}

function downloadDataUrl(dataUrl, filename) {
  const a = document.createElement('a')
  a.href = dataUrl
  a.download = filename
  a.click()
}

function reloadIframe() {
  ready.value = false
  if (iframeRef.value) {
    iframeRef.value.src = iframeSrc
  }
}

onMounted(() => {
  window.addEventListener('message', handleMessage)
})

onBeforeUnmount(() => {
  window.removeEventListener('message', handleMessage)
})
</script>

<template>
  <div class="drawio-tool">
    <div class="draw-toolbar">
      <div class="toolbar-info">
        <span class="tool-label">Draw.io 流程图</span>
        <el-tag v-if="ready" size="small" type="success">已连接</el-tag>
        <el-tag v-else size="small" type="info">连接中...</el-tag>
      </div>
      <div class="toolbar-actions">
        <el-button size="small" :icon="Upload" @click="triggerImport">导入</el-button>
        <el-button size="small" :icon="Download" @click="exportXml">导出</el-button>
        <el-button size="small" type="primary" :icon="Camera" @click="saveAsImage">保存图片</el-button>
        <el-button size="small" :icon="Refresh" @click="reloadIframe">刷新</el-button>
        <input ref="fileInput" type="file" accept=".drawio,.xml,.png,application/xml,image/png" style="display:none" @change="handleFileChange" />
      </div>
    </div>
    <div class="draw-iframe-container">
      <iframe
        ref="iframeRef"
        :src="iframeSrc"
        class="draw-iframe"
        frameborder="0"
        allowfullscreen
      />
    </div>
  </div>
</template>

<style scoped>
.drawio-tool {
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
