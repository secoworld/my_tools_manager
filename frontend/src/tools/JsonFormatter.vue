<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Minus, Lock, Unlock, Sort, CircleCheck, Delete, CopyDocument } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: {
    type: String,
    required: true
  }
})

const inputText = ref('')
const outputText = ref('')
const statusInfo = ref('')

// 状态持久化（刷新页面后输入内容不丢失）
useToolState(props.instanceId, { inputText, outputText })

// 格式化 JSON（2 空格缩进）
const formatJson = () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入 JSON 内容')
    return
  }
  try {
    const parsed = JSON.parse(inputText.value)
    outputText.value = JSON.stringify(parsed, null, 2)
    updateStatus(parsed, true)
    ElMessage.success('格式化成功')
  } catch (error) {
    handleError(error)
  }
}

// 压缩 JSON
const compressJson = () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入 JSON 内容')
    return
  }
  try {
    const parsed = JSON.parse(inputText.value)
    outputText.value = JSON.stringify(parsed)
    updateStatus(parsed, true)
    ElMessage.success('压缩成功')
  } catch (error) {
    handleError(error)
  }
}

// 转义：将 JSON 字符串中的特殊字符转义（如 " -> \"）
const escapeJson = () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  outputText.value = JSON.stringify(inputText.value).slice(1, -1)
  statusInfo.value = '已转义'
  ElMessage.success('转义成功')
}

// 去除转义：将转义后的字符串还原
const unescapeJson = () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  try {
    outputText.value = JSON.parse('"' + inputText.value + '"')
    statusInfo.value = '已去除转义'
    ElMessage.success('去除转义成功')
  } catch (error) {
    ElMessage.error('去除转义失败: ' + error.message)
  }
}

// 按 key 排序
const sortJson = () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入 JSON 内容')
    return
  }
  try {
    const parsed = JSON.parse(inputText.value)
    const sorted = sortObject(parsed)
    outputText.value = JSON.stringify(sorted, null, 2)
    updateStatus(sorted, true)
    ElMessage.success('排序成功')
  } catch (error) {
    handleError(error)
  }
}

// 递归排序对象 key
const sortObject = (obj) => {
  if (Array.isArray(obj)) {
    return obj.map(sortObject)
  } else if (obj !== null && typeof obj === 'object') {
    const sorted = {}
    Object.keys(obj).sort().forEach((key) => {
      sorted[key] = sortObject(obj[key])
    })
    return sorted
  }
  return obj
}

// 校验 JSON
const validateJson = () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入 JSON 内容')
    return
  }
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

// 生成校验报告
const generateReport = (parsed) => {
  const type = Array.isArray(parsed) ? 'Array' : typeof parsed
  const size = new Blob([inputText.value]).size
  let count = 0
  if (Array.isArray(parsed)) {
    count = parsed.length
  } else if (parsed !== null && typeof parsed === 'object') {
    count = Object.keys(parsed).length
  }
  return [
    '✓ JSON 格式正确',
    '',
    `类型:     ${type}`,
    `大小:     ${formatSize(size)}`,
    type === 'Array' ? `元素数量: ${count}` : type === 'object' ? `键数量:   ${count}` : ''
  ].filter(Boolean).join('\n')
}

// 更新状态信息
const updateStatus = (parsed, valid) => {
  if (!valid) {
    statusInfo.value = '格式错误'
    return
  }
  const type = Array.isArray(parsed) ? 'Array' : typeof parsed
  const size = new Blob([outputText.value || inputText.value]).size
  statusInfo.value = `类型: ${type} | 大小: ${formatSize(size)}`
}

// 错误处理
const handleError = (error) => {
  statusInfo.value = '格式错误'
  ElMessage.error('JSON 解析失败: ' + error.message)
}

// 格式化文件大小
const formatSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 复制结果
const copyResult = async () => {
  if (!outputText.value) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(outputText.value)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

// 将输出移到输入
const outputToInput = () => {
  if (!outputText.value) {
    ElMessage.warning('没有可移动的内容')
    return
  }
  inputText.value = outputText.value
  outputText.value = ''
  statusInfo.value = ''
}

// 清空
const clearAll = () => {
  inputText.value = ''
  outputText.value = ''
  statusInfo.value = ''
}
</script>

<template>
  <div class="json-formatter">
    <!-- 工具栏 -->
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
    </div>

    <!-- 内容区 -->
    <div class="content">
      <div class="input-area">
        <div class="label">输入 JSON</div>
        <el-input
          v-model="inputText"
          type="textarea"
          placeholder='请输入 JSON 内容，例如: {"name":"test","value":123}'
        />
      </div>
      <div class="output-area">
        <div class="label-row">
          <span class="label">输出结果</span>
          <div class="output-actions">
            <el-button size="small" :icon="CopyDocument" @click="copyResult">复制</el-button>
            <el-button size="small" @click="outputToInput">移到输入</el-button>
          </div>
        </div>
        <el-input
          v-model="outputText"
          type="textarea"
          readonly
          placeholder="操作结果将显示在这里"
        />
      </div>
    </div>

    <!-- 状态栏 -->
    <div v-if="statusInfo" class="status-bar">
      {{ statusInfo }}
    </div>
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
}

.content {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  min-height: 0;
}

.input-area,
.output-area {
  flex: 1 1 280px;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.label {
  margin-bottom: 8px;
  font-weight: 600;
  color: #606266;
}

.label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.output-actions {
  display: flex;
  gap: 8px;
}

.input-area :deep(.el-textarea__inner),
.output-area :deep(.el-textarea__inner) {
  height: 100%;
  resize: none;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.status-bar {
  margin-top: 8px;
  padding: 6px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
}
</style>
