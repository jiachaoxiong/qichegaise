<template>
  <div>
    <h2>控制台</h2>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card><div style="text-align:center">
          <div style="font-size:36px;font-weight:bold;color:#409eff">{{ stat.value }}</div>
          <div style="color:#999;margin-top:8px">{{ stat.label }}</div>
        </div></el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'

const stats = ref([
  { label: '用户总数', value: 0 }, { label: '门店总数', value: 0 },
  { label: '待审核门店', value: 0 }, { label: '预约总数', value: 0 },
  { label: '作品总数', value: 0 }
])

onMounted(async () => {
  try {
    const res = await api.get('/admin/dashboard')
    const d = res.data.data
    stats.value[0].value = d.userCount
    stats.value[1].value = d.shopCount
    stats.value[2].value = d.pendingShopCount
    stats.value[3].value = d.appointmentCount
    stats.value[4].value = d.photoCount
  } catch {}
})
</script>
