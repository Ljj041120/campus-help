<template>
  <div class="realname-page">
    <div class="page-header">
      <h2>📋 实名认证审核</h2>
      <el-tag type="warning" size="large">{{ pendingCount }} 条待审核</el-tag>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num pending">{{ pendingCount }}</div>
        <div class="stat-label">待审核</div>
      </div>
      <div class="stat-card">
        <div class="stat-num approved">{{ approvedToday }}</div>
        <div class="stat-label">今日通过</div>
      </div>
      <div class="stat-card">
        <div class="stat-num rejected">{{ rejectedToday }}</div>
        <div class="stat-label">今日拒绝</div>
      </div>
    </div>

    <!-- 审核列表 -->
    <el-card shadow="never">
      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.userAvatar" />
              <span>{{ row.userNickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column label="学生证" width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.idCardPhoto"
              :src="row.idCardPhoto"
              :preview-src-list="[row.idCardPhoto]"
              style="width: 50px; height: 50px; border-radius: 6px"
              fit="cover"
            />
            <span v-else class="no-photo">暂无</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleAudit(row, 1)" :loading="auditingId === row.id">
              通过
            </el-button>
            <el-button type="danger" size="small" @click="handleReject(row)" :loading="auditingId === row.id">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 拒绝理由弹窗 -->
    <el-dialog v-model="rejectDialog" title="拒绝原因" width="400px">
      <el-input
        v-model="rejectComment"
        type="textarea"
        :rows="3"
        placeholder="请输入拒绝原因..."
      />
      <template #footer>
        <el-button @click="rejectDialog = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const pendingCount = ref(0)
const approvedToday = ref(0)
const rejectedToday = ref(0)
const auditingId = ref(null)
const rejectDialog = ref(false)
const rejectComment = ref('')
const rejectRow = ref(null)

const token = () => 'Bearer ' + localStorage.getItem('adminToken')

async function loadData() {
  loading.value = true
  try {
    const res = await fetch(`/api/admin/auths/pending?pageNum=${pageNum.value}&pageSize=${pageSize.value}`, {
      headers: { 'Authorization': token() }
    })
    const json = await res.json()
    if (json.code === 200 && json.data) {
      tableData.value = json.data.records || []
      total.value = json.data.total || 0
      pendingCount.value = total.value
    }
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function handleAudit(row, status) {
  auditingId.value = row.id
  try {
    const res = await fetch('/api/admin/auths/audit', {
      method: 'POST',
      headers: {
        'Authorization': token(),
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: new URLSearchParams({
        authId: row.id,
        status: status,
        comment: '',
        auditorId: '1'
      })
    })
    const json = await res.json()
    if (json.code === 200) {
      ElMessage.success(status === 1 ? '审核通过' : '已拒绝')
      approvedToday.value++
      loadData()
    } else {
      ElMessage.error(json.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    auditingId.value = null
  }
}

function handleReject(row) {
  rejectRow.value = row
  rejectComment.value = ''
  rejectDialog.value = true
}

async function confirmReject() {
  if (!rejectRow.value) return
  auditingId.value = rejectRow.value.id
  try {
    const res = await fetch('/api/admin/auths/audit', {
      method: 'POST',
      headers: {
        'Authorization': token(),
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: new URLSearchParams({
        authId: rejectRow.value.id,
        status: '2',
        comment: rejectComment.value || '不符合要求',
        auditorId: '1'
      })
    })
    const json = await res.json()
    if (json.code === 200) {
      ElMessage.success('已拒绝')
      rejectedToday.value++
      rejectDialog.value = false
      loadData()
    } else {
      ElMessage.error(json.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    auditingId.value = null
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.realname-page {
  padding: 30px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;

  h2 {
    margin: 0;
    font-size: 24px;
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  padding: 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  text-align: center;
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 6px;

  &.pending { color: #f59e0b; }
  &.approved { color: #10b981; }
  &.rejected { color: #ef4444; }
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.no-photo {
  color: #ccc;
  font-size: 12px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
