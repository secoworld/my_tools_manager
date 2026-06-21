<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Minus, Lock, Unlock, Sort, CircleCheck, Delete, CopyDocument, ZoomIn, ZoomOut, FullScreen, Switch } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'
import { useZoomFullscreen } from '../composables/useZoomFullscreen'
import { useSplitter } from '../composables/useSplitter'

const props = defineProps({
  instanceId: { type: String, required: true }
})

const inputText = ref('')
const outputText = ref('')
const statusInfo = ref('')

useToolState(props.instanceId, { inputText, outputText })

const inputZoom = useZoomFullscreen(13)
const outputZoom = useZoomFullscreen(13)

// 拖拽分割
const contentRef = ref(null)
const isHorizontal = ref(true)
const splitter = useSplitter(0.5)

let resizeObserver = null
const checkLayout = () => {
  if (!contentRef.value) return
  isHorizontal.value = contentRef.value.clientWidth > 600
}

onMounted(() => {
  nextTick(() => {
    checkLayout()
    if (contentRef.value) {
      resizeObserver = new ResizeObserver(checkLayout)
      resizeObserver.observe(contentRef.value)
    }
  })
})

onUnmounted(() => {
  if (resizeObserver) resizeObserver.disconnect()
})

const onSplitterMouseDown = (e) => {
  if (!isHorizontal.value || !contentRef.value) return
  splitter.startDrag(e, contentRef.value, false)
}

// ---- JSON 操作 ----
const formatJson = () => {
  if (!inputText.value.trim()) { ElMessage.warning('请输入 JSON 内容'); return }
  try {
    const parsed = JSON.parse(inputText.value)
    outputText.value = JSON.stringify(parsed, null, 2)
    updateStatus(parsed, true)
    ElMessage.success('格式化成功')
  } catch (error) { handleError(error) }
}

const compressJson = () => {
  if (!inputText.value.trim()) { ElMessage.warning('请输入 JSON 内容'); return }
  try {
    const parsed = JSON.parse(inputText.value)
    outputText.value = JSON.stringify(parsed)
    updateStatus(parsed, true)
    ElMessage.success('压缩成功')
  } catch (error) { handleError(error) }
}

const escapeJson = () => {
  if (!inputText.value.trim()) { ElMessage.warning('请输入内容'); return }
  outputText.value = JSON.stringify(inputText.value).slice(1, -1)
  statusInfo.value = '已转义'
  ElMessage.success('转义成功')
}

const unescapeJson = () => {
  if (!inputText.value.trim()) { ElMessage.warning('请输入内容'); return }
  try {
    outputText.value = JSON.parse('"' + inputText.value + '"')
    statusInfo.value = '已去除转义'
    ElMessage.success('去除转义成功')
  } catch (error) { ElMessage.error('去除转义失败: ' + error.message) }
}

const sortJson = () => {
  if (!inputText.value.trim()) { ElMessage.warning('请输入 JSON 内容'); return }
  try {
    const parsed = JSON.parse(inputText.value)
    const sorted = sortObject(parsed)
    outputText.value = JSON.stringify(sorted, null, 2)
    updateStatus(sorted, true)
    ElMessage.success('排序成功')
  } catch (error) { handleError(error) }
}

const sortObject = (obj) => {
  if (Array.isArray(obj)) return obj.map(sortObject)
  else if (obj !== null && typeof obj === 'object') {
    const sorted = {}
    Object.keys(obj).sort().forEach((key) => { sorted[key] = sortObject(obj[key]) })
    return sorted
  }
  return obj
}

const validateJson = () => {
  if (!inputText.value.trim()) { ElMessage.warning('请输入 JSON 内容'); return }
  try {
    const parsed = JSON.parse(inputText.value)
    updateStatus(parsed, true)
    outputText.value = generateReport(parsed)
    ElMessage.success('校验通过')
  } catch (error) {
    outputText.value = `✗ JSON 格式错误\n错误信息: ${error.message}`
    statusInfo.value = '校验失败'
    ElMessage.error('校验失败')
  }
}

const generateReport = (parsed) => {
  const type = Array.isArray(parsed) ? 'Array' : typeof parsed
  const size = new Blob([inputText.value]).size
  let count = 0
  if (Array.isArray(parsed)) count = parsed.length
  else if (parsed !== null && typeof parsed === 'object') count = Object.keys(parsed).length
  return [
    '✓ JSON 格式正确', '',
    `类型:     ${type}`,
    `大小:     ${formatSize(size)}`,
    type === 'Array' ? `元素数量: ${count}` : type === 'object' ? `键数量:   ${count}` : ''
  ].filter(Boolean).join('\n')
}

