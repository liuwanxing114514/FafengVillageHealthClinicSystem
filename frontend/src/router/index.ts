import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/login/LoginView.vue'
import SetupView from '@/views/setup/SetupView.vue'
import SettingsView from '@/views/settings/SettingsView.vue'
import MedicineListView from '@/views/medicine/MedicineListView.vue'
import MedicineEditView from '@/views/medicine/MedicineEditView.vue'
import MedicineImportView from '@/views/medicine/MedicineImportView.vue'
import PatientListView from '@/views/patient/PatientListView.vue'
import PatientDetailView from '@/views/patient/PatientDetailView.vue'
import VisitFormView from '@/views/visit/VisitFormView.vue'
import PrescriptionFormView from '@/views/prescription/PrescriptionFormView.vue'
import PrescriptionPrintView from '@/views/prescription/PrescriptionPrintView.vue'
import InboundView from '@/views/inventory/InboundView.vue'
import OutboundView from '@/views/inventory/OutboundView.vue'
import FlowListView from '@/views/inventory/FlowListView.vue'
import AlertListView from '@/views/inventory/AlertListView.vue'

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
      path: '/medicine/import',
      name: 'medicine-import',
      component: MedicineImportView,
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
    {
      path: '/prescription/new',
      name: 'prescription-new',
      component: PrescriptionFormView,
      meta: { requiresAuth: true },
    },
    {
      path: '/prescription/:id/print',
      name: 'prescription-print',
      component: PrescriptionPrintView,
      meta: { requiresAuth: true },
    },
    {
      path: '/prescription/:id',
      name: 'prescription-edit',
      component: PrescriptionFormView,
      meta: { requiresAuth: true },
    },
    {
      path: '/inventory/inbound',
      name: 'inventory-inbound',
      component: InboundView,
      meta: { requiresAuth: true },
    },
    {
      path: '/inventory/outbound',
      name: 'inventory-outbound',
      component: OutboundView,
      meta: { requiresAuth: true },
    },
    {
      path: '/inventory/flows',
      name: 'inventory-flows',
      component: FlowListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/inventory/alerts',
      name: 'inventory-alerts',
      component: AlertListView,
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
