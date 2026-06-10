<template>
  <view class="index-page">
    <!-- 顶部区域 -->
    <view class="header">
      <view class="header-top">
        <view class="user-greeting" @click="goUser">
          <image class="mini-avatar" :src="userInfo.avatar || '/static/logo.png'" mode="aspectFill" />
          <view class="greeting-text">
            <text class="hi">Hi, {{ userInfo.nickname || '同学' }}</text>
            <text class="role-badge" :class="currentRole">{{ currentRole === 'publisher' ? '发布者' : '跑腿员' }}</text>
          </view>
        </view>
        <view class="header-actions">
          <text class="action-icon" @click="switchRole">🔄</text>
        </view>
      </view>
      <!-- 搜索/提示条 -->
      <view class="tip-bar">
        <text>{{ currentRole === 'publisher' ? '发布跑腿任务，找人帮你忙 💪' : '看看附近有什么单子可接吧 🔍' }}</text>
      </view>
    </view>

    <!-- 发布者视角：快捷发布 -->
    <view class="quick-actions" v-if="currentRole === 'publisher'">
      <view class="publish-btn" @click="goPublish">
        <text class="publish-icon">➕</text>
        <text class="publish-text">发布新任务</text>
      </view>
    </view>

    <!-- 订单大厅 -->
    <view class="hall-section">
      <view class="hall-header">
        <text class="hall-title">{{ currentRole === 'runner' ? '📋 订单大厅' : '📋 最新任务' }}</text>
        <view class="filter-tabs">
          <text
            v-for="t in typeFilters"
            :key="t.value"
            class="filter-tab"
            :class="{ active: filterType === t.value }"
            @click="filterType = t.value; loadOrders()"
          >{{ t.label }}</text>
        </view>
      </view>

      <!-- 订单列表 -->
      <view class="order-list">
        <view
          v-for="(order,i) in orders"
          :key="order.id"
          class="order-card"
          @click="goOrderDetail(i)"
        >
          <view class="order-top">
            <view class="order-type-badge" :class="'type-' + order.orderType">
              {{ getTypeLabel(order.orderType) }}
            </view>
            <text class="order-amount">{{ formatAmount(order.amount) }}</text>
          </view>

          <view class="order-route">
            <view class="route-item">
              <text class="route-dot pickup">取</text>
              <text class="route-addr">{{ order.pickupAddress }}</text>
            </view>
            <view class="route-line"></view>
            <view class="route-item">
              <text class="route-dot delivery">送</text>
              <text class="route-addr">{{ order.deliveryAddress }}</text>
            </view>
          </view>

          <view class="order-bottom">
            <text class="order-time">{{ formatTime(order.createdAt) }}</text>
            <text class="order-platform-fee" v-if="order.platformFee > 0">
              {{ formatFee(order.platformFee) }}
            </text>
          </view>
        </view>

        <!-- 空状态 -->
        <view v-if="orders.length === 0 && !loading" class="empty-state">
          <text class="empty-icon">📭</text>
          <text class="empty-text">暂无订单</text>
          <text class="empty-hint">{{ currentRole === 'publisher' ? '快去发布第一个任务吧' : '附近暂时没有可接的订单' }}</text>
        </view>

        <!-- 加载更多 -->
        <view v-if="hasMore && orders.length > 0" class="load-more" @click="loadMore">
          <text>{{ loading ? '加载中...' : '加载更多' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
const api = require('../../api/request.js')

export default {
  data() {
    return {
      userInfo: {
        nickname: '',
        avatar: '',
        creditScore: 80
      },
      currentRole: 'publisher',
      // 订单大厅
      orders: [],
      filterType: null,
      pageNum: 1,
      pageSize: 10,
      hasMore: true,
      loading: false,
      typeFilters: [
        { label: '全部', value: null },
        { label: '代取快递', value: 1 },
        { label: '代买', value: 2 },
        { label: '代送', value: 3 },
        { label: '其他', value: 4 }
      ]
    }
  },
  onShow() {
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.loadUserInfo()
    this.loadOrders()
  },
  onPullDownRefresh() {
    this.pageNum = 1
    this.orders = []
    this.hasMore = true
    this.loadOrders().then(() => {
      uni.stopPullDownRefresh()
    })
  },
  methods: {
    loadUserInfo() {
      try {
        const cached = uni.getStorageSync('userInfo')
        if (cached) {
          const info = JSON.parse(cached)
          this.userInfo = info
          this.currentRole = uni.getStorageSync('currentRole') || 'publisher'
        }
      } catch (e) {}
    },

    switchRole() {
      const newRole = this.currentRole === 'publisher' ? 'runner' : 'publisher'
      this.currentRole = newRole
      uni.setStorageSync('currentRole', newRole)
      uni.showToast({
        title: '已切换为' + (newRole === 'publisher' ? '发布者' : '跑腿员'),
        icon: 'none'
      })
      this.pageNum = 1
      this.orders = []
      this.hasMore = true
      this.loadOrders()
    },

    async loadOrders() {
      if (this.loading) return
      this.loading = true
      try {
        const res = await api.get('/order/hall', {
          pageNum: this.pageNum,
          pageSize: this.pageSize,
          orderType: this.filterType
        })
        if (res && res.data) {
          const data = res.data
          const records = data.records || data || []
          if (this.pageNum === 1) {
            this.orders = records
          } else {
            this.orders = [...this.orders, ...records]
          }
          const total = data.total || 0
          this.hasMore = this.orders.length < total
        }
      } catch (err) {
        console.error('加载订单失败:', err)
      } finally {
        this.loading = false
      }
    },

    loadMore() {
      if (!this.hasMore || this.loading) return
      this.pageNum++
      this.loadOrders()
    },

    getTypeLabel(type) {
      const map = { 1: '代取快递', 2: '代买物品', 3: '代送物品', 4: '其他' }
      return map[type] || '未知'
    },

    formatTime(time) {
      if (!time) return ''
      const now = Date.now()
      const t = new Date(time).getTime()
      const diff = Math.floor((now - t) / 1000)
      if (diff < 60) return '刚刚'
      if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
      if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
      return new Date(time).toLocaleDateString()
    },

    formatAmount(v){return '¥'+v},
    formatFee(v){return '服务费 ¥'+v},
    goPublish() {
      uni.navigateTo({ url: '/pages/publish/publish' })
    },

    goOrderDetail(i) {
      uni.navigateTo({ url: '/pages/order/order?id=' + this.orders[i].id })
    },

    goUser() {
      uni.switchTab({ url: '/pages/user/user' })
    }
  }
}
</script>

<style scoped>
.index-page {
  min-height: 100vh;
  background: #f5f6fa;
  padding-bottom: 20px;
}

/* ---- 头部 ---- */
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px 20px 25px;
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-greeting {
  display: flex;
  align-items: center;
}

.mini-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.5);
  background: #ddd;
}

