import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '../pages/Login.vue'
import Doctors from '../pages/Doctors.vue'
import Patients from '../pages/Patients.vue'
import Diagnosis from '../pages/Diagnosis.vue'
import Notices from '../pages/Notices.vue'
import Settings from '../pages/Settings.vue'

const routes = [
  { path: '/', redirect: '/doctors' },
  { path: '/login', component: Login },
  {
    path: '/doctors',
    component: Doctors,
    beforeEnter: () => {
      return localStorage.getItem('adminToken') ? true : { path: '/login' }
    }
  },
  {
    path: '/patients',
    component: Patients,
    beforeEnter: () => {
      return localStorage.getItem('adminToken') ? true : { path: '/login' }
    }
  },
  {
    path: '/diagnosis',
    component: Diagnosis,
    beforeEnter: () => {
      return localStorage.getItem('adminToken') ? true : { path: '/login' }
    }
  },
  {
    path: '/notices',
    component: Notices,
    beforeEnter: () => {
      return localStorage.getItem('adminToken') ? true : { path: '/login' }
    }
  },
  {
    path: '/settings',
    component: Settings,
    beforeEnter: () => {
      return localStorage.getItem('adminToken') ? true : { path: '/login' }
    }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router

