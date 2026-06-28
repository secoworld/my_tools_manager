<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Refresh, DataLine, Key, Switch } from '@element-plus/icons-vue'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: { type: String, required: true }
})

const inputText = ref('')
const activeTab = ref('stats')
const hexMode = ref('toHex')
const hexSeparator = ref(' ')
const hexWith0x = ref(true)
const outputText = ref('')

useToolState(props.instanceId, { inputText, activeTab, hexMode, hexSeparator, hexWith0x, outputText })

const md5 = (string) => {
  function rotateLeft(lValue, iShiftBits) {
    return (lValue << iShiftBits) | (lValue >>> (32 - iShiftBits))
  }
  function addUnsigned(lX, lY) {
    let lX8 = (lX & 0x80000000)
    let lY8 = (lY & 0x80000000)
    let lX4 = (lX & 0x40000000)
    let lY4 = (lY & 0x40000000)
    let lResult = (lX & 0x3FFFFFFF) + (lY & 0x3FFFFFFF)
    if (lX4 & lY4) return (lResult ^ 0x80000000 ^ lX8 ^ lY8)
    if (lX4 | lY4) {
      if (lResult & 0x40000000) return (lResult ^ 0xC0000000 ^ lX8 ^ lY8)
      else return (lResult ^ 0x40000000 ^ lX8 ^ lY8)
    } else return (lResult ^ lX8 ^ lY8)
  }
  function F(x, y, z) { return (x & y) | ((~x) & z) }
  function G(x, y, z) { return (x & z) | (y & (~z)) }
  function H(x, y, z) { return (x ^ y ^ z) }
  function I(x, y, z) { return (y ^ (x | (~z))) }
  function FF(a, b, c, d, x, s, ac) {
    a = addUnsigned(a, addUnsigned(addUnsigned(F(b, c, d), x), ac))
    return addUnsigned(rotateLeft(a, s), b)
  }
  function GG(a, b, c, d, x, s, ac) {
    a = addUnsigned(a, addUnsigned(addUnsigned(G(b, c, d), x), ac))
    return addUnsigned(rotateLeft(a, s), b)
  }
  function HH(a, b, c, d, x, s, ac) {
    a = addUnsigned(a, addUnsigned(addUnsigned(H(b, c, d), x), ac))
    return addUnsigned(rotateLeft(a, s), b)
  }
  function II(a, b, c, d, x, s, ac) {
    a = addUnsigned(a, addUnsigned(addUnsigned(I(b, c, d), x), ac))
    return addUnsigned(rotateLeft(a, s), b)
  }
  function convertToWordArray(string) {
    let lWordCount
    let lMessageLength = string.length
    let lNumberOfWords_temp1 = lMessageLength + 8
    let lNumberOfWords_temp2 = (lNumberOfWords_temp1 - (lNumberOfWords_temp1 % 64)) / 64
    let lNumberOfWords = (lNumberOfWords_temp2 + 1) * 16
    let lWordArray = Array(lNumberOfWords - 1)
    let lBytePosition = 0
    let lByteCount = 0
    while (lByteCount < lMessageLength) {
      lWordCount = (lByteCount - (lByteCount % 4)) / 4
      lBytePosition = (lByteCount % 4) * 8
      lWordArray[lWordCount] = (lWordArray[lWordCount] | (string.charCodeAt(lByteCount) << lBytePosition))
      lByteCount++
    }
    lWordCount = (lByteCount - (lByteCount % 4)) / 4
    lBytePosition = (lByteCount % 4) * 8
    lWordArray[lWordCount] = lWordArray[lWordCount] | (0x80 << lBytePosition)
    lWordArray[lNumberOfWords - 2] = lMessageLength << 3
    lWordArray[lNumberOfWords - 1] = lMessageLength >>> 29
    return lWordArray
  }
  function wordToHex(lValue) {
    let wordToHexValue = '', wordToHexValue_temp = '', lByte, lCount
    for (lCount = 0; lCount <= 3; lCount++) {
      lByte = (lValue >>> (lCount * 8)) & 255
      wordToHexValue_temp = '0' + lByte.toString(16)
      wordToHexValue = wordToHexValue + wordToHexValue_temp.substr(wordToHexValue_temp.length - 2, 2)
    }
    return wordToHexValue
  }
  function utf8Encode(string) {
    string = string.replace(/\r\n/g, '\n')
    let utftext = ''
    for (let n = 0; n < string.length; n++) {
      let c = string.charCodeAt(n)
      if (c < 128) { utftext += String.fromCharCode(c) }
      else if ((c > 127) && (c < 2048)) {
        utftext += String.fromCharCode((c >> 6) | 192)
        utftext += String.fromCharCode((c & 63) | 128)
      } else {
        utftext += String.fromCharCode((c >> 12) | 224)
        utftext += String.fromCharCode(((c >> 6) & 63) | 128)
        utftext += String.fromCharCode((c & 63) | 128)
      }
    }
    return utftext
  }
  let x = []
  let k, AA, BB, CC, DD, a, b, c, d
  let S11 = 7, S12 = 12, S13 = 17, S14 = 22
  let S21 = 5, S22 = 9, S23 = 14, S24 = 20
  let S31 = 4, S32 = 11, S33 = 16, S34 = 23
  let S41 = 6, S42 = 10, S43 = 15, S44 = 21
  string = utf8Encode(string)
  x = convertToWordArray(string)
  a = 0x67452301; b = 0xEFCDAB89; c = 0x98BADCFE; d = 0x10325476
  for (k = 0; k < x.length; k += 16) {
    AA = a; BB = b; CC = c; DD = d
    a = FF(a, b, c, d, x[k + 0], S11, 0xD76AA478)
    d = FF(d, a, b, c, x[k + 1], S12, 0xE8C7B756)
    c = FF(c, d, a, b, x[k + 2], S13, 0x242070DB)
    b = FF(b, c, d, a, x[k + 3], S14, 0xC1BDCEEE)
    a = FF(a, b, c, d, x[k + 4], S11, 0xF57C0FAF)
    d = FF(d, a, b, c, x[k + 5], S12, 0x4787C62A)
    c = FF(c, d, a, b, x[k + 6], S13, 0xA8304613)
    b = FF(b, c, d, a, x[k + 7], S14, 0xFD469501)
    a = FF(a, b, c, d, x[k + 8], S11, 0x698098D8)
    d = FF(d, a, b, c, x[k + 9], S12, 0x8B44F7AF)
    c = FF(c, d, a, b, x[k + 10], S13, 0xFFFF5BB1)
    b = FF(b, c, d, a, x[k + 11], S14, 0x895CD7BE)
    a = FF(a, b, c, d, x[k + 12], S11, 0x6B901122)
    d = FF(d, a, b, c, x[k + 13], S12, 0xFD987193)
    c = FF(c, d, a, b, x[k + 14], S13, 0xA679438E)
    b = FF(b, c, d, a, x[k + 15], S14, 0x49B40821)
    a = GG(a, b, c, d, x[k + 1], S21, 0xF61E2562)
    d = GG(d, a, b, c, x[k + 6], S22, 0xC040B340)
    c = GG(c, d, a, b, x[k + 11], S23, 0x265E5A51)
    b = GG(b, c, d, a, x[k + 0], S24, 0xE9B6C7AA)
    a = GG(a, b, c, d, x[k + 5], S21, 0xD62F105D)
    d = GG(d, a, b, c, x[k + 10], S22, 0x02441453)
    c = GG(c, d, a, b, x[k + 15], S23, 0xD8A1E681)
    b = GG(b, c, d, a, x[k + 4], S24, 0xE7D3FBC8)
    a = GG(a, b, c, d, x[k + 9], S21, 0x21E1CDE6)
    d = GG(d, a, b, c, x[k + 14], S22, 0xC33707D6)
    c = GG(c, d, a, b, x[k + 3], S23, 0xF4D50D87)
    b = GG(b, c, d, a, x[k + 8], S24, 0x455A14ED)
    a = GG(a, b, c, d, x[k + 13], S21, 0xA9E3E905)
    d = GG(d, a, b, c, x[k + 2], S22, 0xFCEFA3F8)
    c = GG(c, d, a, b, x[k + 7], S23, 0x676F02D9)
    b = GG(b, c, d, a, x[k + 12], S24, 0x8D2A4C8A)
    a = HH(a, b, c, d, x[k + 5], S31, 0xFFFA3942)
    d = HH(d, a, b, c, x[k + 8], S32, 0x8771F681)
    c = HH(c, d, a, b, x[k + 11], S33, 0x6D9D6122)
    b = HH(b, c, d, a, x[k + 14], S34, 0xFDE5380C)
    a = HH(a, b, c, d, x[k + 1], S31, 0xA4BEEA44)
    d = HH(d, a, b, c, x[k + 4], S32, 0x4BDECFA9)
    c = HH(c, d, a, b, x[k + 7], S33, 0xF6BB4B60)
    b = HH(b, c, d, a, x[k + 10], S34, 0xBEBFBC70)
    a = HH(a, b, c, d, x[k + 13], S31, 0x289B7EC6)
    d = HH(d, a, b, c, x[k + 0], S32, 0xEAA127FA)
    c = HH(c, d, a, b, x[k + 3], S33, 0xD4EF3085)
    b = HH(b, c, d, a, x[k + 6], S34, 0x04881D05)
    a = HH(a, b, c, d, x[k + 9], S31, 0xD9D4D039)
    d = HH(d, a, b, c, x[k + 12], S32, 0xE6DB99E5)
    c = HH(c, d, a, b, x[k + 15], S33, 0x1FA27CF8)
    b = HH(b, c, d, a, x[k + 2], S34, 0xC4AC5665)
    a = II(a, b, c, d, x[k + 0], S41, 0xF4292244)
    d = II(d, a, b, c, x[k + 7], S42, 0x432AFF97)
    c = II(c, d, a, b, x[k + 14], S43, 0xAB9423A7)
    b = II(b, c, d, a, x[k + 5], S44, 0xFC93A039)
    a = II(a, b, c, d, x[k + 12], S41, 0x655B59C3)
    d = II(d, a, b, c, x[k + 3], S42, 0x8F0CCC92)
    c = II(c, d, a, b, x[k + 10], S43, 0xFFEFF47D)
    b = II(b, c, d, a, x[k + 1], S44, 0x85845DD1)
    a = II(a, b, c, d, x[k + 8], S41, 0x6FA87E4F)
    d = II(d, a, b, c, x[k + 15], S42, 0xFE2CE6E0)
    c = II(c, d, a, b, x[k + 6], S43, 0xA3014314)
    b = II(b, c, d, a, x[k + 13], S44, 0x4E0811A1)
    a = II(a, b, c, d, x[k + 4], S41, 0xF7537E82)
    d = II(d, a, b, c, x[k + 11], S42, 0xBD3AF235)
    c = II(c, d, a, b, x[k + 2], S43, 0x2AD7D2BB)
    b = II(b, c, d, a, x[k + 9], S44, 0xEB86D391)
    a = addUnsigned(a, AA)
    b = addUnsigned(b, BB)
    c = addUnsigned(c, CC)
    d = addUnsigned(d, DD)
  }
  return (wordToHex(a) + wordToHex(b) + wordToHex(c) + wordToHex(d)).toLowerCase()
}

