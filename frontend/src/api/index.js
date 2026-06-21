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
