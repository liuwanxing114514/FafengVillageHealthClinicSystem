import { getData, putData } from '@/api/http'
import type { SettingItem } from '@/types/api'

export function fetchSettings() {
  return getData<SettingItem[]>('/settings')
}

export function updateSetting(key: string, value: string) {
  return putData<SettingItem>(`/settings/${encodeURIComponent(key)}`, { value })
}