const getByteSize = (str) => {
  let size = 0
  for (let i = 0; i < str.length; i++) {
    const charCode = str.charCodeAt(i)
    if (charCode < 0x80) {
      size += 1
    } else if (charCode < 0x800) {
      size += 2
    } else if (charCode < 0xd800 || charCode >= 0xe000) {
      size += 3
    } else {
      size += 4
    }
  }
  return size
}

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + units[i]
}

const stats = computed(() => {
  const text = inputText.value
  const charCount = text.length
  const byteCount = getByteSize(text)
  const lineCount = text ? text.split('\n').length : 0
  const wordCount = text ? text.trim().split(/\s+/).filter(w => w.length > 0).length : 0
  const chineseCount = (text.match(/[\u4e00-\u9fa5]/g) || []).length
  const numberCount = (text.match(/[0-9]/g) || []).length
  const letterCount = (text.match(/[a-zA-Z]/g) || []).length
  const spaceCount = (text.match(/\s/g) || []).length
  return {
    charCount,
    byteCount,
    byteSizeFormatted: formatSize(byteCount),
    lineCount,
    wordCount,
    chineseCount,
    numberCount,
    letterCount,
    spaceCount
  }
})

const md5Value = computed(() => {
  if (!inputText.value) return ''
  return md5(inputText.value)
})

