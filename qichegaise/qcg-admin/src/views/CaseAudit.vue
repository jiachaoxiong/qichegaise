<template>
  <div>
    <h2>案例管理</h2>
    <el-table :data="cases" border v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="施工前" width="120">
        <template #default="{row}"><img v-if="row.beforeUrl" :src="row.beforeUrl" style="width:100px;height:70px;object-fit:cover;border-radius:4px"/></template>
      </el-table-column>
      <el-table-column label="施工后" width="120">
        <template #default="{row}"><img v-if="row.afterUrl" :src="row.afterUrl" style="width:100px;height:70px;object-fit:cover;border-radius:4px"/></template>
      </el-table-column>
      <el-table-column prop="shopName" label="门店" width="150" />
      <el-table-column prop="carModelName" label="车型" width="130" />
      <el-table-column prop="colorName" label="颜色" width="100">
        <template #default="{row}"><el-tag size="small">{{row.colorName||'-'}}</el-tag></template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="150" />
      <el-table-column prop="likes" label="点赞" width="70" />
      <el-table-column label="操作" width="80">
        <template #default="{row}"><el-button size="small" type="danger" @click="onDel(row.id)">删除</el-button></template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'
const cases = ref([])
const loading = ref(false)
const load = async () => { loading.value=true;try{const r=await api.get('/admin/cases');cases.value=r.data.data||[]}catch{}finally{loading.value=false} }
const onDel = async (id) => { await ElMessageBox.confirm('删除该案例？','提示',{type:'warning'}); try{await api.delete('/admin/cases/'+id);ElMessage.success('已删除');load()}catch{} }
onMounted(load)
</script>
