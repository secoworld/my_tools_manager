import { defineStore } from 'pinia'
import api from '../api'

export const useCommandStore = defineStore('commandStore', {
  state: () => ({
    modules: [],
    currentModuleId: null,
    commands: {} // 以 moduleId 为键存储命令列表
  }),

  actions: {
    // 获取模块列表
    async fetchModules() {
      try {
        const data = await api.getModules()
        this.modules = Array.isArray(data) ? data : data.data || []
        return this.modules
      } catch (error) {
        console.error('获取模块列表失败:', error)
        this.modules = []
        return []
      }
    },

    // 新增模块
    async addModule(data) {
      await api.createModule(data)
      await this.fetchModules()
    },

    // 删除模块
    async removeModule(id) {
      await api.deleteModule(id)
      // 清理本地命令缓存
      delete this.commands[id]
      await this.fetchModules()
    },

    // 更新模块
    async updateModule(id, data) {
      await api.updateModule(id, data)
      await this.fetchModules()
    },

    // 获取模块下的命令
    async fetchCommands(moduleId) {
      try {
        const data = await api.getCommands(moduleId)
        const list = Array.isArray(data) ? data : data.data || []
        this.commands[moduleId] = list
        this.currentModuleId = moduleId
        return list
      } catch (error) {
        console.error('获取命令列表失败:', error)
        this.commands[moduleId] = []
        return []
      }
    },

    // 新增命令
    async addCommand(data) {
      await api.createCommand(data)
      if (data.moduleId) {
        await this.fetchCommands(data.moduleId)
      }
    },

    // 更新命令
    async updateCommand(id, data) {
      await api.updateCommand(id, data)
      if (data.moduleId) {
        await this.fetchCommands(data.moduleId)
      }
    },

    // 删除命令
    async removeCommand(id, moduleId) {
      await api.deleteCommand(id)
      if (moduleId) {
        await this.fetchCommands(moduleId)
      }
    }
  }
})