const convertHex = () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入要转换的内容')
    return
  }
  if (hexMode.value === 'toHex') {
    const encoder = new TextEncoder()
    const bytes = encoder.encode(inputText.value)
    const hexParts = []
    for (let i = 0; i < bytes.length; i++) {
      let hex = bytes[i].toString(16).toUpperCase().padStart(2, '0')
      if (hexWith0x.value) {
        hex = '0x' + hex
      }
      hexParts.push(hex)
    }
    const sep = hexSeparator.value === '\\n' ? '\n' : hexSeparator.value
    outputText.value = hexParts.join(sep)
  } else {
    try {
      const clean = inputText.value.replace(/[\s,0x]+/gi, '')
      if (clean.length % 2 !== 0) {
        ElMessage.warning('16进制字符串长度必须为偶数')
        return
      }
      const bytes = new Uint8Array(clean.length / 2)
      for (let i = 0; i < clean.length; i += 2) {
        bytes[i / 2] = parseInt(clean.substr(i, 2), 16)
      }
      const decoder = new TextDecoder('utf-8')
      outputText.value = decoder.decode(bytes)
    } catch (e) {
      ElMessage.error('转换失败：无效的16进制字符串')
      return
    }
  }
  ElMessage.success('转换成功')
}

const clearAll = () => {
  inputText.value = ''
  outputText.value = ''
}

