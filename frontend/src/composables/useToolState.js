import { watch, onMounted } from 'vue'

/**
 * 工具状态持久化 composable
 * 根据 instanceId 将工具的输入内容缓存到 localStorage，
 * 刷新页面后自动恢复。
 *
 * @param {string} instanceId - 工具实例 ID（由 Dashboard 分配）
 * @param {Object} stateRefs - 需要持久化的响应式引用对象 { key: ref }
 * @param {string} prefix - localStorage key 前缀
 */
export function useToolState(instanceId, stateRefs, prefix = 'tool-state') {
  const storageKey = `${prefix}:${instanceId}`

  // 挂载时恢复状态
  onMounted(() => {
    try {
      const saved = localStorage.getItem(storageKey)
      if (saved) {
        const data = JSON.parse(saved)
        Object.keys(stateRefs).forEach((key) => {
          if (data[key] !== undefined && stateRefs[key]) {
            stateRefs[key].value = data[key]
          }
        })
      }
    } catch (e) {
      console.warn('恢复工具状态失败:', e)
    }
  })

  // 监听变化并保存
  watch(
    () => Object.keys(stateRefs).map((k) => stateRefs[k].value),
    () => {
      try {
        const data = {}
        Object.keys(stateRefs).forEach((key) => {
          data[key] = stateRefs[key].value
        })
        localStorage.setItem(storageKey, JSON.stringify(data))
      } catch (e) {
        console.warn('保存工具状态失败:', e)
      }
    },
    { deep: true }
  )
}

/**
 * 清除指定实例的工具状态缓存
 */
export function clearToolState(instanceId, prefix = 'tool-state') {
  localStorage.removeItem(`${prefix}:${instanceId}`)
}

/**
 * 清除所有工具状态缓存
 */
export function clearAllToolState(prefix = 'tool-state') {
  const keysToRemove = []
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key && key.startsWith(prefix + ':')) {
      keysToRemove.push(key)
    }
  }
  keysToRemove.forEach((key) => localStorage.removeItem(key))
}
