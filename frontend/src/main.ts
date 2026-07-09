import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import { registerSW } from 'virtual:pwa-register'
import App from './App.vue'
import router from './router'

registerSW({
  onNeedRefresh() {
    ElMessage.info('发现新版本，刷新页面后生效')
  },
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
