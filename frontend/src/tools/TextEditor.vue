<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload, Delete } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: {
    type: String,
    required: true
  }
})

const title = ref('无标题笔记')
const content = ref('')

// 状态持久化（刷新页面后内容不丢失）
useToolState(props.instanceId, { title, content })

// 字符统计
const charCount = computed(() => content.value.length)
const lineCount = computed(() => content.value.split('\n').length)

// 下载为 txt 文件
const downloadTxt = () => {
  if (!content.value.trim() && !title.value.trim()) {
    ElMessage.warning('没有内容可下载')
    return
  }
  const blob = new Blob([content.value], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${title.value || '笔记'}.txt`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已下载')
}

// 从 txt 文件加载
const fileInput = ref(null)
const triggerUpload = () => {
  fileInput.value?.click()
}
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (event) => {
    content.value = event.target.result
    // 用文件名作为标题（去掉 .txt 后缀）
    title.value = file.name.replace(/\.txt$/i, '')
    ElMessage.success(`已加载: ${file.name}`)
  }
  reader.onerror = () => ElMessage.error('文件读取失败')
  reader.readAsText(file, 'UTF-8')
  // 重置 input 以便重复选择同一文件
  e.target.value = ''
}

// 清空
const clearAll = () => {
  content.value = ''
  ElMessage.info('已清空')
}
</script>

<template>
  <div class="text-editor">
    <!-- 标题栏 -->
    <div class="editor-header">
      <input
        v-model="title"
        class="title-input"
        placeholder="输入笔记标题..."
      />
      <div class="editor-actions">
        <el-button size="small" :icon="Download" @click="downloadTxt">下载</el-button>
        <el-button size="small" :icon="Upload" @click="triggerUpload">导入</el-button>
        <el-button size="small" type="danger" plain :icon="Delete" @click="clearAll">清空</el-button>
        <input
          ref="fileInput"
          type="file"
          accept=".txt,text/plain"
          style="display:none"
          @change="handleFileChange"
        />
      </div>
    </div>

    <!-- 编辑区 -->
    <textarea
      v-model="content"
      class="editor-textarea"
      placeholder="在此记录你的内容..."
    ></textarea>

    <!-- 状态栏 -->
    <div class="editor-status">
      <span>字符: {{ charCount }}</span>
      <span>行数: {{ lineCount }}</span>
      <span class="auto-save-hint">📝 自动保存</span>
    </div>
  </div>
</template>

<style scoped>
.text-editor {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.title-input {
  flex: 1;
  min-width: 120px;
  border: none;
  border-bottom: 2px solid #e4e7ed;
  background: transparent;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding: 6px 4px;
  outline: none;
  transition: border-color 0.3s;
}

.title-input:focus {
  border-bottom-color: #409eff;
}

.editor-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.editor-textarea {
  flex: 1;
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 14px;
  font-family: 'Consolas', 'Monaco', 'Microsoft YaHei', monospace;
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  resize: none;
  outline: none;
  min-height: 0;
  transition: border-color 0.3s;
}

.editor-textarea:focus {
  border-color: #409eff;
}

.editor-status {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.auto-save-hint {
  margin-left: auto;
  color: #67c23a;
}
</style>
