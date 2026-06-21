<script setup>
import { computed } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { useWindowManagerStore } from '../stores/windowManager'
import Sidebar from '../components/Sidebar.vue'

const windowManager = useWindowManagerStore()

const windows = computed(() => windowManager.windows)
const activeWindowId = computed(() => windowManager.activeWindowId)

// 关闭窗口
const handleClose = (instanceId) => {
  windowManager.closeWindow(instanceId)
}

// 切换窗口
const handleTabClick = (instanceId) => {
  windowManager.setActive(instanceId)
}
</script>

<template>
  <div class="workspace">
    <Sidebar />
    <div class="main-area">
      <!-- Tab 标签栏 -->
      <div v-if="windows.length > 0" class="tab-bar">
        <div
          v-for="win in windows"
          :key="win.instanceId"
          class="tab-item"
          :class="{ active: win.instanceId === activeWindowId }"
          @click="handleTabClick(win.instanceId)"
        >
          <span class="tab-name">{{ win.name }}</span>
          <el-icon class="tab-close" @click.stop="handleClose(win.instanceId)">
            <Close />
          </el-icon>
        </div>
      </div>

      <!-- 内容区 -->
      <div class="content-area">
        <template v-if="windows.length > 0">
          <div
            v-for="win in windows"
            :key="win.instanceId"
            v-show="win.instanceId === activeWindowId"
            class="window-pane"
          >
            <component :is="win.component" :instance-id="win.instanceId" />
          </div>
        </template>
        <!-- 空状态 -->
        <div v-else class="empty-state">
          <el-empty description="请从左侧选择工具" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.workspace {
  display: flex;
  height: 100%;
  width: 100%;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background-color: #f5f7fa;
}

.tab-bar {
  display: flex;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 8px;
  height: 40px;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
}

.tab-item {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  gap: 6px;
  transition: all 0.2s;
}

.tab-item:hover {
  background-color: #f5f7fa;
}

.tab-item.active {
  background-color: #409eff;
  color: #fff;
}

.tab-item.active .tab-close:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.tab-name {
  user-select: none;
}

.tab-close {
  font-size: 12px;
  border-radius: 50%;
  padding: 2px;
  transition: background-color 0.2s;
}

.tab-close:hover {
  background-color: #e4e7ed;
}

.content-area {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.window-pane {
  width: 100%;
  height: 100%;
  background-color: #fff;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
