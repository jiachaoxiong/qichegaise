<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center">
      <h2>颜色管理</h2>
      <el-button type="primary" @click="showDialog = true">添加颜色</el-button>
    </div>
    <el-table :data="colors" style="margin-top:20px" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="色值" width="80">
        <template #default="{row}"><div :style="{width:'30px',height:'30px',borderRadius:'4px',background:row.hexCode}" /></template>
      </el-table-column>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="category" label="分类" />
      <el-table-column prop="hexCode" label="色号" />
      <el-table-column label="操作" width="80">
        <template #default="{row}"><el-button size="small" type="danger" @click="onDelete(row.id)">删除</el-button></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" title="添加颜色" width="400px">
      <el-form><el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="色号"><el-input v-model="form.hexCode" placeholder="#FF0000" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" placeholder="哑光/亮光/金属" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="onAdd">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'

const colors = ref([])
const showDialog = ref(false)
const form = ref({ name: '', hexCode: '#', category: '' })

const load = async () => { const res = await api.get('/colors'); colors.value = res.data.data || [] }
const onDelete = async (id) => { await api.delete('/admin/colors/' + id); load(); ElMessage.success('已删除') }
const onAdd = async () => {
  await api.post('/admin/colors', form.value)
  showDialog.value = false
  form.value = { name: '', hexCode: '#', category: '' }
  load()
  ElMessage.success('已添加')
}
onMounted(load)
</script>
