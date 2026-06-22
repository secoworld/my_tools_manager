<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, InfoFilled } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()

const loginFormRef = ref(null)
const loading = ref(false)
const rememberPassword = ref(false)

const CREDENTIALS_KEY = 'saved-credentials'

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 页面加载时，读取保存的凭据
onMounted(() => {
  try {
    const saved = localStorage.getItem(CREDENTIALS_KEY)
    if (saved) {
      const credentials = JSON.parse(atob(saved))
      loginForm.username = credentials.username || ''
      loginForm.password = credentials.password || ''
      rememberPassword.value = true
    }
  } catch {
    // 解析失败，忽略
  }
})

const handleLogin = async () => {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await auth.login(loginForm.username, loginForm.password)
    if (res.success) {
      // 保存或清除凭据
      if (rememberPassword.value) {
        const credentials = btoa(JSON.stringify({
          username: loginForm.username,
          password: loginForm.password
        }))
        localStorage.setItem(CREDENTIALS_KEY, credentials)
      } else {
        localStorage.removeItem(CREDENTIALS_KEY)
      }
      ElMessage.success('登录成功')
      if (auth.mustChangePassword) {
        router.push('/admin/password')
      } else {
        router.push('/admin')
      }
    } else {
      ElMessage.error(res.message || '登录失败，请检查用户名和密码')
    }
  } catch (e) {
    ElMessage.error('登录请求失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <el-icon :size="40"><Lock /></el-icon>
        </div>
        <h1 class="login-title">管理员登录</h1>
        <p class="login-subtitle">工具管理器后台管理系统</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>
        <div class="login-options">
          <el-checkbox v-model="rememberPassword">记住密码</el-checkbox>
        </div>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-hint">
        <el-icon><InfoFilled /></el-icon>
        <span>默认账号：admin / admin</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 400px;
  max-width: 100%;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 13px;
  color: #909399;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
}

.login-options {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.login-hint {
  margin-top: 8px;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}
</style>
