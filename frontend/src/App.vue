<script setup>
import TopBar from './components/TopBar.vue'
</script>

<template>
  <div class="app-layout">
    <TopBar />
    <div class="app-content">
      <router-view />
    </div>
  </div>
</template>

<style>
#app {
  width: 100%;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue',
    Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #303133;
}

.app-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.app-content {
  flex: 1;
  overflow: hidden;
}
</style>
