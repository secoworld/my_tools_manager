import { createRouter, createWebHistory } from 'vue-router'
import Workspace from '../views/Workspace.vue'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/',
    name: 'Workspace',
    component: Workspace,
    meta: { title: '工具工作区' }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { title: '仪表盘' }
  },
  {
    path: '/commands',
    name: 'CommandLibrary',
    component: () => import('../views/CommandLibrary.vue'),
    meta: { title: '命令库' }
  },
  {
    path: '/dev',
    name: 'PluginDeveloper',
    component: () => import('../views/PluginDeveloper.vue'),
    meta: { title: '插件开发' }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/AdminLogin.vue'),
    meta: { title: '管理员登录', guestOnly: true }
  },
  {
    path: '/admin/password',
    name: 'AdminChangePassword',
    component: () => import('../views/AdminChangePassword.vue'),
    meta: { title: '修改密码', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'AdminPanel',
    component: () => import('../views/AdminPanel.vue'),
    meta: { title: '插件管理', requiresAuth: true }
  },
  {
    path: '/admin/review',
    name: 'PluginReview',
    component: () => import('../views/PluginReview.vue'),
    meta: { title: '插件审核', requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    next('/admin/login')
  } else if (to.meta.guestOnly && auth.isLoggedIn) {
    next('/admin')
  } else if (auth.isLoggedIn && auth.mustChangePassword && to.path !== '/admin/password') {
    next('/admin/password')
  } else {
    next()
  }
})

export default router
