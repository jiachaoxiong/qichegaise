import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../components/AdminLayout.vue'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/', component: AdminLayout,
    children: [
      { path: '', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'colors', name: 'ColorManage', component: () => import('../views/ColorManage.vue') },
      { path: 'car-models', name: 'CarModelManage', component: () => import('../views/CarModelManage.vue') },
      { path: 'shops', name: 'ShopAudit', component: () => import('../views/ShopAudit.vue') },
      { path: 'cases', name: 'CaseAudit', component: () => import('../views/CaseAudit.vue') },
      { path: 'users', name: 'UserManage', component: () => import('../views/UserManage.vue') },
      { path: 'appointments', name: 'AppointmentManage', component: () => import('../views/AppointmentManage.vue') }
    ]
  }
]

export default createRouter({ history: createWebHistory(), routes })
