<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const totalUsers = ref(0)
const totalOrders = ref(0)
const todayOrders = ref(0)
const todayGMV = ref(0)
const onlineUsers = ref(0)
const approvalCount = ref(0)
const ordersCount = ref({ waiting: 0, inProgress: 0, completed: 0 })
const typeDistribution = ref({})
const recentOrders = ref([])
const chartRef1 = ref(null)
const chartRef2 = ref(null)

const token = () => 'Bearer ' + localStorage.getItem('adminToken')
const typeMap = { 1:'代取快递', 2:'代买物品', 3:'代送物品', 4:'其他' }
const statusMap = { 0:'待支付', 1:'待接单', 2:'已接单', 3:'进行中', 4:'待确认', 5:'已完成', 6:'已取消', 7:'申诉中' }

async function loadDashboard() {
  try {
    const res = await fetch('/api/admin/dashboard', {
      headers: { 'Authorization': token() }
    })
    const json = await res.json()
    if (json.code === 200 && json.data) {
      const d = json.data
      totalUsers.value = d.totalUsers || 0
      totalOrders.value = d.totalOrders || 0
      todayOrders.value = d.todayOrders || 0
      onlineUsers.value = d.onlineUsers || 0
      approvalCount.value = d.pendingAuths || 0
      ordersCount.value = d.statusCount || { waiting: 0, inProgress: 0, completed: 0 }
      typeDistribution.value = d.typeDistribution || {}
      todayGMV.value = d.todayGMV || 0
    }
  } catch (e) {
    console.error('Dashboard load failed:', e)
  }
}

async function loadRecentOrders() {
  try {
    const res = await fetch('/api/admin/orders?pageNum=1&pageSize=5', {
      headers: { 'Authorization': token() }
    })
    const json = await res.json()
    if (json.code === 200 && json.data && json.data.records) {
      recentOrders.value = json.data.records
    }
  } catch (e) {}
}

function initCharts() {
  // 图表1：今日订单概览（显示总数，后续可扩展7日趋势）
  const chart1 = echarts.init(chartRef1.value)
  chart1.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['待接单', '进行中', '已完成'],
      axisLabel: { color: 'rgba(255,255,255,0.7)' },
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(255,255,255,0.7)' },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }
    },
    series: [{
      data: [
        ordersCount.value.waiting || 0,
        ordersCount.value.inProgress || 0,
        ordersCount.value.completed || 0
      ],
      type: 'bar',
      barWidth: '40%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#FFD93D' },
          { offset: 1, color: '#f59e0b' }
        ])
      }
    }]
  })

  // 图表2：服务类型占比（饼图 - 真实数据）
  const chart2 = echarts.init(chartRef2.value)
  const typeDist = typeDistribution.value || {}
  const pieData = [
    { value: typeDist['代取快递'] || 0, name: '代取快递', itemStyle: { color: '#667eea' } },
    { value: typeDist['代买物品'] || 0, name: '代买物品', itemStyle: { color: '#f093fb' } },
    { value: typeDist['代送物品'] || 0, name: '代送物品', itemStyle: { color: '#4facfe' } },
    { value: typeDist['其他'] || 0, name: '其他', itemStyle: { color: '#43e97b' } }
  ]
  chart2.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}单 ({d}%)' },
    legend: {
      bottom: '5%',
      textStyle: { color: 'rgba(255,255,255,0.7)' }
    },
    series: [{
      type: 'pie',
      radius: ['35%', '60%'],
      center: ['50%', '40%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: pieData
    }]
  })

  // 窗口变化自适应
  window.addEventListener('resize', () => {
    chart1.resize()
    chart2.resize()
  })
}

onMounted(async () => {
  await Promise.all([loadDashboard(), loadRecentOrders()])
  nextTick(() => initCharts())
})
</script>

