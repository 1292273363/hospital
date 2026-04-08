<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar" style="gap: 12px; align-items: flex-end;">
        <div style="flex: 1; min-width: 220px;">
          <div class="stat-label" style="margin-bottom:6px;">公告标题</div>
          <el-input v-model="form.title" placeholder="公告标题" />
        </div>
        <div style="width: 260px;">
          <div class="stat-label" style="margin-bottom:6px;">置顶排序（可选，数字越大越靠前）</div>
          <el-input v-model="form.sortNo" placeholder="例如：100" />
        </div>
        <div style="width: 130px;">
          <div class="stat-label" style="margin-bottom:6px;">状态</div>
          <el-select v-model="form.status" style="width: 100%;">
            <el-option :value="1" label="显示" />
            <el-option :value="0" label="隐藏" />
          </el-select>
        </div>
        <el-button type="primary" :loading="publishing" @click="publish">发布公告</el-button>
      </div>

      <div style="margin-top: 10px;">
        <div class="stat-label" style="margin-bottom:6px;">公告内容</div>
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="6"
          placeholder="公告内容（小程序主页只展示标题与日期）"
        />
      </div>
    </el-card>

    <el-table :data="list" style="width: 100%; margin-top: 12px;" v-loading="loading">
      <el-table-column prop="id" label="编号" width="90" />
      <el-table-column prop="title" label="标题" />
      <el-table-column label="状态" width="90">
        <template #default="scope">
          {{ scope.row.status === 1 ? '显示' : '隐藏' }}
        </template>
      </el-table-column>
      <el-table-column prop="sortNo" label="置顶" width="110" />
      <el-table-column prop="createTime" label="创建时间" />
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
        @current-change="loadNotices"
        background
      />
    </div>

    <el-dialog v-model="editShow" :title="`编辑公告 #${editId}`" width="720px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 160px;">
            <el-option :value="1" label="显示" />
            <el-option :value="0" label="隐藏" />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶排序">
          <el-input v-model="editForm.sortNo" placeholder="例如：100" />
        </el-form-item>
      </el-form>
      <div class="muted" style="color:#888; line-height:1.7;">
        当前后端编辑接口仅支持修改标题/状态/置顶；内容需要重新发布（或后续扩展）。
      </div>
      <template #footer>
        <el-button @click="editShow = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
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

const list = ref([])
const publishing = ref(false)
const loading = ref(false)

const form = ref({
  title: '',
  content: '',
  status: 1,
  sortNo: ''
})

const editShow = ref(false)
const editId = ref('')
const editForm = ref({
  title: '',
  status: 1,
  sortNo: ''
})

function openEdit(row) {
  editId.value = row.id
  editForm.value = {
    title: row.title || '',
    status: Number(row.status),
    sortNo: row.sortNo ?? 0
  }
  editShow.value = true
}

async function loadNotices() {
  loading.value = true
  const path = `/notices?page=${page.value}&pageSize=${pageSize}`
  try {
    const r = await apiFetch(path, { method: 'GET' })
    if (!r.res.ok || r.data?.code !== 200) {
      if (r.res.status === 401) {
        clearAdminToken()
        router.push('/login')
        return
      }
      alert(r.data?.message || '加载失败')
      return
    }

    const data = r.data.data || {}
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function publish() {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    alert('请填写标题与内容')
    return
  }
  publishing.value = true
  try {
    const body = {
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      status: Number(form.value.status),
      sortNo: form.value.sortNo === '' ? 0 : Number(form.value.sortNo)
    }
    const r = await apiFetch('/notices', { method: 'POST', body })
    if (!r.res.ok || r.data?.code !== 200) {
      if (r.res.status === 401) {
        clearAdminToken()
        router.push('/login')
        return
      }
      alert(r.data?.message || '发布失败')
      return
    }

    form.value.title = ''
    form.value.content = ''
    form.value.sortNo = ''
    form.value.status = 1
    loadNotices()
  } finally {
    publishing.value = false
  }
}

async function submitEdit() {
  const id = editId.value
  const body = {
    title: editForm.value.title,
    status: Number(editForm.value.status),
    sortNo: Number(editForm.value.sortNo)
  }
  const r = await apiFetch(`/notices/${id}`, { method: 'PUT', body })
  if (!r.res.ok || r.data?.code !== 200) {
    if (r.res.status === 401) {
      clearAdminToken()
      router.push('/login')
      return
    }
    alert(r.data?.message || '保存失败')
    return
  }
  editShow.value = false
  loadNotices()
}

onMounted(loadNotices)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.stat-label {
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
}
.muted {
  color: #888;
}
</style>

