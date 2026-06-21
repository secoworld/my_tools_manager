<script setup>
import { computed, ref } from 'vue'
import { Close, ArrowDown } from '@element-plus/icons-vue'
import { useWindowManagerStore } from '../stores/windowManager'
import Sidebar from '../components/Sidebar.vue'

const windowManager = useWindowManagerStore()

const windows = computed(() => windowManager.windows)
const activeWindowId = computed(() => windowManager.activeWindowId)

// ---- 右键菜单 ----
const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  instanceId: null
})

const openContextMenu = (e, instanceId) => {
  e.preventDefault()
  contextMenu.value = {
    visible: true,
    x: e.clientX,
    y: e.clientY,
    instanceId
  }
}

const closeContextMenu = () => {
  contextMenu.value.visible = false
}

// 关闭窗口
const handleClose = (instanceId) => {
  windowManager.closeWindow(instanceId)
}

// 切换窗口
const handleTabClick = (instanceId) => {
  windowManager.setActive(instanceId)
}

// ---- 标签管理操作 ----
const closeAll = () => {
  windowManager.closeAll()
  closeContextMenu()
}

const closeRight = () => {
  if (contextMenu.value.instanceId) {
    windowManager.closeRight(contextMenu.value.instanceId)
  }
  closeContextMenu()
}

const closeLeft = () => {
  if (contextMenu.value.instanceId) {
    windowManager.closeLeft(contextMenu.value.instanceId)
  }
  closeContextMenu()
}

const closeOthers = () => {
  if (contextMenu.value.instanceId) {
    windowManager.closeOthers(contextMenu.value.instanceId)
  }
  closeContextMenu()
}

// 下拉菜单操作（基于当前活动窗口）
const closeAllFromToolbar = () => {
  windowManager.closeAll()
}

const closeRightFromToolbar = () => {
  if (activeWindowId.value) windowManager.closeRight(activeWindowId.value)
}

const closeLeftFromToolbar = () => {
  if (activeWindowId.value) windowManager.closeLeft(activeWindowId.value)
}

const closeOthersFromToolbar = () => {
  if (activeWindowId.value) windowManager.closeOthers(activeWindowId.value)
}

// 全局点击关闭右键菜单
import { onMounted, onUnmounted } from 'vue'
onMounted(() => {
  document.addEventListener('click', closeContextMenu)
})
onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
})
</script>

<template>
  <div class="workspace">
    <Sidebar />
    <div class="main-area">
      <!-- Tab 标签栏 -->
      <div v-if="windows.length > 0" class="tab-bar">
        <div class="tab-list">
          <div
            v-for="win in windows"
            :key="win.instanceId"
            class="tab-item"
            :class="{ active: win.instanceId === activeWindowId }"
            @click="handleTabClick(win.instanceId)"
            @contextmenu="openContextMenu($event, win.instanceId)"
          >
            <span class="tab-name">{{ win.name }}</span>
            <el-icon class="tab-close" @click.stop="handleClose(win.instanceId)">
              <Close />
            </el-icon>
          </div>
        </div>
        <!-- 标签管理下拉菜单 -->
        <el-dropdown trigger="click" @command="(cmd) => {
          if (cmd === 'all') closeAllFromToolbar()
          else if (cmd === 'right') closeRightFromToolbar()
          else if (cmd === 'left') closeLeftFromToolbar()
          else if (cmd === 'others') closeOthersFromToolbar()
        }">
          <div class="tab-manager-btn" title="标签管理">
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="others" :disabled="windows.length <= 1">关闭其他标签</el-dropdown-item>
              <el-dropdown-item command="right" :disabled="windows.length <= 1">关闭右侧标签</el-dropdown-item>
              <el-dropdown-item command="left" :disabled="windows.length <= 1">关闭左侧标签</el-dropdown-item>
              <el-dropdown-item command="all" divided>全部关闭</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
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

    <!-- 右键菜单 -->
    <teleport to="body">
      <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        @click.stop
      >
        <div class="context-menu-item" @click="handleTabClick(contextMenu.instanceId); closeContextMenu()">
          切换到此标签
        </div>
        <div class="context-menu-item" @click="handleClose(contextMenu.instanceId); closeContextMenu()">
          关闭此标签
        </div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" @click="closeOthers" :class="{ disabled: windows.length <= 1 }">
          关闭其他标签
        </div>
        <div class="context-menu-item" @click="closeRight" :class="{ disabled: windows.length <= 1 }">
          关闭右侧标签
        </div>
        <div class="context-menu-item" @click="closeLeft" :class="{ disabled: windows.length <= 1 }">
          关闭左侧标签
        </div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item danger" @click="closeAll">
          全部关闭
        </div>
      </div>
    </teleport>
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
}

.tab-list {
  display: flex;
  overflow-x: auto;
  flex: 1;
  gap: 4px;
  align-items: center;
  height: 100%;
}

.tab-list::-webkit-scrollbar {
  height: 4px;
}

.tab-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 2px;
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
  flex-shrink: 0;
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

/* 标签管理按钮 */
.tab-manager-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  cursor: pointer;
  color: #606266;
  transition: all 0.2s;
  flex-shrink: 0;
}

.tab-manager-btn:hover {
  background-color: #f5f7fa;
  color: #409eff;
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

/* 右键菜单 */
.context-menu {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  padding: 4px 0;
  min-width: 140px;
  user-select: none;
}

.context-menu-item {
  padding: 8px 16px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  transition: background-color 0.2s;
}

.context-menu-item:hover {
  background-color: #ecf5ff;
  color: #409eff;
}

.context-menu-item.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

.context-menu-item.disabled:hover {
  background-color: transparent;
  color: #c0c4cc;
}

.context-menu-item.danger {
  color: #f56c6c;
}

.context-menu-item.danger:hover {
  background-color: #fef0f0;
  color: #f56c6c;
}

.context-menu-divider {
  height: 1px;
  background-color: #e4e7ed;
  margin: 4px 0;
}
</style>
