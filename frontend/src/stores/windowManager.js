import { defineStore } from 'pinia'
import { markRaw } from 'vue'
import { toolRegistry } from '../tools/registry'

// 生成随机字符串
const generateRandomStr = (len = 6) => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  for (let i = 0; i < len; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

export const useWindowManagerStore = defineStore('windowManager', {
  state: () => ({
    windows: [],
    activeWindowId: null
  }),

  getters: {
    activeWindow(state) {
      return state.windows.find((w) => w.instanceId === state.activeWindowId) || null
    }
  },

  actions: {
    // 打开工具，生成唯一实例 ID
    openTool(toolId) {
      const tool = toolRegistry[toolId]
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
        // 使用 markRaw 防止组件被响应式系统 Proxy 包裹，否则 Vue 无法识别为组件
        component: markRaw(tool.component)
      })

      this.activeWindowId = instanceId
    },

    // 关闭窗口
    closeWindow(id) {
      const index = this.windows.findIndex((w) => w.instanceId === id)
      if (index === -1) return

      this.windows.splice(index, 1)

      // 更新 activeWindowId
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

    // 设置活动窗口
    setActive(id) {
      const exists = this.windows.some((w) => w.instanceId === id)
      if (exists) {
        this.activeWindowId = id
      }
    }
  }
})
