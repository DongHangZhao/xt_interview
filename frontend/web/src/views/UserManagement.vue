<template>
  <div class="user-management">
    <div class="search-bar">
      <el-input
        v-model="query.username"
        placeholder="请输入用户名或姓名"
        clearable
        class="search-item"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select v-model="query.status" placeholder="状态" clearable class="search-item">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-select v-model="query.roleLevel" placeholder="权限级别" clearable class="search-item">
        <el-option label="用户权限" :value="1" />
        <el-option label="管理员权限" :value="2" />
        <el-option label="超级管理员权限" :value="3" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
    </div>

    <div class="page-header">
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">
        添加用户
      </el-button>
      <el-button :icon="Refresh" @click="loadUsers">刷新</el-button>
      <span class="total-tip">共 {{ total }} 个用户</span>
    </div>

    <el-table
      v-loading="loading"
      :data="users"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="phone" label="联系方式" width="140" />
      <el-table-column label="所属部门" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.departmentName" type="primary" effect="plain">
            {{ row.departmentName }}
          </el-tag>
          <span v-else style="color: #909399">-</span>
        </template>
      </el-table-column>
      <el-table-column label="权限级别" width="120">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.roleLevel)">
            {{ roleText(row.roleLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <!-- 同级及以上账号不可操作：按钮直接不渲染，避免用户点了才报 403 -->
          <template v-if="canOperate(row)">
            <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
          <span v-else style="color: #c0c4cc; font-size: 13px">无权限</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <!--
        页码做「1-based ↔ 0-based」转换：
        el-pagination 从 1 开始，后端接口从 0 开始，
        故用 :current-page 传入 0-based 值，避免 v-model 双向写回造成换算错乱。
      -->
      <el-pagination
        :current-page="query.page + 1"
        :page-size="query.size"
        :page-sizes="[5, 10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '添加用户'"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="90px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="formData.gender" placeholder="请选择性别" style="width: 100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系方式" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系方式" />
        </el-form-item>
        <el-form-item label="所属部门" prop="departmentId">
          <el-select
            v-model="formData.departmentId"
            placeholder="请选择部门（可不选）"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="dept in departments"
              :key="dept.id"
              :label="dept.name + (dept.status === 1 ? '' : '（已禁用）')"
              :value="dept.id"
              :disabled="dept.status !== 1 && dept.id !== formData.departmentId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改密码' : '留空则默认123456'"
          />
        </el-form-item>
        <el-form-item label="权限级别" prop="roleLevel">
          <el-select v-model="formData.roleLevel" placeholder="请选择权限级别" style="width: 100%">
            <!--
              只能选择不高于自身权限的选项：超管可见全部，
              管理员只能建普通用户和管理员，普通用户本就不会进入此页面。
              后端 PermissionChecker 仍会再校验一次，此处是体验优化。
            -->
            <el-option
              v-for="level in assignableRoleLevels"
              :key="level.value"
              :label="level.label"
              :value="level.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { accountApi, departmentApi, type Account, type Department } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)

const users = ref<Account[]>([])
const departments = ref<Department[]>([])
const total = ref(0)

/** 分页与搜索条件；page 从 0 开始（与后端约定一致），el-pagination 从 1 开始，交互时需 -1 */
const query = reactive({
  page: 0,
  size: 10,
  username: '' as string,
  status: null as number | null,
  roleLevel: null as number | null
})

const formRef = ref<FormInstance>()

const formData = reactive({
  username: '',
  name: '',
  gender: '男',
  phone: '',
  password: '',
  roleLevel: 1,
  status: 1,
  isSuper: 0,
  departmentId: null as number | null
})
const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleLevel: [{ required: true, message: '请选择权限级别', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

/**
 * 可分配的权限选项。
 * - 超管：全部可见
 * - 管理员：仅可见普通用户（不能创建同级，否则可横向提权）
 * 规则与后端 PermissionChecker.checkCreateAccount 保持一致。
 */
const assignableRoleLevels = computed(() => {
  const all: Array<{ value: number; label: string }> = [
    { value: 1, label: '用户权限' },
    { value: 2, label: '管理员权限' },
    { value: 3, label: '超级管理员权限' }
  ]
  if (userStore.isSuperAdmin) {
    return all
  }
  return all.filter((item) => item.value < userStore.roleLevel)
})

function roleText(level: number): string {
  const map: Record<number, string> = { 1: '用户权限', 2: '管理员权限', 3: '超级管理员' }
  return map[level] || '未知'
}

function roleTagType(level: number): 'info' | 'warning' | 'danger' {
  const map: Record<number, 'info' | 'warning' | 'danger'> = {
    1: 'info',
    2: 'warning',
    3: 'danger'
  }
  return map[level] || 'info'
}

/**
 * 是否可操作该行。
 * - 不能操作自己
 * - 超管可操作除自己外的任何人
 * - 非超管不能操作同级及以上
 */
function canOperate(row: Account): boolean {
  if (row.id === userStore.userInfo?.id) return false
  if (userStore.isSuperAdmin) return true
  return row.roleLevel < userStore.roleLevel
}

async function loadUsers() {
  loading.value = true
  try {
    const result = await accountApi.getPage({
      page: query.page,
      size: query.size,
      username: query.username || undefined,
      status: query.status,
      roleLevel: query.roleLevel
    })
    users.value = result.list
    total.value = result.totalElements

    // 删除了当前页最后一条数据时，自动回退一页，避免停留在空白页
    if (result.list.length === 0 && query.page > 0) {
      query.page -= 1
      await loadUsers()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

/** 搜索：条件变化后回到第一页，否则可能在第 5 页搜到空结果 */
function handleSearch() {
  query.page = 0
  loadUsers()
}

function handleReset() {
  query.username = ''
  query.status = null
  query.roleLevel = null
  query.page = 0
  loadUsers()
}

/** el-pagination 的页码从 1 开始，后端从 0 开始，这里做转换 */
function handlePageChange(val: number) {
  query.page = val - 1
  loadUsers()
}

function handleSizeChange(val: number) {
  // 每页条数变化后回到第一页，否则当前页可能超出新的总页数
  query.size = val
  query.page = 0
  loadUsers()
}

/** 加载部门下拉选项。部门加载失败不阻塞用户列表，仅提示 */
async function loadDepartments() {
  try {
    departments.value = await departmentApi.getAll()
  } catch (error: any) {
    ElMessage.warning(error.message || '加载部门列表失败，部门下拉将为空')
  }
}

function resetForm() {
  formData.username = ''
  formData.name = ''
  formData.gender = '男'
  formData.phone = ''
  formData.password = ''
  formData.roleLevel = 1
  formData.status = 1
  formData.isSuper = 0
  formData.departmentId = null
  isEdit.value = false
  editId.value = 0
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: any) {
  resetForm()
  isEdit.value = true
  editId.value = row.id
  formData.username = row.username
  formData.name = row.name
  formData.gender = row.gender || '男'
  formData.phone = row.phone || ''
  formData.roleLevel = row.roleLevel
  formData.status = row.status
  formData.isSuper = row.isSuper
  formData.departmentId = row.departmentId ?? null

  // 同级及以上的账号本就不可操作；若因异常进入编辑态，
  // 角色选项可能不含当前值，兜底选一个可分配值避免下拉框空白
  if (!assignableRoleLevels.value.some((l) => l.value === formData.roleLevel)) {
    formData.roleLevel = assignableRoleLevels.value[0]?.value ?? 1
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return

  // 使用 Promise 写法（回调式 validate 写法在 Element Plus 2.6 已废弃）
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // 密码为空时不提交该字段：新增时后端走默认值 123456，编辑时后端跳过密码修改
    const data: Record<string, any> = { ...formData }
    if (data.password === '') {
      delete data.password
    }

    if (isEdit.value) {
      await accountApi.update(editId.value, data)
      ElMessage.success('用户更新成功')
    } else {
      await accountApi.create(data)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    await loadUsers()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${row.username}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await accountApi.delete(row.id)
    ElMessage.success('用户删除成功')
    await loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadUsers()
  // 部门下拉依赖部门列表，但用户列表不依赖它，故并行加载互不阻塞
  loadDepartments()
})
</script>

<style scoped>
.user-management {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.page-header {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}

.total-tip {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-item {
  width: 200px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