<template>
  <div class="dashboard">
    <h1>📊 数据大屏</h1>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card glass">
        <div class="stat-icon">👥</div>
        <div class="stat-value">{{ totalUsers }}</div>
        <div class="stat-label">总用户数</div>
      </div>
      <div class="stat-card glass">
        <div class="stat-icon">📦</div>
        <div class="stat-value">{{ totalOrders }}</div>
        <div class="stat-label">总订单数</div>
      </div>
      <div class="stat-card glass">
        <div class="stat-icon">📈</div>
        <div class="stat-value">{{ todayOrders }}</div>
        <div class="stat-label">今日订单</div>
      </div>
      <div class="stat-card glass">
        <div class="stat-icon">💰</div>
        <div class="stat-value">¥{{ todayGMV }}</div>
        <div class="stat-label">今日成交额</div>
      </div>
      <div class="stat-card glass">
        <div class="stat-icon">🟢</div>
        <div class="stat-value">{{ onlineUsers }}</div>
        <div class="stat-label">在线用户</div>
      </div>
      <div class="stat-card glass">
        <div class="stat-icon">✅</div>
        <div class="stat-value">{{ approvalCount }}</div>
        <div class="stat-label">待审核</div>
      </div>
    </div>

    <!-- 状态统计 -->
    <div class="status-bar glass">
      <div class="status-item">
        <span class="status-dot waiting"></span>
        <span>待接单 {{ ordersCount.waiting }}</span>
      </div>
      <div class="status-item">
        <span class="status-dot progress"></span>
        <span>进行中 {{ ordersCount.inProgress }}</span>
      </div>
      <div class="status-item">
        <span class="status-dot done"></span>
        <span>已完成 {{ ordersCount.completed }}</span>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <div class="chart-card glass">
        <h3>📊 近7日订单趋势</h3>
        <div ref="chartRef1" class="chart-box"></div>
      </div>
      <div class="chart-card glass">
        <h3>🎯 服务类型占比</h3>
        <div ref="chartRef2" class="chart-box"></div>
      </div>
    </div>

    <!-- 最近订单 -->
    <div class="recent-orders glass">
      <h3>📋 最近订单</h3>
      <el-table :data="recentOrders" stripe style="width: 100%">
        <el-table-column prop="id" label="订单号" width="80" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ typeMap[row.orderType] || '未知' }}</template>
        </el-table-column>
        <el-table-column prop="pickupAddress" label="取货" width="120" show-overflow-tooltip />
        <el-table-column prop="deliveryAddress" label="送货" width="120" show-overflow-tooltip />
        <el-table-column label="金额" width="80">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">{{ statusMap[row.status] || '未知' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" />
      </el-table>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  padding: 30px;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  h1 {
    color: #fff;
    font-size: 32px;
    margin-bottom: 30px;
  }
}

.glass {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
  margin-bottom: 20px;

  .stat-card {
    padding: 24px;
    text-align: center;

    .stat-icon { font-size: 36px; margin-bottom: 12px; }
    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #FFD93D;
      margin-bottom: 8px;
    }
    .stat-label { font-size: 14px; color: rgba(255, 255, 255, 0.7); }
  }
}

.status-bar {
  display: flex;
  gap: 30px;
  padding: 16px 24px;
  margin-bottom: 20px;

  .status-item {
    display: flex;
    align-items: center;
    gap: 8px;
    color: rgba(255, 255, 255, 0.9);
    font-size: 14px;
  }

  .status-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    display: inline-block;

    &.waiting { background: #f59e0b; }
    &.progress { background: #3b82f6; }
    &.done { background: #10b981; }
  }
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 30px;

  .chart-card {
    padding: 24px;

    h3 {
      color: #fff;
      font-size: 18px;
      margin-bottom: 5px;
    }

    .chart-box {
      height: 280px;
    }
  }
}

.recent-orders {
  padding: 24px;

  h3 {
    color: #fff;
    font-size: 18px;
    margin-bottom: 20px;
  }

  :deep(.el-table) {
    background: transparent !important;

    .el-table__header th {
      background: rgba(255, 255, 255, 0.1) !important;
      color: #fff !important;
    }

    .el-table__row {
      background: rgba(255, 255, 255, 0.05) !important;
      &:hover { background: rgba(255, 255, 255, 0.15) !important; }
      td { color: rgba(255, 255, 255, 0.9) !important; }
    }
  }
}
</style>