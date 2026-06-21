// 工具注册表
// 每个工具包含: id, name, icon, category, needBackend, component
import { defineAsyncComponent } from 'vue'

export const toolRegistry = {
  'json-formatter': {
    id: 'json-formatter',
    name: 'JSON格式化',
    icon: 'Document',
    category: '格式化',
    needBackend: false,
    component: defineAsyncComponent(() => import('./JsonFormatter.vue'))
  },
  'base64-converter': {
    id: 'base64-converter',
    name: 'Base64转换',
    icon: 'Lock',
    category: '编码',
    needBackend: true,
    component: defineAsyncComponent(() => import('./Base64Converter.vue'))
  },
  'timestamp-converter': {
    id: 'timestamp-converter',
    name: '时间戳转换',
    icon: 'Clock',
    category: '转换',
    needBackend: true,
    component: defineAsyncComponent(() => import('./TimestampConverter.vue'))
  },
  'text-editor': {
    id: 'text-editor',
    name: '文本编辑器',
    icon: 'EditPen',
    category: '记录',
    needBackend: false,
    component: defineAsyncComponent(() => import('./TextEditor.vue'))
  },
  'storage-converter': {
    id: 'storage-converter',
    name: '存储单位换算',
    icon: 'Coin',
    category: '转换',
    needBackend: false,
    component: defineAsyncComponent(() => import('./StorageConverter.vue'))
  }
}

// 获取工具列表（数组形式）
export const getToolList = () => {
  return Object.values(toolRegistry)
}
