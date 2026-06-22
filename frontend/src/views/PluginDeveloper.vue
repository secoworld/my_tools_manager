<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload, RefreshLeft, EditPen, Document, Cellphone } from '@element-plus/icons-vue'
import {
  Document as IconDocument, Lock as IconLock, Clock as IconClock,
  EditPen as IconEditPen, Coin as IconCoin, Grid as IconGrid,
  Histogram as IconHistogram, Connection as IconConnection,
  DocumentCopy as IconDocumentCopy, Files as IconFiles, Tools as IconTools
} from '@element-plus/icons-vue'
import JSZip from 'jszip'

// 可选图标列表
const iconOptions = [
  { label: 'Tools（工具）', value: 'Tools', icon: IconTools },
  { label: 'Document（文档）', value: 'Document', icon: IconDocument },
  { label: 'Lock（锁）', value: 'Lock', icon: IconLock },
  { label: 'Clock（时钟）', value: 'Clock', icon: IconClock },
  { label: 'EditPen（编辑）', value: 'EditPen', icon: IconEditPen },
  { label: 'Coin（硬币）', value: 'Coin', icon: IconCoin },
  { label: 'Grid（网格）', value: 'Grid', icon: IconGrid },
  { label: 'Histogram（柱状图）', value: 'Histogram', icon: IconHistogram },
  { label: 'Connection（连接）', value: 'Connection', icon: IconConnection },
  { label: 'DocumentCopy（文档副本）', value: 'DocumentCopy', icon: IconDocumentCopy },
  { label: 'Files（文件）', value: 'Files', icon: IconFiles }
]
const iconMap = Object.fromEntries(iconOptions.map(o => [o.value, o.icon]))

// ========== 默认示例 ==========
const DEFAULT_MANIFEST = {
  id: 'my-first-plugin',
  name: '我的第一个插件',
  version: '1.0.0',
  description: '这是一个示例插件，展示插件的基本结构',
  icon: 'Tools',
  category: '自定义',
  author: '开发者',
  entryFile: 'index.html',
  visibility: 'ALL',
  needBackend: false
}

const DEFAULT_HTML = `<div id="app">
  <h1>{{ title }}</h1>
  <p>{{ desc }}</p>
  <div class="counter">
    <button @click="count--">-</button>
    <span>{{ count }}</span>
    <button @click="count++">+</button>
  </div>
  <div class="input-group">
    <input v-model="name" placeholder="输入你的名字" />
    <p v-if="name">你好，{{ name }}！</p>
  </div>
</div>

<style>
  #app {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    padding: 24px;
    color: #333;
  }
  h1 {
    color: #409eff;
    font-size: 22px;
    margin-bottom: 8px;
  }
  p { color: #666; margin-bottom: 16px; }
  .counter {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
  }
  .counter button {
    width: 36px;
    height: 36px;
    border: none;
    border-radius: 8px;
    background: #409eff;
    color: #fff;
    font-size: 18px;
    cursor: pointer;
    transition: all 0.2s;
  }
  .counter button:hover { background: #66b1ff; }
  .counter span {
    font-size: 24px;
    font-weight: 700;
    min-width: 40px;
    text-align: center;
  }
  .input-group input {
    padding: 8px 14px;
    border: 2px solid #dcdfe6;
    border-radius: 8px;
    font-size: 14px;
    outline: none;
    transition: border-color 0.2s;
  }
  .input-group input:focus { border-color: #409eff; }
  .input-group p { margin-top: 8px; color: #67c23a; }
</style>

<script src="https://unpkg.com/vue@3/dist/vue.global.js"><\/script>
<script>
  const { createApp, ref } = Vue
  createApp({
    setup() {
      const title = ref('Hello Plugin!')
      const desc = ref('这是一个使用 Vue 3 编写的插件示例')
      const count = ref(0)
      const name = ref('')
      return { title, desc, count, name }
    }
  }).mount('#app')
<\/script>`

// ========== 状态 ==========
const STORAGE_KEY = 'plugin-developer-state'

