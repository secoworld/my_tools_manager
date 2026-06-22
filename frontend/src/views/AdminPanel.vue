<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { pluginApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import PluginUploadDialog from '../components/PluginUploadDialog.vue'
import PluginPreview from '../components/PluginPreview.vue'
import { Plus, Refresh, Lock, SwitchButton, Delete, Search, Setting } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()

const plugins = ref([])
const loading = ref(false)

// 上传弹窗
const uploadVisible = ref(false)

// 预览弹窗
const previewVisible = ref(false)
const previewPluginId = ref('')
const previewPluginName = ref('')

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
  // 验证 token 是否有效
  const profile = await auth.getProfile()
  if (!profile) {
    router.push('/admin/login')
    return
  }
  loadPlugins()
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
        <el-table-column label="操作" width="160" align="center" fixed="right">
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
</style>
