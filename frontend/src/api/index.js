import axios from 'axios'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 工具相关 API
export const getTools = () => request.get('/tools')

export const executeTool = (toolId, params) =>
  request.post(`/tools/${toolId}/execute`, params)

// 命令模块相关 API
export const getModules = () => request.get('/commands/modules')

export const createModule = (data) => request.post('/commands/modules', data)

export const deleteModule = (id) => request.delete(`/commands/modules/${id}`)

// 命令相关 API
export const getCommands = (moduleId) =>
  request.get(`/commands/module/${moduleId}`)

export const createCommand = (data) => request.post('/commands/', data)

export const updateCommand = (id, data) => request.put(`/commands/${id}`, data)

export const deleteCommand = (id) => request.delete(`/commands/${id}`)

export default {
  getTools,
  executeTool,
  getModules,
  createModule,
  deleteModule,
  getCommands,
  createCommand,
  updateCommand,
  deleteCommand
}

// ========== 认证 API ==========

const ADMIN_BASE = '/api/admin'

export const authApi = {
  login(username, password) {
    return fetch(`${ADMIN_BASE}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    }).then(res => res.json())
  },
  changePassword(oldPassword, newPassword, token) {
    return fetch(`${ADMIN_BASE}/change-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ oldPassword, newPassword })
    }).then(async res => {
      if (res.status === 401) {
        localStorage.removeItem('admin-token')
        window.dispatchEvent(new CustomEvent('auth-expired'))
        return { success: false, message: '登录已过期，请重新登录' }
      }
      const text = await res.text()
      try {
        return JSON.parse(text)
      } catch {
        return { success: false, message: `服务器返回了非JSON响应 (HTTP ${res.status})` }
      }
    })
  },
  logout(token) {
    return fetch(`${ADMIN_BASE}/logout`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    }).then(res => res.json())
  },
  profile(token) {
    return fetch(`${ADMIN_BASE}/profile`, {
      headers: { 'Authorization': `Bearer ${token}` }
    }).then(async res => {
      if (res.status === 401) {
        localStorage.removeItem('admin-token')
        window.dispatchEvent(new CustomEvent('auth-expired'))
        return { success: false, message: '登录已过期' }
      }
      return res.json()
    })
  }
}

// ========== 插件管理 API ==========

const PLUGIN_BASE = '/api/plugins'

// 带认证的 fetch 封装，自动处理 401 过期
function fetchWithAuth(url, options = {}) {
  const token = localStorage.getItem('admin-token')
  const headers = { ...options.headers }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return fetch(url, { ...options, headers }).then(async res => {
    if (res.status === 401) {
      // Token 过期，清除并触发全局事件
      localStorage.removeItem('admin-token')
      window.dispatchEvent(new CustomEvent('auth-expired'))
      throw new Error('登录已过期')
    }
    const text = await res.text()
    try {
      return JSON.parse(text)
    } catch {
      return { success: false, message: `服务器返回了非JSON响应 (HTTP ${res.status})` }
    }
  })
}

export const pluginApi = {
  getEnabled() {
    return fetch(PLUGIN_BASE).then(res => res.json())
  },
  getAll(token) {
    return fetchWithAuth(`${PLUGIN_BASE}/all`)
  },
  getContent(pluginId) {
    return fetch(`${PLUGIN_BASE}/${pluginId}/content`).then(res => res.text())
  },
  upload(file, token, force = false) {
    const formData = new FormData()
    formData.append('file', file)
    const url = force ? `${PLUGIN_BASE}/upload?force=true` : `${PLUGIN_BASE}/upload`
    return fetchWithAuth(url, {
      method: 'POST',
      body: formData
    })
  },
  delete(pluginId, token) {
    return fetchWithAuth(`${PLUGIN_BASE}/${pluginId}`, {
      method: 'DELETE'
    })
  },
  enable(pluginId, token) {
    return fetchWithAuth(`${PLUGIN_BASE}/${pluginId}/enable`, {
      method: 'PUT'
    })
  },
  disable(pluginId, token) {
    return fetchWithAuth(`${PLUGIN_BASE}/${pluginId}/disable`, {
      method: 'PUT'
    })
  },
  updateVisibility(pluginId, visibility, token) {
    return fetchWithAuth(`${PLUGIN_BASE}/${pluginId}/visibility`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ visibility })
    })
  }
}
