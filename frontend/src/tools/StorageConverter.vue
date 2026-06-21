<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: { type: String, required: true }
})

// 换算基数：1024（二进制）或 1000（十进制）
const base = ref(1024)
// 是否显示千分位逗号
const withComma = ref(false)

// 单位定义（从小到大）
const units = [
  { symbol: 'b', name: '比特', index: 0 },
  { symbol: 'B', name: '字节', index: 1 },
  { symbol: 'KB', name: '千字节', index: 2 },
  { symbol: 'MB', name: '兆字节', index: 3 },
  { symbol: 'GB', name: '千兆字节', index: 4 },
  { symbol: 'TB', name: '太字节', index: 5 },
  { symbol: 'PB', name: '拍字节', index: 6 },
  { symbol: 'EB', name: '艾字节', index: 7 },
  { symbol: 'ZB', name: '泽字节', index: 8 },
  { symbol: 'YB', name: '尧字节', index: 9 }
]

// 每个单位的输入值（字符串，支持双向编辑）
const values = reactive({
  b: '', B: '1', KB: '', MB: '', GB: '', TB: '', PB: '', EB: '', ZB: '', YB: ''
})

// 当前正在编辑的单位
const activeUnit = ref('B')

// 状态持久化
useToolState(props.instanceId, { values, base, activeUnit, withComma })

// 获取单位的换算因子（转成 bits）
const getFactor = (unitIndex) => {
  if (unitIndex === 0) return 1           // bit
  if (unitIndex === 1) return 8           // Byte
  return 8 * Math.pow(base.value, unitIndex - 1)
}

// 格式化数值
const formatValue = (value) => {
  if (value === 0) return '0'
  if (Math.abs(value) < 0.000001) return value.toExponential(4)
  if (Math.abs(value) < 1) return value.toFixed(8).replace(/\.?0+$/, '')
  if (Math.abs(value) < 1024) return value.toFixed(4).replace(/\.?0+$/, '')
  if (Math.abs(value) < 1e15) {
    // 大数值：根据 withComma 选项决定是否带千分位逗号
    return withComma.value
      ? value.toLocaleString('en-US', { maximumFractionDigits: 2 })
      : value.toFixed(2).replace(/\.?0+$/, '')
  }
  return value.toExponential(4)
}

// 当某个输入框变化时，更新所有其他输入框
const onInput = (unitSymbol) => {
  activeUnit.value = unitSymbol
  const raw = values[unitSymbol]
  const num = parseFloat(raw)
  if (raw === '' || isNaN(num)) {
    // 输入为空或无效，清空所有
    units.forEach(u => {
      if (u.symbol !== unitSymbol) values[u.symbol] = ''
    })
    return
  }

  // 计算输入值的 bits 数
  const unitIndex = units.findIndex(u => u.symbol === unitSymbol)
  const bits = num * getFactor(unitIndex)

  // 更新所有其他单位
  units.forEach(u => {
    if (u.symbol === unitSymbol) return
    const factor = getFactor(u.index)
    const converted = bits / factor
    values[u.symbol] = formatValue(converted)
  })
}

// 初始化：触发一次 B 的计算
onInput('B')

// 复制结果
const copyResult = async (text, symbol) => {
  if (!text) { ElMessage.warning('没有可复制的内容'); return }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`已复制: ${text} ${symbol}`)
  } catch {
    ElMessage.error('复制失败')
  }
}

// 快捷预设
const presets = [
  { label: '1 KB', value: '1', unit: 'KB' },
  { label: '1 MB', value: '1', unit: 'MB' },
  { label: '1 GB', value: '1', unit: 'GB' },
  { label: '1 TB', value: '1', unit: 'TB' },
  { label: '500 GB', value: '500', unit: 'GB' },
  { label: '2 TB', value: '2', unit: 'TB' }
]

const applyPreset = (preset) => {
  // 清空所有
  units.forEach(u => { values[u.symbol] = '' })
  values[preset.unit] = preset.value
  onInput(preset.unit)
}

