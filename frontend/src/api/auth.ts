import { getData, postData } from '@/api/http'
import type { SessionInfo, SetupStatus } from '@/types/api'

export function fetchSetupStatus() {
  return getData<SetupStatus>('/system/setup-status')
}

export function setupPassword(password: string, confirmPassword: string) {
  return postData<void>('/system/setup-password', { password, confirmPassword })
}

export function login(password: string) {
  return postData<void>('/auth/login', { password })
}

export function logout() {
  return postData<void>('/auth/logout')
}

export function fetchSession() {
  return getData<SessionInfo>('/auth/session')
}

export function changePassword(
  currentPassword: string,
  newPassword: string,
  confirmPassword: string,
) {
  return postData<void>('/auth/change-password', {
    currentPassword,
    newPassword,
    confirmPassword,
  })
}
