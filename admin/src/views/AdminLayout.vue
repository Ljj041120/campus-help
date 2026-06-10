<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="logo-area">
        <span class="logo-icon">🏃</span>
        <span class="logo-text">校园帮管理</span>
      </div>

      <nav class="nav-menu">
        <router-link to="/dashboard" class="nav-item" active-class="active">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据大屏</span>
        </router-link>
        <router-link to="/users" class="nav-item" active-class="active">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </router-link>
        <router-link to="/orders" class="nav-item" active-class="active">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </router-link>
        <router-link to="/realname" class="nav-item" active-class="active">
          <el-icon><Checked /></el-icon>
          <span>实名审核</span>
          <el-badge v-if="pendingCount > 0" :value="pendingCount" class="nav-badge" />
        </router-link>
        <router-link to="/appeals" class="nav-item" active-class="active">
          <el-icon><WarningFilled /></el-icon>
          <span>申诉仲裁</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info">
          <el-icon><UserFilled /></el-icon>
          <span>管理员</span>
        </div>
        <el-button type="danger" size="small" text @click="handleLogout">退出</el-button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const pendingCount = ref(0)

onMounted(async () => {
  try {
    const res = await fetch('/api/admin/auths/pending?pageNum=1&pageSize=1', {
      headers: { 'Authorization': 'Bearer ' + localStorage.getItem('adminToken') }
    })
    const data = await res.json()
    if (data.code === 200 && data.data) {
      pendingCount.value = data.data.total || 0
    }
  } catch (e) {}
})

function handleLogout() {
  localStorage.removeItem('adminToken')
  router.push('/login')
}
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.logo-area {
  padding: 24px 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  font-size: 28px;
  margin-right: 10px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
}

.nav-menu {
  flex: 1;
  padding: 12px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 14px 24px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-size: 15px;
  transition: all 0.2s;
  position: relative;

  .el-icon {
    margin-right: 10px;
    font-size: 18px;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    color: #fff;
  }

  &.active {
    background: rgba(102, 126, 234, 0.3);
    color: #fff;
    border-right: 3px solid #667eea;
  }
}

.nav-badge {
  margin-left: auto;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-info {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);

  .el-icon {
    margin-right: 6px;
  }
}

.main-content {
  flex: 1;
  overflow-y: auto;
}
</style>