// 切换基数时重新计算
const onBaseChange = () => {
  onInput(activeUnit.value)
}

// 切换千分位逗号时重新计算
const onCommaChange = () => {
  onInput(activeUnit.value)
}
</script>

<template>
  <div class="storage-converter">
    <!-- 顶部控制区 -->
    <div class="control-section">
      <div class="base-toggle">
        <span class="toggle-label">换算基数：</span>
        <el-radio-group v-model="base" size="small" @change="onBaseChange">
          <el-radio-button :value="1024">1024 (二进制)</el-radio-button>
          <el-radio-button :value="1000">1000 (十进制)</el-radio-button>
        </el-radio-group>
        <el-checkbox v-model="withComma" size="small" @change="onCommaChange">千分位逗号</el-checkbox>
        <el-popover trigger="hover" placement="bottom" :width="280">
          <template #reference>
            <el-icon class="tips-icon"><QuestionFilled /></el-icon>
          </template>
          <div style="font-size: 13px; line-height: 1.8;">
            <p><b>换算关系：</b></p>
            <p>1 B = 8 b（字节 = 8 比特）</p>
            <p>1 KB = {{ base }} B</p>
            <p>1 MB = {{ base }} KB</p>
            <p>1 GB = {{ base }} MB ...</p>
            <p style="margin-top: 8px; color: #909399;">在任意行输入数值，其他行自动联动更新</p>
          </div>
        </el-popover>
      </div>

      <!-- 快捷预设 -->
      <div class="presets">
        <span class="preset-label">快捷：</span>
        <el-button
          v-for="preset in presets"
          :key="preset.label"
          size="small"
          text
          @click="applyPreset(preset)"
        >
          {{ preset.label }}
        </el-button>
      </div>
    </div>

    <!-- 换算列表（每行可输入） -->
    <div class="results-section">
      <div class="results-header">
        <span>在各行输入数值，其他行自动联动</span>
      </div>
      <div class="results-list">
        <div
          v-for="unit in units"
          :key="unit.symbol"
          class="result-item"
          :class="{ active: unit.symbol === activeUnit }"
        >
          <div class="result-info">
            <span class="result-symbol">{{ unit.symbol }}</span>
            <span class="result-name">{{ unit.name }}</span>
          </div>
          <el-input
            v-model="values[unit.symbol]"
            :placeholder="unit.symbol === activeUnit ? '输入数值...' : '—'"
            class="value-input"
            size="small"
            @input="onInput(unit.symbol)"
            @focus="activeUnit = unit.symbol"
          />
          <el-button
            size="small"
            text
            :icon="CopyDocument"
            class="copy-btn"
            @click="copyResult(values[unit.symbol], unit.symbol)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.storage-converter {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.control-section {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.base-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.toggle-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.tips-icon {
  font-size: 16px;
  color: #c0c4cc;
  cursor: pointer;
}

.tips-icon:hover {
  color: #409eff;
}

.presets {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.preset-label {
  font-size: 13px;
  color: #909399;
  margin-right: 4px;
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
}

.result-item {
  display: flex;
  align-items: center;
  padding: 6px 14px;
  border-bottom: 1px solid #f0f0f0;
  gap: 8px;
  transition: background 0.2s;
}

.result-item:hover {
  background: #ecf5ff;
}

.result-item.active {
  background: #e6f7ff;
  border-left: 3px solid #409eff;
  padding-left: 11px;
}

.result-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 110px;
  flex-shrink: 0;
}

.result-symbol {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
  min-width: 28px;
}

.result-name {
  font-size: 12px;
  color: #909399;
}

.value-input {
  flex: 1;
}

.value-input :deep(.el-input__inner) {
  text-align: right;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
}

.copy-btn {
  flex-shrink: 0;
  color: #c0c4cc;
}

.copy-btn:hover {
  color: #409eff;
}
</style>
