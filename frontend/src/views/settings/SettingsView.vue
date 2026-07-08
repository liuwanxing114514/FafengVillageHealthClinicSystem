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

async function loadSettings() {
  settings.value = await fetchSettings()
  clinicName.value = settings.value.find((s) => s.key === 'clinic_name')?.value ?? ''
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
</style>
