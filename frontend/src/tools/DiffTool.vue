<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Delete, Switch } from '@element-plus/icons-vue'
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

// 选项
const ignoreCase = ref(false)
const ignoreWhitespace = ref(false)
const ignoreEmptyLines = ref(false)
const onlyDiff = ref(false) // 只显示差异部分
const contextLines = ref(3) // 差异上下文行数（onlyDiff 为 true 时生效）

// 状态持久化
useToolState(props.instanceId, { leftText, rightText, ignoreCase, ignoreWhitespace, ignoreEmptyLines, onlyDiff, contextLines, showLineNumbers })

// 缩放和全屏
const { fontSize, zoomIn, zoomOut, toggleFullscreen, exitFullscreen, isFullscreen } = useZoomFullscreen()
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
  e.target.value = '' // 允许重复上传同一文件
}

// 规范化行（根据选项处理）
const normalizeLine = (line) => {
  let result = line
  if (ignoreWhitespace.value) {
    result = result.trim().replace(/\s+/g, ' ')
  }
  if (ignoreCase.value) {
    result = result.toLowerCase()
  }
  return result
}

// 过滤行
const filterLines = (lines) => {
  if (!ignoreEmptyLines.value) return lines
  return lines.filter((l) => l.trim() !== '')
}

// LCS 算法计算 diff
const computeDiff = (leftLines, rightLines) => {
  // 规范化用于比较
  const normLeft = leftLines.map(normalizeLine)
  const normRight = rightLines.map(normalizeLine)

  const m = normLeft.length
  const n = normRight.length

  // 构建 LCS 表
  const dp = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0))
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (normLeft[i - 1] === normRight[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
      }
    }
  }

  // 回溯生成 diff 结果
  const result = []
  let i = m, j = n
  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && normLeft[i - 1] === normRight[j - 1]) {
      result.unshift({
        type: 'equal',
        leftLine: leftLines[i - 1],
        rightLine: rightLines[j - 1],
        leftNum: i,
        rightNum: j
      })
      i--; j--
    } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
      result.unshift({
        type: 'added',
        leftLine: null,
        rightLine: rightLines[j - 1],
        leftNum: null,
        rightNum: j
      })
      j--
    } else {
      result.unshift({
        type: 'removed',
        leftLine: leftLines[i - 1],
        rightLine: null,
        leftNum: i,
        rightNum: null
      })
      i--
    }
  }
  return result
}

// Diff 结果
const diffResult = computed(() => {
  const leftLines = filterLines(leftText.value.split('\n'))
  const rightLines = filterLines(rightText.value.split('\n'))
  return computeDiff(leftLines, rightLines)
})

// 过滤后的 Diff 结果（只显示差异部分 + 上下文）
const filteredDiffResult = computed(() => {
  if (!onlyDiff.value) return diffResult.value

  const ctx = contextLines.value
  const result = diffResult.value
  if (result.length === 0) return result

  // 找出所有差异行的索引
  const diffIndices = []
  result.forEach((item, idx) => {
    if (item.type !== 'equal') diffIndices.push(idx)
  })

  if (diffIndices.length === 0) return []

  // 计算需要保留的区间 [start, end]
  const ranges = []
  diffIndices.forEach((idx) => {
    const start = Math.max(0, idx - ctx)
    const end = Math.min(result.length - 1, idx + ctx)
    ranges.push([start, end])
  })

  // 合并重叠的区间
  const merged = [ranges[0]]
  for (let i = 1; i < ranges.length; i++) {
    const last = merged[merged.length - 1]
    if (ranges[i][0] <= last[1] + 1) {
      // 重叠或相邻，合并
      last[1] = Math.max(last[1], ranges[i][1])
    } else {
      merged.push(ranges[i])
    }
  }

  // 构建过滤后的结果，在区间之间插入折叠标记
  const filtered = []
  let lastEnd = -1
  merged.forEach(([start, end]) => {
    // 如果前面有跳过的行，插入折叠标记
    if (lastEnd >= 0 && start > lastEnd + 1) {
      filtered.push({
        type: 'folded',
        skippedCount: start - lastEnd - 1
      })
    } else if (lastEnd < 0 && start > 0) {
      filtered.push({
        type: 'folded',
        skippedCount: start
      })
    }
    // 添加区间内的行
    for (let i = start; i <= end; i++) {
      filtered.push(result[i])
    }
    lastEnd = end
  })

  // 末尾如果有跳过的行
  if (lastEnd < result.length - 1) {
    filtered.push({
      type: 'folded',
      skippedCount: result.length - 1 - lastEnd
    })
  }

  return filtered
})

