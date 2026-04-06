<template>
  <div class="diag-wrap">
    <div class="diag-title">历史诊断记录</div>

    <el-card shadow="never" class="diag-card diag-search">
      <div class="search-row">
        <div class="search-left">
          <div class="input-label">查找和管理所有历史诊断记录</div>
          <el-input v-model="keyword" placeholder="搜索患者姓名或诊断类型" clearable />
        </div>

        <div class="search-filters">
          <div class="filter-item">
            <div class="filter-label">时间</div>
            <el-select v-model="timePreset" style="width: 160px;">
              <el-option value="all" label="全部时间" />
              <el-option value="7d" label="近7天" />
              <el-option value="30d" label="近30天" />
            </el-select>
          </div>
          <div class="filter-item">
            <div class="filter-label">项目</div>
            <el-select v-model="projectPreset" style="width: 160px;">
              <el-option value="all" label="全部项目" />
              <el-option value="expert" label="专家项目" />
              <el-option value="other" label="非专家项目" />
            </el-select>
          </div>

          <div class="filter-actions">
            <el-button type="primary" @click="applyFilter">筛选</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <div class="diag-list">
      <el-card
        v-for="r in list"
        :key="r.id"
        class="diag-entry"
        shadow="never"
      >
        <template #header>
          <div class="entry-head">
            <div class="entry-head-left">
              <div class="entry-patient">
                患者：<span class="entry-patient-name">{{ r.patientName || '-' }}</span>
              </div>
              <div class="entry-id">ID：{{ formatPatientId(r) }}</div>
            </div>
            <el-tag :type="r.diagnosisReport ? 'success' : 'warning'" effect="plain">
              {{ r.diagnosisReport ? '已完成' : '待完成' }}
            </el-tag>
          </div>
        </template>

        <div class="entry-body">
          <div class="kv">
            <div class="kv-row">
              <div class="kv-label">诊断时间：</div>
              <div class="kv-value">{{ r.visitTime || '-' }}</div>
            </div>
            <div class="kv-row">
              <div class="kv-label">项目类型：</div>
              <div class="kv-value">{{ r.doctorLevel || '-' }}</div>
            </div>
            <div class="kv-row">
              <div class="kv-label">诊断项目：</div>
              <div class="kv-value">{{ parseDiagnosisItem(r.diagnosisReport) || '-' }}</div>
            </div>
          </div>

          <div class="entry-summary">
            <div class="summary-title">诊断简介：</div>
            <div class="summary-text">{{ parseDiagnosisIntro(r.diagnosisReport) }}</div>
          </div>

          <div class="entry-actions">
            <el-button size="small" type="info" @click="showDetail(r)">查看详情</el-button>
            <el-button size="small" type="primary" @click="exportRecord(r)">导出记录</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <div class="toolbar diag-pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        background
        @current-change="loadDiagnosis"
      />
    </div>

    <el-dialog
      v-model="detailShow"
      :title="`诊断详情 #${detailId}`"
      width="860px"
    >
      <div class="pre">{{ detailReport }}</div>
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
const timePreset = ref('all') // all | 7d | 30d
const projectPreset = ref('all') // all | expert | other

const list = ref([])

const detailShow = ref(false)
const detailId = ref('')
const detailReport = ref('')

function showDetail(row) {
  detailId.value = row.id
  detailReport.value = row.diagnosisReport || ''
  detailShow.value = true
}

function formatPatientId(r) {
  // 没有后端的业务 ID 字段，这里用 visit_record 的 id 生成展示用字符串
  const base = String(r.id || '')
  const prefix = 'P'
  return base ? `${prefix}${base}` : '—'
}

function parseDiagnosisItem(report) {
  const s = (report || '').toString().trim()
  if (!s) return ''
  // 取第一句/第一行作为“诊断项目”展示
  const firstLine = s.split(/\r?\n/)[0]
  return firstLine.slice(0, 30)
}

function parseDiagnosisIntro(report) {
  const s = (report || '').toString().trim()
  if (!s) return '暂无诊断简介'
  return s.length > 160 ? s.slice(0, 160) + '...' : s
}

