import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

export interface CurrentUser {
  id: number
  username: string
  name: string
  roleLevel: number
  status: number
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<CurrentUser | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  /** 当前用户角色级别，未登录时按最低权限（普通用户）处理 */
  const roleLevel = computed(() => userInfo.value?.roleLevel ?? 1)

  /** 是否管理员及以上 */
  const isAdmin = computed(() => roleLevel.value >= 2)

  /** 是否超级管理员 */
  const isSuperAdmin = computed(() => roleLevel.value >= 3)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info: CurrentUser) {
    userInfo.value = info
    localStorage.setItem('currentUser', JSON.stringify(info))
  }

  function logout() {
    token.value = null
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('currentUser')
  }

  async function validateToken() {
    if (!token.value) return { valid: false }
    try {
      const res: any = await authApi.validate()
      return res
    } catch (e) {
      console.error('Token验证失败', e)
      return { valid: false }
    }
  }

  function loadFromStorage() {
    const stored = localStorage.getItem('currentUser')
    if (stored) {
      try {
        userInfo.value = JSON.parse(stored)
      } catch {
        // 数据损坏时清掉，避免后续逻辑因解析失败而崩溃
        localStorage.removeItem('currentUser')
      }
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    roleLevel,
    isAdmin,
    isSuperAdmin,
    setToken,
    setUserInfo,
    logout,
    validateToken,
    loadFromStorage
  }
})