const copyResult = async (text, label) => {
  if (!text) { ElMessage.warning('没有可复制的内容'); return }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`已复制${label || ''}`)
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

const swapInputOutput = () => {
  const temp = inputText.value
  inputText.value = outputText.value
  outputText.value = temp
  hexMode.value = hexMode.value === 'toHex' ? 'fromHex' : 'toHex'
  ElMessage.success(`已交换输入/输出，模式切换为${hexMode.value === 'toHex' ? '文本转16进制' : '16进制转文本'}`)
}
</script>

<template>
  <div class="text-utils">
    <div class="input-section">
      <div class="label-row">
        <span class="label">输入文本</span>
        <el-button size="small" type="info" @click="clearAll">清空</el-button>
      </div>
      <el-input
        v-model="inputText"
        type="textarea"
        placeholder="在此输入文本..."
        :rows="6"
        resize="vertical"
      />
    </div>

    <el-tabs v-model="activeTab" class="tabs-section">
      <el-tab-pane label="文本统计" name="stats">
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-icon"><DataLine /></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.charCount }}</div>
              <div class="stat-label">字符数</div>
            </div>
            <el-button size="small" text @click="copyResult(String(stats.charCount), '字符数')"><CopyDocument /></el-button>
          </div>
          <div class="stat-item">
            <div class="stat-icon"><DataLine /></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.byteCount }}</div>
              <div class="stat-label">字节数 (UTF-8)</div>
            </div>
            <el-button size="small" text @click="copyResult(String(stats.byteCount), '字节数')"><CopyDocument /></el-button>
          </div>
          <div class="stat-item">
            <div class="stat-icon"><DataLine /></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.byteSizeFormatted }}</div>
              <div class="stat-label">占据大小</div>
            </div>
            <el-button size="small" text @click="copyResult(stats.byteSizeFormatted, '占据大小')"><CopyDocument /></el-button>
          </div>
          <div class="stat-item">
            <div class="stat-icon"><DataLine /></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.lineCount }}</div>
              <div class="stat-label">行数</div>
            </div>
            <el-button size="small" text @click="copyResult(String(stats.lineCount), '行数')"><CopyDocument /></el-button>
          </div>
          <div class="stat-item">
            <div class="stat-icon"><DataLine /></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.wordCount }}</div>
              <div class="stat-label">单词数</div>
            </div>
            <el-button size="small" text @click="copyResult(String(stats.wordCount), '单词数')"><CopyDocument /></el-button>
          </div>
          <div class="stat-item">
            <div class="stat-icon"><DataLine /></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.chineseCount }}</div>
              <div class="stat-label">中文字符</div>
            </div>
            <el-button size="small" text @click="copyResult(String(stats.chineseCount), '中文字符数')"><CopyDocument /></el-button>
          </div>
          <div class="stat-item">
            <div class="stat-icon"><DataLine /></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.letterCount }}</div>
              <div class="stat-label">英文字母</div>
            </div>
            <el-button size="small" text @click="copyResult(String(stats.letterCount), '英文字母数')"><CopyDocument /></el-button>
          </div>
          <div class="stat-item">
            <div class="stat-icon"><DataLine /></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.numberCount }}</div>
              <div class="stat-label">数字字符</div>
            </div>
            <el-button size="small" text @click="copyResult(String(stats.numberCount), '数字字符数')"><CopyDocument /></el-button>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="MD5" name="md5">
        <div class="md5-section">
          <div class="md5-header">
            <span class="md5-label"><el-icon :size="16" style="margin-right: 6px; vertical-align: middle;"><Key /></el-icon> MD5 哈希值</span>
            <el-button size="small" :icon="CopyDocument" @click="copyResult(md5Value, 'MD5值')">复制</el-button>
          </div>
          <div class="md5-value">{{ md5Value || '请在上方输入文本' }}</div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="16进制转换" name="hex">
        <div class="hex-section">
          <div class="hex-toolbar">
            <el-radio-group v-model="hexMode" size="small">
              <el-radio value="toHex">文本转16进制</el-radio>
              <el-radio value="fromHex">16进制转文本</el-radio>
            </el-radio-group>
            <el-select v-if="hexMode === 'toHex'" v-model="hexSeparator" size="small" style="width: 120px">
              <el-option label="空格分隔" value=" " />
              <el-option label="逗号分隔" value="," />
              <el-option label="无分隔" value="" />
              <el-option label="换行分隔" value="\n" />
            </el-select>
            <el-checkbox v-if="hexMode === 'toHex'" v-model="hexWith0x" size="small">0x前缀</el-checkbox>
            <el-button type="primary" size="small" @click="convertHex">转换</el-button>
            <el-button type="warning" size="small" :icon="Switch" @click="swapInputOutput">交换</el-button>
          </div>
          <div class="hex-output">
            <div class="label-row">
              <span class="label">输出结果</span>
              <el-button size="small" :icon="CopyDocument" @click="copyResult(outputText, '转换结果')">复制</el-button>
            </div>
            <el-input
              v-model="outputText"
              type="textarea"
              placeholder="转换结果将显示在这里"
              :rows="6"
              readonly
              resize="vertical"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.text-utils {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-section {
  flex-shrink: 0;
}

.label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.label {
  font-weight: 600;
  color: #606266;
}

.tabs-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.tabs-section :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.tabs-section :deep(.el-tab-pane) {
  height: 100%;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  padding: 4px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  transition: all 0.2s;
}

.stat-item:hover {
  background: #ecf5ff;
  border-color: #b3d8ff;
}

.stat-icon {
  font-size: 22px;
  color: #409eff;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  font-family: 'Consolas', 'Monaco', monospace;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.md5-section {
  padding: 20px;
}

.md5-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.md5-label {
  font-weight: 600;
  color: #606266;
  font-size: 14px;
}

.md5-value {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  color: #409eff;
  word-break: break-all;
  border: 1px solid #ebeef5;
  min-height: 60px;
}

.hex-section {
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hex-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hex-output {
  flex: 1;
  min-height: 0;
}
</style>