// 统计
const stats = computed(() => {
  let added = 0, removed = 0, equal = 0
  diffResult.value.forEach((d) => {
    if (d.type === 'added') added++
    else if (d.type === 'removed') removed++
    else equal++
  })
  return { added, removed, equal, total: diffResult.value.length }
})

// 是否一致
const isIdentical = computed(() => stats.value.added === 0 && stats.value.removed === 0)

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

// 复制
const copyResult = () => {
  const text = diffResult.value.map((d) => {
    if (d.type === 'equal') return `  ${d.leftNum || ''}\t  ${d.leftLine}`
    if (d.type === 'added') return `+ ${d.rightNum}\t+ ${d.rightLine}`
    if (d.type === 'removed') return `- ${d.leftNum}\t- ${d.leftLine}`
  }).join('\n')
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制 diff 结果'))
}
</script>

<template>
  <div class="diff-tool" ref="containerRef">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="options">
        <el-checkbox v-model="ignoreCase" size="small">忽略大小写</el-checkbox>
        <el-checkbox v-model="ignoreWhitespace" size="small">忽略空白</el-checkbox>
        <el-checkbox v-model="ignoreEmptyLines" size="small">忽略空行</el-checkbox>
        <el-divider direction="vertical" />
        <el-checkbox v-model="showLineNumbers" size="small">显示行号</el-checkbox>
      </div>
      <div class="actions">
        <el-button size="small" :icon="Switch" @click="swap" title="交换左右">交换</el-button>
        <el-button size="small" @click="copyResult" title="复制结果">复制</el-button>
        <el-button size="small" :icon="Delete" @click="clearAll" title="清空">清空</el-button>
      </div>
    </div>

    <!-- 输入区（可拖拽分割条） -->
    <div class="input-section">
      <div class="input-pane" :style="{ flex: ratio }">
        <div class="pane-header">
          <span class="pane-title">原始文本 (A)</span>
          <div class="pane-actions">
            <label class="upload-btn">
              <el-icon><Upload /></el-icon>
              <span>上传文件</span>
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

      <!-- 分割条 -->
      <div class="splitter" @mousedown="startDrag($event, containerRef, false)">
        <div class="splitter-line"></div>
      </div>

      <div class="input-pane" :style="{ flex: 1 - ratio }">
        <div class="pane-header">
          <span class="pane-title">对比文本 (B)</span>
          <div class="pane-actions">
            <label class="upload-btn">
              <el-icon><Upload /></el-icon>
              <span>上传文件</span>
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

    <!-- Diff 结果区 -->
    <div v-if="leftText || rightText" class="diff-result">
      <!-- 统计栏 -->
      <div class="stats-bar">
        <span class="stat-item">A: {{ leftLineCount }} 行</span>
        <span class="stat-item">B: {{ rightLineCount }} 行</span>
        <el-divider direction="vertical" />
        <span v-if="isIdentical" class="stat-identical">✅ 内容完全一致</span>
        <span v-else class="stat-different">❌ 存在差异</span>
        <span class="stat-item">相同: {{ stats.equal }}</span>
        <span class="stat-item add">新增: {{ stats.added }}</span>
        <span class="stat-item del">删除: {{ stats.removed }}</span>
      </div>
      <div class="result-header">
        <span class="result-title">Diff 结果</span>
        <div class="result-options">
          <el-checkbox v-model="onlyDiff" size="small">只显示差异</el-checkbox>
          <el-input-number
            v-if="onlyDiff"
            v-model="contextLines"
            size="small"
            :min="0"
            :max="20"
            :step="1"
            controls-position="right"
            style="width: 100px;"
            title="差异上下文行数"
          />
          <span v-if="onlyDiff" class="ctx-label">行上下文</span>
        </div>
        <div class="result-actions">
          <el-button size="small" text @click="zoomOut">A-</el-button>
          <span class="font-size">{{ fontSize }}px</span>
          <el-button size="small" text @click="zoomIn">A+</el-button>
          <el-button size="small" text @click="resetSplit">重置分割</el-button>
          <el-button size="small" text @click="openResultDialog" title="放大结果区">🔍</el-button>
        </div>
      </div>
      <div class="diff-content" :style="{ fontSize: fontSize + 'px' }">
        <div v-if="diffResult.length === 0" class="diff-empty">无内容</div>
        <template v-for="(item, idx) in filteredDiffResult" :key="idx">
          <!-- 折叠标记行 -->
          <div v-if="item.type === 'folded'" class="diff-line folded">
            <span class="line-num left-num"></span>
            <span class="line-num right-num"></span>
            <span class="line-marker">⋯</span>
            <span class="line-content folded-text">已折叠 {{ item.skippedCount }} 行相同内容</span>
          </div>
          <!-- 正常 diff 行 -->
          <div
            v-else
            class="diff-line"
            :class="item.type"
          >
            <span class="line-num left-num">{{ item.leftNum || '' }}</span>
            <span class="line-num right-num">{{ item.rightNum || '' }}</span>
            <span class="line-marker">{{ item.type === 'added' ? '+' : item.type === 'removed' ? '-' : ' ' }}</span>
            <span class="line-content">{{ item.type === 'added' ? item.rightLine : item.leftLine }}</span>
          </div>
        </template>
      </div>
    </div>

    <!-- 输入区放大弹窗 -->
    <el-dialog
      v-model="showInputDialog"
      title="Diff 对比 - 输入区放大"
      width="95%"
      top="2vh"
      class="diff-dialog"
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
              <span class="pane-title">原始文本 (A)</span>
              <label class="upload-btn">
                <el-icon><Upload /></el-icon>
                <span>上传文件</span>
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
              <span class="pane-title">对比文本 (B)</span>
              <label class="upload-btn">
                <el-icon><Upload /></el-icon>
                <span>上传文件</span>
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
      title="Diff 对比 - 结果区放大"
      width="95%"
      top="2vh"
      class="diff-dialog"
    >
      <div class="dialog-content">
        <div class="dialog-toolbar">
          <div class="options">
            <el-checkbox v-model="ignoreCase" size="small">忽略大小写</el-checkbox>
            <el-checkbox v-model="ignoreWhitespace" size="small">忽略空白</el-checkbox>
            <el-checkbox v-model="ignoreEmptyLines" size="small">忽略空行</el-checkbox>
            <el-divider direction="vertical" />
            <el-checkbox v-model="onlyDiff" size="small">只显示差异</el-checkbox>
            <el-input-number
              v-if="onlyDiff"
              v-model="contextLines"
              size="small"
              :min="0"
              :max="20"
              :step="1"
              controls-position="right"
              style="width: 100px;"
            />
            <span v-if="onlyDiff" class="ctx-label">行上下文</span>
          </div>
          <div class="actions">
            <el-button size="small" @click="zoomOut">A-</el-button>
            <span class="font-size">{{ fontSize }}px</span>
            <el-button size="small" @click="zoomIn">A+</el-button>
            <el-button size="small" @click="copyResult">复制结果</el-button>
          </div>
        </div>
        <div class="stats-bar">
          <span class="stat-item">A: {{ leftLineCount }} 行</span>
          <span class="stat-item">B: {{ rightLineCount }} 行</span>
          <el-divider direction="vertical" />
          <span v-if="isIdentical" class="stat-identical">✅ 内容完全一致</span>
          <span v-else class="stat-different">❌ 存在差异</span>
          <span class="stat-item">相同: {{ stats.equal }}</span>
          <span class="stat-item add">新增: {{ stats.added }}</span>
          <span class="stat-item del">删除: {{ stats.removed }}</span>
        </div>
        <div class="diff-content dialog-diff" :style="{ fontSize: fontSize + 'px' }">
          <template v-for="(item, idx) in filteredDiffResult" :key="idx">
            <div v-if="item.type === 'folded'" class="diff-line folded">
              <span class="line-num left-num"></span>
              <span class="line-num right-num"></span>
              <span class="line-marker">⋯</span>
              <span class="line-content folded-text">已折叠 {{ item.skippedCount }} 行相同内容</span>
            </div>
            <div
              v-else
              class="diff-line"
              :class="item.type"
            >
              <span class="line-num left-num">{{ item.leftNum || '' }}</span>
              <span class="line-num right-num">{{ item.rightNum || '' }}</span>
              <span class="line-marker">{{ item.type === 'added' ? '+' : item.type === 'removed' ? '-' : ' ' }}</span>
              <span class="line-content">{{ item.type === 'added' ? item.rightLine : item.leftLine }}</span>
            </div>
          </template>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.diff-tool {
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
  gap: 12px;
  flex-wrap: wrap;
}

