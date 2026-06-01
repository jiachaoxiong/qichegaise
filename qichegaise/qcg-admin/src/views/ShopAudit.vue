<template>
  <div>
    <h2>门店审核</h2>
    <el-table :data="shops" style="margin-top:20px" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="门店名称" />
      <el-table-column prop="address" label="地址" />
      <el-table-column prop="phone" label="电话" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'PENDING' ? 'warning' : 'danger'">
            {{ row.status === 'PENDING' ? '待审核' : row.status === 'APPROVED' ? '已通过' : '已拒绝' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="onAudit(row.id, 'APPROVED')">通过</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="onAudit(row.id, 'REJECTED')">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'
const shops = ref([])
const load = async () => { const res = await api.get('/admin/shops'); shops.value = res.data.data || [] }
const onAudit = async (id, status) => { await api.put('/admin/shops/' + id + '/audit', { status }); load(); ElMessage.success('已审核') }
onMounted(load)
</script>