const updateStatus = (parsed, valid) => {
  if (!valid) { statusInfo.value = '格式错误'; return }
  const type = Array.isArray(parsed) ? 'Array' : typeof parsed
  const size = new Blob([outputText.value || inputText.value]).size
  statusInfo.value = `类型: ${type} | 大小: ${formatSize(size)}`
}

const handleError = (error) => {
  statusInfo.value = '格式错误'
  ElMessage.error('JSON 解析失败: ' + error.message)
}

const formatSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const copyResult = async () => {
  if (!outputText.value) { ElMessage.warning('没有可复制的内容'); return }
  try { await navigator.clipboard.writeText(outputText.value); ElMessage.success('已复制到剪贴板') }
  catch (e) { ElMessage.error('复制失败') }
}

const outputToInput = () => {
  if (!outputText.value) { ElMessage.warning('没有可移动的内容'); return }
  inputText.value = outputText.value
  outputText.value = ''
  statusInfo.value = ''
}

const clearAll = () => {
  inputText.value = ''
  outputText.value = ''
  statusInfo.value = ''
}

const swapInputOutput = () => {
  const temp = inputText.value
  inputText.value = outputText.value
  outputText.value = temp
  ElMessage.success('已交换输入/输出')
}
</script>

<template>
  <div class="json-formatter">
    <div class="toolbar">
      <el-button type="primary" :icon="MagicStick" @click="formatJson">格式化</el-button>
      <el-button type="success" :icon="Minus" @click="compressJson">压缩</el-button>
      <el-divider direction="vertical" />
      <el-button :icon="Lock" @click="escapeJson">转义</el-button>
      <el-button :icon="Unlock" @click="unescapeJson">去除转义</el-button>
      <el-divider direction="vertical" />
      <el-button :icon="Sort" @click="sortJson">排序</el-button>
      <el-button :icon="CircleCheck" @click="validateJson">校验</el-button>
      <el-divider direction="vertical" />
      <el-button type="info" :icon="Delete" @click="clearAll">清空</el-button>
      <el-button type="warning" :icon="Switch" @click="swapInputOutput">交换</el-button>
    </div>

    <div
      ref="contentRef"
      class="content"
      :class="{ horizontal: isHorizontal, vertical: !isHorizontal }"
    >
      <!-- 输入区 -->
      <div
        class="input-area"
        :style="isHorizontal ? { flex: splitter.ratio.value + ' 1 0' } : {}"
      >
        <div class="label-row">
          <span class="label">输入 JSON</span>
          <div class="zoom-actions">
            <el-button size="small" :icon="ZoomOut" @click="inputZoom.zoomOut()" />
            <span class="font-size-display">{{ inputZoom.fontSize.value }}px</span>
            <el-button size="small" :icon="ZoomIn" @click="inputZoom.zoomIn()" />
            <el-button size="small" :icon="FullScreen" @click="inputZoom.toggleFullscreen()" />
          </div>
        </div>
        <el-input
          v-model="inputText"
          type="textarea"
          placeholder='请输入 JSON 内容，例如: {"name":"test","value":123}'
          :style="{ fontSize: inputZoom.fontSize.value + 'px' }"
        />
      </div>

      <!-- 拖拽分割条（仅水平布局时显示） -->
      <div
        v-if="isHorizontal"
        class="splitter"
        :class="{ active: splitter.isDragging.value }"
        @mousedown="onSplitterMouseDown"
      >
        <div class="splitter-line"></div>
      </div>

      <!-- 输出区 -->
      <div
        class="output-area"
        :style="isHorizontal ? { flex: (1 - splitter.ratio.value) + ' 1 0' } : {}"
      >
        <div class="label-row">
          <div class="output-label-left">
            <span class="label">输出结果</span>
            <div class="output-actions">
              <el-button size="small" :icon="CopyDocument" @click="copyResult">复制</el-button>
              <el-button size="small" @click="outputToInput">移到输入</el-button>
            </div>
          </div>
          <div class="zoom-actions">
            <el-button size="small" :icon="ZoomOut" @click="outputZoom.zoomOut()" />
            <span class="font-size-display">{{ outputZoom.fontSize.value }}px</span>
            <el-button size="small" :icon="ZoomIn" @click="outputZoom.zoomIn()" />
            <el-button size="small" :icon="FullScreen" @click="outputZoom.toggleFullscreen()" />
          </div>
        </div>
        <el-input
          v-model="outputText"
          type="textarea"
          readonly
          placeholder="操作结果将显示在这里"
          :style="{ fontSize: outputZoom.fontSize.value + 'px' }"
        />
      </div>
    </div>

    <div v-if="statusInfo" class="status-bar">{{ statusInfo }}</div>

    <!-- 输入区放大弹窗 -->
    <el-dialog
      v-model="inputZoom.isFullscreen.value"
      title="输入 JSON（放大查看）"
      width="85%"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div class="dialog-toolbar">
        <div class="zoom-actions">
          <el-button size="small" :icon="ZoomOut" @click="inputZoom.zoomOut()" />
          <span class="font-size-display">{{ inputZoom.fontSize.value }}px</span>
          <el-button size="small" :icon="ZoomIn" @click="inputZoom.zoomIn()" />
          <el-button size="small" @click="inputZoom.resetZoom()">重置</el-button>
        </div>
      </div>
      <el-input
        v-model="inputText"
        type="textarea"
        :autosize="false"
        class="dialog-textarea-wrapper"
        :style="{ fontSize: inputZoom.fontSize.value + 'px' }"
      />
    </el-dialog>

    <!-- 输出区放大弹窗 -->
    <el-dialog
      v-model="outputZoom.isFullscreen.value"
      title="输出结果（放大查看）"
      width="85%"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div class="dialog-toolbar">
        <div class="zoom-actions">
          <el-button size="small" :icon="CopyDocument" @click="copyResult">复制</el-button>
          <el-button size="small" @click="outputToInput">移到输入</el-button>
          <el-divider direction="vertical" />
          <el-button size="small" :icon="ZoomOut" @click="outputZoom.zoomOut()" />
          <span class="font-size-display">{{ outputZoom.fontSize.value }}px</span>
          <el-button size="small" :icon="ZoomIn" @click="outputZoom.zoomIn()" />
          <el-button size="small" @click="outputZoom.resetZoom()">重置</el-button>
        </div>
      </div>
      <el-input
        v-model="outputText"
        type="textarea"
        readonly
        :autosize="false"
        class="dialog-textarea-wrapper"
        :style="{ fontSize: outputZoom.fontSize.value + 'px' }"
      />
    </el-dialog>
  </div>
