<template>
  <div class="users-page">
    <div class="page-header">
      <h2>👥 用户管理</h2>
      <el-input v-model="keyword" placeholder="搜索用户昵称..." style="width: 240px" clearable @clear="loadData" @keyup.enter="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <el-card shadow="never">
      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="头像" width="70">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatar" />
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column label="信用分" width="90">
          <template #default="{ row }">
            <el-tag :type="row.creditScore >= 90 ? 'success' : row.creditScore >= 70 ? 'warning' : 'danger'" size="small">
              {{ row.creditScore }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="实名" width="80">
          <template #default="{ row }">
            <span :style="{ color: row.isRealname ? '#10b981' : '#ef4444' }">
              {{ row.isRealname ? '已认证' : '未认证' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="roles" label="角色" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              active-color="#10b981"
              inactive-color="#ef4444"
              @change="(val) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
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
const keyword = ref('')

const token = () => 'Bearer ' + localStorage.getItem('adminToken')

async function loadData() {
  loading.value = true
  try {
    const params = new URLSearchParams({ pageNum: pageNum.value.toString(), pageSize: pageSize.value.toString() })
    if (keyword.value) params.append('keyword', keyword.value)
    const res = await fetch('/api/admin/users?' + params, {
      headers: { 'Authorization': token() }
    })
    const json = await res.json()
    if (json.code === 200 && json.data) {
      tableData.value = json.data.records || []
      total.value = json.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row, val) {
  try {
    const res = await fetch(`/api/admin/users/${row.id}/status?status=${val ? 1 : 0}`, {
      method: 'PUT',
      headers: { 'Authorization': token() }
    })
    const json = await res.json()
    if (json.code === 200) {
      row.status = val ? 1 : 0
      ElMessage.success(val ? '已启用' : '已禁用')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除用户 "${row.nickname}" 吗？`, '警告', { type: 'warning' })
    .then(() => {
      ElMessage.info('删除功能待实现')
    })
    .catch(() => {})
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.users-page {
  padding: 30px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 24px;
  }
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
