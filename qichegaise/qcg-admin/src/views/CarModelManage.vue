<template>
  <div>
    <h2>车型管理（{{total}} 条）</h2>
    <el-select v-model="brandFilter" placeholder="筛选品牌" clearable @change="load" style="width:200px;margin-bottom:12px">
      <el-option v-for="b in brands" :key="b" :label="b" :value="b" />
    </el-select>

    <el-table :data="pageModels" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="预览" width="90">
        <template #default="{row}">
          <img v-if="row.imageUrl" :src="row.imageUrl" style="width:70px;height:50px;object-fit:cover;border-radius:4px" />
          <span v-else style="font-size:36px">🚗</span>
        </template>
      </el-table-column>
      <el-table-column prop="brandName" label="品牌" width="100" />
      <el-table-column prop="modelName" label="车型" min-width="160" />
      <el-table-column prop="bodyType" label="类型" width="80" />
      <el-table-column prop="year" label="年款" width="70" />
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="onDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showEdit" title="编辑车型" width="480px">
      <div v-if="editRow">
        <p>品牌：<b>{{editRow.brandName}}</b></p>
        <p>车型：<b>{{editRow.modelName}}</b></p>
        <p style="margin-top:12px">车身类型：</p>
        <el-select v-model="editRow.bodyType" clearable style="width:200px">
          <el-option v-for="t in ['SUV','轿车','MPV','跑车','旅行车','皮卡','面包车']" :key="t" :label="t" :value="t" />
        </el-select>
        <p style="margin-top:12px">年款：</p>
        <el-input v-model="editRow.year" placeholder="如 2024" style="width:200px" />
        <p style="margin-top:12px">预览图：</p>
        <img v-if="editRow.imageUrl" :src="editRow.imageUrl" style="width:200px;border-radius:4px;margin-bottom:8px" />
        <el-upload :action="uploadUrl" :headers="uploadHeaders" accept="image/*"
          :show-file-list="false" :on-success="onUploadOk" :before-upload="checkFile">
          <el-button size="small">📷 上传图片</el-button>
        </el-upload>
        <el-input v-model="editRow.imageUrl" placeholder="或粘贴图片URL" style="margin-top:8px" />
      </div>
      <template #footer>
        <el-button @click="showEdit=false">取消</el-button>
        <el-button type="primary" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../utils/api'

const models = ref([])
const brands = ref([])
const brandFilter = ref('')
const loading = ref(false)
const showEdit = ref(false)
const editRow = ref(null)
const total = computed(() => models.value.length)
const pageModels = computed(() => models.value.slice(0, 100))

const uploadUrl = '/api/photos/upload'
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + localStorage.getItem('admin_token') }))

const load = async () => {
  loading.value = true
  let url = '/admin/car-models'
  if (brandFilter.value) url += '?brand=' + encodeURIComponent(brandFilter.value)
  const res = await api.get(url)
  models.value = res.data.data || []
  if (!brandFilter.value) {
    brands.value = [...new Set(models.value.map(m => m.brandName).filter(Boolean))].sort()
  }
  loading.value = false
}

const openEdit = (row) => { editRow.value = {...row}; showEdit.value = true }
const onSave = async () => {
  await api.put('/admin/car-models/' + editRow.value.id, {
    imageUrl: editRow.value.imageUrl || null,
    bodyType: editRow.value.bodyType || null,
    year: editRow.value.year || null
  })
  ElMessage.success('保存成功')
  showEdit.value = false
  load()
}
const onDelete = async (id) => {
  await ElMessageBox.confirm('删除该车型？', '提示', {type:'warning'})
  await api.delete('/admin/car-models/' + id)
  ElMessage.success('已删除')
  load()
}
const checkFile = (f) => {
  if (!f.type.startsWith('image/')) { ElMessage.error('仅支持图片'); return false }
  if (f.size > 10485760) { ElMessage.error('不超过10MB'); return false }
  return true
}
const onUploadOk = (res) => {
  if (res.data) editRow.value.imageUrl = res.data.originalUrl
  ElMessage.success('上传成功')
}
onMounted(load)
</script>
