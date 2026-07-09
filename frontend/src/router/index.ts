import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTabsStore } from '@/stores/tabs'
import MainLayout from '@/layouts/MainLayout.vue'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/login/LoginView.vue'
import SetupView from '@/views/setup/SetupView.vue'
import SettingsView from '@/views/settings/SettingsView.vue'
import QuickPhraseManageView from '@/views/settings/QuickPhraseManageView.vue'
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
import BatchOutboundView from '@/views/inventory/BatchOutboundView.vue'
import FlowListView from '@/views/inventory/FlowListView.vue'
import AlertListView from '@/views/inventory/AlertListView.vue'
import AiAssistantView from '@/views/ai/AiAssistantView.vue'
import InboundDraftView from '@/views/ai/InboundDraftView.vue'
import VisitDraftView from '@/views/ai/VisitDraftView.vue'

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
      path: '/prescription/:id/print',
      name: 'prescription-print',
      component: PrescriptionPrintView,
      meta: { requiresAuth: true, standalone: true, title: '处方打印' },
    },
    {
      path: '/',
      component: MainLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView,
          meta: { title: '首页' },
        },
        {
          path: 'settings',
          name: 'settings',
          component: SettingsView,
          meta: { title: '设置' },
        },
        {
          path: 'settings/quick-phrases',
          name: 'quick-phrases',
          component: QuickPhraseManageView,
          meta: { title: '快捷语' },
        },
        {
          path: 'medicine',
          name: 'medicine',
          component: MedicineListView,
          meta: { title: '药品' },
        },
        {
          path: 'medicine/import',
          name: 'medicine-import',
          component: MedicineImportView,
          meta: { title: 'Excel 导入' },
        },
        {
          path: 'medicine/:id',
          name: 'medicine-edit',
          component: MedicineEditView,
          meta: { title: '药品' },
        },
        {
          path: 'patient',
          name: 'patient',
          component: PatientListView,
          meta: { title: '患者' },
        },
        {
          path: 'patient/:id',
          name: 'patient-detail',
          component: PatientDetailView,
          meta: { title: '患者' },
        },
        {
          path: 'visit/:id',
          name: 'visit-form',
          component: VisitFormView,
          meta: { title: '病历' },
        },
        {
          path: 'prescription/new',
          name: 'prescription-new',
          component: PrescriptionFormView,
          meta: { title: '新建处方' },
        },
        {
          path: 'prescription/:id',
          name: 'prescription-edit',
          component: PrescriptionFormView,
          meta: { title: '处方' },
        },
        {
          path: 'ai',
          name: 'ai-assistant',
          component: AiAssistantView,
          meta: { title: 'AI 助手' },
        },
        {
          path: 'ai/drafts/inbound/:id',
          name: 'inbound-draft',
          component: InboundDraftView,
          meta: { title: 'OCR 入库草稿' },
        },
        {
          path: 'ai/drafts/visit/:id',
          name: 'visit-draft',
          component: VisitDraftView,
          meta: { title: '病历草稿' },
        },
        {
          path: 'inventory/inbound',
          name: 'inventory-inbound',
          component: InboundView,
          meta: { title: '入库' },
        },
        {
          path: 'inventory/outbound/batch',
          name: 'inventory-batch-outbound',
          component: BatchOutboundView,
          meta: { title: '批量出库' },
        },
        {
          path: 'inventory/outbound',
          name: 'inventory-outbound',
          component: OutboundView,
          meta: { title: '出库' },
        },
        {
          path: 'inventory/flows',
          name: 'inventory-flows',
          component: FlowListView,
          meta: { title: '库存流水' },
        },
        {
          path: 'inventory/alerts',
          name: 'inventory-alerts',
          component: AlertListView,
          meta: { title: '库存预警' },
        },
      ],
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

router.afterEach((to) => {
  if (to.meta.public || to.meta.standalone) return
  if (to.name === 'login' || to.name === 'setup') return
  useTabsStore().syncFromRoute(to)
})

export default router
