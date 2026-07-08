import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/login/LoginView.vue'
import SetupView from '@/views/setup/SetupView.vue'
import SettingsView from '@/views/settings/SettingsView.vue'
import MedicineListView from '@/views/medicine/MedicineListView.vue'
import MedicineEditView from '@/views/medicine/MedicineEditView.vue'
import PatientListView from '@/views/patient/PatientListView.vue'
import PatientDetailView from '@/views/patient/PatientDetailView.vue'
import VisitFormView from '@/views/visit/VisitFormView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/setup',
      name: 'setup',
      component: SetupView,
      meta: { public: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true },
    },
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/settings',
      name: 'settings',
      component: SettingsView,
      meta: { requiresAuth: true },
    },
    {
      path: '/medicine',
      name: 'medicine',
      component: MedicineListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/medicine/:id',
      name: 'medicine-edit',
      component: MedicineEditView,
      meta: { requiresAuth: true },
    },
    {
      path: '/patient',
      name: 'patient',
      component: PatientListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/patient/:id',
      name: 'patient-detail',
      component: PatientDetailView,
      meta: { requiresAuth: true },
    },
    {
      path: '/visit/:id',
      name: 'visit',
      component: VisitFormView,
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) {
    await auth.loadBootstrap()
  }

  if (auth.needSetup && to.name !== 'setup') {
    return { name: 'setup' }
  }

  if (!auth.needSetup && to.name === 'setup') {
    return auth.authenticated ? { name: 'home' } : { name: 'login' }
  }

  if (to.meta.requiresAuth && !auth.authenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.name === 'login' && auth.authenticated) {
    return { name: 'home' }
  }

  return true
})

export default router
