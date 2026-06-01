<template><div><h2>预约管理</h2><el-table :data="appointments" border>
  <el-table-column prop="id" label="ID" width="80"/>
  <el-table-column prop="userName" label="用户"/>
  <el-table-column prop="shopName" label="门店"/>
  <el-table-column prop="status" label="状态"/>
</el-table></div></template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'
const appointments = ref([])
onMounted(async () => { const res = await api.get('/admin/appointments'); appointments.value = res.data.data || [] })
</script>
