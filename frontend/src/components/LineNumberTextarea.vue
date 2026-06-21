<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  showLineNumbers: { type: Boolean, default: false },
  fontSize: { type: Number, default: 14 },
  placeholder: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const textareaRef = ref(null)
const lineNumbersRef = ref(null)

// 行号列表
const lineCount = computed(() => {
  if (!props.modelValue) return 1
  const count = props.modelValue.split('\n').length
  return count
})

const lineNumbers = computed(() => {
  const arr = []
  for (let i = 1; i <= lineCount.value; i++) {
    arr.push(i)
  }
  return arr
})

// 同步行号滚动
const onScroll = () => {
  if (lineNumbersRef.value && textareaRef.value) {
    lineNumbersRef.value.scrollTop = textareaRef.value.scrollTop
  }
}

// 输入事件
const onInput = (e) => {
  emit('update:modelValue', e.target.value)
}

// 字号变化时重新同步
watch(() => props.fontSize, () => {
  nextTick(onScroll)
})

onMounted(onScroll)
</script>

<template>
  <div class="line-number-textarea" :class="{ 'show-line-numbers': showLineNumbers }">
    <!-- 行号栏 -->
    <div v-if="showLineNumbers" ref="lineNumbersRef" class="line-numbers" :style="{ fontSize: fontSize + 'px' }">
      <div v-for="n in lineNumbers" :key="n" class="line-number">{{ n }}</div>
    </div>
    <!-- 文本输入 -->
    <textarea
      ref="textareaRef"
      :value="modelValue"
      :placeholder="placeholder"
      class="text-area"
      :style="{ fontSize: fontSize + 'px' }"
      @input="onInput"
      @scroll="onScroll"
      spellcheck="false"
    />
  </div>
</template>

<style scoped>
.line-number-textarea {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.line-numbers {
  flex-shrink: 0;
  width: 42px;
  padding: 8px 6px 8px 0;
  text-align: right;
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  overflow: hidden;
  font-family: 'Consolas', 'Monaco', monospace;
  line-height: 1.6;
  color: #909399;
  user-select: none;
}

.line-number {
  height: calc(1em * 1.6);
}

.text-area {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  padding: 8px 12px;
  font-family: 'Consolas', 'Monaco', monospace;
  line-height: 1.6;
  color: #303133;
  background: transparent;
  width: 100%;
  height: 100%;
}

.text-area::placeholder {
  color: #c0c4cc;
}

.text-area::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.text-area::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 4px;
}

.text-area::-webkit-scrollbar-track {
  background: #f5f7fa;
}
</style>
