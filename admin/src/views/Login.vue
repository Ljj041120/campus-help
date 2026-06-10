<script setup>
import { useRouter } from 'vue-router'
import { ref } from 'vue'

const router = useRouter()
const loginForm = ref({ username: '', password: '' })

async function handleLogin() {
  try {
    const res = await fetch('/api/admin/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(loginForm.value)
    })
    const data = await res.json()
    if (data.code === 200 && data.data && data.data.token) {
      localStorage.setItem('adminToken', data.data.token)
      router.push('/dashboard')
    } else {
      // fallback: 硬编码登录
      if (loginForm.value.username === 'admin' && loginForm.value.password === 'admin123') {
        localStorage.setItem('adminToken', 'admin-token')
        router.push('/dashboard')
        return
      }
      ElMessage.error(data.message || '用户名或密码错误')
    }
  } catch (e) {
    // API 未启动时的 fallback
    if (loginForm.value.username === 'admin' && loginForm.value.password === 'admin123') {
      localStorage.setItem('adminToken', 'admin-token')
      router.push('/dashboard')
    } else {
      ElMessage.error('用户名或密码错误（admin/admin123）')
    }
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card glass">
      <div class="login-header">
        <h1>🏃 校园帮</h1>
        <p>管理后台</p>
      </div>

      <el-form :model="loginForm" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="loginForm.username"
            placeholder="用户名 (admin)"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码 (admin123)"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.glass {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
}

.login-card {
  padding: 60px 50px;
  width: 420px;

  .login-header {
    text-align: center;
    margin-bottom: 40px;

    h1 {
      font-size: 36px;
      font-weight: 700;
      color: #fff;
      margin-bottom: 8px;
    }
    p {
      font-size: 16px;
      color: rgba(255, 255, 255, 0.7);
    }
  }
}
</style>
