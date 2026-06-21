<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, CopyDocument } from '@element-plus/icons-vue'
import { useCommandStore } from '../stores/commandStore'

const commandStore = useCommandStore()

// 模块弹窗
const moduleDialogVisible = ref(false)
const moduleForm = ref({
  name: '',
  description: ''
})

// 命令弹窗
const commandDialogVisible = ref(false)
const commandForm = ref({
  moduleId: null,
  name: '',
  command: '',
  description: ''
})

// 当前展开的模块
const activeModuleIds = ref([])

// 打开新增模块弹窗
const openModuleDialog = () => {
  moduleForm.value = { name: '', description: '' }
  moduleDialogVisible.value = true
}

// 提交新增模块
const submitModule = async () => {
  if (!moduleForm.value.name.trim()) {
    ElMessage.warning('请输入模块名称')
    return
  }
  try {
    await commandStore.addModule(moduleForm.value)
    ElMessage.success('模块创建成功')
    moduleDialogVisible.value = false
  } catch (error) {
    ElMessage.error('模块创建失败: ' + (error.message || '未知错误'))
  }
}

// 删除模块
const handleDeleteModule = (module) => {
  ElMessageBox.confirm(
    `确定要删除模块 "${module.name}" 吗？该操作将删除模块下所有命令。`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        await commandStore.removeModule(module.id)
        ElMessage.success('删除成功')
      } catch (error) {
        ElMessage.error('删除失败: ' + (error.message || '未知错误'))
      }
    })
    .catch(() => {})
}

// 打开新增命令弹窗
const openCommandDialog = (moduleId) => {
  commandForm.value = { moduleId, name: '', command: '', description: '' }
  commandDialogVisible.value = true
}

// 提交新增命令
const submitCommand = async () => {
  if (!commandForm.value.name.trim()) {
    ElMessage.warning('请输入命令名称')
    return
  }
  if (!commandForm.value.command.trim()) {
    ElMessage.warning('请输入命令内容')
    return
  }
  try {
    await commandStore.addCommand(commandForm.value)
    ElMessage.success('命令创建成功')
    commandDialogVisible.value = false
  } catch (error) {
    ElMessage.error('命令创建失败: ' + (error.message || '未知错误'))
  }
}

// 删除命令
const handleDeleteCommand = (command, moduleId) => {
  ElMessageBox.confirm(
    `确定要删除命令 "${command.name}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        await commandStore.removeCommand(command.id, moduleId)
        ElMessage.success('删除成功')
      } catch (error) {
        ElMessage.error('删除失败: ' + (error.message || '未知错误'))
      }
    })
    .catch(() => {})
}

// 复制命令内容
const handleCopy = async (code) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败: ' + (error.message || '未知错误'))
  }
}

// 模块变更时加载命令
const handleModuleChange = (activeNames) => {
  const names = Array.isArray(activeNames) ? activeNames : [activeNames]
  names.forEach((name) => {
    if (name && !commandStore.commands[name]) {
      commandStore.fetchCommands(name)
    }
  })
}

onMounted(() => {
  commandStore.fetchModules()
})
</script>

<template>
  <div class="command-library">
    <div class="page-header">
      <h2 class="page-title">命令库管理</h2>
      <el-button type="primary" :icon="Plus" @click="openModuleDialog">新增模块</el-button>
    </div>

    <div class="page-content">
      <el-empty v-if="commandStore.modules.length === 0" description="暂无模块，请点击右上角新增模块" />

      <el-collapse v-else v-model="activeModuleIds" @change="handleModuleChange">
        <el-collapse-item
          v-for="module in commandStore.modules"
          :key="module.id"
          :name="module.id"
        >
          <template #title>
            <div class="module-header">
              <span class="module-name">{{ module.name }}</span>
              <span v-if="module.description" class="module-desc">{{ module.description }}</span>
              <el-button
                type="danger"
                size="small"
                :icon="Delete"
                circle
                @click.stop="handleDeleteModule(module)"
              />
            </div>
          </template>

          <div class="module-body">
            <div class="module-toolbar">
              <el-button type="primary" size="small" :icon="Plus" @click="openCommandDialog(module.id)">
                新增命令
              </el-button>
            </div>

            <div v-if="commandStore.commands[module.id]" class="command-list">
              <el-empty
                v-if="commandStore.commands[module.id].length === 0"
                description="暂无命令"
                :image-size="60"
              />
              <div
                v-for="command in commandStore.commands[module.id]"
                :key="command.id"
                class="command-item"
              >
                <div class="command-info">
                  <div class="command-name">{{ command.name }}</div>
                  <div class="command-code">
                    <code>{{ command.command }}</code>
                  </div>
                  <div v-if="command.description" class="command-desc">{{ command.description }}</div>
                </div>
                <div class="command-actions">
                  <el-button size="small" :icon="CopyDocument" @click="handleCopy(command.command)">
                    复制
                  </el-button>
                  <el-button type="danger" size="small" :icon="Delete" @click="handleDeleteCommand(command, module.id)">
                    删除
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- 新增模块弹窗 -->
    <el-dialog v-model="moduleDialogVisible" title="新增模块" width="500px">
      <el-form :model="moduleForm" label-width="80px">
        <el-form-item label="模块名称">
          <el-input v-model="moduleForm.name" placeholder="请输入模块名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="moduleForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模块描述（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moduleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitModule">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增命令弹窗 -->
    <el-dialog v-model="commandDialogVisible" title="新增命令" width="600px">
      <el-form :model="commandForm" label-width="80px">
        <el-form-item label="命令名称">
          <el-input v-model="commandForm.name" placeholder="请输入命令名称" />
        </el-form-item>
        <el-form-item label="命令内容">
          <el-input
            v-model="commandForm.command"
            type="textarea"
            :rows="5"
            placeholder="请输入命令内容"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="commandForm.description"
            type="textarea"
            :rows="2"
            placeholder="请输入命令描述（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commandDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCommand">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.command-library {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.module-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding-right: 16px;
}

.module-name {
  font-weight: 600;
  color: #303133;
}

.module-desc {
  color: #909399;
  font-size: 12px;
  flex: 1;
}

.module-body {
  padding: 12px 0;
}

.module-toolbar {
  margin-bottom: 12px;
}

.command-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.command-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  gap: 16px;
}

.command-info {
  flex: 1;
  min-width: 0;
}

.command-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.command-code {
  background-color: #fff;
  padding: 8px 12px;
  border-radius: 4px;
  margin-bottom: 6px;
  border: 1px solid #e4e7ed;
}

.command-code code {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #e6a23c;
  word-break: break-all;
  white-space: pre-wrap;
}

.command-desc {
  color: #909399;
  font-size: 12px;
}

.command-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}
</style>