</template>

<style scoped>
.json-formatter {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.content {
  flex: 1;
  min-height: 0;
  gap: 0;
}

/* 水平布局（左右并排） */
.content.horizontal {
  display: flex;
  flex-wrap: nowrap;
}

/* 垂直布局（上下堆叠） */
.content.vertical {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-area,
.output-area {
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 垂直布局时各占一半高度 */
.content.vertical .input-area,
.content.vertical .output-area {
  flex: 1 1 0;
}

.label { font-weight: 600; color: #606266; flex-shrink: 0; }

.label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 4px;
  flex-shrink: 0;
}

.output-label-left { display: flex; align-items: center; gap: 8px; }
.output-actions { display: flex; gap: 4px; }
.zoom-actions { display: flex; align-items: center; gap: 4px; }
.font-size-display { font-size: 12px; color: #909399; min-width: 36px; text-align: center; }

/* 关键：让 el-textarea 填满剩余高度 */
.input-area :deep(.el-textarea),
.output-area :deep(.el-textarea) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.input-area :deep(.el-textarea__inner),
.output-area :deep(.el-textarea__inner) {
  flex: 1;
  height: 100% !important;
  resize: none;
  font-family: 'Consolas', 'Monaco', monospace;
}

/* 拖拽分割条 */
.splitter {
  width: 6px;
  cursor: ew-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 5;
}

.splitter:hover .splitter-line,
.splitter.active .splitter-line {
  background: #409eff;
  width: 3px;
}

.splitter-line {
  width: 1px;
  height: 100%;
  background: #dcdfe6;
  transition: width 0.2s, background 0.2s;
}

.splitter.active .splitter-line {
  width: 3px;
}

.status-bar {
  margin-top: 8px;
  padding: 6px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

/* 弹窗样式 */
.dialog-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.dialog-textarea-wrapper :deep(.el-textarea__inner) {
  height: 70vh;
  resize: none;
  font-family: 'Consolas', 'Monaco', monospace;
  line-height: 1.6;
}
</style>
