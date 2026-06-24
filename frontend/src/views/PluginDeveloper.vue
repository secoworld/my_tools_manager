<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload, RefreshLeft, EditPen, Document, Cellphone, FullScreen, Monitor } from '@element-plus/icons-vue'
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

<script>
  // 优先加载本地 vue.global.js，失败时回退到 CDN
  (function loadVue(onReady) {
    var s = document.createElement('script')
    s.src = '/vendor/vue.global.js'
    s.onload = function () {
      if (window.Vue) { onReady(); return }
      tryFallback(onReady)
    }
    s.onerror = function () { tryFallback(onReady) }
    document.head.appendChild(s)

    function tryFallback(cb) {
      var f = document.createElement('script')
      f.src = 'https://unpkg.com/vue@3.5.38/dist/vue.global.js'
      f.onload = function () { cb() }
      f.onerror = function () {
        document.getElementById('app').innerHTML = '<div style="padding:24px;color:#f56c6c;">Vue 加载失败，请检查网络连接</div>'
      }
      document.head.appendChild(f)
    }
  })(function () {
    initApp()
  })

  function initApp() {
    var createApp = Vue.createApp
    var ref = Vue.ref
    createApp({
      setup: function () {
        var title = ref('Hello Plugin!')
        var desc = ref('这是一个使用 Vue 3 编写的插件示例')
        var count = ref(0)
        var name = ref('')
        return { title: title, desc: desc, count: count, name: name }
      }
    }).mount('#app')
  }
<\/script>`

// ========== 进制转换示例 ==========
const CONVERTER_MANIFEST = {
  id: 'base-converter',
  name: '进制转换工具',
  version: '1.0.0',
  description: '支持二进制、八进制、十进制、十六进制之间的互相转换',
  icon: 'Histogram',
  category: '计算工具',
  author: '开发者',
  entryFile: 'index.html',
  visibility: 'ALL',
  needBackend: false
}

const CONVERTER_HTML = `<div id="app">
  <h1>进制转换工具</h1>
  <p class="desc">输入任意进制的数值，自动转换为其他进制</p>
  <div class="converter">
    <div class="input-row" v-for="item in bases" :key="item.base">
      <label>{{ item.label }}</label>
      <div class="input-wrap">
        <input
          v-model="item.value"
          :placeholder="item.placeholder"
          @input="onInput(item)"
          @focus="onFocus(item)"
        />
        <span class="base-tag">{{ item.tag }}</span>
      </div>
    </div>
  </div>
  <div v-if="error" class="error">{{ error }}</div>
</div>

