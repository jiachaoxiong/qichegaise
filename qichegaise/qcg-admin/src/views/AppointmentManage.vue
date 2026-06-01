<template>
  <div>
    <h2>预约管理 ({{appointments.length}})</h2>
    <el-table :data="appointments" border v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="userName" label="用户" width="100" />
      <el-table-column prop="shopName" label="门店" width="150" />
      <el-table-column prop="colorName" label="意向颜色" width="100">
        <template #default="{row}">
          <span v-if="row.colorName">
            <span :style="{display:'inline-block',width:'12px',height:'12px',borderRadius:'2px',background:row.colorHex||'#ccc',marginRight:'6px'}"></span>
            {{row.colorName}}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="appointmentTime" label="预约时间" width="160">
        <template #default="{row}">{{row.appointmentTime?.substring(0,16)||'-'}}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status==='CONFIRMED'?'success':row.status==='CANCELLED'?'danger':row.status==='COMPLETED'?'info':'warning'">
            {{row.status==='PENDING'?'待确认':row.status==='CONFIRMED'?'已确认':row.status==='CANCELLED'?'已取消':'已完成'}}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" />
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button v-if="row.status==='PENDING'" size="small" type="success" @click="onStatus(row,'CONFIRMED')">确认</el-button>
          <el-button v-if="row.status==='CONFIRMED'" size="small" type="primary" @click="onStatus(row,'COMPLETED')">完成</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'
const appointments = ref([])
const loading = ref(false)
const load = async () => { loading.value=true;try{const r=await api.get('/admin/appointments');appointments.value=r.data.data||[]}catch{}finally{loading.value=false} }
const onStatus = async (row, status) => { try{await api.put('/appointments/'+row.id+'/status',{status});load();ElMessage.success('已更新')}catch{} }
onMounted(load)
</script>
