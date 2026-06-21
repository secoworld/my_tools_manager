import { ref, onUnmounted } from 'vue'

/**
 * 拖拽分割条 composable
 * 用于在两个区域之间拖拽调整大小比例
 *
 * @param {number} initialRatio 初始比例（0~1），默认 0.5
 * @returns {object} { ratio, isDragging, startDrag, resetSplit }
 */
export function useSplitter(initialRatio = 0.5) {
  const ratio = ref(initialRatio)
  const isDragging = ref(false)

  let containerEl = null
  let isVertical = false // true = 上下分割, false = 左右分割

  const onMouseMove = (e) => {
    if (!isDragging.value || !containerEl) return

    const rect = containerEl.getBoundingClientRect()
    let newRatio

    if (isVertical) {
      // 上下布局：按 Y 轴计算
      const offsetY = e.clientY - rect.top
      newRatio = offsetY / rect.height
    } else {
      // 左右布局：按 X 轴计算
      const offsetX = e.clientX - rect.left
      newRatio = offsetX / rect.width
    }

    // 限制在 10%~90% 之间
    ratio.value = Math.max(0.1, Math.min(0.9, newRatio))
  }

  const onMouseUp = () => {
    isDragging.value = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }

  /**
   * 开始拖拽
   * @param {MouseEvent} e 鼠标事件
   * @param {HTMLElement} container 容器元素（用于计算比例）
   * @param {boolean} vertical 是否为垂直（上下）分割
   */
  const startDrag = (e, container, vertical = false) => {
    e.preventDefault()
    e.stopPropagation()
    containerEl = container
    isVertical = vertical
    isDragging.value = true

    document.addEventListener('mousemove', onMouseMove)
    document.addEventListener('mouseup', onMouseUp)
    document.body.style.cursor = isVertical ? 'ns-resize' : 'ew-resize'
    document.body.style.userSelect = 'none'
  }

  const resetSplit = () => {
    ratio.value = initialRatio
  }

  onUnmounted(() => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  })

  return {
    ratio,
    isDragging,
    startDrag,
    resetSplit
  }
}
