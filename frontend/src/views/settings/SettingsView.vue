<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/auth'
import { fetchSettings, updateSetting } from '@/api/settings'
import type { SettingItem } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const settings = ref<SettingItem[]>([])
const pwdForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const clinicName = ref('')
const printTemplate = ref('default-a4')

async function loadSettings() {
  settings.value = await fetchSettings()
  clinicName.value = settings.value.find((s) => s.key === 'clinic_name')?.value ?? ''
  printTemplate.value =
    settings.value.find((s) => s.key === 'prescription_print_active_template')?.value ?? 'default-a4'
}

async function onSavePrintTemplate() {
  loading.value = true
  try {
    await updateSetting('prescription_print_active_template', printTemplate.value)
    ElMessage.success('处方打印模板已保存')
    await loadSettings()
  } finally {
    loading.value = false
  }
}

async function onChangePassword() {
  loading.value = true
  try {
    await changePassword(
      pwdForm.currentPassword,
      pwdForm.newPassword,
      pwdForm.confirmPassword,
    )
    ElMessage.success('密码已修改')
    pwdForm.currentPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } finally {
    loading.value = false
  }
}

async function onSaveClinicName() {
  loading.value = true
  try {
    await updateSetting('clinic_name', clinicName.value)
    ElMessage.success('诊所名称已保存')
    await loadSettings()
  } finally {
    loading.value = false
  }
}

async function onLogout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  await router.replace('/login')
}

onMounted(loadSettings)
</script>

<template>
  <main class="page">
    <el-card class="card" shadow="hover">
      <template #header>
        <span class="title">系统设置</span>
      </template>

      <h3 class="section">修改密码</h3>
      <el-form label-width="100px" @submit.prevent="onChangePassword">
        <el-form-item label="当前密码">
          <el-input v-model="pwdForm.currentPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">保存密码</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3 class="section">基础设置</h3>
      <el-form label-width="100px" @submit.prevent="onSaveClinicName">
        <el-form-item label="诊所名称">
          <el-input v-model="clinicName" placeholder="发凤村卫生室" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">保存设置</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3 class="section">处方打印</h3>
      <el-form label-width="120px" @submit.prevent="onSavePrintTemplate">
        <el-form-item label="打印模板">
          <el-select v-model="printTemplate" style="width: 100%">
            <el-option label="A4 通用模板（默认）" value="default-a4" />
            <el-option label="预印纸对齐（仅打印数据）" value="preprinted-fafeng" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">保存打印模板</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3 class="section">快捷语</h3>
      <p class="hint">管理病历录入常用文本，也可在录入页点击候选语快速填入。</p>
      <el-button type="primary" plain @click="router.push('/settings/quick-phrases')">
        管理快捷语
      </el-button>

      <el-divider />

      <el-button type="danger" plain @click="onLogout">退出登录</el-button>
    </el-card>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px;
  display: flex;
  justify-content: center;
}

.card {
  width: min(560px, 100%);
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 1.25rem;
  font-weight: 600;
}

.section {
  margin: 0 0 12px;
  font-size: 1rem;
  color: #303133;
}

.hint {
  margin: 0 0 12px;
  color: #909399;
  font-size: 14px;
}
</style>
