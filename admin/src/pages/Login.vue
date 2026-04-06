<template>
  <div class="wrap">
    <el-card class="card" shadow="never">
      <div class="brand">医院小程序后台管理</div>
      <div class="sub">请输入管理员账号密码登录</div>

      <el-form :model="form" label-width="70px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="123456" show-password />
        </el-form-item>
      </el-form>

      <div class="actions">
        <el-button type="primary" :loading="loading" @click="onSubmit" style="width:100%;">
          {{ loading ? '登录中...' : '登录' }}
        </el-button>
      </div>

      <div class="hint">token 将保存在浏览器本地，用于后续请求的 `X-Admin-Token`。</div>
      <el-alert v-if="error" :title="error" type="error" show-icon class="err" />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api.js'

const router = useRouter()
const error = ref('')
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456'
})

async function onSubmit() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  try {
    const { res, data } = await login(form.username.trim(), form.password)
  if (!res.ok || !data || data.code !== 200) {
    error.value = data?.message || '登录失败'
    return
  }

  const token = data.data?.token
  if (!token) {
    error.value = '登录成功但未获得 token'
    return
  }

  localStorage.setItem('adminToken', token)
  router.push('/doctors')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.brand {
  font-weight: 900;
  color: var(--primary);
  font-size: 20px;
  margin-bottom: 6px;
}
.sub { color: #666; font-size: 13px; margin-bottom: 18px; }
.card {
  width: min(420px, 100%);
}
.actions {
  margin-top: 16px;
}
.hint { margin-top: 14px; color: #888; font-size: 12px; line-height: 1.5; }
.err { margin-top: 12px; }
</style>

