<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: {
    type: String,
    required: true
  }
})

// 输入值和单位
const inputValue = ref('1')
const fromUnit = ref('MB')
// 换算基数：1024（二进制）或 1000（十进制）
const base = ref(1024)

// 状态持久化
useToolState(props.instanceId, { inputValue, fromUnit, base })

// 单位定义（从小到大）
const units = [
  { symbol: 'b', name: '比特', factor: 1 },           // bit
  { symbol: 'B', name: '字节', factor: 8 },            // Byte = 8 bits
  { symbol: 'KB', name: '千字节', factor: 8 * 1024 },
  { symbol: 'MB', name: '兆字节', factor: 8 * 1024 ** 2 },
  { symbol: 'GB', name: '千兆字节', factor: 8 * 1024 ** 3 },
  { symbol: 'TB', name: '太字节', factor: 8 * 1024 ** 4 },
  { symbol: 'PB', name: '拍字节', factor: 8 * 1024 ** 5 },
  { symbol: 'EB', name: '艾字节', factor: 8 * 1024 ** 6 },
  { symbol: 'ZB', name: '泽字节', factor: 8 * 1024 ** 7 },
  { symbol: 'YB', name: '尧字节', factor: 8 * 1024 ** 8 }
]

// 根据 base 重新计算 factor
const getFactor = (index) => {
  if (index === 0) return 1           // bit
  if (index === 1) return 8           // Byte
  // KB 及以上：8 * base^(index-1)
  return 8 * Math.pow(base.value, index - 1)
}

// 将输入值转换为 bits（基准单位）
const bitsValue = computed(() => {
  const num = parseFloat(inputValue.value)
  if (isNaN(num)) return 0
  const unitIndex = units.findIndex(u => u.symbol === fromUnit.value)
  if (unitIndex === -1) return 0
  return num * getFactor(unitIndex)
})

// 所有单位的换算结果
const results = computed(() => {
  return units.map((unit, index) => {
    const factor = getFactor(index)
    const value = bitsValue.value / factor
    return {
      ...unit,
      value: formatValue(value)
    }
  })
})

// 格式化数值：大数用科学计数法，小数保留适当位数
const formatValue = (value) => {
  if (value === 0) return '0'
  if (Math.abs(value) < 0.000001) return value.toExponential(4)
  if (Math.abs(value) < 1) return value.toFixed(8).replace(/\.?0+$/, '')
  if (Math.abs(value) < 1024) return value.toFixed(4).replace(/\.?0+$/, '')
  if (Math.abs(value) < 1e15) return value.toLocaleString('en-US', { maximumFractionDigits: 2 })
  return value.toExponential(4)
}

// 复制结果
const copyResult = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制: ' + text)
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
  inputValue.value = preset.value
  fromUnit.value = preset.unit
}
</script>

<template>
  <div class="storage-converter">
    <!-- 输入区 -->
    <div class="input-section">
      <div class="input-row">
        <el-input
          v-model="inputValue"
          placeholder="输入数值"
          class="value-input"
          type="number"
        />
        <el-select v-model="fromUnit" class="unit-select" placeholder="单位">
          <el-option
            v-for="unit in units"
            :key="unit.symbol"
            :label="`${unit.symbol} (${unit.name})`"
            :value="unit.symbol"
          />
        </el-select>
      </div>

      <!-- 换算基数切换 -->
      <div class="base-toggle">
        <span class="toggle-label">换算基数：</span>
        <el-radio-group v-model="base" size="small">
          <el-radio-button :value="1024">1024 (二进制)</el-radio-button>
          <el-radio-button :value="1000">1000 (十进制)</el-radio-button>
        </el-radio-group>
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

    <!-- 换算结果 -->
    <div class="results-section">
      <div class="results-header">换算结果</div>
      <div class="results-list">
        <div
          v-for="item in results"
          :key="item.symbol"
          class="result-item"
          :class="{ active: item.symbol === fromUnit }"
          @click="copyResult(`${item.value} ${item.symbol}`)"
        >
          <div class="result-info">
            <span class="result-symbol">{{ item.symbol }}</span>
            <span class="result-name">{{ item.name }}</span>
          </div>
          <div class="result-value">{{ item.value }}</div>
          <el-icon class="copy-icon"><CopyDocument /></el-icon>
        </div>
      </div>
    </div>

    <!-- 说明 -->
    <div class="tips">
      <el-popover trigger="hover" placement="top" :width="300">
        <template #reference>
          <el-icon class="tips-icon"><QuestionFilled /></el-icon>
        </template>
        <div style="font-size: 13px; line-height: 1.8;">
          <p><b>换算关系：</b></p>
          <p>1 B = 8 b（字节 = 8 比特）</p>
          <p>1 KB = {{ base }} B</p>
          <p>1 MB = {{ base }} KB</p>
          <p>1 GB = {{ base }} MB ...</p>
          <p style="margin-top: 8px; color: #909399;">点击任意结果行可复制</p>
        </div>
      </el-popover>
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

.input-section {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.input-row {
  display: flex;
  gap: 8px;
}

.value-input {
  flex: 1;
}

.unit-select {
  width: 160px;
  flex-shrink: 0;
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
  font-size: 13px;
  font-weight: 600;
  color: #303133;
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
  padding: 8px 14px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
  gap: 8px;
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
  min-width: 120px;
}

.result-symbol {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
  min-width: 32px;
}

.result-name {
  font-size: 12px;
  color: #909399;
}

.result-value {
  flex: 1;
  text-align: right;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
  word-break: break-all;
}

.copy-icon {
  color: #c0c4cc;
  font-size: 14px;
  flex-shrink: 0;
}

.result-item:hover .copy-icon {
  color: #409eff;
}

/* 提示 */
.tips {
  position: absolute;
  top: 12px;
  right: 12px;
}

.tips-icon {
  font-size: 18px;
  color: #c0c4cc;
  cursor: pointer;
}

.tips-icon:hover {
  color: #409eff;
}
</style>
