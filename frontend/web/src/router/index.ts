import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/users',
      children: [
        {
          path: 'users',
          name: 'UserManagement',
          component: () => import('@/views/UserManagement.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '用户管理' }
        },
        {
          path: 'departments',
          name: 'DepartmentManagement',
          component: () => import('@/views/DepartmentManagement.vue'),
          meta: { requiresAuth: true, requiresAdmin: true, title: '部门管理' }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/Profile.vue'),
          meta: { requiresAuth: true, title: '个人中心' }
        }
      ]
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  /**
   * 管理员权限守卫。
   * 注意：这里只是前端体验优化，真正的安全边界在后端 PermissionChecker——
   * 即使用户绕过前端直接调接口，后端仍会返回 403。
   */
  if (to.meta.requiresAdmin) {
    // 刷新页面时 Pinia store 可能尚未从 localStorage 恢复，这里补一次
    if (!userStore.userInfo) {
      userStore.loadFromStorage()
    }
    if (!userStore.isAdmin) {
      next('/profile')
      return
    }
  }

  if (to.meta.title) {
    document.title = `${to.meta.title} - 欣铁 面试项目`
  }
  next()
})

export default router
