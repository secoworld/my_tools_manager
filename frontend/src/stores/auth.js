import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('admin-token') || '')
  const username = ref('')
  const mustChangePassword = ref(false)
  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('admin-token', newToken)
    } else {
      localStorage.removeItem('admin-token')
    }
  }

  async function login(user, pass) {
    const res = await authApi.login(user, pass)
    if (res.success) {
      setToken(res.token)
      username.value = res.username
      mustChangePassword.value = res.mustChangePassword
    }
    return res
  }

  async function changePassword(oldPwd, newPwd) {
    const res = await authApi.changePassword(oldPwd, newPwd, token.value)
    if (res.success) {
      mustChangePassword.value = false
      if (res.token) {
        setToken(res.token)
      }
    }
    return res
  }

  async function logout() {
    try {
      await authApi.logout(token.value)
    } catch (e) {
      // 忽略网络错误
    }
    setToken('')
    username.value = ''
    mustChangePassword.value = false
  }

  async function getProfile() {
    if (!token.value) return null
    try {
      const res = await authApi.profile(token.value)
      if (res.success) {
        username.value = res.username
        mustChangePassword.value = res.mustChangePassword
      }
      return res
    } catch (e) {
      setToken('')
      return null
    }
  }

  return {
    token,
    username,
    mustChangePassword,
    isLoggedIn,
    setToken,
    login,
    changePassword,
    logout,
    getProfile
  }
})
