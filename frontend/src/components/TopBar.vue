<script setup>
import { useRoute, useRouter } from 'vue-router'
import { Tools, Document, Operation } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const goTo = (path) => {
  router.push(path)
}
</script>

<template>
  <div class="top-bar">
    <div class="top-bar-left">
      <el-icon class="logo-icon"><Tools /></el-icon>
      <span class="logo-text">工具管理器</span>
    </div>

    <div class="top-bar-nav">
      <div
        class="nav-item"
        :class="{ active: route.path === '/' }"
        @click="goTo('/')"
      >
        <el-icon><Tools /></el-icon>
        <span>常用工具箱</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: route.path === '/dashboard' }"
        @click="goTo('/dashboard')"
      >
        <el-icon><Operation /></el-icon>
        <span>仪表盘</span>
      </div>
      <div
        class="nav-item"
        :class="{ active: route.path === '/commands' }"
        @click="goTo('/commands')"
      >
        <el-icon><Document /></el-icon>
        <span>命令库</span>
      </div>
    </div>

    <div class="top-bar-right" />
  </div>
</template>

<style scoped>
.top-bar {
  height: 56px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 40px;
  flex-shrink: 0;
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  font-size: 24px;
  color: #409eff;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.top-bar-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  gap: 6px;
  transition: all 0.2s;
  user-select: none;
}

.nav-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.nav-item.active {
  background-color: #409eff;
  color: #fff;
}

.top-bar-right {
  flex: 1;
}
</style>
