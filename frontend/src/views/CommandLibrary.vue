<script setup>
import { ref, shallowRef, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, CopyDocument, Download, Upload, Edit } from '@element-plus/icons-vue'
import { useCommandStore } from '../stores/commandStore'

const commandStore = useCommandStore()

// MdPreview 延迟加载（md-editor-v3），避免首屏加载过大
const MdPreview = shallowRef(null)
const previewLoaded = ref(false)
async function loadPreview() {
  if (previewLoaded.value) return
  const mod = await import('md-editor-v3')
  await import('md-editor-v3/lib/style.css')
  MdPreview.value = mod.MdPreview
  previewLoaded.value = true
}

// 模块弹窗
const moduleDialogVisible = ref(false)
const moduleForm = ref({
  name: '',
  description: '',
  format: 'command' // command | gitbook
})

// 命令弹窗（command 模块用）
const commandDialogVisible = ref(false)
const commandForm = ref({
  moduleId: null,
  name: '',
  command: '',
  description: ''
})

// 章节弹窗（gitbook 模块用）
const chapterDialogVisible = ref(false)
const chapterDialogMode = ref('create') // create | edit
const chapterForm = ref({
  id: null,
  moduleId: null,
  name: '',
  content: '',
  sortOrder: 0
})

// 当前展开的模块
const activeModuleIds = ref([])

// 当前选中的章节（按 moduleId 索引）
const activeChapterIds = ref({})

// 导入文件 input 引用
const importFileInput = ref(null)

// 兼容旧数据：未设置 format 默认为 command
const moduleFormat = (module) => module.format || 'command'

