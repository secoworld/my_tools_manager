<script setup>
import { ref, computed, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload, Delete, Edit, View, Document, Loading } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: { type: String, required: true }
})

// 延迟导入 md-editor-v3，避免首次加载过大
const MdEditor = shallowRef(null)
const MdPreview = shallowRef(null)
const editorLoaded = ref(false)

async function loadEditor() {
  if (editorLoaded.value) return
  const mod = await import('md-editor-v3')
  await import('md-editor-v3/lib/style.css')
  MdEditor.value = mod.MdEditor
  MdPreview.value = mod.MdPreview
  editorLoaded.value = true
}

const title = ref('无标题文档')
const content = ref(`# Markdown 编辑器

支持 **数学公式**、*流程图*、表格等。

## 数学公式

行内公式：$E = mc^2$

块级公式：

$$
\\int_0^\\infty e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}
$$

## 流程图（Mermaid）

\`\`\`mermaid
graph TD
    A[开始] --> B{条件判断}
    B -->|是| C[执行操作]
    B -->|否| D[跳过]
    C --> E[结束]
    D --> E
\`\`\`

## 表格

| 功能 | 支持 |
|------|------|
| 数学公式 | ✅ |
| 流程图 | ✅ |
| 表格 | ✅ |
`)
const mode = ref('live') // 'edit' | 'preview' | 'live'

useToolState(props.instanceId, { title, content, mode })

const charCount = computed(() => content.value.length)

const downloadMd = () => {
  if (!content.value.trim()) { ElMessage.warning('没有内容可下载'); return }
  const blob = new Blob([content.value], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${title.value || '文档'}.md`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已下载')
}

const fileInput = ref(null)
const triggerUpload = () => { fileInput.value?.click() }
const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (event) => {
    content.value = event.target.result
    title.value = file.name.replace(/\.md$/i, '')
    ElMessage.success(`已加载: ${file.name}`)
  }
  reader.onerror = () => ElMessage.error('文件读取失败')
  reader.readAsText(file, 'UTF-8')
  e.target.value = ''
}

const clearAll = () => { content.value = ''; ElMessage.info('已清空') }

// 切换模式时确保编辑器已加载
async function switchMode(newMode) {
  await loadEditor()
  mode.value = newMode
}

// 组件挂载时预加载编辑器
loadEditor()
</script>

<template>
  <div class="md-editor-tool">
    <!-- 标题栏 -->
    <div class="editor-header">
      <input v-model="title" class="title-input" placeholder="输入文档标题..." />
      <div class="editor-actions">
        <el-button-group>
          <el-button size="small" :type="mode === 'edit' ? 'primary' : ''" :icon="Edit" @click="switchMode('edit')">编辑</el-button>
          <el-button size="small" :type="mode === 'live' ? 'primary' : ''" :icon="Document" @click="switchMode('live')">实时预览</el-button>
          <el-button size="small" :type="mode === 'preview' ? 'primary' : ''" :icon="View" @click="switchMode('preview')">预览</el-button>
        </el-button-group>
        <el-button size="small" :icon="Download" @click="downloadMd">导出</el-button>
        <el-button size="small" :icon="Upload" @click="triggerUpload">导入</el-button>
        <el-button size="small" type="danger" plain :icon="Delete" @click="clearAll">清空</el-button>
        <input ref="fileInput" type="file" accept=".md,.markdown,.txt,text/markdown,text/plain" style="display:none" @change="handleFileChange" />
      </div>
    </div>

    <!-- 编辑器区域 -->
    <div class="editor-body">
      <div v-if="!editorLoaded" class="loading-placeholder">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <span>加载编辑器中...</span>
      </div>
      <template v-else>
        <!-- 仅编辑模式 -->
        <MdEditor
          v-if="mode === 'edit'"
          v-model="content"
          :preview="false"
          :toolbarsExclude="['github', 'save', 'pageFullscreen', 'fullscreen', 'htmlPreview', 'catalog']"
          placeholder="在此输入 Markdown 内容..."
          style="height: 100%;"
        />
        <!-- 实时预览模式 -->
        <MdEditor
          v-else-if="mode === 'live'"
          v-model="content"
          :preview="true"
          :toolbarsExclude="['github', 'save', 'pageFullscreen', 'fullscreen', 'htmlPreview', 'catalog']"
          placeholder="在此输入 Markdown 内容..."
          style="height: 100%;"
        />
        <!-- 仅预览模式 -->
        <div v-else class="preview-only">
          <MdPreview :modelValue="content" />
        </div>
      </template>
    </div>

    <!-- 状态栏 -->
    <div class="editor-status">
      <span>字符: {{ charCount }}</span>
      <span>模式: {{ mode === 'edit' ? '仅编辑' : mode === 'live' ? '实时预览' : '仅预览' }}</span>
      <span class="auto-save-hint">自动保存</span>
    </div>
  </div>
</template>

<style scoped>
.md-editor-tool {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
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

.title-input:focus { border-bottom-color: #409eff; }

.editor-actions { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }

.editor-body {
  flex: 1;
  min-height: 0;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.loading-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: #909399;
}

.preview-only {
  height: 100%;
  overflow-y: auto;
  padding: 20px;
}

.editor-status {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.auto-save-hint { margin-left: auto; color: #67c23a; }
</style>
