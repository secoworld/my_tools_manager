<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { QuestionFilled, CopyDocument, Clock } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: { type: String, required: true }
})

// 三个输入位的值
const values = reactive({
  date: '',
  seconds: '',
  millis: ''
})

// 当前正在编辑的字段
const activeField = ref('date')

// 状态持久化
useToolState(props.instanceId, { values, activeField })

// 日期格式化为 YYYY-MM-DD HH:mm:ss.SSS
const formatDate = (date) => {
  const pad = (n, len = 2) => String(n).padStart(len, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.${pad(date.getMilliseconds(), 3)}`
}

// 解析日期字符串
const parseDate = (str) => {
  const trimmed = str.trim()
  if (!trimmed) return null
  // 支持 YYYY-MM-DD 或 YYYY-MM-DD HH:mm:ss
  const date = new Date(trimmed.replace(/-/g, '/'))
  if (isNaN(date.getTime())) return null
  return date
}

// 当某个输入框变化时，更新其他输入框
const onInput = (field) => {
  activeField.value = field
  const raw = values[field].trim()
  if (!raw) {
    // 输入为空，清空其他
    Object.keys(values).forEach(k => {
      if (k !== field) values[k] = ''
    })
    return
  }

  if (field === 'date') {
    // 日期 → 秒 + 毫秒
    const date = parseDate(raw)
    if (!date) return
    const ms = date.getTime()
    values.seconds = String(Math.floor(ms / 1000))
    values.millis = String(ms)
  } else if (field === 'seconds') {
    // 秒 → 日期 + 毫秒
    const sec = parseInt(raw)
    if (isNaN(sec)) return
    const ms = sec * 1000
    values.date = formatDate(new Date(ms))
    values.millis = String(ms)
  } else if (field === 'millis') {
    // 毫秒 → 日期 + 秒
    const ms = parseInt(raw)
    if (isNaN(ms)) return
    values.date = formatDate(new Date(ms))
    values.seconds = String(Math.floor(ms / 1000))
  }
}

// 初始化为当前时间
const fillNow = () => {
  const now = new Date()
  values.date = formatDate(now)
  values.seconds = String(Math.floor(now.getTime() / 1000))
  values.millis = String(now.getTime())
  activeField.value = 'date'
}

// 如果所有字段都为空，自动填充当前时间
if (!values.date && !values.seconds && !values.millis) {
  fillNow()
} else {
  // 恢复后触发一次联动
  onInput(activeField.value)
}

// 清空
const clearAll = () => {
  values.date = ''
  values.seconds = ''
  values.millis = ''
}

// 复制
const copyResult = async (text, label) => {
  if (!text) { ElMessage.warning('没有可复制的内容'); return }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`已复制${label}: ${text}`)
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>

<template>
  <div class="timestamp-converter">
    <!-- 顶部控制区 -->
    <div class="control-section">
      <el-button type="primary" :icon="Clock" size="small" @click="fillNow">当前时间</el-button>
      <el-button type="info" size="small" @click="clearAll">清空</el-button>
      <el-popover
        placement="bottom"
        :width="300"
        trigger="hover"
      >
        <template #reference>
          <el-icon class="tips-icon"><QuestionFilled /></el-icon>
        </template>
        <div style="font-size: 13px; line-height: 1.8;">
          <p style="font-weight: bold;">使用说明</p>
          <p>在任意行输入数值，其他行自动联动更新：</p>
          <p>• <b>日期</b>：格式如 2024-01-01 12:00:00.000 或 2024-01-01</p>
          <p>• <b>秒(s)</b>：10位数字，如 1704067200</p>
          <p>• <b>毫秒(ms)</b>：13位数字，如 1704067200000</p>
          <p style="color: #999; margin-top: 6px;">点击"当前时间"可快速填入此刻</p>
        </div>
      </el-popover>
    </div>

    <!-- 联动输入列表 -->
    <div class="results-section">
      <div class="results-header">
        <span>在任意行输入，其他行自动联动更新</span>
      </div>
      <div class="results-list">
        <!-- 日期 -->
        <div class="result-item" :class="{ active: activeField === 'date' }">
          <div class="result-info">
            <span class="result-label">日期</span>
            <span class="result-hint">YYYY-MM-DD HH:mm:ss.SSS</span>
          </div>
          <el-input
            v-model="values.date"
            placeholder="输入日期..."
            class="value-input"
            @input="onInput('date')"
            @focus="activeField = 'date'"
          />
          <el-button
            size="small"
            text
            :icon="CopyDocument"
            class="copy-btn"
            @click="copyResult(values.date, '日期')"
          />
        </div>

        <!-- 秒 -->
        <div class="result-item" :class="{ active: activeField === 'seconds' }">
          <div class="result-info">
            <span class="result-label">秒 (s)</span>
            <span class="result-hint">10位时间戳</span>
          </div>
          <el-input
            v-model="values.seconds"
            placeholder="输入秒级时间戳..."
            class="value-input"
            @input="onInput('seconds')"
            @focus="activeField = 'seconds'"
          />
          <el-button
            size="small"
            text
            :icon="CopyDocument"
            class="copy-btn"
            @click="copyResult(values.seconds, '秒')"
          />
        </div>

        <!-- 毫秒 -->
        <div class="result-item" :class="{ active: activeField === 'millis' }">
          <div class="result-info">
            <span class="result-label">毫秒 (ms)</span>
            <span class="result-hint">13位时间戳</span>
          </div>
          <el-input
            v-model="values.millis"
            placeholder="输入毫秒级时间戳..."
            class="value-input"
            @input="onInput('millis')"
            @focus="activeField = 'millis'"
          />
          <el-button
            size="small"
            text
            :icon="CopyDocument"
            class="copy-btn"
            @click="copyResult(values.millis, '毫秒')"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timestamp-converter {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.control-section {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
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
  gap: 8px;
  padding: 12px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
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
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.result-hint {
  font-size: 11px;
  color: #909399;
}

.value-input {
  flex: 1;
}

.value-input :deep(.el-input__inner) {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  font-weight: 500;
  color: #409eff;
}

.copy-btn {
  flex-shrink: 0;
  color: #c0c4cc;
}

.copy-btn:hover {
  color: #409eff;
}
</style>
