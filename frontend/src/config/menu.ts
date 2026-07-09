export interface MenuItem {
  path: string
  title: string
  icon: string
}

/** 左侧固定菜单（不含动态详情页） */
export const MENU_ITEMS: MenuItem[] = [
  { path: '/', title: '首页', icon: 'HomeFilled' },
  { path: '/patient', title: '患者', icon: 'User' },
  { path: '/medicine', title: '药品', icon: 'FirstAidKit' },
  { path: '/inventory/inbound', title: '入库', icon: 'Download' },
  { path: '/inventory/outbound', title: '出库', icon: 'Upload' },
  { path: '/inventory/flows', title: '库存流水', icon: 'List' },
  { path: '/inventory/alerts', title: '库存预警', icon: 'Warning' },
  { path: '/ai', title: 'AI 助手', icon: 'ChatDotRound' },
  { path: '/settings', title: '设置', icon: 'Setting' },
]

export const MAX_TABS = 10