.actions {
  display: flex;
  gap: 6px;
}

.stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
  flex-shrink: 0;
}

.stat-identical { color: #67c23a; font-weight: 600; }
.stat-different { color: #f56c6c; font-weight: 600; }
.stat-item { color: #606266; }
.stat-item.add { color: #e6a23c; }
.stat-item.del { color: #f56c6c; }
.stat-empty { color: #909399; }

.input-section {
  display: flex;
  flex: 1;
  min-height: 200px;
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

/* Diff 结果 */
.diff-result {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 150px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  gap: 12px;
  flex-wrap: wrap;
}

.result-title {
  font-weight: 600;
}

.result-options {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: normal;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: normal;
}

.font-size {
  font-size: 12px;
  color: #909399;
  min-width: 40px;
  text-align: center;
}

.diff-content {
  flex: 1;
  overflow: auto;
  font-family: 'Consolas', 'Courier New', monospace;
  line-height: 1.6;
  background: #fafafa;
}

.diff-empty {
  padding: 20px;
  text-align: center;
  color: #909399;
}

.diff-line {
  display: flex;
  padding: 0 4px;
  white-space: pre-wrap;
  word-break: break-all;
}

.diff-line.equal {
  background: #fafafa;
  color: #606266;
}

.diff-line.added {
  background: #e6ffec;
  color: #22863a;
}

.diff-line.removed {
  background: #ffebe9;
  color: #cb2431;
}

/* 折叠行 */
.diff-line.folded {
  background: #f0f2f5;
  color: #909399;
  font-style: italic;
  font-size: 0.9em;
}

.folded-text {
  color: #909399;
  font-style: italic;
}

.ctx-label {
  font-size: 12px;
  color: #909399;
}

.line-num {
  width: 40px;
  min-width: 40px;
  text-align: right;
  padding: 0 6px;
  color: #c0c4cc;
  font-size: 0.85em;
  user-select: none;
  border-right: 1px solid #e4e7ed;
}

.line-marker {
  width: 20px;
  min-width: 20px;
  text-align: center;
  font-weight: bold;
  user-select: none;
}

.line-content {
  flex: 1;
  padding-left: 8px;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 弹窗 */
.diff-dialog :deep(.el-dialog__body) {
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

.dialog-diff {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.dialog-input {
  flex: 1;
  min-height: 0;
}
</style>
