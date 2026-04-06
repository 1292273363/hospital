<template>
  <el-container class="layout">
    <el-aside width="260px" class="aside">
      <div class="brand">医院小程序后台管理</div>
      <el-menu
        router
        :default-active="activePath"
        class="menu"
        background-color="#ffffff"
        text-color="#333"
        active-text-color="#0d8a0c"
      >
        <el-menu-item v-for="m in menu" :key="m.key" :index="m.path">
          {{ m.label }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-main class="main">
      <div class="topbar">
        <div class="title">{{ pageTitle }}</div>
        <el-button type="info" size="small" @click="logout">退出登录</el-button>
      </div>
      <div class="content">
        <slot />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAdminToken } from '../utils/auth.js'

const menu = [
  { key: 'doctors', label: '医生管理', path: '/doctors' },
  { key: 'patients', label: '患者管理', path: '/patients' },
  { key: 'diagnosis', label: '诊断记录管理', path: '/diagnosis' },
  { key: 'notices', label: '通知管理', path: '/notices' },
  { key: 'settings', label: '系统设置', path: '/settings' }
]

const route = useRoute()
const router = useRouter()

const activePath = computed(() => route.path)
const pageTitle = computed(() => {
  const found = menu.find(m => m.path === route.path)
  return found?.label || '后台管理'
})

function logout() {
  clearAdminToken()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.aside {
  background: #ffffff;
  border-right: 1px solid var(--border);
  padding: 14px;
}
.brand {
  font-weight: 900;
  color: var(--primary);
  margin-bottom: 10px;
}
.menu {
  border-right: none;
}
.main {
  padding: 14px;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.title { font-weight: 900; color: var(--text); font-size: 16px; }
.content { }
</style>

