<script setup>
import { ref, watch } from 'vue'
import { pluginApi } from '../api'
import { Loading, WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  pluginId: {
    type: String,
    default: ''
  },
  pluginName: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:visible'])

const content = ref('')
const loading = ref(false)
const errorMsg = ref('')

const fetchContent = async (id) => {
  if (!id) return
  loading.value = true
  errorMsg.value = ''
  content.value = ''
  try {
    const html = await pluginApi.getContent(id)
    content.value = html || ''
  } catch (e) {
    errorMsg.value = '加载插件内容失败：' + (e.message || '网络错误')
  } finally {
    loading.value = false
  }
}

// 监听弹窗打开与插件 ID 变化
watch(
  () => [props.visible, props.pluginId],
  ([isVisible, id]) => {
    if (isVisible && id) {
      fetchContent(id)
    } else if (!isVisible) {
      content.value = ''
      errorMsg.value = ''
    }
  },
  { immediate: true }
)

const handleClose = () => {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="pluginName ? `插件预览 - ${pluginName}` : '插件预览'"
    width="80%"
    top="6vh"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="(v) => emit('update:visible', v)"
  >
    <div class="preview-wrapper" v-loading="loading">
      <div v-if="errorMsg" class="preview-error">
        <el-icon class="error-icon"><WarningFilled /></el-icon>
        <p>{{ errorMsg }}</p>
      </div>
      <iframe
        v-else
        class="preview-iframe"
        :srcdoc="content"
        sandbox="allow-scripts allow-same-origin"
      />
    </div>
    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.preview-wrapper {
  width: 100%;
  height: 70vh;
  background: #f5f7fa;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}

.preview-error {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #f56c6c;
}

.error-icon {
  font-size: 48px;
}

.preview-error p {
  font-size: 14px;
}
</style>
