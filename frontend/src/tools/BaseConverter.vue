<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: { type: String, required: true }
})

// 格式选项
const uppercase = ref(true)
const withPrefix = ref(false)

// 各进制的输入值（字符串，支持双向编辑）
const values = reactive({
  2: '', 8: '', 10: '255', 16: ''
})

// 当前正在编辑的进制
const activeBase = ref(10)

// 状态持久化
useToolState(props.instanceId, { values, uppercase, withPrefix, activeBase })

// 进制选项
const baseOptions = [
  { value: 2, label: '二进制', prefix: '0b' },
  { value: 8, label: '八进制', prefix: '0o' },
  { value: 10, label: '十进制', prefix: '' },
  { value: 16, label: '十六进制', prefix: '0x' }
]

// 去除前缀
const stripPrefix = (val) => {
  if (val.startsWith('0b') || val.startsWith('0B')) return { clean: val.slice(2), base: 2 }
  if (val.startsWith('0o') || val.startsWith('0O')) return { clean: val.slice(2), base: 8 }
  if (val.startsWith('0x') || val.startsWith('0X')) return { clean: val.slice(2), base: 16 }
  return { clean: val, base: null }
}

// 格式化输出
const formatOutput = (decimalVal, base) => {
  if (decimalVal === null || isNaN(decimalVal)) return ''
  let str = decimalVal.toString(base)
  // 大写处理（仅 16 进制有字母）
  if (base === 16 && uppercase.value) {
    str = str.toUpperCase()
  }
  // 前缀处理
  const prefixInfo = baseOptions.find(b => b.value === base)
  if (withPrefix.value && prefixInfo && prefixInfo.prefix) {
    str = prefixInfo.prefix + str
  }
  return str
}

// 当某个输入框变化时，更新所有其他输入框
const onInput = (base) => {
  activeBase.value = base
  const raw = values[base].trim()
  if (!raw) {
    // 输入为空，清空所有
    baseOptions.forEach(b => {
      if (b.value !== base) values[b.value] = ''
    })
    return
  }

  // 解析输入值（支持前缀自动识别）
  let parseBase = base
  let cleanVal = raw
  const stripped = stripPrefix(raw)
  if (stripped.base !== null) {
    parseBase = stripped.base
    cleanVal = stripped.clean
  }

  const decimalVal = parseInt(cleanVal, parseBase)
  if (isNaN(decimalVal)) return

  // 更新所有其他进制
  baseOptions.forEach(b => {
    if (b.value === base) return
    values[b.value] = formatOutput(decimalVal, b.value)
  })
}

// 初始化
onInput(10)

// 复制结果
const copyValue = (text, base) => {
  if (!text) { ElMessage.warning('没有可复制的内容'); return }
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success(`已复制: ${text}`)
  })
}

// 快捷填入
const quickFill = (val, base) => {
  // 清空所有
  baseOptions.forEach(b => { values[b.value] = '' })
  values[base] = val
  onInput(base)
}

// 格式选项变化时重新计算
const onFormatChange = () => {
  onInput(activeBase.value)
}
</script>

<template>
  <div class="base-converter-tool">
    <!-- 顶部控制区 -->
    <div class="control-section">
      <div class="options">
        <el-checkbox v-model="uppercase" @change="onFormatChange">十六进制大写</el-checkbox>
        <el-checkbox v-model="withPrefix" @change="onFormatChange">带前缀 (0b/0o/0x)</el-checkbox>
        <el-popover placement="bottom" :width="260" trigger="hover">
          <template #reference>
            <el-icon class="tips-icon"><QuestionFilled /></el-icon>
          </template>
          <div style="font-size: 13px; line-height: 1.8;">
            <p style="font-weight: bold;">使用说明</p>
            <p>在任意进制行输入数值，其他进制自动联动更新</p>
            <p>支持前缀自动识别: 0b(二) 0o(八) 0x(十六)</p>
            <p style="color: #999; margin-top: 6px;">点击复制按钮可复制对应值</p>
          </div>
        </el-popover>
      </div>

      <!-- 快捷预设 -->
      <div class="presets">
        <el-button size="small" @click="quickFill('255', 10)">255</el-button>
        <el-button size="small" @click="quickFill('1024', 10)">1024</el-button>
        <el-button size="small" @click="quickFill('0xFF', 16)">0xFF</el-button>
        <el-button size="small" @click="quickFill('0b1010', 2)">0b1010</el-button>
        <el-button size="small" @click="quickFill('65535', 10)">65535</el-button>
      </div>
    </div>

    <!-- 进制列表（每行可输入） -->
    <div class="results-section">
      <div class="results-header">
        <span>在各行输入数值，其他行自动联动</span>
      </div>
      <div class="results-list">
        <div
          v-for="opt in baseOptions"
          :key="opt.value"
          class="result-item"
          :class="{ active: opt.value === activeBase }"
        >
          <div class="result-info">
            <span class="result-base">{{ opt.label }}</span>
            <span class="result-base-num">({{ opt.value }})</span>
          </div>
          <el-input
            v-model="values[opt.value]"
            :placeholder="opt.value === activeBase ? '输入数值...' : '—'"
            class="value-input"
            size="default"
            @input="onInput(opt.value)"
            @focus="activeBase = opt.value"
          />
          <el-button
            size="small"
            text
            :icon="CopyDocument"
            class="copy-btn"
            @click="copyValue(values[opt.value], opt.value)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.base-converter-tool {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 12px;
  padding: 16px;
}

.control-section {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.options {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.tips-icon {
  font-size: 16px;
  color: #c0c4cc;
  cursor: pointer;
  margin-left: auto;
}

.tips-icon:hover {
  color: #409eff;
}

.presets {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.presets .el-button {
  margin: 0;
  font-family: 'Consolas', monospace;
}

/* 结果区 */
.results-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.results-header {
  padding: 8px 14px;
  background: #f5f7fa;
  font-size: 12px;
  color: #909399;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.results-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.2s;
}

.result-item:hover {
  background: #ecf5ff;
  border-color: #c6e2ff;
}

.result-item.active {
  background: #ecf5ff;
  border-color: #409eff;
  border-left: 3px solid #409eff;
}

.result-info {
  min-width: 90px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.result-base {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.result-base-num {
  font-size: 11px;
  color: #909399;
}

.value-input {
  flex: 1;
}

.value-input :deep(.el-input__inner) {
  text-align: right;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 15px;
  font-weight: bold;
  color: #303133;
}

.copy-btn {
  flex-shrink: 0;
  color: #c0c4cc;
}

.copy-btn:hover {
  color: #409eff;
}
</style>
