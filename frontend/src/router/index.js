import { createRouter, createWebHistory } from 'vue-router'
import Workspace from '../views/Workspace.vue'

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
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
