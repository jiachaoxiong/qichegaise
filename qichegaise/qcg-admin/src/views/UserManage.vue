<template>
  <div>
    <h2>用户管理</h2>
    <el-table :data="users" style="margin-top:20px" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{row}"><el-tag>{{ row.role === 'ADMIN' ? '管理员' : row.role === 'SHOP' ? '门店主' : '用户' }}</el-tag></template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'
const users = ref([])
onMounted(async () => { const res = await api.get('/admin/users'); users.value = res.data.data || [] })
</script>