<style>
  #app {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    padding: 20px;
    color: #333;
  }
  h1 {
    color: #409eff;
    font-size: 18px;
    margin-bottom: 4px;
  }
  .desc {
    color: #909399;
    font-size: 12px;
    margin-bottom: 16px;
  }
  .converter {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .input-row {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .input-row label {
    width: 70px;
    font-size: 13px;
    color: #606266;
    flex-shrink: 0;
  }
  .input-wrap {
    flex: 1;
    position: relative;
  }
  .input-wrap input {
    width: 100%;
    padding: 8px 50px 8px 12px;
    border: 2px solid #dcdfe6;
    border-radius: 6px;
    font-size: 14px;
    font-family: 'Courier New', monospace;
    outline: none;
    transition: border-color 0.2s;
    box-sizing: border-box;
  }
  .input-wrap input:focus {
    border-color: #409eff;
  }
  .base-tag {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 11px;
    color: #909399;
    background: #f0f2f5;
    padding: 2px 6px;
    border-radius: 4px;
  }
  .error {
    margin-top: 12px;
    padding: 8px 12px;
    background: #fef0f0;
    color: #f56c6c;
    border-radius: 6px;
    font-size: 13px;
  }
</style>

<script>
  // 优先加载本地 vue.global.js，失败时回退到 CDN
  (function loadVue(onReady) {
    var s = document.createElement('script')
    s.src = '/vendor/vue.global.js'
    s.onload = function () {
      if (window.Vue) { onReady(); return }
      tryFallback(onReady)
    }
    s.onerror = function () { tryFallback(onReady) }
    document.head.appendChild(s)

    function tryFallback(cb) {
      var f = document.createElement('script')
      f.src = 'https://unpkg.com/vue@3.5.38/dist/vue.global.js'
      f.onload = function () { cb() }
      f.onerror = function () {
        document.getElementById('app').innerHTML = '<div style="padding:24px;color:#f56c6c;">Vue 加载失败，请检查网络连接</div>'
      }
      document.head.appendChild(f)
    }
  })(function () { initApp() })

  function initApp() {
    var createApp = Vue.createApp
    var ref = Vue.ref
    createApp({
      setup: function () {
        var bases = ref([
          { base: 2, label: '二进制', tag: 'BIN', value: '', placeholder: '0-1' },
          { base: 8, label: '八进制', tag: 'OCT', value: '', placeholder: '0-7' },
          { base: 10, label: '十进制', tag: 'DEC', value: '', placeholder: '0-9' },
          { base: 16, label: '十六进制', tag: 'HEX', value: '', placeholder: '0-9, A-F' }
        ])
        var error = ref('')
        var currentBase = ref(10)

        var patterns = {
          2: /^[01]*$/,
          8: /^[0-7]*$/,
          10: /^[0-9]*$/,
          16: /^[0-9a-fA-F]*$/
        }

        function onInput(item) {
          var val = item.value.trim()
          if (!val) {
            error.value = ''
            bases.value.forEach(function (b) { if (b !== item) b.value = '' })
            return
          }
          if (!patterns[item.base].test(val)) {
            error.value = item.label + '只能包含: ' + item.placeholder
            return
          }
          error.value = ''
          currentBase.value = item.base
          var decimal = parseInt(val, item.base)
          if (isNaN(decimal)) {
            error.value = '转换失败'
            return
          }
          bases.value.forEach(function (b) {
            if (b.base !== item.base) {
              b.value = decimal.toString(b.base).toUpperCase()
            }
          })
        }

        function onFocus(item) { currentBase.value = item.base }

        return { bases: bases, error: error, onInput: onInput, onFocus: onFocus }
      }
    }).mount('#app')
  }
<\/script>`

// ========== 示例列表 ==========
const EXAMPLES = [
  { name: '基础示例（Vue计数器）', manifest: DEFAULT_MANIFEST, html: DEFAULT_HTML },
  { name: '进制转换工具', manifest: CONVERTER_MANIFEST, html: CONVERTER_HTML }
]

// ========== 状态 ==========
const STORAGE_KEY = 'plugin-developer-state'

const manifest = ref({ ...DEFAULT_MANIFEST })
const htmlCode = ref(DEFAULT_HTML)
const activeTab = ref('manifest')
const previewKey = ref(0)
const previewHtml = ref('')
const selectedExample = ref('基础示例（Vue计数器）')

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

// ========== 预览弹窗 / 全屏 ==========
const dialogPreviewVisible = ref(false)
const fullscreenPreview = ref(false)

function handlePreviewDialog() {
  dialogPreviewVisible.value = true
}

function handlePreviewFullscreen() {
  fullscreenPreview.value = true
  // 监听 ESC 退出全屏
  document.addEventListener('keydown', handleEscKey)
}

function handleEscKey(e) {
  if (e.key === 'Escape' && fullscreenPreview.value) {
    exitFullscreen()
  }
}

function exitFullscreen() {
  fullscreenPreview.value = false
  document.removeEventListener('keydown', handleEscKey)
}

// ========== 加载示例 ==========
function loadExample(exampleName) {
  const example = EXAMPLES.find(e => e.name === exampleName)
  if (example) {
    manifest.value = { ...example.manifest }
    htmlCode.value = example.html
    updatePreview()
    saveState()
  }
}

// ========== 重置 ==========
function handleReset() {
  ElMessageBox.confirm('确定要重置为当前示例吗？当前编辑的内容将丢失。', '重置确认', {
    confirmButtonText: '确定重置',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    loadExample(selectedExample.value)
    ElMessage.success('已重置为' + selectedExample.value)
  }).catch(() => {})
}

// ========== 切换示例 ==========
function handleExampleChange(name) {
  ElMessageBox.confirm('切换示例将覆盖当前编辑的内容，确定切换吗？', '切换示例', {
    confirmButtonText: '确定切换',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    loadExample(name)
    ElMessage.success('已加载示例: ' + name)
  }).catch(() => {
    // 用户取消，恢复选择
    selectedExample.value = EXAMPLES.find(e =>
      e.manifest.id === manifest.value.id
    )?.name || selectedExample.value
  })
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

// 生成 ZIP Blob（公共函数，上传和提交审核共用）
async function generateZipBlob() {
  const manifestJson = JSON.stringify(manifest.value, null, 2)
  const zip = new JSZip()
  zip.file('manifest.json', manifestJson)
  zip.file(manifest.value.entryFile || 'index.html', htmlCode.value)
  return await zip.generateAsync({ type: 'blob' })
}

async function doUpload(token, force = false) {
  const blob = await generateZipBlob()

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

// ========== 提交审核（非 admin 用户） ==========
const submitting = ref(false)

async function handleSubmitForReview() {
  try {
    await ElMessageBox.confirm(
      '提交后插件将进入待审核状态，管理员审核通过后才会显示在插件列表中。\n相同 ID 的待审核插件会被覆盖，只保留最新提交。\n\n是否继续？',
      '提交插件审核',
      {
        confirmButtonText: '确认提交',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
  } catch {
    return
  }

  try {
    submitting.value = true
    // 生成 ZIP blob
    const blob = await generateZipBlob()
    const formData = new FormData()
    const fileName = `${manifest.value.id || 'plugin'}.zip`
    formData.append('file', blob, fileName)

    // 提交审核不需要登录，但如果已登录则带上 token 用于记录提交者
    const token = localStorage.getItem('admin-token')
    const headers = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    const response = await fetch('/api/plugins/submit', {
      method: 'POST',
      headers,
      body: formData
    })
    const result = await response.json()

    if (result.success) {
      ElMessage.success(result.message || '插件提交成功，等待管理员审核')
    } else {
      ElMessage.error(result.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error('提交失败: ' + e.message)
  } finally {
    submitting.value = false
  }
}

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
        <el-select
          v-model="selectedExample"
          placeholder="选择示例"
          size="default"
          style="width: 180px"
          @change="handleExampleChange"
        >
          <el-option
            v-for="example in EXAMPLES"
            :key="example.name"
            :label="example.name"
            :value="example.name"
          />
        </el-select>
        <el-button :icon="RefreshLeft" @click="handleReset">重置示例</el-button>
        <el-button :icon="Download" @click="handleDownload">下载插件包</el-button>
        <el-button :icon="Upload" @click="handleUploadClick">导入插件包</el-button>
        <el-button type="primary" :icon="Upload" :loading="uploading" @click="handleUploadToServer">
          上传到服务器
        </el-button>
        <el-button type="warning" :icon="Upload" :loading="submitting" @click="handleSubmitForReview">
          提交审核
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
          <div class="pd-preview-actions">
            <el-tooltip content="弹窗预览" placement="bottom">
              <el-button
                size="small"
                :icon="Monitor"
                circle
                @click="handlePreviewDialog"
              />
            </el-tooltip>
            <el-tooltip content="全屏预览" placement="bottom">
              <el-button
                size="small"
                :icon="FullScreen"
                circle
                @click="handlePreviewFullscreen"
              />
            </el-tooltip>
          </div>
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

    <!-- 弹窗预览 -->
    <el-dialog
      v-model="dialogPreviewVisible"
      title="弹窗预览"
      width="80%"
      top="6vh"
      destroy-on-close
    >
      <div class="pd-dialog-preview-body">
        <iframe
          :srcdoc="previewHtml"
          class="pd-iframe"
          sandbox="allow-scripts allow-same-origin"
          referrerpolicy="no-referrer"
        />
      </div>
    </el-dialog>

    <!-- 全屏预览 -->
    <div v-if="fullscreenPreview" class="pd-fullscreen-overlay">
      <div class="pd-fullscreen-header">
        <span class="pd-fullscreen-title">全屏预览</span>
        <el-button type="danger" size="small" @click="exitFullscreen">退出全屏 (ESC)</el-button>
      </div>
      <div class="pd-fullscreen-body">
        <iframe
          :srcdoc="previewHtml"
          class="pd-iframe"
          sandbox="allow-scripts allow-same-origin"
          referrerpolicy="no-referrer"
        />
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

.pd-preview-actions {
  margin-left: auto;
  display: flex;
  gap: 6px;
}

.pd-dialog-preview-body {
  width: 100%;
  height: 70vh;
  background: #f5f7fa;
  border-radius: 6px;
  overflow: hidden;
}

.pd-dialog-preview-body .pd-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}

/* 全屏预览 */
.pd-fullscreen-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 3000;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.pd-fullscreen-header {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.pd-fullscreen-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.pd-fullscreen-body {
  flex: 1;
  overflow: hidden;
}

.pd-fullscreen-body .pd-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
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
