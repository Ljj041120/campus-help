<template>
  <view class="user-page">
    <!-- 头部信息卡片 -->
    <view class="profile-card">
      <view class="card-bg"></view>
      <view class="profile-info" @click="handleEditProfile">
        <image class="avatar" :src="userInfo.avatar || '/static/logo.png'" mode="aspectFill" />
        <view class="info-text">
          <text class="nickname">{{ userInfo.nickname || '校园帮用户' }}</text>
          <view class="credit-row">
            <text class="credit-label">信用分</text>
            <text class="credit-score" :class="creditClass">{{ userInfo.creditScore || 80 }}</text>
          </view>
        </view>
        <text class="edit-icon">⚙️</text>
      </view>

      <!-- 实名认证状态 -->
      <view class="realname-bar" @click="handleRealName">
        <text class="rn-icon">{{ userInfo.isRealname ? '✅' : '⚠️' }}</text>
        <text class="rn-text">{{ userInfo.isRealname ? '已实名认证' : '未实名认证（点击认证）' }}</text>
        <text class="rn-arrow">›</text>
      </view>
    </view>

    <!-- 角色切换 -->
    <view class="role-switch-card">
      <text class="section-label">当前角色</text>
      <view class="role-options">
        <view
          class="role-item"
          :class="{ active: currentRole === 'publisher' }"
          @click="switchRole('publisher')"
        >
          <text class="role-emoji">📝</text>
          <text class="role-name">发布者</text>
          <text class="role-desc">发布任务</text>
        </view>
        <view
          class="role-item"
          :class="{ active: currentRole === 'runner' }"
          @click="switchRole('runner')"
        >
          <text class="role-emoji">🏃</text>
          <text class="role-name">跑腿员</text>
          <text class="role-desc">接单赚钱</text>
        </view>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-card">
      <view class="menu-item" @click="goPage('/pages/wallet/wallet')">
        <text class="menu-icon">💰</text>
        <text class="menu-text">我的钱包</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/order/order')">
        <text class="menu-icon">📋</text>
        <text class="menu-text">我的订单</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/chat/chat')">
        <text class="menu-icon">💬</text>
        <text class="menu-text">我的消息</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="menu-card">
      <view class="menu-item" @click="handleAbout">
        <text class="menu-icon">ℹ️</text>
        <text class="menu-text">关于我们</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="handleFeedback">
        <text class="menu-icon">📧</text>
        <text class="menu-text">意见反馈</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" @click="handleLogout">
      <text>退出登录</text>
    </view>

    <!-- ======= 实名认证弹窗 ======= -->
    <view class="modal-mask" v-if="showRealNameModal" @click="showRealNameModal = false">
      <view class="modal-card" @click.stop>
        <text class="modal-title">实名认证</text>

        <view class="form-item">
          <text class="form-label">学号/工号</text>
          <input
            class="form-input"
            v-model="realNameForm.studentNo"
            placeholder="请输入学号（8-12位）"
            maxlength="12"
          />
        </view>

        <view class="form-item">
          <text class="form-label">真实姓名</text>
          <input
            class="form-input"
            v-model="realNameForm.name"
            placeholder="请输入姓名"
            maxlength="10"
          />
        </view>

        <view class="form-item">
          <text class="form-label">学生证照片</text>
          <view class="upload-area" @click="handleUploadPhoto">
            <image v-if="realNameForm.idCardPhoto" :src="realNameForm.idCardPhoto" class="upload-preview" mode="aspectFit" />
            <text v-else class="upload-placeholder">📷 点击上传学生证</text>
          </view>
        </view>

        <view class="modal-btns">
          <button class="btn-cancel" @click="showRealNameModal = false">取消</button>
          <button class="btn-submit" :loading="submitting" @click="submitRealName">提交认证</button>
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
        userId: null,
        nickname: '',
        avatar: '',
        creditScore: 80,
        isRealname: 0,
        roles: ''
      },
      currentRole: 'publisher',
      showRealNameModal: false,
      realNameForm: {
        studentNo: '',
        name: '',
        idCardPhoto: ''
      },
      submitting: false
    }
  },
  computed: {
    creditClass() {
      const score = this.userInfo.creditScore || 80
      if (score >= 90) return 'high'
      if (score >= 70) return 'normal'
      return 'low'
    }
  },
  onShow() {
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.loadUserInfo()
  },
  methods: {
    /**
     * 加载用户信息
     */
    async loadUserInfo() {
      try {
        const cached = uni.getStorageSync('userInfo')
        if (cached) {
          this.userInfo = JSON.parse(cached)
          // 解析角色
          if (this.userInfo.roles && this.userInfo.roles.includes('publisher')) {
            this.currentRole = 'publisher'
          }
        }

        // 从服务器拉取最新信息
        const res = await api.get('/auth/info')
        if (res && res.data) {
          this.userInfo = {
            ...this.userInfo,
            ...res.data
          }
          uni.setStorageSync('userInfo', JSON.stringify(this.userInfo))
        }
      } catch (err) {
        console.error('获取用户信息失败:', err)
      }
    },

    /**
     * 切换角色
     */
    switchRole(role) {
      this.currentRole = role
      uni.setStorageSync('currentRole', role)
      uni.showToast({
        title: role === 'publisher' ? '已切换为发布者' : '已切换为跑腿员',
        icon: 'none'
      })
    },

    /**
     * 实名认证
     */
    handleRealName() {
      if (this.userInfo.isRealname) {
        uni.showToast({ title: '您已完成实名认证', icon: 'none' })
        return
      }
      this.showRealNameModal = true
    },

    /**
     * 上传学生证照片
     */
    handleUploadPhoto() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: async (res) => {
          const filePath = res.tempFilePaths[0]
          // 上传到服务器
          try {
            uni.showLoading({ title: '上传中...' })
            const uploadRes = await uni.uploadFile({
              url: api.getBaseUrl() + '/file/upload',
              filePath: filePath,
              name: 'file',
              header: {
                'Authorization': 'Bearer ' + uni.getStorageSync('token')
              }
            })
            uni.hideLoading()
            const data = JSON.parse(uploadRes.data)
            if (data.code === 200 && data.data) {
              this.realNameForm.idCardPhoto = data.data.url
            }
          } catch (err) {
            uni.hideLoading()
            uni.showToast({ title: '上传失败', icon: 'none' })
          }
        }
      })
    },

    /**
     * 提交实名认证
     */
    async submitRealName() {
      if (!this.realNameForm.studentNo || this.realNameForm.studentNo.length < 8) {
        uni.showToast({ title: '请输入正确的学号', icon: 'none' })
        return
      }
      if (!this.realNameForm.name || this.realNameForm.name.length < 2) {
        uni.showToast({ title: '请输入姓名', icon: 'none' })
        return
      }

      this.submitting = true
      try {
        await api.post('/auth/realname', {
          studentNo: this.realNameForm.studentNo,
          name: this.realNameForm.name,
          idCardPhoto: this.realNameForm.idCardPhoto || ''
        })

        uni.showToast({ title: '提交成功，等待审核', icon: 'success' })
        this.showRealNameModal = false

        // 刷新用户信息
        setTimeout(() => this.loadUserInfo(), 1000)

      } catch (err) {
        uni.showToast({ title: err.message || '提交失败', icon: 'none' })
      } finally {
        this.submitting = false
      }
    },

    /**
     * 跳转页面
     */
    goPage(url) {
      uni.navigateTo({ url })
    },

    /**
     * 退出登录
     */
    handleLogout() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            uni.clearStorageSync()
            uni.reLaunch({ url: '/pages/login/login' })
          }
        }
      })
    },

    handleEditProfile() {
      uni.showToast({ title: '编辑资料功能开发中', icon: 'none' })
    },

    handleAbout() {
      uni.showModal({
        title: '校园帮 v1.0',
        content: '校园互助跑腿平台\n让校园生活更便捷\n\n© 2026 CampusHelp',
        showCancel: false
      })
    },

    handleFeedback() {
      uni.showToast({ title: '意见反馈功能开发中', icon: 'none' })
    }
  }
}
</script>

