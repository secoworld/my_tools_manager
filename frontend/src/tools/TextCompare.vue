<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Delete, Switch, CopyDocument } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'
import { useZoomFullscreen } from '../composables/useZoomFullscreen'
import { useSplitter } from '../composables/useSplitter'
import LineNumberTextarea from '../components/LineNumberTextarea.vue'

const props = defineProps({
  instanceId: { type: String, required: true }
})

// 左右文本
const leftText = ref('')
const rightText = ref('')

// 显示行号
const showLineNumbers = ref(false)

// 忽略选项
const ignoreSpace = ref(true)
const ignoreTab = ref(true)
const ignoreCR = ref(true) // 回车 \r
const ignoreEmptyLines = ref(true)
const ignoreCase = ref(false)
const trimLine = ref(true)

// 当前查看的结果标签
const activeResult = ref('both') // both | onlyA | onlyB

// 状态持久化
useToolState(props.instanceId, {
  leftText, rightText,
  ignoreSpace, ignoreTab, ignoreCR, ignoreEmptyLines, ignoreCase, trimLine,
  activeResult, showLineNumbers
})

// 缩放和全屏
const { fontSize, zoomIn, zoomOut } = useZoomFullscreen()
const containerRef = ref(null)
const { ratio, startDrag, resetSplit } = useSplitter(0.5)

// 全屏弹窗 - 输入区
const showInputDialog = ref(false)
const openInputDialog = () => { showInputDialog.value = true }

// 全屏弹窗 - 结果区
const showResultDialog = ref(false)
const openResultDialog = () => { showResultDialog.value = true }

// 文件上传
const onFileUpload = (side, e) => {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    if (side === 'left') leftText.value = ev.target.result
    else rightText.value = ev.target.result
    ElMessage.success(`已加载: ${file.name}`)
  }
  reader.readAsText(file)
  e.target.value = ''
}

// 规范化行（根据选项处理）
const normalizeLine = (line) => {
  let result = line
  if (ignoreCR.value) {
    result = result.replace(/\r/g, '')
  }
  if (ignoreTab.value) {
    result = result.replace(/\t/g, '')
  }
  if (ignoreSpace.value) {
    result = result.replace(/ /g, '')
  }
  if (trimLine.value) {
    result = result.trim()
  }
  if (ignoreCase.value) {
    result = result.toLowerCase()
  }
  return result
}

// 处理文本为行的集合
const processLines = (text) => {
  let lines = text.split('\n')
  if (ignoreEmptyLines.value) {
    lines = lines.filter((l) => normalizeLine(l) !== '')
  }
  return lines
}

// 计算集合
const comparison = computed(() => {
  const leftLines = processLines(leftText.value)
  const rightLines = processLines(rightText.value)

  // 规范化后的行用于比较
  const normLeftSet = new Set()
  const normRightSet = new Set()
  // 保留原始行（第一次出现的）
  const leftOrigMap = new Map() // normLine -> origLine
  const rightOrigMap = new Map()

  leftLines.forEach((line) => {
    const norm = normalizeLine(line)
    if (!normLeftSet.has(norm)) {
      normLeftSet.add(norm)
      leftOrigMap.set(norm, line)
    }
  })

  rightLines.forEach((line) => {
    const norm = normalizeLine(line)
    if (!normRightSet.has(norm)) {
      normRightSet.add(norm)
      rightOrigMap.set(norm, line)
    }
  })

  // 交集：A 和 B 都存在
  const both = []
  // 仅 A 存在
  const onlyA = []
  // 仅 B 存在
  const onlyB = []

  normLeftSet.forEach((norm) => {
    if (normRightSet.has(norm)) {
      both.push(leftOrigMap.get(norm))
    } else {
      onlyA.push(leftOrigMap.get(norm))
    }
  })

  normRightSet.forEach((norm) => {
    if (!normLeftSet.has(norm)) {
      onlyB.push(rightOrigMap.get(norm))
    }
  })

  return {
    both,
    onlyA,
    onlyB,
    leftCount: normLeftSet.size,
    rightCount: normRightSet.size
  }
})

// 当前显示的结果
const currentResult = computed(() => {
  if (activeResult.value === 'both') return comparison.value.both
  if (activeResult.value === 'onlyA') return comparison.value.onlyA
  return comparison.value.onlyB
})

// 原始文件/文本的行数
const leftLineCount = computed(() => {
  if (!leftText.value) return 0
  return leftText.value.split('\n').length
})
const rightLineCount = computed(() => {
  if (!rightText.value) return 0
  return rightText.value.split('\n').length
})

// 交换
const swap = () => {
  const tmp = leftText.value
  leftText.value = rightText.value
  rightText.value = tmp
}