function buildTimeRange() {
  const now = new Date()
  const toDate = now.toISOString().slice(0, 10) // yyyy-MM-dd

  if (timePreset.value === 'all') {
    return { dateFrom: '', dateTo: '' }
  }

  const days = timePreset.value === '7d' ? 7 : 30
  const from = new Date(now.getTime() - days * 24 * 60 * 60 * 1000)
  const dateFrom = from.toISOString().slice(0, 10)
  return { dateFrom, dateTo: toDate }
}

function buildDoctorLevelFilter() {
  // 后端当前支持精确 doctor_level = ?，因此 “非专家项目”这里不做严格等值筛选：
  // 传 expert 时过滤专家；other 时不传 doctorLevel，让“全部/非专家”由 keyword 搜索覆盖。
  if (projectPreset.value === 'expert') return '专家'
  if (projectPreset.value === 'other') return '' // 不传
  return ''
}

async function loadDiagnosis() {
  const { dateFrom, dateTo } = buildTimeRange()
  const doctorLevel = buildDoctorLevelFilter()

  let path = `/diagnosis/records?page=${page.value}&pageSize=${pageSize}`
  if (keyword.value) path += `&keyword=${encodeURIComponent(keyword.value)}`
  if (dateFrom) path += `&dateFrom=${encodeURIComponent(dateFrom)}`
  if (dateTo) path += `&dateTo=${encodeURIComponent(dateTo)}`
  if (doctorLevel) path += `&doctorLevel=${encodeURIComponent(doctorLevel)}`

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
}

function applyFilter() {
  page.value = 1
  loadDiagnosis()
}

function exportRecord(row) {
  const report = row.diagnosisReport || ''
  const content = [
    `患者：${row.patientName || '-'}`,
    `ID：${formatPatientId(row)}`,
    `诊断时间：${row.visitTime || '-'}`,
    `项目类型：${row.doctorLevel || '-'}`,
    `诊断项目：${parseDiagnosisItem(report) || '-'}`,
    `诊断简介：${parseDiagnosisIntro(report)}`,
    `---`,
    report
  ].join('\n')

  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  const safeName = (row.patientName || 'patient').toString().replace(/[\\/:*?"<>|]/g, '_')
  a.href = url
  a.download = `diagnosis_${safeName}_${row.id || ''}.txt`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

onMounted(loadDiagnosis)
</script>

<style scoped>
.diag-wrap {
  padding: 14px 0 26px;
}

.diag-title {
  font-weight: 900;
  color: var(--text);
  font-size: 18px;
  margin-bottom: 10px;
}

.diag-search {
  background: transparent;
  border: 1px solid rgba(233, 238, 243, 0.8);
}

.search-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.search-left {
  flex: 1;
  min-width: 300px;
}
.input-label {
  color: #888;
  font-size: 13px;
  margin-bottom: 8px;
}

.input-row {
  display: flex;
  gap: 10px;
}
.search-filters {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  flex-wrap: wrap;
}
.filter-item {
  min-width: 160px;
}
.filter-label {
  color: #888;
  font-size: 13px;
  margin-bottom: 6px;
}

.filter-actions {
  padding-bottom: 2px;
}

.diag-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.diag-entry {
  background: #fff;
  border: 1px solid rgba(233, 238, 243, 1);
  border-radius: 12px;
  padding: 16px;
}

.entry-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.entry-patient {
  font-weight: 800;
  color: var(--text);
  font-size: 16px;
}
.entry-patient-name {
  color: var(--primary);
}
.entry-id {
  color: #888;
  font-size: 13px;
  margin-top: 6px;
}

.entry-pill {
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 800;
  border: 1px solid #30a392;
  color: #1f7e6d;
  background: #e7fff8;
  white-space: nowrap;
}
.pill-ok {
}
.pill-wait {
  border-color: #faad14;
  color: #9a6a00;
  background: #fff7e6;
}

.entry-body {
  margin-top: 12px;
}

.kv {
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
}
.kv-row {
  display: flex;
  gap: 8px;
}
.kv-label {
  min-width: 86px;
  color: #888;
  font-size: 13px;
  font-weight: 700;
}
.kv-value {
  color: var(--text);
  font-size: 13px;
  flex: 1;
}

.entry-summary {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fbfa;
  border: 1px solid #eef7f5;
}
.summary-title {
  color: #888;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 6px;
}
.summary-text {
  color: var(--text);
  font-size: 13px;
  line-height: 1.6;
}

.entry-actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.diag-pagination {
  margin-top: 14px;
}
</style>

