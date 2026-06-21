<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { ZoomIn, ZoomOut, FullScreen, CopyDocument, Switch } from '@element-plus/icons-vue'
import { executeTool } from '../api'
import { useToolState } from '../composables/useToolState'
import { useZoomFullscreen } from '../composables/useZoomFullscreen'
import { useSplitter } from '../composables/useSplitter'

const props = defineProps({
  instanceId: { type: String, required: true }
})

const inputText = ref('')
const outputText = ref('')
const action = ref('encode')
const keepNonBase64 = ref(false)
const loading = ref(false)

useToolState(props.instanceId, { inputText, outputText, action, keepNonBase64 })

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

const convert = async () => {
  if (!inputText.value.trim()) { ElMessage.warning('请输入要转换的内容'); return }
  loading.value = true
  try {
    const actualAction = (action.value === 'decode' && keepNonBase64.value) ? 'mixed-decode' : action.value
    const result = await executeTool('base64-converter', { action: actualAction, text: inputText.value })
    outputText.value = typeof result === 'string' ? result : result.data || result.result || ''
    ElMessage.success('转换成功')
  } catch (error) {
    ElMessage.error('转换失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const clearAll = () => { inputText.value = ''; outputText.value = '' }

const swapInputOutput = () => {
  const temp = inputText.value
  inputText.value = outputText.value
  outputText.value = temp
  // 同时切换编码/解码模式
  action.value = action.value === 'encode' ? 'decode' : 'encode'
  ElMessage.success(`已交换输入/输出，模式切换为${action.value === 'encode' ? '编码' : '解码'}`)
}

const copyResult = async () => {
  if (!outputText.value) { ElMessage.warning('没有可复制的内容'); return }
  try { await navigator.clipboard.writeText(outputText.value); ElMessage.success('已复制') }
  catch (e) { ElMessage.error('复制失败') }
}
</script>

<template>
  <div class="base64-converter">
    <div class="toolbar">
      <el-radio-group v-model="action">
        <el-radio value="encode">编码</el-radio>
        <el-radio value="decode">解码</el-radio>
      </el-radio-group>
      <el-checkbox v-if="action === 'decode'" v-model="keepNonBase64">保留非Base64内容</el-checkbox>
      <el-button type="primary" :loading="loading" @click="convert">转换</el-button>
      <el-button type="warning" :icon="Switch" @click="swapInputOutput">交换</el-button>
      <el-button type="info" @click="clearAll">清空</el-button>
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
          <span class="label">输入内容</span>
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
          placeholder="请输入要转换的内容"
          :style="{ fontSize: inputZoom.fontSize.value + 'px' }"
        />
      </div>

      <!-- 拖拽分割条 -->
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
            <el-button size="small" :icon="CopyDocument" @click="copyResult">复制</el-button>
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
          placeholder="转换结果将显示在这里"
          :style="{ fontSize: outputZoom.fontSize.value + 'px' }"
        />
      </div>
    </div>

    <!-- 输入区放大弹窗 -->
    <el-dialog
      v-model="inputZoom.isFullscreen.value"
      title="输入内容（放大查看）"
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
.base64-converter {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.content {
  flex: 1;
  min-height: 0;
  gap: 0;
}

.content.horizontal {
  display: flex;
  flex-wrap: nowrap;
}

.content.vertical {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-area, .output-area {
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

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
