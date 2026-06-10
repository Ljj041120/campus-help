<template>
  <div class="appeals-page">
    <div class="page-header">
      <h2>⚖️ 申诉仲裁</h2>
      <el-select v-model="filterStatus" placeholder="状态" clearable style="width:120px" @change="loadData">
        <el-option label="待处理" :value="0" />
        <el-option label="已处理" :value="1" />
      </el-select>
    </div>

    <el-card shadow="never">
      <el-table :data="tableData" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderId" label="订单号" width="80" />
        <el-table-column prop="reason" label="申诉原因" width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{row}">
            <el-tag :type="row.status===0?'danger':'success'" size="small">{{ row.status===0?'待处理':'已处理' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="仲裁结果" width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="申诉时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{row}">
            <el-button type="primary" size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status===0" type="warning" size="small" @click="showAudit(row)">仲裁</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total,prev,pager,next" @current-change="loadData" />
      </div>
    </el-card>

    <!-- 仲裁弹窗 -->
    <el-dialog v-model="auditDialog" title="仲裁处理" width="500px">
      <el-form>
        <el-form-item label="申诉原因">
          <el-input :model-value="currentRow?.reason" disabled type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="仲裁结果">
          <el-radio-group v-model="auditResult">
            <el-radio :value="1" label="裁定申诉成立">裁定申诉成立</el-radio>
            <el-radio :value="1" label="裁定申诉不成立">裁定申诉不成立</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="auditComment" type="textarea" :rows="2" placeholder="仲裁备注..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialog=false">取消</el-button>
        <el-button type="primary" @click="doAudit">确认仲裁</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialog" title="申诉详情" width="600px">
      <div v-if="detailData">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申诉ID">{{ detailData.appeal?.id }}</el-descriptions-item>
          <el-descriptions-item label="订单ID">{{ detailData.appeal?.orderId }}</el-descriptions-item>
          <el-descriptions-item label="申诉人">{{ detailData.user?.nickname }}</el-descriptions-item>
          <el-descriptions-item label="申诉原因">{{ detailData.appeal?.reason }}</el-descriptions-item>
          <el-descriptions-item label="证据">{{ detailData.appeal?.evidence || '无' }}</el-descriptions-item>
          <el-descriptions-item label="仲裁结果">{{ detailData.appeal?.result || '未处理' }}</el-descriptions-item>
          <el-descriptions-item label="订单地址">{{ detailData.order?.pickupAddress }} → {{ detailData.order?.deliveryAddress }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
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
const auditDialog = ref(false)
const detailDialog = ref(false)
const currentRow = ref(null)
const auditResult = ref('')
const auditComment = ref('')
const detailData = ref(null)

const token = () => 'Bearer ' + localStorage.getItem('adminToken')

async function loadData() {
  loading.value = true
  try {
    const params = new URLSearchParams({ pageNum: pageNum.value.toString(), pageSize: pageSize.value.toString() })
    if (filterStatus.value !== null && filterStatus.value !== '') params.append('status', filterStatus.value.toString())
    const res = await fetch('/api/admin/appeals?' + params, { headers: { 'Authorization': token() } })
    const json = await res.json()
    if (json.code === 200 && json.data) {
      tableData.value = json.data.records || []
      total.value = json.data.total || 0
    }
  } catch (e) { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

function showAudit(row) { currentRow.value = row; auditResult.value = '裁定申诉成立'; auditComment.value = ''; auditDialog.value = true }

async function showDetail(row) {
  try {
    const res = await fetch('/api/admin/appeals/' + row.id, { headers: { 'Authorization': token() } })
    const json = await res.json()
    if (json.code === 200) { detailData.value = json.data; detailDialog.value = true }
  } catch (e) { ElMessage.error('加载失败') }
}

async function doAudit() {
  try {
    const body = new URLSearchParams({ appealId: currentRow.value.id.toString(), status: '1', result: auditResult.value + (auditComment.value ? ': ' + auditComment.value : '') })
    const res = await fetch('/api/admin/appeals/audit', { method: 'POST', headers: { 'Authorization': token(), 'Content-Type': 'application/x-www-form-urlencoded' }, body })
    const json = await res.json()
    if (json.code === 200) { ElMessage.success('仲裁完成'); auditDialog.value = false; loadData() }
    else ElMessage.error(json.message || '操作失败')
  } catch (e) { ElMessage.error('操作失败') }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.appeals-page { padding: 30px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; h2 { margin: 0; font-size: 24px; } }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
