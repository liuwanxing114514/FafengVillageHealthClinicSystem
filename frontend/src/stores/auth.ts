import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchSession, fetchSetupStatus, login as apiLogin, logout as apiLogout } from '@/api/auth'
import { useTabsStore } from '@/stores/tabs'

export const useAuthStore = defineStore('auth', () => {
  const needSetup = ref<boolean | null>(null)
  const authenticated = ref(false)
  const operator = ref<string | null>(null)
  const initialized = ref(false)

  async function loadBootstrap() {
    const status = await fetchSetupStatus()
    needSetup.value = status.needSetup
    if (!status.needSetup) {
      const session = await fetchSession()
      authenticated.value = session.authenticated
      operator.value = session.operator
    } else {
      authenticated.value = false
      operator.value = null
    }
    initialized.value = true
  }

  async function login(password: string) {
    await apiLogin(password)
    authenticated.value = true
    operator.value = 'admin'
    needSetup.value = false
  }

  async function logout() {
    await apiLogout()
    authenticated.value = false
    operator.value = null
    useTabsStore().reset()
  }

  return {
    needSetup,
    authenticated,
    operator,
    initialized,
    loadBootstrap,
    login,
    logout,
  }
})
