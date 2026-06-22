<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { pluginApi } from '../api'
import { ElMessage } from 'element-plus'
import { Upload, ArrowRight, Check, WarningFilled, Close } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'success', 'close'])

const auth = useAuthStore()

// 当前步骤：1 选择文件 / 2 确认信息 / 3 上传结果
const step = ref(1)
const uploading = ref(false)

// 选中的文件
const selectedFile = ref(null)

// 上传结果
const uploadResult = ref(null) // { success, message }

// 文件大小格式化
const formatSize = (size) => {
  if (size == null) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(2) + ' MB'
}

// el-upload 自定义选择（不自动上传）
const handleFileChange = (file) => {
  // file.raw 是原生 File 对象，已包含 name 和 size
  selectedFile.value = file.raw
}

const handleFileRemove = () => {
  selectedFile.value = null
}

const handleExceed = () => {
  ElMessage.warning('只能选择一个文件，请先移除当前文件')
}

// 步骤控制
const goNext = () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择插件文件')
    return
  }
  step.value = 2
}

const goPrev = () => {
  step.value = 1
}

// 确认上传
const confirmUpload = async () => {
  uploading.value = true
  uploadResult.value = null
  try {
    const res = await pluginApi.upload(selectedFile.value, auth.token)
    if (res.success) {
      uploadResult.value = { success: true, message: res.message || '插件上传成功' }
    } else {
      uploadResult.value = { success: false, message: res.message || '上传失败' }
    }
  } catch (e) {
    uploadResult.value = {
      success: false,
      message: '上传请求失败：' + (e.message || '网络错误')
    }
  } finally {
    uploading.value = false
    step.value = 3
  }
}

// 完成
const handleFinish = () => {
  if (uploadResult.value && uploadResult.value.success) {
    emit('success')
  }
  closeDialog()
}

// 关闭弹窗
const closeDialog = () => {
  emit('update:visible', false)
  emit('close')
}

// 弹窗关闭时重置状态
const handleClosed = () => {
  step.value = 1
  selectedFile.value = null
  uploadResult.value = null
  uploading.value = false
}

const canNext = computed(() => !!selectedFile.value)
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="上传插件"
    width="560px"
    :close-on-click-modal="false"
    @update:model-value="(v) => emit('update:visible', v)"
    @closed="handleClosed"
  >
    <!-- 步骤指示器 -->
    <div class="steps-wrapper">
      <el-steps :active="step - 1" align-center finish-status="success">
        <el-step title="选择文件" />
        <el-step title="确认信息" />
        <el-step title="上传结果" />
      </el-steps>
    </div>

    <!-- 步骤 1：选择文件 -->
    <div v-if="step === 1" class="step-content">
      <el-upload
        drag
        accept=".zip"
        :limit="1"
        :auto-upload="false"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :on-exceed="handleExceed"
      >
        <el-icon class="upload-icon"><Upload /></el-icon>
        <div class="upload-text">将插件文件拖到此处，或点击上传</div>
        <template #tip>
          <div class="upload-tip">仅支持 .zip 格式的插件包</div>
        </template>
      </el-upload>
    </div>

    <!-- 步骤 2：确认信息 -->
    <div v-else-if="step === 2" class="step-content">
      <div class="file-info-card">
        <div class="info-row">
          <span class="info-label">文件名称</span>
          <span class="info-value">{{ selectedFile?.name }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">文件大小</span>
          <span class="info-value">{{ formatSize(selectedFile?.size) }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">文件类型</span>
          <span class="info-value">{{ selectedFile?.name?.endsWith('.zip') ? 'ZIP 压缩包' : '未知' }}</span>
        </div>
      </div>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="插件校验将在服务端进行，上传后系统会自动解析 manifest.json 并校验插件结构。"
      />
    </div>

    <!-- 步骤 3：上传结果 -->
    <div v-else-if="step === 3" class="step-content">
      <div
        v-if="uploadResult && uploadResult.success"
        class="result-box success"
      >
        <el-icon class="result-icon"><Check /></el-icon>
        <p class="result-title">上传成功</p>
        <p class="result-desc">{{ uploadResult.message }}</p>
      </div>
      <div v-else class="result-box error">
        <el-icon class="result-icon"><WarningFilled /></el-icon>
        <p class="result-title">上传失败</p>
        <p class="result-desc">{{ uploadResult?.message || '未知错误' }}</p>
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <template v-if="step === 1">
          <el-button @click="closeDialog">取消</el-button>
          <el-button type="primary" :disabled="!canNext" @click="goNext">
            下一步
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </template>
        <template v-else-if="step === 2">
          <el-button @click="goPrev">上一步</el-button>
          <el-button
            type="primary"
            :loading="uploading"
            @click="confirmUpload"
          >
            确认上传
          </el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="handleFinish">完成</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.steps-wrapper {
  margin-bottom: 24px;
}

.step-content {
  min-height: 200px;
}

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

.file-info-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  padding: 6px 0;
  font-size: 14px;
}

.info-label {
  width: 80px;
  color: #909399;
  flex-shrink: 0;
}

.info-value {
  color: #303133;
  word-break: break-all;
}

.result-box {
  text-align: center;
  padding: 30px 20px;
}

.result-icon {
  font-size: 56px;
  margin-bottom: 12px;
}

.result-box.success .result-icon {
  color: #67c23a;
}

.result-box.error .result-icon {
  color: #f56c6c;
}

.result-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.result-desc {
  font-size: 13px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
