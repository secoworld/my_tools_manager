import { ref } from 'vue'

/**
 * 缩放和全屏 composable
 * 为工具的输入/输出区域提供字体缩放和全屏查看功能
 *
 * @param {number} defaultFontSize 默认字体大小
 * @returns {object} { fontSize, isFullscreen, zoomIn, zoomOut, resetZoom, toggleFullscreen, exitFullscreen }
 */
export function useZoomFullscreen(defaultFontSize = 13) {
  const fontSize = ref(defaultFontSize)
  const isFullscreen = ref(false)

  const zoomIn = () => {
    fontSize.value = Math.min(36, fontSize.value + 2)
  }

  const zoomOut = () => {
    fontSize.value = Math.max(10, fontSize.value - 2)
  }

  const resetZoom = () => {
    fontSize.value = defaultFontSize
  }

  const toggleFullscreen = () => {
    isFullscreen.value = !isFullscreen.value
  }

  const exitFullscreen = () => {
    isFullscreen.value = false
  }

  return {
    fontSize,
    isFullscreen,
    zoomIn,
    zoomOut,
    resetZoom,
    toggleFullscreen,
    exitFullscreen
  }
}
