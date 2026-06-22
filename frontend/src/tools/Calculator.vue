<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Delete, QuestionFilled, Promotion } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: {
    type: String,
    required: true
  }
})

// 输入算式
const expression = ref('')
// 计算结果
const result = ref('')
// 历史记录
const history = ref([])

// 状态持久化
useToolState(props.instanceId, { expression, result, history })

// 支持的运算符说明
const operators = [
  { op: '+', desc: '加法' },
  { op: '-', desc: '减法' },
  { op: '*', desc: '乘法' },
  { op: '/', desc: '除法' },
  { op: '%', desc: '取模' },
  { op: '^', desc: '幂运算' },
  { op: '<<', desc: '左移' },
  { op: '>>', desc: '右移' },
  { op: '&', desc: '按位与' },
  { op: '|', desc: '按位或' },
  { op: '~', desc: '按位非（一元）' }
]

// 安全计算函数
const calculate = () => {
  const expr = expression.value.trim()
  if (!expr) {
    ElMessage.warning('请输入算式')
    return
  }

  try {
    const sanitized = expr.replace(/\s+/g, '')
    const tempExpr = sanitized.replace(/<<</g, '\x01').replace(/>>/g, '\x02')
    const validPattern = /^[0-9+\-*/%^().&|\x01\x02~]+$/
    if (!validPattern.test(tempExpr)) {
      throw new Error('包含不支持的字符')
    }

    let jsExpr = sanitized.replace(/\^/g, '**')
    const value = Function('"use strict"; return (' + jsExpr + ')')()

    if (value === undefined || value === null || Number.isNaN(value)) {
      throw new Error('计算结果无效')
    }

    let formatted
    if (Number.isInteger(value)) {
      formatted = value.toString()
    } else {
      formatted = parseFloat(value.toFixed(10)).toString()
    }

    result.value = formatted

    history.value.unshift({
      expression: expr,
      result: formatted,
      time: new Date().toLocaleTimeString()
    })
    if (history.value.length > 20) {
      history.value = history.value.slice(0, 20)
    }
  } catch (e) {
    result.value = '错误: ' + e.message
    ElMessage.error('算式有误: ' + e.message)
  }
}

const clearAll = () => {
  expression.value = ''
  result.value = ''
}

const clearHistory = () => {
  history.value = []
}

const copyResult = () => {
  if (!result.value) return
  navigator.clipboard.writeText(result.value).then(() => {
    ElMessage.success('已复制结果')
  })
}

const useHistory = (item) => {
  expression.value = item.expression
  result.value = item.result
}

const appendOp = (op) => {
  expression.value += op
}

// 键盘回车计算（Shift+Enter 换行，Enter 计算）
const onKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    calculate()
  }
}
</script>

<template>
  <div class="calculator-tool">
    <!-- 输入区 -->
    <div class="input-section">
      <div class="input-header">
        <span class="label">输入算式</span>
        <div class="header-actions">
          <el-popover placement="bottom" :width="280" trigger="hover">
            <template #reference>
              <el-button size="small" text :icon="QuestionFilled">运算符</el-button>
            </template>
            <div class="help-content">
              <p style="font-weight: bold; margin-bottom: 8px;">支持的运算符</p>
              <div v-for="item in operators" :key="item.op" class="op-item">
                <code>{{ item.op }}</code> — {{ item.desc }}
              </div>
              <p style="margin-top: 8px; color: #999; font-size: 12px;">
                支持括号 () 改变优先级<br>
                回车键快速计算，Shift+Enter 换行
              </p>
            </div>
          </el-popover>
          <el-button size="small" text :icon="Delete" @click="clearAll">清空</el-button>
        </div>
      </div>

      <!-- 输入框 + 计算按钮 -->
      <div class="input-wrapper">
        <el-input
          v-model="expression"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 5 }"
          placeholder="例如: 2 + 3 * 4 或 1 << 8"
          @keydown="onKeydown"
          class="expr-input"
        />
        <el-button
          type="primary"
          :icon="Promotion"
          class="calc-btn"
          @click="calculate"
        >计算</el-button>
      </div>

      <!-- 快捷运算符 -->
      <div class="quick-ops">
        <el-button v-for="op in ['+', '-', '*', '/', '%', '^', '<<', '>>', '&', '|', '(', ')']"
          :key="op"
          size="small"
          @click="appendOp(op)"
        >{{ op }}</el-button>
      </div>
    </div>

    <!-- 结果区 -->
    <div class="result-section">
      <div class="result-bar">
        <span class="label">结果</span>
        <el-button v-if="result" size="small" text :icon="CopyDocument" @click="copyResult">复制</el-button>
      </div>
      <div class="result-display" :class="{ error: result.startsWith('错误') }">
        {{ result || '—' }}
      </div>
    </div>

    <!-- 历史记录 -->
    <div class="history-area" v-if="history.length > 0">
      <div class="history-header">
        <span class="label">历史记录</span>
        <el-button size="small" text @click="clearHistory">清空</el-button>
      </div>
      <div class="history-list">
        <div
          v-for="(item, idx) in history"
          :key="idx"
          class="history-item"
          @click="useHistory(item)"
        >
          <span class="history-expr">{{ item.expression }}</span>
          <span class="history-eq">=</span>
          <span class="history-result">{{ item.result }}</span>
          <span class="history-time">{{ item.time }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.calculator-tool {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 8px;
}

.label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

/* ========== 输入区 ========== */
.input-section {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  gap: 4px;
}

/* 输入框 + 计算按钮容器 */
.input-wrapper {
  position: relative;
}

.expr-input :deep(.el-textarea__inner) {
  font-family: 'Consolas', 'Courier New', monospace;
  padding-right: 80px; /* 右侧留出按钮空间 */
}

.calc-btn {
  position: absolute;
  right: 8px;
  bottom: 8px;
  z-index: 1;
}

.quick-ops {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.quick-ops .el-button {
  margin: 0;
  min-width: 36px;
  font-family: 'Consolas', monospace;
}

/* ========== 结果区 ========== */
.result-section {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-display {
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  word-break: break-all;
  min-height: 36px;
  display: flex;
  align-items: center;
}

.result-display.error {
  color: #f56c6c;
  font-size: 13px;
  font-weight: normal;
}

/* ========== 历史记录 ========== */
.history-area {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-list {
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  flex: 1;
  min-height: 0;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
  transition: background 0.2s;
}

.history-item:hover {
  background: #f5f7fa;
}

.history-item:last-child {
  border-bottom: none;
}

.history-expr {
  font-family: 'Consolas', monospace;
  color: #606266;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-eq {
  color: #909399;
}

.history-result {
  font-family: 'Consolas', monospace;
  font-weight: bold;
  color: #409eff;
}

.history-time {
  color: #c0c4cc;
  font-size: 11px;
  margin-left: auto;
}

.help-content .op-item {
  font-size: 13px;
  line-height: 1.8;
}

.help-content code {
  background: #f0f0f0;
  padding: 1px 6px;
  border-radius: 3px;
  font-family: 'Consolas', monospace;
}
</style>