<style scoped>
.user-page {
  min-height: 100vh;
  background: #f5f6fa;
  padding-bottom: 30px;
}

/* ---- 个人信息卡片 ---- */
.profile-card {
  margin: 0;
  position: relative;
  overflow: hidden;
}

.card-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 200px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 0 30px 30px;
}

.profile-info {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  padding: 40px 25px 20px;
}

.avatar {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.5);
  background: #eee;
}

.info-text {
  flex: 1;
  margin-left: 15px;
}

.nickname {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  display: block;
}

.credit-row {
  display: flex;
  align-items: center;
  margin-top: 8px;
}

.credit-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-right: 8px;
}

.credit-score {
  font-size: 16px;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 10px;
  color: #fff;
}

.credit-score.high { background: #4caf50; }
.credit-score.normal { background: #ff9800; }
.credit-score.low { background: #f44336; }

.edit-icon {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.6);
  padding: 10px;
}

.realname-bar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  margin: 0 20px 15px;
  padding: 10px 15px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
}

.rn-icon { font-size: 16px; margin-right: 8px; }
.rn-text { flex: 1; font-size: 14px; color: #fff; }
.rn-arrow { font-size: 20px; color: rgba(255, 255, 255, 0.6); }

/* ---- 角色切换 ---- */
.role-switch-card {
  margin: 15px;
  background: #fff;
  border-radius: 15px;
  padding: 15px 20px;
}

.section-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 12px;
  display: block;
}

.role-options {
  display: flex;
  gap: 12px;
}

.role-item {
  flex: 1;
  padding: 15px;
  border-radius: 12px;
  text-align: center;
  border: 2px solid #eee;
  transition: all 0.2s;
}

.role-item.active {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.08);
}