.greeting-text {
  margin-left: 12px;
}

.hi {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  display: block;
}

.role-badge {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  margin-top: 4px;
  display: inline-block;
}

.role-badge.publisher {
  background: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.role-badge.runner {
  background: rgba(255, 200, 0, 0.3);
  color: #ffd93d;
}

.action-icon {
  font-size: 22px;
  padding: 8px;
}

.tip-bar {
  margin-top: 15px;
  padding: 10px 15px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 10px;
}

.tip-bar text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

/* ---- 发布按钮 ---- */
.quick-actions {
  margin: -15px 20px 15px;
  position: relative;
  z-index: 1;
}

.publish-btn {
  background: #fff;
  border-radius: 15px;
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.publish-icon {
  font-size: 24px;
  margin-right: 10px;
}

.publish-text {
  font-size: 17px;
  font-weight: 600;
  color: #667eea;
}

/* ---- 订单大厅 ---- */
.hall-section {
  margin: 0 15px;
}

.hall-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.hall-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.filter-tabs {
  display: flex;
  gap: 4px;
}

.filter-tab {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  background: #eee;
  color: #666;
}

.filter-tab.active {
  background: #667eea;
  color: #fff;
}

/* ---- 订单卡片 ---- */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  background: #fff;
  border-radius: 15px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-type-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 8px;
  font-weight: 600;
}

.type-1 { background: #e3f2fd; color: #1976d2; }
.type-2 { background: #fff3e0; color: #f57c00; }
.type-3 { background: #e8f5e9; color: #388e3c; }
.type-4 { background: #f3e5f5; color: #7b1fa2; }

.order-amount {
  font-size: 20px;
  font-weight: 700;
  color: #f44336;
}

.order-route {
  margin-bottom: 12px;
}

.route-item {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.route-dot {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  font-size: 11px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  flex-shrink: 0;
  font-weight: 600;
}

.route-dot.pickup { background: #4caf50; }
.route-dot.delivery { background: #f44336; }

.route-addr {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-line {
  width: 2px;
  height: 16px;
  background: #ddd;
  margin-left: 10px;
  margin-bottom: 4px;
}

.order-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-time {
  font-size: 12px;
  color: #999;
}

.order-platform-fee {
  font-size: 11px;
  color: #bbb;
}

/* ---- 空状态 ---- */
.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 50px;
  display: block;
  margin-bottom: 15px;
}

.empty-text {
  font-size: 16px;
  color: #999;
  display: block;
}

.empty-hint {
  font-size: 13px;
  color: #ccc;
  margin-top: 8px;
  display: block;
}

.load-more {
  text-align: center;
  padding: 15px;
  color: #667eea;
  font-size: 14px;
}
</style>
