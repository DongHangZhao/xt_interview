import { get, post, put, del } from '@/utils/request'

// ==================== 数据类型 ====================
export interface Department {
  id: number
  name: string
  code: string
  description: string | null
  status: number
  deleted: number
  createTime: string | null
  updateTime: string | null
}

export interface Account {
  id: number
  username: string
  name: string
  gender: string | null
  phone: string | null
  roleLevel: number
  status: number
  isSuper: number
  departmentId?: number | null
  departmentName?: string | null
  createTime: string | null
  updateTime: string | null
}

export interface PageResult<T> {
  list: T[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
}

/** 分页查询参数，字段均为可选 */
export interface AccountQuery {
  page?: number
  size?: number
  username?: string
  status?: number | null
  roleLevel?: number | null
}

// ==================== 认证相关 API ====================
export const authApi = {
  login(data: { username: string; password: string }) {
    return post('/auth/login', data)
  },
  logout() {
    return post('/auth/logout')
  },
  validate() {
    return get('/auth/validate')
  }
}

// ==================== 账号管理 API ====================
export const accountApi = {
  getAll() {
    return get<Account[]>('/accounts')
  },
  getById(id: number) {
    return get<Account>(`/accounts/${id}`)
  },
  /** 分页查询：支持用户名模糊搜索、状态筛选、权限级别筛选 */
  getPage(params: AccountQuery) {
    return get<PageResult<Account>>('/accounts/page', { params })
  },
  create(data: any) {
    return post<Account>('/accounts', data)
  },
  update(id: number, data: any) {
    return put<Account>(`/accounts/${id}`, data)
  },
  delete(id: number) {
    return del<void>(`/accounts/${id}`)
  },
  /** 修改我的资料：无 id，后端从 token 识别当前用户，仅开放安全字段 */
  updateMyProfile(data: {
    username?: string
    name?: string
    phone?: string | null
    gender?: string | null
    password?: string
    newPassword?: string
  }) {
    return put<Account>('/accounts/my-profile', data)
  }
}

// ==================== 部门管理 API ====================
export const departmentApi = {
  getAll() {
    return get<Department[]>('/departments')
  },
  getById(id: number) {
    return get<Department>(`/departments/${id}`)
  },
  create(data: any) {
    return post<Department>('/departments', data)
  },
  update(id: number, data: any) {
    return put<Department>(`/departments/${id}`, data)
  },
  delete(id: number) {
    return del<void>(`/departments/${id}`)
  }
}
