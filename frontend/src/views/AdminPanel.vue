<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { pluginApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import PluginUploadDialog from '../components/PluginUploadDialog.vue'
import PluginPreview from '../components/PluginPreview.vue'
import { Plus, Refresh, Lock, SwitchButton, Delete, Search, Setting, RefreshRight, Download, Upload, Document } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()

const plugins = ref([])
const loading = ref(false)

// 上传弹窗
const uploadVisible = ref(false)
// 更新弹窗
const updateVisible = ref(false)
const updatePluginId = ref('')

// 预览弹窗
const previewVisible = ref(false)
const previewPluginId = ref('')
const previewPluginName = ref('')

// 批量导入弹窗
const batchImportVisible = ref(false)
const batchImportFiles = ref([])
const batchImportFileList = ref([]) // el-upload 的文件列表（双向绑定）
const batchImportResults = ref([])
const batchImporting = ref(false)

// 监听 auth-expired 事件，自动跳转登录页
const handleAuthExpired = () => {
  ElMessage.warning('登录已过期，请重新登录')
  auth.setToken('')
  router.push('/admin/login')
}

// 显示范围选项
const visibilityOptions = [
  { label: '全部可见', value: 'ALL' },
  { label: '工作区', value: 'WORKSPACE' },
  { label: '仪表盘', value: 'DASHBOARD' }
]

const visibilityLabel = (val) => {
  const item = visibilityOptions.find((o) => o.value === val)
  return item ? item.label : val
}

// 加载插件列表
const loadPlugins = async () => {
  loading.value = true
  try {
    const res = await pluginApi.getAll(auth.token)
    if (Array.isArray(res)) {
      plugins.value = res
    } else if (res && Array.isArray(res.data)) {
      plugins.value = res.data
    } else {
      plugins.value = []
    }
  } catch (e) {
    ElMessage.error('加载插件列表失败')
    plugins.value = []
  } finally {
    loading.value = false
  }
}

// 切换启用状态（el-switch 的 @change 在 v-model 更新后触发，row.enabled 已是新值）
const handleToggleEnabled = async (row) => {
  try {
    if (row.enabled) {
      await pluginApi.enable(row.pluginId, auth.token)
      ElMessage.success('已启用')
    } else {
      await pluginApi.disable(row.pluginId, auth.token)
      ElMessage.success('已禁用')
    }
    await loadPlugins()
  } catch (e) {
    ElMessage.error('操作失败')
    // 回滚 UI 状态
    row.enabled = !row.enabled
  }
}

// 修改显示范围
const handleVisibilityChange = async (row) => {
  try {
    await pluginApi.updateVisibility(row.pluginId, row.visibility, auth.token)
    ElMessage.success('显示范围已更新')
  } catch (e) {
    ElMessage.error('更新显示范围失败')
    await loadPlugins()
  }
}

// 预览插件
const handlePreview = (row) => {
  previewPluginId.value = row.pluginId
  previewPluginName.value = row.name
  previewVisible.value = true
}

// 更新插件
const handleUpdate = (row) => {
  updatePluginId.value = row.pluginId
  updateVisible.value = true
}

// 批量导入：选择文件
const handleBatchImportChange = (file) => {
  if (file.raw) {
    batchImportFiles.value.push(file.raw)
  }
}

const handleBatchImportRemove = (file) => {
  const idx = batchImportFiles.value.findIndex(f => f.name === file.name && f.size === file.size)
  if (idx > -1) batchImportFiles.value.splice(idx, 1)
}

// 批量导入：执行
const handleBatchImportConfirm = async () => {
  if (batchImportFiles.value.length === 0) {
    ElMessage.warning('请先选择插件文件')
    return
  }
  batchImporting.value = true
  batchImportResults.value = []

  for (const file of batchImportFiles.value) {
    try {
      // 先尝试解析是否为多插件压缩包
      const pluginZips = await extractPluginZips(file)
      if (pluginZips.length > 1) {
        // 多插件压缩包：逐个上传
        for (const pz of pluginZips) {
          try {
            const res = await pluginApi.upload(pz.blob, auth.token, true, `${pz.id}.zip`)
            batchImportResults.value.push({
              name: `${pz.id} (${pz.name})`,
              success: !!res.success,
              message: res.message || (res.success ? '成功' : '失败')
            })
          } catch (e) {
            batchImportResults.value.push({
              name: pz.id,
              success: false,
              message: e.message || '网络错误'
            })
          }
        }
      } else {
        // 单插件 ZIP：直接上传
        const res = await pluginApi.upload(file, auth.token, true)
        batchImportResults.value.push({
          name: file.name,
          success: !!res.success,
          message: res.message || (res.success ? '成功' : '失败')
        })
      }
    } catch (e) {
      batchImportResults.value.push({
        name: file.name,
        success: false,
        message: e.message || '解析失败'
      })
    }
  }
  batchImporting.value = false
  await loadPlugins()
}

/**
 * 从 ZIP 文件中提取插件列表
 * 支持两种格式：
 * 1. 单插件 ZIP（根目录有 manifest.json）
 * 2. 多插件 ZIP（每个子目录是一个插件，子目录中有 manifest.json）
 * 返回 { id, name, blob } 数组
 */
async function extractPluginZips(file) {
  const { default: JSZip } = await import('jszip')
  const zip = await JSZip.loadAsync(file)

  // 检查根目录是否有 manifest.json（单插件格式）
  const rootManifest = zip.file('manifest.json')
  if (rootManifest) {
    return [{ id: file.name, name: file.name, blob: file }]
  }

  // 检查子目录（多插件格式）
  const manifestFiles = zip.file(/^([^/]+)\/manifest\.json$/)
  if (manifestFiles.length === 0) {
    // 没有找到 manifest.json，当作单插件处理
    return [{ id: file.name, name: file.name, blob: file }]
  }

  // 为每个子目录创建单独的 ZIP
  const results = []
  for (const manifestFile of manifestFiles) {
    // 提取子目录名
    const dirName = manifestFile.name.split('/')[0]
    // 读取 manifest 获取插件信息
    const manifestText = await manifestFile.async('string')
    let pluginId = dirName
    let pluginName = dirName
    try {
      const manifest = JSON.parse(manifestText)
      pluginId = manifest.id || dirName
      pluginName = manifest.name || dirName
    } catch {}

    // 创建新 ZIP，只包含该子目录中的文件（去掉子目录前缀）
    const newZip = new JSZip()
    const dirPrefix = dirName + '/'
    const dirFiles = zip.filter((relativePath) => relativePath.startsWith(dirPrefix))
    for (const f of dirFiles) {
      const newPath = f.name.substring(dirPrefix.length)
      if (newPath && !f.dir) {
        const content = await f.async('blob')
        newZip.file(newPath, content)
      }
    }
    const blob = await newZip.generateAsync({ type: 'blob' })
    results.push({ id: pluginId, name: pluginName, blob })
  }
  return results
}

// 批量导入：关闭弹窗重置
const handleBatchImportClosed = () => {
  batchImportFiles.value = []
  batchImportFileList.value = [] // 清除 el-upload 内部文件列表
  batchImportResults.value = []
  batchImporting.value = false
}

// 批量导出
const exportVisible = ref(false)
const exportSelected = ref([])
const exportLoading = ref(false)
const exportTableRef = ref(null)

const handleBatchExport = () => {
  if (plugins.value.length === 0) {
    ElMessage.warning('暂无可导出的插件')
    return
  }
  exportSelected.value = []
  exportVisible.value = true
  // 等待弹窗渲染后清除表格选中状态
  nextTick(() => {
    exportTableRef.value?.clearSelection()
  })
}

const handleExportSelectAll = (val) => {
  if (val) {
    exportTableRef.value?.toggleAllSelection()
  } else {
    exportTableRef.value?.clearSelection()
  }
}

const handleExportConfirm = async () => {
  if (exportSelected.value.length === 0) {
    ElMessage.warning('请至少选择一个插件')
    return
  }
  exportLoading.value = true
  try {
    const { default: JSZip } = await import('jszip')
    const zip = new JSZip()
    let count = 0
    for (const plugin of plugins.value) {
      if (!exportSelected.value.includes(plugin.pluginId)) continue
      try {
        const html = await pluginApi.getContent(plugin.pluginId)
        const manifest = {
          id: plugin.pluginId,
          name: plugin.name,
          version: plugin.version,
          description: plugin.description,
          icon: plugin.icon,
          category: plugin.category,
          author: plugin.author,
          entryFile: plugin.entryFile,
          visibility: plugin.visibility,
          needBackend: plugin.needBackend
        }
        const folder = zip.folder(plugin.pluginId)
        folder.file('manifest.json', JSON.stringify(manifest, null, 2))
        folder.file(plugin.entryFile || 'index.html', html)
        count++
      } catch (e) {
        console.warn(`导出插件 ${plugin.pluginId} 失败:`, e)
      }
    }
    if (count === 0) {
      ElMessage.error('没有插件可导出')
      return
    }
    const blob = await zip.generateAsync({ type: 'blob' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `plugins-export-${Date.now()}.zip`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${count} 个插件`)
    exportVisible.value = false
  } catch (e) {
    ElMessage.error('导出失败: ' + e.message)
  } finally {
    exportLoading.value = false
  }
}

// 删除插件
const handleDelete = async (row) => {
  try {
    await pluginApi.delete(row.pluginId, auth.token)
    ElMessage.success('插件已删除')
    await loadPlugins()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// 上传成功
const handleUploadSuccess = () => {
  loadPlugins()
}

// 退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await auth.logout()
  ElMessage.success('已退出登录')
  router.push('/admin/login')
}

const goChangePassword = () => {
  router.push('/admin/password')
}

onMounted(async () => {
  window.addEventListener('auth-expired', handleAuthExpired)
  // 验证 token 是否有效
  const profile = await auth.getProfile()
  if (!profile || !profile.success) {
    router.push('/admin/login')
    return
  }
  loadPlugins()
})

onUnmounted(() => {
  window.removeEventListener('auth-expired', handleAuthExpired)
})
</script>

<template>
  <div class="admin-panel">
    <!-- 头部 -->
    <div class="panel-header">
      <div class="header-left">
        <el-icon class="header-icon"><Setting /></el-icon>
        <span class="header-title">插件管理</span>
      </div>
      <div class="header-right">
        <el-button :icon="Lock" @click="goChangePassword">修改密码</el-button>
        <el-button :icon="SwitchButton" type="danger" plain @click="handleLogout">
          退出登录
        </el-button>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="panel-content">
      <!-- 工具栏 -->
      <div class="panel-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" :icon="Plus" @click="uploadVisible = true">
            上传插件
          </el-button>
          <el-button :icon="Upload" @click="batchImportVisible = true">
            批量导入
          </el-button>
          <el-button :icon="Download" @click="handleBatchExport">
            批量导出
          </el-button>
          <el-button :icon="Document" @click="router.push('/admin/review')">
            插件审核
          </el-button>
          <el-button :icon="Refresh" @click="loadPlugins">刷新</el-button>
        </div>
        <div class="toolbar-right">
          <span class="plugin-count">共 {{ plugins.length }} 个插件</span>
        </div>
      </div>

      <!-- 插件表格 -->
      <el-table
        v-loading="loading"
        :data="plugins"
        border
        stripe
        class="plugin-table"
        empty-text="暂无插件，请点击「上传插件」"
      >
        <el-table-column label="插件名称" prop="name" min-width="160">
          <template #default="{ row }">
            <span class="plugin-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="版本" prop="version" width="90" align="center" />
        <el-table-column label="分类" prop="category" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.category || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="显示范围" width="150" align="center">
          <template #default="{ row }">
            <el-select
              v-model="row.visibility"
              size="small"
              @change="handleVisibilityChange(row)"
            >
              <el-option
                v-for="opt in visibilityOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="handleToggleEnabled(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :icon="Search"
              link
              @click="handlePreview(row)"
            >
              预览
            </el-button>
            <el-button
              type="warning"
              size="small"
              :icon="RefreshRight"
              link
              @click="handleUpdate(row)"
            >
              更新
            </el-button>
            <el-popconfirm
              title="确定删除该插件吗？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button
                  type="danger"
                  size="small"
                  :icon="Delete"
                  link
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 上传弹窗 -->
    <PluginUploadDialog
      v-model:visible="uploadVisible"
      @success="handleUploadSuccess"
    />

    <!-- 更新弹窗 -->
    <PluginUploadDialog
      v-model:visible="updateVisible"
      :update-plugin-id="updatePluginId"
      @success="handleUploadSuccess"
    />

    <!-- 批量导入弹窗 -->
    <el-dialog
      v-model="batchImportVisible"
      title="批量导入插件"
      width="600px"
      :close-on-click-modal="false"
      @closed="handleBatchImportClosed"
    >
      <el-upload
        v-model:file-list="batchImportFileList"
        drag
        accept=".zip"
        multiple
        :auto-upload="false"
        :on-change="handleBatchImportChange"
        :on-remove="handleBatchImportRemove"
      >
        <el-icon class="upload-icon"><Upload /></el-icon>
        <div class="upload-text">将多个插件包拖到此处，或点击选择</div>
        <template #tip>
          <div class="upload-tip">仅支持 .zip 格式，已存在的插件将自动更新</div>
        </template>
      </el-upload>

      <!-- 导入结果 -->
      <div v-if="batchImportResults.length > 0" class="batch-results">
        <div class="batch-results-title">导入结果</div>
        <div
          v-for="(item, idx) in batchImportResults"
          :key="idx"
          class="batch-result-item"
          :class="{ success: item.success, error: !item.success }"
        >
          <span class="batch-result-name">{{ item.name }}</span>
          <span class="batch-result-msg">{{ item.message }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="batchImportVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :loading="batchImporting"
          :disabled="batchImportFiles.length === 0"
          @click="handleBatchImportConfirm"
        >
          开始导入 ({{ batchImportFiles.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量导出弹窗 -->
    <el-dialog
      v-model="exportVisible"
      title="选择要导出的插件"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="export-toolbar">
        <el-checkbox
          :model-value="exportSelected.length === plugins.length && plugins.length > 0"
          :indeterminate="exportSelected.length > 0 && exportSelected.length < plugins.length"
          @change="handleExportSelectAll"
        >
          全选 ({{ exportSelected.length }}/{{ plugins.length }})
        </el-checkbox>
      </div>
      <el-table
        ref="exportTableRef"
        :data="plugins"
        border
        stripe
        row-key="pluginId"
        max-height="400"
        @selection-change="(rows) => exportSelected = rows.map(r => r.pluginId)"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="插件名称" prop="name" min-width="160" />
        <el-table-column label="版本" prop="version" width="90" align="center" />
        <el-table-column label="分类" prop="category" width="120" align="center" />
      </el-table>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="exportLoading"
          :disabled="exportSelected.length === 0"
          @click="handleExportConfirm"
        >
          导出 ({{ exportSelected.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <PluginPreview
      v-model:visible="previewVisible"
      :plugin-id="previewPluginId"
      :plugin-name="previewPluginName"
    />
  </div>
</template>

<style scoped>
.admin-panel {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

.panel-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  font-size: 22px;
  color: #409eff;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-content {
  flex: 1;
  overflow: auto;
  padding: 20px 24px;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  gap: 10px;
}

.plugin-count {
  font-size: 13px;
  color: #909399;
}

.plugin-table {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.plugin-name {
  font-weight: 500;
  color: #303133;
}

/* 批量导入弹窗样式 */
.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
  color: #606266;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.batch-results {
  margin-top: 16px;
  max-height: 240px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.batch-results-title {
  padding: 8px 12px;
  background: #f5f7fa;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
}

.batch-result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  font-size: 13px;
  border-bottom: 1px solid #f0f0f0;
}

.batch-result-item:last-child {
  border-bottom: none;
}

.batch-result-item.success {
  color: #67c23a;
}

.batch-result-item.error {
  color: #f56c6c;
}

.batch-result-name {
  flex: 1;
  word-break: break-all;
}

.batch-result-msg {
  flex-shrink: 0;
  margin-left: 12px;
}

.export-toolbar {
  margin-bottom: 12px;
  padding: 8px 0;
}
</style>
