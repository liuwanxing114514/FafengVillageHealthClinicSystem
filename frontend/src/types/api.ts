export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface SetupStatus {
  needSetup: boolean
}

export interface SessionInfo {
  authenticated: boolean
  operator: string | null
}

export interface SettingItem {
  key: string
  value: string
  remark: string
}
