<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pluginApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { Search, Check, Close, Delete, Refresh } from '@element-plus/icons-vue'

const auth = useAuthStore()

const loading = ref(false)
const pendingPlugins = ref([])
const previewVisible = ref(false)
const previewPluginId = ref('')
const previewPluginName = ref('')
const previewHtml = ref('')

// 拒绝对话框
const rejectVisible = ref(false)
const rejectPluginId = ref('')
const rejectComment = ref('')

// 加载待审核插件列表
async function loadPendingPlugins() {
  loading.value = true
  try {
    const data = await pluginApi.getPendingPlugins()
    pendingPlugins.value = Array.isArray(data) ? data : []
  } catch (e) {
    ElMessage.error('加载待审核列表失败')
    pendingPlugins.value = []
  } finally {
    loading.value = false
  }
}

// 预览插件
async function handlePreview(row) {
  previewPluginId.value = row.pluginId
  previewPluginName.value = row.name
  try {
    const html = await pluginApi.getPendingContent(row.pluginId)
    previewHtml.value = html
    previewVisible.value = true
  } catch (e) {
    ElMessage.error('获取插件内容失败: ' + e.message)
  }
}

// 审核通过
async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(
      `确认审核通过插件 "${row.name}" (v${row.version})？\n审核通过后插件将出现在插件管理页面中。`,
      '审核确认',
      {
        confirmButtonText: '通过',
        cancelButtonText: '取消',
        type: 'success'
      }
    )
    const res = await pluginApi.approvePlugin(row.pluginId, '')
    if (res.success) {
      ElMessage.success('审核通过，插件已安装')
      await loadPendingPlugins()
    } else {
      ElMessage.error(res.message || '审核失败')
    }
  } catch {
    // 用户取消
  }
}

// 审核拒绝
function handleReject(row) {
  rejectPluginId.value = row.pluginId
  rejectComment.value = ''
  rejectVisible.value = true
}

async function handleRejectConfirm() {
  if (!rejectComment.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  try {
    const res = await pluginApi.rejectPlugin(rejectPluginId.value, rejectComment.value)
    if (res.success) {
      ElMessage.success('已拒绝插件')
      rejectVisible.value = false
      await loadPendingPlugins()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  }
}

// 删除待审核插件
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除待审核插件 "${row.name}"？`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const res = await pluginApi.deletePendingPlugin(row.pluginId)
    if (res.success) {
      ElMessage.success('已删除')
      await loadPendingPlugins()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

// 格式化时间
function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadPendingPlugins()
})
</script>

<template>
  <div class="pending-review-panel">
    <!-- 工具栏 -->
    <div class="panel-toolbar">
      <div class="toolbar-left">
        <h2 class="panel-title">插件审核</h2>
        <el-tag type="warning" size="large">待审核: {{ pendingPlugins.length }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-button :icon="Refresh" @click="loadPendingPlugins">刷新</el-button>
        <el-button @click="$router.push('/admin')">返回插件管理</el-button>
      </div>
    </div>

    <!-- 提示 -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="非 admin 用户提交的插件将在此处审核。审核通过后插件会出现在插件管理页面中。相同 ID 的待审核插件会自动覆盖，只保留最新提交。"
      style="margin-bottom: 16px;"
    />

    <!-- 待审核插件表格 -->
    <el-table
      v-loading="loading"
      :data="pendingPlugins"
      border
      stripe
      class="plugin-table"
      empty-text="暂无待审核插件"
    >
      <el-table-column label="插件名称" prop="name" min-width="140">
        <template #default="{ row }">
          <span class="plugin-name">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="插件ID" prop="pluginId" width="160">
        <template #default="{ row }">
          <code class="plugin-id-code">{{ row.pluginId }}</code>
        </template>
      </el-table-column>
      <el-table-column label="版本" prop="version" width="90" align="center" />
      <el-table-column label="分类" prop="category" width="120" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.category || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交者" prop="submitter" width="100" align="center" />
      <el-table-column label="提交时间" width="170" align="center">
        <template #default="{ row }">
          {{ formatTime(row.submittedAt) }}
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
            type="success"
            size="small"
            :icon="Check"
            link
            @click="handleApprove(row)"
          >
            通过
          </el-button>
          <el-button
            type="warning"
            size="small"
            :icon="Close"
            link
            @click="handleReject(row)"
          >
            拒绝
          </el-button>
          <el-popconfirm
            title="确定删除该待审核插件吗？"
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

    <!-- 预览弹窗 -->
    <el-dialog
      v-model="previewVisible"
      :title="`预览: ${previewPluginName}`"
      width="80%"
      top="6vh"
      destroy-on-close
    >
      <div class="preview-body">
        <iframe
          :srcdoc="previewHtml"
          class="preview-iframe"
          sandbox="allow-scripts allow-same-origin"
          referrerpolicy="no-referrer"
        />
      </div>
    </el-dialog>

    <!-- 拒绝对话框 -->
    <el-dialog
      v-model="rejectVisible"
      title="拒绝插件"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form>
        <el-form-item label="拒绝原因">
          <el-input
            v-model="rejectComment"
            type="textarea"
            :rows="4"
            placeholder="请填写拒绝原因（将通知提交者）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="warning" @click="handleRejectConfirm">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pending-review-panel {
  padding: 20px;
  background: #fff;
  min-height: 100vh;
}

.panel-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel-title {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.toolbar-right {
  display: flex;
  gap: 10px;
}

.plugin-table {
  width: 100%;
}

.plugin-name {
  font-weight: 500;
  color: #303133;
}

.plugin-id-code {
  font-family: 'Consolas', monospace;
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
  padding: 2px 6px;
  border-radius: 3px;
}

.preview-body {
  width: 100%;
  height: 70vh;
  background: #f5f7fa;
  border-radius: 6px;
  overflow: hidden;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}
</style>
