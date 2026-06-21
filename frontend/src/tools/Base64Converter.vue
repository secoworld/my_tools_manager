<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { executeTool } from '../api'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: {
    type: String,
    required: true
  }
})

const inputText = ref('')
const outputText = ref('')
const action = ref('encode') // encode | decode
const keepNonBase64 = ref(false) // 解码时是否保留非 Base64 内容
const loading = ref(false)

// 状态持久化（刷新页面后输入内容和选项不丢失）
useToolState(props.instanceId, { inputText, outputText, action, keepNonBase64 })

// 转换
const convert = async () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入要转换的内容')
    return
  }
  loading.value = true
  try {
    // 勾选"保留非Base64内容"时，使用 mixed-decode action
    const actualAction = (action.value === 'decode' && keepNonBase64.value) ? 'mixed-decode' : action.value
    const result = await executeTool('base64-converter', {
      action: actualAction,
      text: inputText.value
    })
    outputText.value = typeof result === 'string' ? result : result.data || result.result || ''
    ElMessage.success('转换成功')
  } catch (error) {
    ElMessage.error('转换失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 清空
const clearAll = () => {
  inputText.value = ''
  outputText.value = ''
}
</script>

<template>
  <div class="base64-converter">
    <div class="toolbar">
      <el-radio-group v-model="action">
        <el-radio value="encode">编码</el-radio>
        <el-radio value="decode">解码</el-radio>
      </el-radio-group>
      <el-checkbox
        v-if="action === 'decode'"
        v-model="keepNonBase64"
        label="保留非Base64内容"
      >
        保留非Base64内容
      </el-checkbox>
      <el-button type="primary" :loading="loading" @click="convert">转换</el-button>
      <el-button type="info" @click="clearAll">清空</el-button>
    </div>
    <div class="content">
      <div class="input-area">
        <div class="label">输入内容</div>
        <el-input
          v-model="inputText"
          type="textarea"
          placeholder="请输入要转换的内容"
        />
      </div>
      <div class="output-area">
        <div class="label">输出结果</div>
        <el-input
          v-model="outputText"
          type="textarea"
          readonly
          placeholder="转换结果将显示在这里"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.base64-converter {
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

.input-area :deep(.el-textarea__inner),
.output-area :deep(.el-textarea__inner) {
  height: 100%;
  resize: none;
  font-family: 'Consolas', 'Monaco', monospace;
}
</style>
