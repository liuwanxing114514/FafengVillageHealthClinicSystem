<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createPatient } from '@/api/patient'
import type { SavePatientPayload } from '@/types/patient'

const visible = defineModel<boolean>('visible', { default: false })

const emit = defineEmits<{
  created: [patient: { id: number; name: string }]
}>()

const saving = ref(false)
const form = reactive({
  name: '',
  gender: 'UNKNOWN' as 'M' | 'F' | 'UNKNOWN',
  phone: '',
  address: '',
})

function resetForm() {
  form.name = ''
  form.gender = 'UNKNOWN'
  form.phone = ''
  form.address = ''
}

watch(visible, (open) => {
  if (open) resetForm()
})

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写姓名')
    return
  }
  saving.value = true
  try {
    const payload: SavePatientPayload = {
      name: form.name.trim(),
      gender: form.gender,
      phone: form.phone.trim(),
      address: form.address.trim(),
    }
    const created = await createPatient(payload)
    ElMessage.success('患者已创建')
    emit('created', { id: created.id, name: created.name })
    visible.value = false
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="快速新建患者" width="480px" destroy-on-close>
    <el-form label-width="80px">
      <el-form-item label="姓名" required>
        <el-input v-model="form.name" placeholder="患者姓名" />
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio value="M">男</el-radio>
          <el-radio value="F">女</el-radio>
          <el-radio value="UNKNOWN">未知</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="电话">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="住址">
        <el-input v-model="form.address" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">保存并关联</el-button>
    </template>
  </el-dialog>
</template>
