<script setup>
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'

const props = defineProps({
  instanceId: { type: String, required: true },
  pluginId: { type: String, default: '' }
})

// 优先使用 pluginId prop，否则从 instanceId 提取
const resolvedPluginId = props.pluginId || props.instanceId.replace(/-[^-]+-[^-]+$/, '')
const htmlContent = ref('')
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const response = await fetch(`/api/plugins/${resolvedPluginId}/content`)
    if (!response.ok) {
      throw new Error(`加载失败: ${response.status}`)
    }
    htmlContent.value = await response.text()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="plugin-renderer">
    <div v-if="loading" class="plugin-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    <div v-else-if="error" class="plugin-error">
      <el-empty :description="error" />
    </div>
    <iframe
      v-else
      :srcdoc="htmlContent"
      class="plugin-iframe"
      sandbox="allow-scripts allow-same-origin"
      referrerpolicy="no-referrer"
    />
  </div>
</template>

<style scoped>
.plugin-renderer {
  width: 100%;
  height: 100%;
}
.plugin-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
.plugin-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  color: #909399;
  font-size: 14px;
}
.plugin-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
