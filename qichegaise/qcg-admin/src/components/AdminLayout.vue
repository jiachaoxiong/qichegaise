<template>
  <el-container style="min-height:100vh">
    <el-aside width="220px" style="background:#304156">
      <div style="color:#fff;text-align:center;padding:20px;font-size:20px;font-weight:bold">QCG 管理后台</div>
      <el-menu router :default-active="$route.path" background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/"><el-icon><DataAnalysis /></el-icon>控制台</el-menu-item>
        <el-menu-item index="/colors"><el-icon><Picture /></el-icon>颜色管理</el-menu-item>
        <el-menu-item index="/car-models"><el-icon><Van /></el-icon>车型管理</el-menu-item>
        <el-menu-item index="/shops"><el-icon><Shop /></el-icon>门店审核</el-menu-item>
        <el-menu-item index="/cases"><el-icon><FolderOpened /></el-icon>案例审核</el-menu-item>
        <el-menu-item index="/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/appointments"><el-icon><Calendar /></el-icon>预约管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff;display:flex;align-items:center;justify-content:flex-end;border-bottom:1px solid #e6e6e6">
        <el-button @click="onLogout">退出登录</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
const onLogout = () => { localStorage.removeItem('admin_token'); router.push('/login') }
</script>
