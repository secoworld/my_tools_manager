<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import { executeTool } from '../api'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: {
    type: String,
    required: true
  }
})

const inputValue = ref('')
const action = ref('date-to-timestamp') // date-to-timestamp | timestamp-to-date
const loading = ref(false)

// 结果
const resultSeconds = ref('')
const resultMillis = ref('')
const resultDate = ref('')

// 状态持久化（刷新页面后输入内容和结果不丢失）
useToolState(props.instanceId, { inputValue, action, resultSeconds, resultMillis, resultDate })

// 转换
const convert = async () => {
  if (!inputValue.value.trim()) {
    ElMessage.warning('请输入要转换的值')
    return
  }
  loading.value = true
  try {
    const result = await executeTool('timestamp-converter', {
      action: action.value,
      value: inputValue.value
    })
    // result 是后端返回的 ToolResult 对象 { success, data, message }
    const data = result.data
    if (action.value === 'date-to-timestamp') {
      resultSeconds.value = String(data.seconds)
      resultMillis.value = String(data.milliseconds)
      resultDate.value = ''
    } else {
      resultDate.value = typeof data === 'string' ? data : String(data)
      resultSeconds.value = ''
      resultMillis.value = ''
    }
    ElMessage.success('转换成功')
  } catch (error) {
    ElMessage.error('转换失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 清空
const clearAll = () => {
  inputValue.value = ''
  resultSeconds.value = ''
  resultMillis.value = ''
  resultDate.value = ''
}

// 复制结果
const copyResult = async (text) => {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}
</script>

<template>
  <div class="timestamp-converter">
    <div class="toolbar">
      <el-radio-group v-model="action">
        <el-radio value="date-to-timestamp">日期转时间戳</el-radio>
        <el-radio value="timestamp-to-date">时间戳转日期</el-radio>
      </el-radio-group>
      <el-button type="primary" :loading="loading" @click="convert">转换</el-button>
      <el-button type="info" @click="clearAll">清空</el-button>
      <el-popover
        placement="bottom"
        title="使用提示"
        :width="280"
        trigger="hover"
        :content="'日期转时间戳：输入格式如 2024-01-01 12:00:00 或 2024-01-01，将同时显示秒级和毫秒级时间戳。时间戳转日期：输入秒级(10位)或毫秒级(13位)时间戳，自动识别。'"
      >
        <template #reference>
          <el-button class="tips-btn" circle size="small">
            <el-icon><QuestionFilled /></el-icon>
          </el-button>
        </template>
      </el-popover>
    </div>
    <div class="content">
      <div class="input-area">
        <div class="label">
          {{ action === 'date-to-timestamp' ? '输入日期 (如 2024-01-01 12:00:00)' : '输入时间戳 (如 1704067200 或 1704067200000)' }}
        </div>
        <el-input
          v-model="inputValue"
          placeholder="请输入要转换的值"
        />
      </div>
      <div class="output-area">
        <!-- 日期转时间戳：同时展示秒和毫秒 -->
        <template v-if="action === 'date-to-timestamp'">
          <div class="label">输出结果</div>
          <div class="result-row">
            <div class="result-label">秒 (s)</div>
            <el-input :model-value="resultSeconds" readonly placeholder="秒级时间戳">
              <template #append>
                <el-button @click="copyResult(resultSeconds)">复制</el-button>
              </template>
            </el-input>
          </div>
          <div class="result-row">
            <div class="result-label">毫秒 (ms)</div>
            <el-input :model-value="resultMillis" readonly placeholder="毫秒级时间戳">
              <template #append>
                <el-button @click="copyResult(resultMillis)">复制</el-button>
              </template>
            </el-input>
          </div>
        </template>
        <!-- 时间戳转日期 -->
        <template v-else>
          <div class="label">输出结果</div>
          <el-input :model-value="resultDate" readonly placeholder="转换结果将显示在这里">
            <template #append>
              <el-button @click="copyResult(resultDate)">复制</el-button>
            </template>
          </el-input>
        </template>
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
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.content {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  min-height: 0;
  margin-bottom: 16px;
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

.result-row {
  margin-bottom: 12px;
}

.result-label {
  margin-bottom: 6px;
  font-size: 13px;
  color: #909399;
}

.tips-btn {
  margin-left: auto;
}
</style>