.role-emoji {
  font-size: 28px;
  display: block;
  margin-bottom: 6px;
}

.role-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  display: block;
}

.role-desc {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
  display: block;
}

/* ---- 菜单 ---- */
.menu-card {
  margin: 15px;
  background: #fff;
  border-radius: 15px;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 20px;
  margin-right: 12px;
  width: 24px;
  text-align: center;
}

.menu-text {
  flex: 1;
  font-size: 15px;
  color: #333;
}

.menu-arrow {
  font-size: 20px;
  color: #ccc;
}

/* ---- 退出 ---- */
.logout-btn {
  margin: 30px 15px;
  height: 48px;
  background: #fff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f44336;
  font-size: 15px;
}

/* ---- 实名认证弹窗 ---- */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.modal-card {
  background: #fff;
  border-radius: 20px 20px 0 0;
  width: 100%;
  padding: 25px 20px 40px;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 25px;
  display: block;
}

.form-item {
  margin-bottom: 18px;
}

.form-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
  display: block;
}

.form-input {
  height: 44px;
  background: #f5f6fa;
  border-radius: 10px;
  padding: 0 15px;
  font-size: 14px;
}

.upload-area {
  height: 120px;
  background: #f5f6fa;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #ddd;
  overflow: hidden;
}

.upload-preview {
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  font-size: 16px;
  color: #999;
}

.modal-btns {
  display: flex;
  gap: 12px;
  margin-top: 25px;
}

.btn-cancel {
  flex: 1;
  height: 44px;
  background: #f5f5f5;
  border-radius: 10px;
  font-size: 15px;
  color: #666;
  border: none;
}

.btn-cancel::after { border: none; }

.btn-submit {
  flex: 2;
  height: 44px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 10px;
  font-size: 15px;
  color: #fff;
  font-weight: 600;
  border: none;
}

.btn-submit::after { border: none; }
</style>
