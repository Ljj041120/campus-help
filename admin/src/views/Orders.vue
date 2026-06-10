<template>
  <div class="orders-page">
    <div class="page-header">
      <h2>📦 订单管理</h2>
      <el-select v-model="filterStatus" placeholder="筛选状态" clearable style="width: 150px" @change="loadData">
        <el-option label="待支付" :value="0" />
        <el-option label="待接单" :value="1" />
        <el-option label="已接单" :value="2" />
        <el-option label="进行中" :value="3" />
        <el-option label="待确认" :value="4" />
        <el-option label="已完成" :value="5" />
        <el-option label="已取消" :value="6" />
        <el-option label="申诉中" :value="7" />
      </el-select>
    </div>

    <el-card shadow="never">
      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="订单号" width="80" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeColor(row.orderType)" size="small">{{ typeLabel(row.orderType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pickupAddress" label="取货地址" width="160" show-overflow-tooltip />
        <el-table-column prop="deliveryAddress" label="送货地址" width="160" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="80">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusColor(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text>详情</el-button>
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
import { ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref(null)

const token = () => 'Bearer ' + localStorage.getItem('adminToken')

function typeLabel(t) {
  const m = {1:'代取快递',2:'代买物品',3:'代送物品',4:'其他'}
  return m[t] || '未知'
}
function typeColor(t) {
  const m = {1:'primary',2:'warning',3:'success',4:'info'}
  return m[t] || 'info'
}
function statusLabel(s) {
  const m = {0:'待支付',1:'待接单',2:'已接单',3:'进行中',4:'待确认',5:'已完成',6:'已取消',7:'申诉中'}
  return m[s] || '未知'
}
function statusColor(s) {
  const m = {0:'info',1:'warning',2:'',3:'',4:'warning',5:'success',6:'danger',7:'danger'}
  return m[s] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const params = new URLSearchParams({ pageNum: pageNum.value.toString(), pageSize: pageSize.value.toString() })
    if (filterStatus.value !== null && filterStatus.value !== '') params.append('status', filterStatus.value.toString())
    const res = await fetch('/api/admin/orders?' + params, {
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

onMounted(loadData)
</script>

<style scoped lang="scss">
.orders-page {
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
