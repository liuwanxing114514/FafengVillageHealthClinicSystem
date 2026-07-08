import axios from 'axios'
import type { ApiResult } from '@/types/api'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 15000,
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(body)
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      ElMessage.error('未登录或会话已过期')
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export async function getData<T>(url: string): Promise<T> {
  const { data } = await http.get<ApiResult<T>>(url)
  return data.data
}

export async function postData<T>(url: string, payload?: unknown): Promise<T> {
  const { data } = await http.post<ApiResult<T>>(url, payload)
  return data.data
}

export async function putData<T>(url: string, payload?: unknown): Promise<T> {
  const { data } = await http.put<ApiResult<T>>(url, payload)
  return data.data
}

export default http
