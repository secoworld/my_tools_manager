<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { Lock, ArrowRight, CircleCheck, CircleClose } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateNotSame = (rule, value, callback) => {
  if (value && value === form.oldPassword) {
    callback(new Error('新密码不能与旧密码相同'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度为 8-32 位', trigger: 'blur' },
    { validator: validateNotSame, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

// 密码强度检测
const checks = computed(() => {
  const pwd = form.newPassword
  return {
    length: pwd.length >= 8 && pwd.length <= 32,
    upper: /[A-Z]/.test(pwd),
    lower: /[a-z]/.test(pwd),
    digit: /\d/.test(pwd)
  }
})

const strengthLevel = computed(() => {
  const c = checks.value
  const score = [c.length, c.upper, c.lower, c.digit].filter(Boolean).length
  return score
})

const strengthText = computed(() => {
  const level = strengthLevel.value
  if (level <= 1) return '弱'
  if (level === 2) return '一般'
  if (level === 3) return '良好'
  return '强'
})

const strengthColor = computed(() => {
  const level = strengthLevel.value
  if (level <= 1) return '#f56c6c'
  if (level === 2) return '#e6a23c'
  if (level === 3) return '#409eff'
  return '#67c23a'
})

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await auth.changePassword(form.oldPassword, form.newPassword)
    if (res.success) {
      ElMessage.success('密码修改成功')
      // 确保状态已更新后跳转
      await router.push('/admin')
    } else {
      ElMessage.error(res.message || '密码修改失败')
    }
  } catch (e) {
    ElMessage.error('请求失败: ' + (e.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

const goLogin = () => {
  router.push('/admin/login')
}
</script>

<template>
  <div class="change-pwd-page">
    <div class="change-pwd-card">
      <div class="card-header">
        <div class="header-icon">
          <el-icon :size="32"><Lock /></el-icon>
        </div>
        <h1 class="card-title">修改密码</h1>
        <p class="card-desc">为了账户安全，请设置一个强密码</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        size="large"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <!-- 密码强度与要求 -->
        <div v-if="form.newPassword" class="pwd-strength">
          <div class="strength-bar">
            <div class="strength-label">
              <span>密码强度</span>
              <span class="strength-value" :style="{ color: strengthColor }">{{ strengthText }}</span>
            </div>
            <div class="strength-track">
              <div
                class="strength-fill"
                :style="{ width: (strengthLevel / 4 * 100) + '%', background: strengthColor }"
              />
            </div>
          </div>
          <ul class="pwd-requirements">
            <li :class="{ ok: checks.length }">
              <el-icon v-if="checks.length"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
              长度 8-32 位
            </li>
            <li :class="{ ok: checks.upper }">
              <el-icon v-if="checks.upper"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
              包含大写字母
            </li>
            <li :class="{ ok: checks.lower }">
              <el-icon v-if="checks.lower"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
              包含小写字母
            </li>
            <li :class="{ ok: checks.digit }">
              <el-icon v-if="checks.digit"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
              包含数字
            </li>
          </ul>
        </div>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleSubmit"
          >
            确认修改
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </el-form-item>
      </el-form>

      <div class="back-link">
        <a @click="goLogin">返回登录</a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.change-pwd-page {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  overflow: auto;
}

.change-pwd-card {
  width: 460px;
  max-width: 100%;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.card-header {
  text-align: center;
  margin-bottom: 28px;
}

.header-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.card-desc {
  font-size: 13px;
  color: #909399;
}

.pwd-strength {
  margin: -4px 0 18px 90px;
  padding: 14px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.strength-bar {
  margin-bottom: 12px;
}

.strength-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #606266;
  margin-bottom: 6px;
}

.strength-value {
  font-weight: 600;
}

.strength-track {
  height: 6px;
  background: #e4e7ed;
  border-radius: 3px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s, background 0.3s;
}

.pwd-requirements {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 12px;
}

.pwd-requirements li {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #c0c4cc;
}

.pwd-requirements li.ok {
  color: #67c23a;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
}

.back-link {
  text-align: center;
  margin-top: 4px;
}

.back-link a {
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
}

.back-link a:hover {
  text-decoration: underline;
}
</style>
