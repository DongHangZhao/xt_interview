<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <h2>欣铁 面试</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <!-- 前端按权限隐藏菜单；后端接口仍有校验，前端隐藏仅为体验优化 -->
        <el-menu-item v-if="userStore.isAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin" index="/departments">
          <el-icon><OfficeBuilding /></el-icon>
          <span>部门管理</span>
        </el-menu-item>
        <el-menu-item v-else index="/profile">
          <el-icon><Avatar /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <el-tag v-if="userStore.isAdmin" type="warning" size="small" effect="plain">
            {{ roleText }}
          </el-tag>
          <span class="user-info">欢迎，<strong>{{ userName }}</strong></span>
          <el-button type="danger" plain size="small" @click="handleLogout">
            退出登录
          </el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, OfficeBuilding, Avatar } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const activeMenu = ref('/users')

const userName = computed(() => {
  if (userStore.userInfo?.name) {
    return userStore.userInfo.name
  }
  return userStore.userInfo?.username || '管理员'
})

const pageTitle = computed(() => {
  const titleMap: Record<string, string> = {
    '/users': '用户管理',
    '/departments': '部门管理',
    '/profile': '个人中心'
  }
  return titleMap[route.path] || '首页'
})

const roleText = computed(() => {
  const map: Record<number, string> = {
    1: '普通用户',
    2: '管理员',
    3: '超级管理员'
  }
  return map[userStore.roleLevel] || '未知角色'
})

// 普通用户无管理权限，若通过地址栏直接访问管理页则送回个人中心
watch(
  () => userStore.isAdmin,
  (isAdmin) => {
    if (!isAdmin && (route.path === '/users' || route.path === '/departments')) {
      router.replace('/profile')
    }
  },
  { immediate: true }
)

// 路由变化时同步侧边栏高亮，否则从用户管理切到部门管理后高亮不会跟随
watch(
  () => route.path,
  (path) => {
    activeMenu.value = path
  },
  { immediate: true }
)

function handleMenuSelect(index: string) {
  router.push(index)
}

onMounted(() => {
  userStore.loadFromStorage()
  if (!userStore.isLoggedIn) {
    router.push('/login')
  }
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await authApi.logout()
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // user cancelled
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background: #1e3c72;
  background: linear-gradient(180deg, #1e3c72 0%, #2a5298 100%);
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  color: #fff;
  font-size: 20px;
  font-weight: 600;
}

.sidebar-menu {
  background: transparent;
  border-right: none;
}

.sidebar-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.85);
  font-size: 15px;
}

.sidebar-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.sidebar-menu .el-menu-item.is-active {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  font-size: 14px;
  color: #606266;
}

.main-content {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
