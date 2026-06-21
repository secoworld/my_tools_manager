import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 从环境变量读取配置（支持 config.bat 统一配置）
const FRONTEND_PORT = process.env.FRONTEND_PORT || 5173
const BACKEND_PORT = process.env.BACKEND_PORT || 8080

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: Number(FRONTEND_PORT),
    proxy: {
      '/api': {
        target: `http://localhost:${BACKEND_PORT}`,
        changeOrigin: true
      }
    }
  }
})