const manifest = ref({ ...DEFAULT_MANIFEST })
const htmlCode = ref(DEFAULT_HTML)
const activeTab = ref('manifest')
const previewKey = ref(0)
const previewHtml = ref('')

// ========== 持久化 ==========
function saveState() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      manifest: manifest.value,
      htmlCode: htmlCode.value
    }))
  } catch (e) {
    console.warn('保存失败:', e)
  }
}

function loadState() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      const data = JSON.parse(saved)
      if (data.manifest) manifest.value = { ...data.manifest }
      if (data.htmlCode) htmlCode.value = data.htmlCode
    }
  } catch (e) {
    console.warn('加载失败:', e)
  }
}

// ========== 实时预览 ==========
let debounceTimer = null
function updatePreview() {
  previewHtml.value = htmlCode.value
  previewKey.value++
}

watch(htmlCode, () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(updatePreview, 400)
}, { immediate: false })

// ========== 重置 ==========
function handleReset() {
  ElMessageBox.confirm('确定要重置为默认示例吗？当前编辑的内容将丢失。', '重置确认', {
    confirmButtonText: '确定重置',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    manifest.value = { ...DEFAULT_MANIFEST }
    htmlCode.value = DEFAULT_HTML
    updatePreview()
    saveState()
    ElMessage.success('已重置为默认示例')
  }).catch(() => {})
}

