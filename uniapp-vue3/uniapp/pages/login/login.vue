<template>
  <view class="login-page">
    <!-- Logo & 标题 -->
    <view class="header">
      <view class="logo">🏃</view>
      <text class="app-name">校园帮</text>
      <text class="slogan">互助跑腿，让校园更便捷</text>
    </view>

    <!-- 微信一键登录按钮 -->
    <view class="login-area">
      <button
        class="wechat-btn"
        :loading="loading"
        :disabled="loading"
        @click="handleWechatLogin"
      >
        <text class="wechat-icon">💬</text>
        <text>微信一键登录</text>
      </button>

      <text class="agreement-text">
        登录即表示同意
        <text class="link">《用户协议》</text>和<text class="link">《隐私政策》</text>
      </text>
    </view>

    <!-- 测试登录（手机预览用） -->
    <view class="other-login">
      <text class="divider">———— 测试登录 ————</text>
      <button class="phone-btn" @click="handleTestLogin">
        <text>🔑 测试登录（免微信授权）</text>
      </button>
    </view>
  </view>
</template>

<script>
const api = require('../../api/request.js')

export default {
  data() {
    return {
      loading: false
    }
  },
  onShow() {
    // 检查是否已登录，有token则跳转首页
    const token = uni.getStorageSync('token')
    if (token) {
      uni.switchTab({ url: '/pages/index/index' })
    }
  },
  methods: {
    /**
     * 微信一键登录
     */
    async handleWechatLogin() {
      this.loading = true
      try {
        // 1. 调用微信登录获取 code
        const loginRes = await this.getWechatCode()
        if (!loginRes.code) {
          throw new Error('获取微信授权失败')
        }

        // 2. 调用后端换取 token
        const res = await api.post('/auth/wechat', {
          code: loginRes.code
        })

        // 3. 存储登录信息
        this.saveLoginInfo(res.data)

        // 4. 跳转首页
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 500)

      } catch (err) {
        console.error('登录失败:', err)
        uni.showToast({
          title: err.message || '登录失败，请重试',
          icon: 'none'
        })
      } finally {
        this.loading = false
      }
    },

    /**
     * 获取微信登录 code
     */
    getWechatCode() {
      return new Promise((resolve) => {
        // 尝试真实微信登录，失败自动降级为模拟登录
        uni.login({
          provider: 'weixin',
          success: resolve,
          fail: () => {
            // 真微信登录失败→降级用模拟code
            console.log('微信登录失败，使用模拟登录')
            resolve({ code: 'mock_code_' + Date.now() })
          }
        })
        // 3秒超时兜底
        setTimeout(() => {
          resolve({ code: 'mock_code_' + Date.now() })
        }, 3000)
      })
    },

    /**
     * 获取微信用户信息（需用户授权）
     */
    getWechatUserInfo() {
      return new Promise((resolve) => {
        // #ifdef MP-WEIXIN
        uni.getUserProfile({
          desc: '用于完善用户资料',
          success: (res) => {
            resolve({
              nickname: res.userInfo.nickName,
              avatar: res.userInfo.avatarUrl
            })
          },
          fail: () => resolve(null)
        })
        // #endif

        // #ifndef MP-WEIXIN
        resolve(null)
        // #endif
      })
    },

    /**
     * 保存登录信息
     */
    saveLoginInfo(data) {
      if (data.token) {
        uni.setStorageSync('token', data.token)
      }
      if (data.userId) {
        uni.setStorageSync('userId', data.userId)
      }
      uni.setStorageSync('userInfo', JSON.stringify({
        userId: data.userId,
        nickname: data.nickname || '校园帮用户',
        avatar: data.avatar || '',
        creditScore: data.creditScore || 80,
        isRealname: data.isRealname || 0,
        roles: data.roles || 'publisher,runner'
      }))
    },

    /**
     * 手机号登录（备用）
     */
    handleTestLogin() {
      this.loading = true
      api.post('/auth/wechat', { code: 'mock_phone_' + Date.now() })
        .then(res => {
          this.saveLoginInfo(res.data)
          uni.showToast({ title: '登录成功', icon: 'success' })
          setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500)
        })
        .catch(err => {
          console.error('登录失败:', err)
          const msg = (err && err.data && err.data.message) ? err.data.message : '网络错误，请稍后重试'
          uni.showToast({ title: msg, icon: 'none' })
        })
        .finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 30px;
}

.header {
  text-align: center;
  margin-bottom: 80px;
}

.logo {
  font-size: 80px;
  display: block;
  margin-bottom: 20px;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.app-name {
  font-size: 40px;
  font-weight: 700;
  color: #fff;
  display: block;
  letter-spacing: 4px;
}

.slogan {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  display: block;
  margin-top: 12px;
}

.login-area {
  width: 100%;
  max-width: 320px;
}

.wechat-btn {
  width: 100%;
  height: 50px;
  background: #fff;
  border-radius: 25px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 600;
  color: #333;
  border: none;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
}

.wechat-btn::after {
  border: none;
}

.wechat-icon {
  font-size: 22px;
  margin-right: 8px;
}

.agreement-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
  display: block;
  margin-top: 16px;
  line-height: 1.6;
}

.link {
  color: rgba(255, 255, 255, 0.9);
}

.other-login {
  width: 100%;
  max-width: 320px;
  margin-top: 60px;
  text-align: center;
}

.divider {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  display: block;
  margin-bottom: 20px;
}

.phone-btn {
  width: 100%;
  height: 44px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.phone-btn::after {
  border: none;
}
</style>
