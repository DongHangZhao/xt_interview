import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data

      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('currentUser')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        // 权限不足：由后端 PermissionChecker 抛出，交调用方展示具体原因
        return Promise.reject(data)
      } else if (status === 400) {
        // 后端 GlobalExceptionHandler 返回 { success:false, message }
        return Promise.reject(data)
      } else {
        ElMessage.error(data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器已剥离 AxiosResponse 外壳、直接返回后端数据，
 * 但 axios 的类型签名仍标注为 AxiosResponse<T>。
 * 这里统一把返回类型重新声明为 T，否则调用方赋值给具体类型时会报 TS2740。
 */
export function get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request.get(url, config) as unknown as Promise<T>
}

export function post<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request.post(url, data, config) as unknown as Promise<T>
}

export function put<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request.put(url, data, config) as unknown as Promise<T>
}

export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request.delete(url, config) as unknown as Promise<T>
}

export default request
