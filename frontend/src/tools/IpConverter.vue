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
const withPrefix = ref(true)

// 各输入位的值
const values = reactive({
  ip: '192.168.1.1',
  dec: '',
  hex: '',
  bin: ''
})

// 当前正在编辑的输入位
const activeField = ref('ip')

// 状态持久化
useToolState(props.instanceId, { values, uppercase, withPrefix, activeField })

// 输入位配置
const fields = [
  { key: 'ip', label: 'IP 地址', placeholder: '192.168.1.1', mono: true },
  { key: 'dec', label: '十进制数字', placeholder: '3232235777', mono: true },
  { key: 'hex', label: '十六进制', placeholder: '0xC0A80101', mono: true },
  { key: 'bin', label: '二进制', placeholder: '0b11000000...', mono: true }
]

// IP 地址 → 32 位无符号整型
const ipToInt = (ip) => {
  const parts = ip.split('.').map((p) => parseInt(p.trim(), 10))
  if (parts.length !== 4 || parts.some((p) => isNaN(p) || p < 0 || p > 255)) {
    return null
  }
  // 使用 >>> 0 确保为无符号 32 位整型
  return ((parts[0] << 24) | (parts[1] << 16) | (parts[2] << 8) | parts[3]) >>> 0
}

// 32 位无符号整型 → IP 地址
const intToIp = (num) => {
  if (num < 0 || num > 4294967295 || !Number.isInteger(num)) return null
  return [
    (num >>> 24) & 0xff,
    (num >>> 16) & 0xff,
    (num >>> 8) & 0xff,
    num & 0xff
  ].join('.')
}

// 去除前缀
const stripPrefix = (val) => {
  if (val.startsWith('0x') || val.startsWith('0X')) return { clean: val.slice(2), base: 16 }
  if (val.startsWith('0b') || val.startsWith('0B')) return { clean: val.slice(2), base: 2 }
  return { clean: val, base: null }
}

// 格式化十六进制输出
const formatHex = (num) => {
  let str = num.toString(16)
  if (uppercase.value) str = str.toUpperCase()
  if (withPrefix.value) str = '0x' + str
  return str
}

// 格式化二进制输出
const formatBin = (num) => {
  // 补齐到 32 位
  let str = num.toString(2).padStart(32, '0')
  if (withPrefix.value) str = '0b' + str
  return str
}

// 当某个输入框变化时，更新所有其他输入框
const onInput = (field) => {
  activeField.value = field
  const raw = values[field].trim()
  if (!raw) {
    // 输入为空，清空所有
    fields.forEach((f) => {
      if (f.key !== field) values[f.key] = ''
    })
    return
  }

  let intVal = null

  if (field === 'ip') {
    intVal = ipToInt(raw)
  } else if (field === 'dec') {
    const num = parseInt(raw, 10)
    if (!isNaN(num) && num >= 0 && num <= 4294967295) intVal = num
  } else if (field === 'hex') {
    const stripped = stripPrefix(raw)
    const num = parseInt(stripped.clean, stripped.base || 16)
    if (!isNaN(num) && num >= 0 && num <= 4294967295) intVal = num >>> 0
  } else if (field === 'bin') {
    const stripped = stripPrefix(raw)
    const num = parseInt(stripped.clean, stripped.base || 2)
    if (!isNaN(num) && num >= 0 && num <= 4294967295) intVal = num >>> 0
  }

  if (intVal === null) return

  // 更新所有其他输入位
  fields.forEach((f) => {
    if (f.key === field) return
    if (f.key === 'ip') values.ip = intToIp(intVal)
    else if (f.key === 'dec') values.dec = String(intVal)
    else if (f.key === 'hex') values.hex = formatHex(intVal)
    else if (f.key === 'bin') values.bin = formatBin(intVal)
  })
}

// 初始化
onInput('ip')

// 复制结果
const copyValue = (text, field) => {
  if (!text) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success(`已复制: ${text}`)
  })
}

// 快捷填入
const quickFill = (val, field) => {
  fields.forEach((f) => { values[f.key] = '' })
  values[field] = val
  onInput(field)
}

// 格式选项变化时重新计算
const onFormatChange = () => {
  onInput(activeField.value)
}
</script>

<template>
  <div class="ip-converter-tool">
    <!-- 顶部控制区 -->
    <div class="control-section">
      <div class="options">
        <el-checkbox v-model="uppercase" @change="onFormatChange">十六进制大写</el-checkbox>
        <el-checkbox v-model="withPrefix" @change="onFormatChange">带前缀 (0x/0b)</el-checkbox>
        <el-popover placement="bottom" :width="280" trigger="hover">
          <template #reference>
            <el-icon class="tips-icon"><QuestionFilled /></el-icon>
          </template>
          <div style="font-size: 13px; line-height: 1.8;">
            <p style="font-weight: bold;">使用说明</p>
            <p>在任意输入位输入，其他位置自动联动更新</p>
            <p>IP 格式: xxx.xxx.xxx.xxx (每段 0-255)</p>
            <p>数字范围: 0 ~ 4294967295 (2^32 - 1)</p>
            <p>支持前缀: 0x(十六进制) 0b(二进制)</p>
            <p style="color: #999; margin-top: 6px;">点击复制按钮可复制对应值</p>
          </div>
        </el-popover>
      </div>

      <!-- 快捷预设 -->
      <div class="presets">
        <el-button size="small" @click="quickFill('192.168.1.1', 'ip')">192.168.1.1</el-button>
        <el-button size="small" @click="quickFill('10.0.0.1', 'ip')">10.0.0.1</el-button>
        <el-button size="small" @click="quickFill('255.255.255.0', 'ip')">255.255.255.0</el-button>
        <el-button size="small" @click="quickFill('0.0.0.0', 'ip')">0.0.0.0</el-button>
        <el-button size="small" @click="quickFill('255.255.255.255', 'ip')">255.255.255.255</el-button>
      </div>
    </div>

    <!-- 输入位列表（每行可输入） -->
    <div class="results-section">
      <div class="results-header">
        <span>在各行输入，其他行自动联动</span>
      </div>
      <div class="results-list">
        <div
          v-for="opt in fields"
          :key="opt.key"
          class="result-item"
          :class="{ active: opt.key === activeField }"
        >
          <div class="result-info">
            <span class="result-label">{{ opt.label }}</span>
          </div>
          <el-input
            v-model="values[opt.key]"
            :placeholder="opt.key === activeField ? opt.placeholder : '—'"
            class="value-input"
            size="default"
            @input="onInput(opt.key)"
            @focus="activeField = opt.key"
          />
          <el-button
            size="small"
            text
            :icon="CopyDocument"
            class="copy-btn"
            @click="copyValue(values[opt.key], opt.key)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ip-converter-tool {
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
  min-width: 100px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.result-label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.value-input {
  flex: 1;
}

.value-input :deep(.el-input__inner) {
  text-align: right;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 14px;
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