// ========== 下载插件包 ==========
async function handleDownload() {
  try {
    const manifestJson = JSON.stringify(manifest.value, null, 2)
    const zip = new JSZip()
    zip.file('manifest.json', manifestJson)
    zip.file(manifest.value.entryFile || 'index.html', htmlCode.value)

    const blob = await zip.generateAsync({ type: 'blob' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${manifest.value.id || 'plugin'}.zip`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('插件包下载成功')
  } catch (e) {
    ElMessage.error('下载失败: ' + e.message)
  }
}

// ========== 上传插件包 ==========
const uploadRef = ref(null)

function handleUploadClick() {
  uploadRef.value?.click()
}

async function handleFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  try {
    const zip = await JSZip.loadAsync(file)
    const manifestFile = zip.file('manifest.json')
    if (!manifestFile) {
      ElMessage.error('ZIP 包中未找到 manifest.json')
      return
    }

    const manifestText = await manifestFile.async('string')
    const parsed = JSON.parse(manifestText)
    manifest.value = {
      id: parsed.id || '',
      name: parsed.name || '',
      version: parsed.version || '1.0.0',
      description: parsed.description || '',
      icon: parsed.icon || 'Tools',
      category: parsed.category || '自定义',
      author: parsed.author || '',
      entryFile: parsed.entryFile || 'index.html',
      visibility: parsed.visibility || 'ALL',
      needBackend: parsed.needBackend || false
    }

    const entryFileName = manifest.value.entryFile
    const entryFile = zip.file(entryFileName)
    if (entryFile) {
      htmlCode.value = await entryFile.async('string')
    } else {
      ElMessage.warning(`未找到入口文件 ${entryFileName}，HTML 内容未更新`)
    }

    updatePreview()
    saveState()
    ElMessage.success('插件包加载成功')
  } catch (e) {
    ElMessage.error('解析失败: ' + e.message)
  }

  // 清空 input 以便重复选择同一文件
  event.target.value = ''
}

// ========== 上传到服务器 ==========
const uploading = ref(false)

async function doUpload(token, force = false) {
  const manifestJson = JSON.stringify(manifest.value, null, 2)
  const zip = new JSZip()
  zip.file('manifest.json', manifestJson)
  zip.file(manifest.value.entryFile || 'index.html', htmlCode.value)
  const blob = await zip.generateAsync({ type: 'blob' })

  const formData = new FormData()
  const fileName = `${manifest.value.id || 'plugin'}.zip`
  formData.append('file', blob, fileName)

  const url = force ? '/api/plugins/upload?force=true' : '/api/plugins/upload'
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  })
  return await response.json()
}

async function handleUploadToServer() {
  // 检查是否登录
  const token = localStorage.getItem('admin-token')
  if (!token) {
    ElMessage.warning('请先登录后台管理')
    return
  }

  try {
    uploading.value = true
    let result = await doUpload(token, false)

    if (result.success) {
      ElMessage.success(result.message || '插件上传成功')
      return
    }

    // 插件已存在，提示是否更新
    if (result.code === 'PLUGIN_EXISTS') {
      try {
        await ElMessageBox.confirm(
          `${result.message}\n更新将覆盖旧版本文件，保留启用状态和显示范围设置。`,
          '插件已存在',
          {
            confirmButtonText: '确认更新',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        // 用户确认更新
        result = await doUpload(token, true)
        if (result.success) {
          ElMessage.success(result.message || '插件更新成功')
        } else {
          ElMessage.error(result.message || '更新失败')
        }
      } catch {
        // 用户取消
      }
      return
    }

    ElMessage.error(result.message || '上传失败')
  } catch (e) {
    ElMessage.error('上传失败: ' + e.message)
  } finally {
    uploading.value = false
  }
}

// ========== 自动保存 ==========
watch([manifest, htmlCode], () => {
  saveState()
}, { deep: true })

// ========== 生命周期 ==========
onMounted(() => {
  loadState()
  updatePreview()
})
</script>

<template>
  <div class="plugin-developer">
    <!-- 顶部工具栏 -->
    <div class="pd-header">
      <div class="pd-header-left">
        <el-icon class="pd-title-icon"><EditPen /></el-icon>
        <span class="pd-title">插件开发工具</span>
        <el-tag size="small" type="info" effect="plain">实时预览</el-tag>
      </div>
      <div class="pd-header-right">
        <el-button :icon="RefreshLeft" @click="handleReset">重置示例</el-button>
        <el-button :icon="Download" @click="handleDownload">下载插件包</el-button>
        <el-button :icon="Upload" @click="handleUploadClick">导入插件包</el-button>
        <el-button type="primary" :icon="Upload" :loading="uploading" @click="handleUploadToServer">
          上传到服务器
        </el-button>
        <input
          ref="uploadRef"
          type="file"
          accept=".zip"
          style="display:none"
          @change="handleFileChange"
        />
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="pd-body">
      <!-- 左侧：编辑栏 -->
      <div class="pd-editor">
        <el-tabs v-model="activeTab" class="pd-tabs">
          <!-- Manifest 配置 -->
          <el-tab-pane label="插件配置" name="manifest">
            <div class="pd-form-scroll">
              <el-form :model="manifest" label-width="100px" label-position="right" size="default">
                <el-form-item label="插件ID">
                  <el-input
                    v-model="manifest.id"
                    placeholder="如: my-tool（小写字母开头，含字母数字和连字符）"
                  />
                </el-form-item>
                <el-form-item label="插件名称">
                  <el-input v-model="manifest.name" placeholder="显示名称" />
                </el-form-item>
                <el-form-item label="版本号">
                  <el-input v-model="manifest.version" placeholder="如: 1.0.0" />
                </el-form-item>
                <el-form-item label="描述">
                  <el-input
                    v-model="manifest.description"
                    type="textarea"
                    :rows="2"
                    placeholder="插件功能描述"
                  />
                </el-form-item>
                <el-form-item label="图标">
                  <el-select
                    v-model="manifest.icon"
                    placeholder="选择图标"
                    style="width: 100%"
                    filterable
                  >
                    <template #prefix>
                      <el-icon v-if="iconMap[manifest.icon]">
                        <component :is="iconMap[manifest.icon]" />
                      </el-icon>
                    </template>
                    <el-option
                      v-for="opt in iconOptions"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    >
                      <span style="display: flex; align-items: center; gap: 8px;">
                        <el-icon><component :is="opt.icon" /></el-icon>
                        <span>{{ opt.label }}</span>
                      </span>
                    </el-option>
                  </el-select>
                </el-form-item>
                <el-form-item label="分类">
                  <el-input v-model="manifest.category" placeholder="如: 自定义" />
                </el-form-item>
                <el-form-item label="作者">
                  <el-input v-model="manifest.author" placeholder="作者名" />
                </el-form-item>
                <el-form-item label="入口文件">
                  <el-input v-model="manifest.entryFile" placeholder="index.html" />
                </el-form-item>
                <el-form-item label="显示范围">
                  <el-select v-model="manifest.visibility" style="width: 100%">
                    <el-option label="全部（工作区 + 仪表盘）" value="ALL" />
                    <el-option label="仅工作区" value="WORKSPACE" />
                    <el-option label="仅仪表盘" value="DASHBOARD" />
                  </el-select>
                </el-form-item>
                <el-form-item label="需要后端">
                  <el-switch v-model="manifest.needBackend" />
                  <span class="pd-hint">开启后插件标记为需要后端支持</span>
                </el-form-item>
              </el-form>

              <!-- Manifest JSON 预览 -->
              <div class="pd-json-preview">
                <div class="pd-json-header">
                  <el-icon><Document /></el-icon>
                  <span>manifest.json 预览</span>
                </div>
                <pre class="pd-json-code">{{ JSON.stringify(manifest, null, 2) }}</pre>
              </div>
            </div>
          </el-tab-pane>

          <!-- HTML 代码编辑 -->
          <el-tab-pane label="HTML 代码" name="html">
            <div class="pd-code-editor">
              <textarea
                v-model="htmlCode"
                class="pd-textarea"
                spellcheck="false"
                placeholder="在此编写 HTML 代码（支持 Vue 写法）..."
              ></textarea>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 分割线 -->
      <div class="pd-divider" />

      <!-- 右侧：展示栏 -->
      <div class="pd-preview">
        <div class="pd-preview-header">
          <el-icon><Cellphone /></el-icon>
          <span>实时预览</span>
          <el-tag size="small" type="success" effect="plain">iframe 隔离</el-tag>
        </div>
        <div class="pd-preview-body">
          <iframe
            :key="previewKey"
            :srcdoc="previewHtml"
            class="pd-iframe"
            sandbox="allow-scripts allow-same-origin"
            referrerpolicy="no-referrer"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.plugin-developer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
}

/* ========== 顶部工具栏 ========== */
.pd-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.pd-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pd-title-icon {
  font-size: 20px;
  color: #409eff;
}

.pd-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.pd-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ========== 主体区域 ========== */
.pd-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ========== 编辑栏 ========== */
.pd-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
  min-width: 0;
}

.pd-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pd-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid #e4e7ed;
}

.pd-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
}

.pd-tabs :deep(.el-tab-pane) {
  height: 100%;
}

/* ========== 表单滚动区 ========== */
.pd-form-scroll {
  height: 100%;
  overflow-y: auto;
  padding: 20px 24px;
}

.pd-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

/* ========== JSON 预览 ========== */
.pd-json-preview {
  margin-top: 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.pd-json-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #f5f7fa;
  font-size: 13px;
  color: #606266;
  border-bottom: 1px solid #e4e7ed;
}

.pd-json-code {
  margin: 0;
  padding: 14px;
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  background: #fafafa;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

/* ========== 代码编辑器 ========== */
.pd-code-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.pd-textarea {
  flex: 1;
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  padding: 16px 20px;
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #303133;
  background: #fafafa;
  tab-size: 2;
}

.pd-textarea:focus {
  background: #fff;
}

/* ========== 分割线 ========== */
.pd-divider {
  width: 4px;
  background: #e4e7ed;
  cursor: col-resize;
  flex-shrink: 0;
  transition: background 0.2s;
}

.pd-divider:hover {
  background: #409eff;
}

/* ========== 展示栏 ========== */
.pd-preview {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
  min-width: 0;
}

.pd-preview-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  height: 40px;
  background: #fafafa;
  border-bottom: 1px solid #e4e7ed;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}

.pd-preview-body {
  flex: 1;
  overflow: hidden;
  background: #fff;
}

.pd-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
