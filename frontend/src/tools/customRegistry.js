import { ref } from 'vue'
import PluginRenderer from '../components/PluginRenderer.vue'

const customPlugins = ref([])

async function loadCustomPlugins() {
  try {
    const response = await fetch('/api/plugins')
    if (!response.ok) return
    const plugins = await response.json()
    customPlugins.value = plugins.map(p => ({
      id: p.pluginId,
      name: p.name,
      icon: p.icon || 'Tools',
      category: p.category || '自定义',
      needBackend: p.needBackend || false,
      visibility: p.visibility,
      isCustom: true,
      component: PluginRenderer
    }))
  } catch (e) {
    console.warn('加载自定义插件失败:', e)
  }
}

function getCustomPlugins(visibility) {
  if (!visibility) {
    return customPlugins.value
  }
  return customPlugins.value.filter(p =>
    p.visibility === 'ALL' || p.visibility === visibility
  )
}

export { customPlugins, loadCustomPlugins, getCustomPlugins }
