<template>
  <div>
    <div class="grid-2">
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">当前患者数量</div>
        <div class="stat-num">{{ stats.total }}</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-label">启用 / 禁用</div>
        <div class="stat-num" style="font-size:16px;">
          <span>{{ stats.activeCount }}</span> / <span>{{ stats.disabledCount }}</span>
        </div>
      </el-card>
    </div>

    <el-card shadow="never" style="margin-top: 12px;">
      <div class="toolbar" style="gap: 12px;">
        <el-input v-model="keyword" placeholder="搜索：电话/昵称" style="width: 360px;" />
        <el-button type="info" @click="page = 1, loadPatients()">查询</el-button>
      </div>
    </el-card>

    <el-table :data="list" style="width: 100%; margin-top: 12px;" v-loading="loading">
      <el-table-column prop="id" label="编号" width="100" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="nickName" label="昵称" />
      <el-table-column label="状态" width="110">
        <template #default="scope">
          {{ scope.row.status === 0 ? '正常' : '禁用' }}
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button type="info" size="small" @click="openEdit(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="toolbar" style="margin-top: 12px; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        layout="prev, pager, next, total"
        :total="total"
        @current-change="loadPatients"
      />
    </div>

    <el-dialog v-model="modalShow" :title="modalTitle" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="手机号（只读）">
          <el-input v-model="form.phone" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickName" />
        </el-form-item>
        <el-form-item label="头像 URL（可选）">
          <el-input v-model="form.avatarUrl" placeholder="例如：https://..." />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 160px;">
            <el-option :value="0" label="正常" />
            <el-option :value="1" label="禁用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalShow = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch } from '../api.js'
import { clearAdminToken } from '../utils/auth.js'

const router = useRouter()
const pageSize = 20
const page = ref(1)
const total = ref(0)
const pages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const keyword = ref('')

const list = ref([])
const stats = ref({ total: 0, activeCount: 0, disabledCount: 0 })
const loading = ref(false)

const modalShow = ref(false)
const modalTitle = ref('编辑患者')
const submitting = ref(false)
const form = ref({
  id: null,
  phone: '',
  nickName: '',
  avatarUrl: '',
  status: 0
})

function openEdit(row) {
  form.value = {
    id: row.id,
    phone: row.phone,
    nickName: row.nickName || '',
    avatarUrl: row.avatarUrl || '',
    status: Number(row.status)
  }
  modalShow.value = true
}

async function loadPatients() {
  loading.value = true
  try {
    const resStats = await apiFetch('/patients/stats', { method: 'GET' })
    if (resStats.res.ok && resStats.data?.code === 200) {
      stats.value = resStats.data.data || stats.value
    } else if (resStats.res.status === 401) {
      clearAdminToken()
      router.push('/login')
      return
    }

    const path = `/patients?page=${page.value}&pageSize=${pageSize}${keyword.value ? `&keyword=${encodeURIComponent(keyword.value)}` : ''}`
    const res = await apiFetch(path, { method: 'GET' })
    if (!res.res.ok || res.data?.code !== 200) {
      if (res.res.status === 401) {
        clearAdminToken()
        router.push('/login')
        return
      }
      alert(res.data?.message || '加载失败')
      return
    }

    const data = res.data.data || {}
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function submit() {
  submitting.value = true
  try {
  const id = form.value.id
  const body = {
    nickName: form.value.nickName,
    avatarUrl: form.value.avatarUrl && form.value.avatarUrl.trim() ? form.value.avatarUrl.trim() : null,
    status: Number(form.value.status)
  }
  const res = await apiFetch(`/patients/${id}`, { method: 'PUT', body })
  if (!res.res.ok || res.data?.code !== 200) {
    if (res.res.status === 401) {
      clearAdminToken()
      router.push('/login')
      return
    }
    alert(res.data?.message || '保存失败')
    return
  }
  modalShow.value = false
  loadPatients()
  } finally {
    submitting.value = false
  }
}

onMounted(loadPatients)
</script>

<style scoped>
.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.stat-card {
  min-height: 88px;
}
.stat-num {
  font-weight: 900;
  font-size: 22px;
  color: var(--text);
  margin-top: 6px;
}
.stat-label {
  color: var(--muted);
  font-size: 13px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>