// 清空
const clearAll = () => {
  leftText.value = ''
  rightText.value = ''
}

// 复制结果
const copyResult = () => {
  if (currentResult.value.length === 0) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  const text = currentResult.value.join('\n')
  navigator.clipboard.writeText(text).then(() => ElMessage.success(`已复制 ${currentResult.value.length} 行`))
}
</script>

<template>
  <div class="compare-tool" ref="containerRef">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="options">
        <el-checkbox v-model="ignoreCase" size="small">忽略大小写</el-checkbox>
        <el-checkbox v-model="ignoreSpace" size="small">忽略空格</el-checkbox>
        <el-checkbox v-model="ignoreTab" size="small">忽略Tab</el-checkbox>
        <el-checkbox v-model="ignoreCR" size="small">忽略回车</el-checkbox>
        <el-checkbox v-model="trimLine" size="small">去除行首尾空白</el-checkbox>
        <el-checkbox v-model="ignoreEmptyLines" size="small">忽略空行</el-checkbox>
        <el-divider direction="vertical" />
        <el-checkbox v-model="showLineNumbers" size="small">显示行号</el-checkbox>
      </div>
      <div class="actions">
        <el-button size="small" :icon="Switch" @click="swap" title="交换A/B">交换</el-button>
        <el-button size="small" :icon="Delete" @click="clearAll" title="清空">清空</el-button>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-section">
      <div class="input-pane" :style="{ flex: ratio }">
        <div class="pane-header">
          <span class="pane-title">文本 A</span>
          <div class="pane-actions">
            <label class="upload-btn">
              <el-icon><Upload /></el-icon>
              <span>上传</span>
              <input type="file" accept=".txt,.md,.json,.js,.ts,.vue,.java,.py,.xml,.yml,.yaml,.csv,.log" @change="onFileUpload('left', $event)" hidden />
            </label>
            <el-button size="small" text @click="openInputDialog" title="放大输入区">🔍</el-button>
          </div>
        </div>
        <LineNumberTextarea
          v-model="leftText"
          :show-line-numbers="showLineNumbers"
          :font-size="fontSize"
          placeholder="粘贴或输入文本 A..."
          class="text-area"
        />
      </div>

      <div class="splitter" @mousedown="startDrag($event, containerRef, false)">
        <div class="splitter-line"></div>
      </div>

      <div class="input-pane" :style="{ flex: 1 - ratio }">
        <div class="pane-header">
          <span class="pane-title">文本 B</span>
          <div class="pane-actions">
            <label class="upload-btn">
              <el-icon><Upload /></el-icon>
              <span>上传</span>
              <input type="file" accept=".txt,.md,.json,.js,.ts,.vue,.java,.py,.xml,.yml,.yaml,.csv,.log" @change="onFileUpload('right', $event)" hidden />
            </label>
            <el-button size="small" text @click="openInputDialog" title="放大输入区">🔍</el-button>
          </div>
        </div>
        <LineNumberTextarea
          v-model="rightText"
          :show-line-numbers="showLineNumbers"
          :font-size="fontSize"
          placeholder="粘贴或输入文本 B..."
          class="text-area"
        />
      </div>
    </div>

    <!-- 结果区 -->
    <div v-if="leftText || rightText" class="result-section">
      <!-- 统计栏 -->
      <div class="stats-bar">
        <span class="stat-item">A: {{ leftLineCount }} 行 (去重后 {{ comparison.leftCount }})</span>
        <span class="stat-item">B: {{ rightLineCount }} 行 (去重后 {{ comparison.rightCount }})</span>
        <el-divider direction="vertical" />
        <span class="stat-item add">交集: {{ comparison.both.length }}</span>
        <span class="stat-item warn">仅A: {{ comparison.onlyA.length }}</span>
        <span class="stat-item del">仅B: {{ comparison.onlyB.length }}</span>
      </div>
      <div class="result-header">
        <div class="result-tabs">
          <div
            class="result-tab"
            :class="{ active: activeResult === 'both' }"
            @click="activeResult = 'both'"
          >
            <span class="tab-label">A ∩ B 都存在</span>
            <span class="tab-count">{{ comparison.both.length }}</span>
          </div>
          <div
            class="result-tab"
            :class="{ active: activeResult === 'onlyA' }"
            @click="activeResult = 'onlyA'"
          >
            <span class="tab-label">A - B 仅A存在</span>
            <span class="tab-count">{{ comparison.onlyA.length }}</span>
          </div>
          <div
            class="result-tab"
            :class="{ active: activeResult === 'onlyB' }"
            @click="activeResult = 'onlyB'"
          >
            <span class="tab-label">B - A 仅B存在</span>
            <span class="tab-count">{{ comparison.onlyB.length }}</span>
          </div>
        </div>
        <div class="result-actions">
          <el-button size="small" text @click="zoomOut">A-</el-button>
          <span class="font-size">{{ fontSize }}px</span>
          <el-button size="small" text @click="zoomIn">A+</el-button>
          <el-button size="small" text :icon="CopyDocument" @click="copyResult">复制</el-button>
          <el-button size="small" text @click="resetSplit">重置</el-button>
          <el-button size="small" text @click="openResultDialog" title="放大结果区">🔍</el-button>
        </div>
      </div>
      <div class="result-content" :style="{ fontSize: fontSize + 'px' }">
        <div v-if="currentResult.length === 0" class="result-empty">
          无匹配结果
        </div>
        <div v-else>
          <div
            v-for="(line, idx) in currentResult"
            :key="idx"
            class="result-line"
            :class="'line-' + activeResult"
          >
            <span class="line-idx">{{ idx + 1 }}</span>
            <span class="line-text">{{ line }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区放大弹窗 -->
    <el-dialog
      v-model="showInputDialog"
      title="文本对比 - 输入区放大"
      width="95%"
      top="2vh"
      class="compare-dialog"
    >
      <div class="dialog-content">
        <div class="dialog-toolbar">
          <div class="options">
            <el-button size="small" :icon="Switch" @click="swap">交换</el-button>
            <el-button size="small" @click="resetSplit">重置分割</el-button>
          </div>
          <div class="actions">
            <el-button size="small" @click="zoomOut">A-</el-button>
            <span class="font-size">{{ fontSize }}px</span>
            <el-button size="small" @click="zoomIn">A+</el-button>
          </div>
        </div>
        <div class="input-section dialog-input">
          <div class="input-pane" :style="{ flex: ratio }">
            <div class="pane-header">
              <span class="pane-title">文本 A</span>
              <label class="upload-btn">
                <el-icon><Upload /></el-icon>
                <span>上传</span>
                <input type="file" accept=".txt,.md,.json,.js,.ts,.vue,.java,.py,.xml,.yml,.yaml,.csv,.log" @change="onFileUpload('left', $event)" hidden />
              </label>
            </div>
            <LineNumberTextarea
              v-model="leftText"
              :show-line-numbers="showLineNumbers"
              :font-size="fontSize"
              placeholder="粘贴或输入文本 A..."
              class="text-area"
            />
          </div>
          <div class="splitter" @mousedown="startDrag($event, containerRef, false)">
            <div class="splitter-line"></div>
          </div>
          <div class="input-pane" :style="{ flex: 1 - ratio }">
            <div class="pane-header">
              <span class="pane-title">文本 B</span>
              <label class="upload-btn">
                <el-icon><Upload /></el-icon>
                <span>上传</span>
                <input type="file" accept=".txt,.md,.json,.js,.ts,.vue,.java,.py,.xml,.yml,.yaml,.csv,.log" @change="onFileUpload('right', $event)" hidden />
              </label>
            </div>
            <LineNumberTextarea
              v-model="rightText"
              :show-line-numbers="showLineNumbers"
              :font-size="fontSize"
              placeholder="粘贴或输入文本 B..."
              class="text-area"
            />
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 结果区放大弹窗 -->
    <el-dialog
      v-model="showResultDialog"
      title="文本对比 - 结果区放大"
      width="95%"
      top="2vh"
      class="compare-dialog"
    >
      <div class="dialog-content">
        <div class="dialog-toolbar">
          <div class="options">
            <el-checkbox v-model="ignoreCase" size="small">忽略大小写</el-checkbox>
            <el-checkbox v-model="ignoreSpace" size="small">忽略空格</el-checkbox>
            <el-checkbox v-model="ignoreTab" size="small">忽略Tab</el-checkbox>
            <el-checkbox v-model="ignoreCR" size="small">忽略回车</el-checkbox>
            <el-checkbox v-model="trimLine" size="small">去首尾空白</el-checkbox>
            <el-checkbox v-model="ignoreEmptyLines" size="small">忽略空行</el-checkbox>
          </div>
          <div class="actions">
            <el-button size="small" @click="zoomOut">A-</el-button>
            <span class="font-size">{{ fontSize }}px</span>
            <el-button size="small" @click="zoomIn">A+</el-button>
            <el-button size="small" :icon="CopyDocument" @click="copyResult">复制</el-button>
          </div>
        </div>
        <div class="stats-bar">
          <span class="stat-item">A: {{ leftLineCount }} 行 (去重后 {{ comparison.leftCount }})</span>
          <span class="stat-item">B: {{ rightLineCount }} 行 (去重后 {{ comparison.rightCount }})</span>
          <el-divider direction="vertical" />
          <span class="stat-item add">交集: {{ comparison.both.length }}</span>
          <span class="stat-item warn">仅A: {{ comparison.onlyA.length }}</span>
          <span class="stat-item del">仅B: {{ comparison.onlyB.length }}</span>
        </div>
        <div class="result-tabs dialog-tabs">
          <div
            class="result-tab"
            :class="{ active: activeResult === 'both' }"
            @click="activeResult = 'both'"
          >
            <span class="tab-label">A ∩ B 都存在</span>
            <span class="tab-count">{{ comparison.both.length }}</span>
          </div>
          <div
            class="result-tab"
            :class="{ active: activeResult === 'onlyA' }"
            @click="activeResult = 'onlyA'"
          >
            <span class="tab-label">A - B 仅A存在</span>
            <span class="tab-count">{{ comparison.onlyA.length }}</span>
          </div>
          <div
            class="result-tab"
            :class="{ active: activeResult === 'onlyB' }"
            @click="activeResult = 'onlyB'"
          >
            <span class="tab-label">B - A 仅B存在</span>
            <span class="tab-count">{{ comparison.onlyB.length }}</span>
          </div>
        </div>
        <div class="result-content dialog-result" :style="{ fontSize: fontSize + 'px' }">
          <div v-if="currentResult.length === 0" class="result-empty">
            无匹配结果
          </div>
          <div v-else>
            <div
              v-for="(line, idx) in currentResult"
              :key="idx"
              class="result-line"
              :class="'line-' + activeResult"
            >
              <span class="line-idx">{{ idx + 1 }}</span>
              <span class="line-text">{{ line }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.compare-tool {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 8px;
  padding: 12px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  flex-shrink: 0;
}

.options {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.actions {
  display: flex;
  gap: 6px;
}

.input-section {
  display: flex;
  flex: 1;
  min-height: 180px;
  gap: 0;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.input-pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.pane-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.pane-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.pane-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.upload-btn:hover {
  background: #ecf5ff;
}

.text-area {
  flex: 1;
  min-height: 0;
}

.text-area :deep(.el-textarea) {
  height: 100%;
}

.text-area :deep(.el-textarea__inner) {
  height: 100% !important;
  resize: none;
  border: none;
  border-radius: 0;
  font-family: 'Consolas', 'Courier New', monospace;
  line-height: 1.5;
}

/* 分割条 */
.splitter {
  width: 6px;
  background: #e4e7ed;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}

.splitter:hover {
  background: #409eff;
}

.splitter-line {
  width: 2px;
  height: 40px;
  background: #c0c4cc;
  border-radius: 1px;
}

/* 结果区 */
.result-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 150px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.stats-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-size: 13px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.stat-item { color: #606266; }
.stat-item.add { color: #67c23a; font-weight: 600; }
.stat-item.warn { color: #e6a23c; font-weight: 600; }
.stat-item.del { color: #f56c6c; font-weight: 600; }

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
}

.result-tabs {
  display: flex;
  gap: 4px;
}

.result-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.result-tab:hover {
  color: #409eff;
}

.result-tab.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 600;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  background: #e4e7ed;
  color: #606266;
  font-size: 11px;
  font-weight: 600;
}

.result-tab.active .tab-count {
  background: #409eff;
  color: #fff;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.font-size {
  font-size: 12px;
  color: #909399;
  min-width: 40px;
  text-align: center;
}

.result-content {
  flex: 1;
  overflow: auto;
  font-family: 'Consolas', 'Courier New', monospace;
  line-height: 1.6;
  background: #fafafa;
}

.result-empty {
  padding: 20px;
  text-align: center;
  color: #909399;
}

.result-line {
  display: flex;
  padding: 0 8px;
  white-space: pre-wrap;
  word-break: break-all;
}

.result-line:hover {
  background: #f0f7ff;
}

.line-idx {
  width: 50px;
  min-width: 50px;
  text-align: right;
  padding: 0 8px;
  color: #c0c4cc;
  font-size: 0.85em;
  user-select: none;
  border-right: 1px solid #e4e7ed;
}

.line-text {
  flex: 1;
  padding-left: 8px;
}

/* 不同结果的行颜色 */
.line-both {
  background: #f0f9eb;
  border-left: 3px solid #67c23a;
}

.line-onlyA {
  background: #fdf6ec;
  border-left: 3px solid #e6a23c;
}

.line-onlyB {
  background: #fef0f0;
  border-left: 3px solid #f56c6c;
}

/* 弹窗 */
.compare-dialog :deep(.el-dialog__body) {
  padding: 12px;
}

.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 85vh;
}

.dialog-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.dialog-tabs {
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
}

.dialog-result {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.dialog-input {
  flex: 1;
  min-height: 0;
}
</style>
