<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Delete, Calendar } from '@element-plus/icons-vue'
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
    // 验证只包含允许的字符：数字、运算符、括号、空格、小数点
    // 允许: 0-9 + - * / % ^ ( ) . 空格 << >> & | ~
    const sanitized = expr.replace(/\s+/g, '')
    // 临时替换 << >> 为单字符占位符便于验证
    const tempExpr = sanitized.replace(/<<</g, '\x01').replace(/>>/g, '\x02')
    const validPattern = /^[0-9+\-*/%^().&|\x01\x02~]+$/
    if (!validPattern.test(tempExpr)) {
      throw new Error('包含不支持的字符')
    }

    // 将 ^ 替换为 ** （JS 幂运算）
    // 将 << 替换为 << （JS 原生支持）
    // 将 >> 替换为 >> （JS 原生支持）
    let jsExpr = sanitized.replace(/\^/g, '**')

    // 使用 Function 构造器安全求值（比 eval 稍安全）
    const value = Function('"use strict"; return (' + jsExpr + ')')()

    if (value === undefined || value === null || Number.isNaN(value)) {
      throw new Error('计算结果无效')
    }

    // 格式化结果
    let formatted
    if (Number.isInteger(value)) {
      formatted = value.toString()
    } else {
      // 浮点数保留 10 位小数，去掉末尾的 0
      formatted = parseFloat(value.toFixed(10)).toString()
    }

    result.value = formatted

    // 添加到历史记录
    history.value.unshift({
      expression: expr,
      result: formatted,
      time: new Date().toLocaleTimeString()
    })
    // 保留最近 20 条
    if (history.value.length > 20) {
      history.value = history.value.slice(0, 20)
    }
  } catch (e) {
    result.value = '错误: ' + e.message
    ElMessage.error('算式有误: ' + e.message)
  }
}

// 清空
const clearAll = () => {
  expression.value = ''
  result.value = ''
}

// 清空历史
const clearHistory = () => {
  history.value = []
}

// 复制结果
const copyResult = () => {
  if (!result.value) return
  navigator.clipboard.writeText(result.value).then(() => {
    ElMessage.success('已复制结果')
  })
}

// 点击历史记录填入
const useHistory = (item) => {
  expression.value = item.expression
  result.value = item.result
}

// 快捷输入
const appendOp = (op) => {
  expression.value += op
}

// 键盘回车计算
const onKeyup = (e) => {
  if (e.key === 'Enter') {
    calculate()
  }
}
</script>

<template>
  <div class="calculator-tool">
    <div class="toolbar">
      <el-button type="primary" size="small" @click="calculate">计算</el-button>
      <el-button size="small" :icon="Delete" @click="clearAll">清空</el-button>
      <el-popover placement="bottom" :width="280" trigger="hover">
        <template #reference>
          <el-button size="small" circle>
            <el-icon><QuestionFilled /></el-icon>
          </el-button>
        </template>
        <div class="help-content">
          <p style="font-weight: bold; margin-bottom: 8px;">支持的运算符</p>
          <div v-for="item in operators" :key="item.op" class="op-item">
            <code>{{ item.op }}</code> — {{ item.desc }}
          </div>
          <p style="margin-top: 8px; color: #999; font-size: 12px;">
            支持括号 () 改变优先级<br>
            回车键快速计算
          </p>
        </div>
      </el-popover>
    </div>

    <div class="content">
      <div class="input-area">
        <div class="label">输入算式</div>
        <el-input
          v-model="expression"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 6 }"
          placeholder="例如: 2 + 3 * 4 或 1 << 8 或 255 & 0x0F"
          @keyup="onKeyup"
          style="font-family: 'Consolas', monospace;"
        />
        <!-- 快捷运算符 -->
        <div class="quick-ops">
          <el-button v-for="op in ['+', '-', '*', '/', '%', '^', '<<', '>>', '&', '|', '(', ')']"
            :key="op"
            size="small"
            @click="appendOp(op)"
          >{{ op }}</el-button>
        </div>
      </div>

      <div class="output-area">
        <div class="label">
          结果
          <el-button v-if="result" size="small" text :icon="CopyDocument" @click="copyResult">复制</el-button>
        </div>
        <div class="result-display" :class="{ error: result.startsWith('错误') }">
          {{ result || '—' }}
        </div>
      </div>
    </div>

    <!-- 历史记录 -->
    <div class="history-area" v-if="history.length > 0">
      <div class="history-header">
        <span class="label">历史记录</span>
        <el-button size="small" text @click="clearHistory">清空历史</el-button>
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

.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}

.content {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 0;
}

.input-area,
.output-area {
  flex: 1 1 280px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.result-display {
  flex: 1;
  min-height: 60px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  word-break: break-all;
  display: flex;
  align-items: center;
  overflow: auto;
}

.result-display.error {
  color: #f56c6c;
  font-size: 14px;
  font-weight: normal;
}

.history-area {
  flex-shrink: 0;
  max-height: 200px;
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
