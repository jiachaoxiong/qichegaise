<template>
  <div class="login-wrapper">
    <el-card class="login-card" header="QCG 管理员登录">
      <el-form @submit.prevent="onLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-button type="primary" @click="onLogin" :loading="loading" style="width:100%">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../utils/api'

const router = useRouter()
const username = ref('admin')
const password = ref('')
const loading = ref(false)

const onLogin = async () => {
  loading.value = true
  try {
    const res = await api.post('/admin/login', { username: username.value, password: password.value })
    localStorage.setItem('admin_token', res.data.data.token)
    router.push('/')
  } catch {
    ElMessage.error('登录失败')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-wrapper { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: #f0f2f5; }
.login-card { width: 400px; }
</style>