// 打开新增模块弹窗
const openModuleDialog = () => {
  moduleForm.value = { name: '', description: '', format: 'command' }
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
    `确定要删除模块 "${module.name}" 吗？该操作将删除模块下所有内容。`,
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

// 模块展开/收起时加载命令（或章节）
const handleModuleChange = async (activeNames) => {
  const names = Array.isArray(activeNames) ? activeNames : [activeNames]
  for (const name of names) {
    if (name && !commandStore.commands[name]) {
      await commandStore.fetchCommands(name)
      const module = commandStore.modules.find((m) => m.id === name)
      // 文档库模块：默认选中第一个章节，并预加载 Markdown 预览组件
      if (module && moduleFormat(module) === 'gitbook') {
        const list = commandStore.commands[name]
        if (list && list.length > 0 && !activeChapterIds.value[name]) {
          activeChapterIds.value[name] = list[0].id
        }
        loadPreview()
      }
    }
  }
}

// ========== GitBook 章节相关 ==========

// 获取模块当前选中章节对象
const getActiveChapter = (moduleId) => {
  const list = commandStore.commands[moduleId] || []
  if (list.length === 0) return null
  const cid = activeChapterIds.value[moduleId]
  if (cid) {
    const found = list.find((c) => c.id === cid)
    if (found) return found
  }
  return list[0]
}

// 章节是否高亮
const isActiveChapter = (moduleId, chapterId) => {
  const active = getActiveChapter(moduleId)
  return active && active.id === chapterId
}

// 选中章节
const selectChapter = (moduleId, chapterId) => {
  activeChapterIds.value[moduleId] = chapterId
}

// 打开新增章节弹窗
const openChapterDialog = (moduleId) => {
  chapterDialogMode.value = 'create'
  const list = commandStore.commands[moduleId] || []
  chapterForm.value = {
    id: null,
    moduleId,
    name: '',
    content: '',
    sortOrder: list.length
  }
  chapterDialogVisible.value = true
}

// 打开编辑章节弹窗
const openEditChapterDialog = (chapter) => {
  chapterDialogMode.value = 'edit'
  chapterForm.value = {
    id: chapter.id,
    moduleId: chapter.moduleId,
    name: chapter.name || '',
    content: chapter.content || '',
    sortOrder: chapter.sortOrder || 0
  }
  chapterDialogVisible.value = true
}

// 提交章节（新增或编辑）
const submitChapter = async () => {
  if (!chapterForm.value.name.trim()) {
    ElMessage.warning('请输入章节标题')
    return
  }
  if (!chapterForm.value.content.trim()) {
    ElMessage.warning('请输入章节内容')
    return
  }
  try {
    const payload = {
      moduleId: chapterForm.value.moduleId,
      name: chapterForm.value.name,
      command: '',
      content: chapterForm.value.content,
      sortOrder: chapterForm.value.sortOrder
    }
    if (chapterDialogMode.value === 'create') {
      await commandStore.addCommand(payload)
      ElMessage.success('章节创建成功')
    } else {
      await commandStore.updateCommand(chapterForm.value.id, payload)
      ElMessage.success('章节更新成功')
    }
    chapterDialogVisible.value = false
    // 重新拉取章节列表，并选中刚操作的章节
    await commandStore.fetchCommands(chapterForm.value.moduleId)
    const list = commandStore.commands[chapterForm.value.moduleId] || []
    if (list.length > 0) {
      // 优先选回当前编辑的章节；新增则选最后一个
      const target = chapterDialogMode.value === 'edit'
        ? list.find((c) => c.id === chapterForm.value.id)
        : list[list.length - 1]
      activeChapterIds.value[chapterForm.value.moduleId] = target ? target.id : list[0].id
    }
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

// 删除章节
const handleDeleteChapter = (chapter, moduleId) => {
  ElMessageBox.confirm(
    `确定要删除章节 "${chapter.name}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        await commandStore.removeCommand(chapter.id, moduleId)
        // 切换到第一个章节
        const list = commandStore.commands[moduleId] || []
        activeChapterIds.value[moduleId] = list.length > 0 ? list[0].id : null
        ElMessage.success('删除成功')
      } catch (error) {
        ElMessage.error('删除失败: ' + (error.message || '未知错误'))
      }
    })
    .catch(() => {})
}

// 导出 gitbook 模块为 JSON 文件下载
const handleExportModule = (module) => {
  const chapters = (commandStore.commands[module.id] || []).map((c) => ({
    name: c.name,
    content: c.content || '',
    sortOrder: c.sortOrder != null ? c.sortOrder : 0
  }))
  const data = {
    module: {
      name: module.name,
      description: module.description || '',
      format: 'gitbook'
    },
    chapters
  }
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: 'application/json;charset=utf-8'
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${module.name || 'gitbook'}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出')
}

// 触发导入文件选择
const triggerImport = () => {
  importFileInput.value?.click()
}

// 导入 gitbook JSON：先创建模块，再逐个创建章节
const handleImportFile = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  try {
    const text = await file.text()
    const data = JSON.parse(text)
    if (!data.module || !data.module.name) {
      ElMessage.error('JSON 格式不正确：缺少 module.name')
      e.target.value = ''
      return
    }
    if (data.module.format !== 'gitbook') {
      ElMessage.error('JSON 格式不正确：format 必须为 gitbook')
      e.target.value = ''
      return
    }
    // 先创建模块
    await commandStore.addModule({
      name: data.module.name,
      description: data.module.description || '',
      format: 'gitbook'
    })
    // 找到刚创建的模块（取 id 最大的匹配项）
    const matches = commandStore.modules.filter(
      (m) => m.name === data.module.name && moduleFormat(m) === 'gitbook'
    )
    const created = matches.reduce((max, cur) => (!max || cur.id > max.id ? cur : max), null)
    if (!created) {
      ElMessage.error('模块创建失败')
      e.target.value = ''
      return
    }
    // 逐个创建章节
    const chapters = Array.isArray(data.chapters) ? data.chapters : []
    for (let i = 0; i < chapters.length; i++) {
      const ch = chapters[i]
      await commandStore.addCommand({
        moduleId: created.id,
        name: ch.name || `章节 ${i + 1}`,
        command: '',
        content: ch.content || '',
        sortOrder: ch.sortOrder != null ? ch.sortOrder : i
      })
    }
    ElMessage.success(`导入成功：${chapters.length} 个章节`)
    // 展开新模块并选中首个章节
    activeModuleIds.value = [...activeModuleIds.value, created.id]
    await commandStore.fetchCommands(created.id)
    const list = commandStore.commands[created.id] || []
    if (list.length > 0) {
      activeChapterIds.value[created.id] = list[0].id
    }
    loadPreview()
  } catch (err) {
    ElMessage.error('导入失败: ' + (err.message || '未知错误'))
  }
  e.target.value = ''
}

onMounted(() => {
  commandStore.fetchModules()
})
</script>

<template>
  <div class="command-library">
    <div class="page-header">
      <h2 class="page-title">命令库管理</h2>
      <div class="header-actions">
        <el-button :icon="Upload" @click="triggerImport">导入文档</el-button>
        <el-button type="primary" :icon="Plus" @click="openModuleDialog">新增模块</el-button>
        <input
          ref="importFileInput"
          type="file"
          accept=".json,application/json"
          style="display: none"
          @change="handleImportFile"
        />
      </div>
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
              <el-tag
                v-if="moduleFormat(module) === 'gitbook'"
                type="success"
                size="small"
                effect="plain"
              >
                文档库
              </el-tag>
              <el-tag v-else type="info" size="small" effect="plain">命令库</el-tag>
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

          <!-- 命令库模块（保持原有 UI） -->
          <div v-if="moduleFormat(module) === 'command'" class="module-body">
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

          <!-- GitBook 文档模块 -->
          <div v-else class="gitbook-body">
            <div class="module-toolbar">
              <el-button type="primary" size="small" :icon="Plus" @click="openChapterDialog(module.id)">
                新增章节
              </el-button>
              <el-button size="small" :icon="Download" @click="handleExportModule(module)">
                导出
              </el-button>
            </div>

            <div class="gitbook-content">
              <!-- 左侧章节列表 -->
              <div class="chapter-list">
                <el-empty
                  v-if="!commandStore.commands[module.id] || commandStore.commands[module.id].length === 0"
                  description="暂无章节"
                  :image-size="60"
                />
                <template v-else>
                  <div
                    v-for="chapter in commandStore.commands[module.id]"
                    :key="chapter.id"
                    class="chapter-item"
                    :class="{ active: isActiveChapter(module.id, chapter.id) }"
                    @click="selectChapter(module.id, chapter.id)"
                  >
                    <span class="chapter-name">{{ chapter.name }}</span>
                    <div class="chapter-ops" @click.stop>
                      <el-button
                        size="small"
                        :icon="Edit"
                        link
                        @click="openEditChapterDialog(chapter)"
                      />
                      <el-button
                        size="small"
                        type="danger"
                        :icon="Delete"
                        link
                        @click="handleDeleteChapter(chapter, module.id)"
                      />
                    </div>
                  </div>
                </template>
              </div>

              <!-- 右侧 Markdown 预览 -->
              <div class="chapter-preview">
                <div v-if="!previewLoaded" class="preview-loading">加载中...</div>
                <template v-else>
                  <MdPreview
                    v-if="getActiveChapter(module.id)"
                    :modelValue="getActiveChapter(module.id).content || ''"
                  />
                  <el-empty v-else description="请选择或新增章节" :image-size="80" />
                </template>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- 新增模块弹窗 -->
    <el-dialog v-model="moduleDialogVisible" title="新增模块" width="500px">
      <el-form :model="moduleForm" label-width="80px">
        <el-form-item label="模块格式">
          <el-radio-group v-model="moduleForm.format">
            <el-radio value="command">命令库</el-radio>
            <el-radio value="gitbook">文档库</el-radio>
          </el-radio-group>
        </el-form-item>
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

    <!-- 新增/编辑章节弹窗（gitbook） -->
    <el-dialog
      v-model="chapterDialogVisible"
      :title="chapterDialogMode === 'create' ? '新增章节' : '编辑章节'"
      width="720px"
    >
      <el-form :model="chapterForm" label-width="80px">
        <el-form-item label="章节标题">
          <el-input v-model="chapterForm.name" placeholder="请输入章节标题" />
        </el-form-item>
        <el-form-item label="章节内容">
          <el-input
            v-model="chapterForm.content"
            type="textarea"
            :rows="16"
            placeholder="请输入 Markdown 内容"
            resize="vertical"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chapterDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChapter">确定</el-button>
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

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
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
  display: flex;
  gap: 8px;
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

/* GitBook 文档模块样式 */
.gitbook-body {
  padding: 12px 0;
}

.gitbook-content {
  display: flex;
  gap: 12px;
  height: 480px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #fff;
  overflow: hidden;
}

.chapter-list {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid #ebeef5;
  overflow-y: auto;
  padding: 8px 0;
  background-color: #fafafa;
}

.chapter-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: background-color 0.2s;
  gap: 4px;
}

.chapter-item:hover {
  background-color: #ecf5ff;
}

.chapter-item.active {
  background-color: #ecf5ff;
  border-left-color: #409eff;
}

.chapter-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #303133;
}

.chapter-ops {
  display: flex;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.chapter-item:hover .chapter-ops,
.chapter-item.active .chapter-ops {
  opacity: 1;
}

.chapter-preview {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 16px 20px;
}

.preview-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}
</style>
