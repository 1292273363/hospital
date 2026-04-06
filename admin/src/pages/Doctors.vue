<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar" style="gap: 12px;">
        <el-input v-model="keyword" placeholder="搜索：电话/姓名" style="width: 360px;" />
        <el-button type="info" @click="page = 1, loadDoctors()">查询</el-button>
        <div style="flex:1;"></div>
        <el-button type="primary" @click="openAdd">新增医生</el-button>
      </div>
    </el-card>

    <el-table :data="list" style="width: 100%; margin-top: 12px;" v-loading="loading">
      <el-table-column prop="id" label="编号" width="100" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="doctorName" label="医生姓名" />
      <el-table-column prop="doctorLevel" label="级别" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="scope">
          {{ scope.row.status === 0 ? '正常' : '禁用' }}
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button type="info" size="small" @click="openEdit(scope.row)">编辑/密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="toolbar" style="margin-top: 12px; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        layout="prev, pager, next, total"
        :total="total"
        @current-change="loadDoctors"
      />
    </div>

    <el-dialog v-model="modalShow" :title="modalTitle" width="720px">
      <el-form :model="form" label-width="100px">
        <el-form-item v-if="modalMode === 'add'" label="手机号">
          <el-input v-model="form.phone" placeholder="例如：13800000001" />
        </el-form-item>
        <el-form-item label="医生姓名">
          <el-input v-model="form.doctorName" :disabled="modalMode === 'edit'" :placeholder="modalMode === 'add' ? '医生姓名' : ''" />
        </el-form-item>

        <el-form-item label="级别">
          <el-select v-model="form.doctorLevel" style="width: 200px;">
            <el-option value="专家" label="专家" />
            <el-option value="普通" label="普通" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 160px;">
            <el-option :value="0" label="正常" />
            <el-option :value="1" label="禁用" />
          </el-select>
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" :placeholder="modalMode === 'add' ? '请输入密码' : '留空则不修改'" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="modalShow = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ submitting ? '保存中...' : '保存' }}
        </el-button>
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

const modalShow = ref(false)
const modalTitle = ref('')
const modalMode = ref('add') // add | edit
const submitting = ref(false)
const loading = ref(false)

const form = ref({
  id: null,
  phone: '',
  doctorName: '',
  doctorLevel: '专家',
  status: 0,
  password: ''
})

function openAdd() {
  modalMode.value = 'add'
  modalTitle.value = '新增医生'
  form.value = { id: null, phone: '', doctorName: '', doctorLevel: '专家', status: 0, password: '' }
  modalShow.value = true
}

function openEdit(row) {
  modalMode.value = 'edit'
  modalTitle.value = '编辑医生 / 修改密码'
  form.value = {
    id: row.id,
    phone: row.phone,
    doctorName: row.doctorName,
    doctorLevel: row.doctorLevel,
    status: Number(row.status),
    password: ''
  }
  modalShow.value = true
}

async function loadDoctors() {
  loading.value = true
  const path = `/doctors?page=${page.value}&pageSize=${pageSize}${keyword.value ? `&keyword=${encodeURIComponent(keyword.value)}` : ''}`

  try {
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
    if (modalMode.value === 'add') {
      if (!form.value.phone || !form.value.doctorName || !form.value.doctorLevel || !form.value.password) {
        alert('请完整填写手机号/姓名/级别/密码')
        return
      }

      const { res, data } = await apiFetch('/doctors', {
        method: 'POST',
        body: {
          phone: form.value.phone.trim(),
          doctorName: form.value.doctorName.trim(),
          doctorLevel: form.value.doctorLevel,
          status: Number(form.value.status),
          password: form.value.password
        }
      })
      if (!res.ok) {
        if (res.status === 401) {
          clearAdminToken()
          router.push('/login')
        } else alert(data?.message || '新增失败')
        return
      }
      if (data?.code !== 200) return alert(data?.message || '新增失败')
    } else {
      const id = form.value.id
      const body = {
        doctorName: form.value.doctorName,
        doctorLevel: form.value.doctorLevel,
        status: Number(form.value.status),
        password: form.value.password && form.value.password.trim() ? form.value.password.trim() : null
      }
      const { res, data } = await apiFetch(`/doctors/${id}`, { method: 'PUT', body })
      if (!res.ok) {
        if (res.status === 401) {
          clearAdminToken()
          router.push('/login')
        } else alert(data?.message || '保存失败')
        return
      }
      if (data?.code !== 200) return alert(data?.message || '保存失败')
    }

    modalShow.value = false
    loadDoctors()
  } finally {
    submitting.value = false
  }
}

onMounted(loadDoctors)
</script>

