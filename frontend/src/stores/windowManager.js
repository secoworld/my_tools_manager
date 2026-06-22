import { defineStore } from 'pinia'
import { markRaw } from 'vue'
import { toolRegistry } from '../tools/registry'
import { customPlugins } from '../tools/customRegistry'

// 查找工具：先从内置注册表，再从自定义插件
const findTool = (toolId) => {
  let tool = toolRegistry[toolId]
  if (!tool) {
    tool = customPlugins.value.find(p => p.id === toolId)
  }
  return tool
}

const STORAGE_KEY = 'tools-manager-workspace-tabs'

// 生成随机字符串
const generateRandomStr = (len = 6) => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  for (let i = 0; i < len; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

// 从 localStorage 恢复标签页
const loadTabs = () => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (!saved) return { windows: [], activeWindowId: null }
    const data = JSON.parse(saved)
    const windows = (data.windows || [])
      .map(w => {
        const tool = findTool(w.toolId)
        if (!tool) return null
        return {
          instanceId: w.instanceId,
          toolId: w.toolId,
          name: w.name,
          icon: w.icon,
          category: w.category,
          needBackend: w.needBackend,
          component: markRaw(tool.component)
        }
      })
      .filter(Boolean)
    const activeWindowId = data.activeWindowId && windows.find(w => w.instanceId === data.activeWindowId)
      ? data.activeWindowId
      : (windows.length > 0 ? windows[0].instanceId : null)
    return { windows, activeWindowId }
  } catch {
    return { windows: [], activeWindowId: null }
  }
}

// 保存标签页到 localStorage（排除不可序列化的 component）
export const saveTabs = (state) => {
  try {
    const serializable = {
      windows: state.windows.map(w => ({
        instanceId: w.instanceId,
        toolId: w.toolId,
        name: w.name,
        icon: w.icon,
        category: w.category,
        needBackend: w.needBackend
      })),
      activeWindowId: state.activeWindowId
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(serializable))
  } catch {
    // 静默失败
  }
}

const saved = loadTabs()

export const useWindowManagerStore = defineStore('windowManager', {
  state: () => ({
    windows: saved.windows,
    activeWindowId: saved.activeWindowId
  }),

  getters: {
    activeWindow(state) {
      return state.windows.find((w) => w.instanceId === state.activeWindowId) || null
    }
  },

  actions: {
    openTool(toolId) {
      const tool = findTool(toolId)
      if (!tool) {
        console.warn(`工具 ${toolId} 未在注册表中找到`)
        return
      }

      const instanceId = `${toolId}-${Date.now()}-${generateRandomStr()}`

      this.windows.push({
        instanceId,
        toolId,
        name: tool.name,
        icon: tool.icon,
        category: tool.category,
        needBackend: tool.needBackend,
        component: markRaw(tool.component)
      })

      this.activeWindowId = instanceId
    },

    closeWindow(id) {
      const index = this.windows.findIndex((w) => w.instanceId === id)
      if (index === -1) return

      this.windows.splice(index, 1)

      if (this.activeWindowId === id) {
        if (this.windows.length === 0) {
          this.activeWindowId = null
        } else if (index >= this.windows.length) {
          this.activeWindowId = this.windows[this.windows.length - 1].instanceId
        } else {
          this.activeWindowId = this.windows[index].instanceId
        }
      }
    },

    setActive(id) {
      const exists = this.windows.some((w) => w.instanceId === id)
      if (exists) {
        this.activeWindowId = id
      }
    },

    closeAll() {
      this.windows = []
      this.activeWindowId = null
    },

    closeRight(instanceId) {
      const index = this.windows.findIndex((w) => w.instanceId === instanceId)
      if (index === -1) return
      this.windows = this.windows.slice(0, index + 1)
      if (!this.windows.find((w) => w.instanceId === this.activeWindowId)) {
        this.activeWindowId = this.windows.length > 0 ? this.windows[this.windows.length - 1].instanceId : null
      }
    },

    closeLeft(instanceId) {
      const index = this.windows.findIndex((w) => w.instanceId === instanceId)
      if (index === -1) return
      this.windows = this.windows.slice(index)
      if (!this.windows.find((w) => w.instanceId === this.activeWindowId)) {
        this.activeWindowId = this.windows.length > 0 ? this.windows[0].instanceId : null
      }
    },

    closeOthers(instanceId) {
      this.windows = this.windows.filter((w) => w.instanceId === instanceId)
      this.activeWindowId = instanceId
    }
  }
})
