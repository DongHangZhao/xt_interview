<template>
  <div class="profile">
    <el-descriptions title="个人信息" :column="1" border>
      <el-descriptions-item label="用户名">{{ userInfo?.username }}</el-descriptions-item>
      <el-descriptions-item label="姓名">{{ userInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="角色">
        <el-tag :type="roleTagType">{{ roleText }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="权限说明">
        <span style="color: #606266; line-height: 1.6">{{ permissionTip }}</span>
      </el-descriptions-item>
    </el-descriptions>

    <div class="edit-section">
      <h3>修改我的资料</h3>
      <p class="section-tip">
        可修改姓名、联系方式、性别、登录用户名与密码。
        权限级别、所属部门、账号状态由管理员统一分配，不可自行修改。
      </p>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="110px"
        style="max-width: 520px"
      >
        <el-form-item label="登录用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入登录用户名" />
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
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="formData.newPassword"
            type="password"
            show-password
            placeholder="留空则不修改密码"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入新密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            保存修改
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-alert
      v-if="!userStore.isAdmin"
      type="info"
      show-icon
      :closable="false"
      title="当前为普通用户权限"
      description="普通用户无法访问用户管理与部门管理模块。如需查看完整数据，请使用管理员账号登录。"
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { accountApi } from '@/api'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const formRef = ref<FormInstance>()
const submitting = ref(false)

const formData = reactive({
  username: '',
  name: '',
  gender: '',
  phone: '',
  newPassword: '',
  confirmPassword: ''
})

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入登录用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为 2 到 50 个字符', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  newPassword: [
    {
      validator: (_rule: any, value: string, callback: (e?: Error) => void) => {
        if (value && value !== formData.confirmPassword) {
          return callback(new Error('两次输入的新密码不一致'))
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function loadProfile() {
  const info = userInfo.value
  if (!info) return
  formData.username = info.username
  formData.name = info.name
  formData.gender = ''
  formData.phone = ''
  formData.newPassword = ''
  formData.confirmPassword = ''
}

function resetForm() {
  loadProfile()
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: Record<string, any> = {
      username: formData.username,
      name: formData.name,
      gender: formData.gender,
      phone: formData.phone
    }
    if (formData.newPassword) {
      payload.newPassword = formData.newPassword
      payload.password = '' // 留空表示不校验旧密码
    }

    const updated = await accountApi.updateMyProfile(payload)
    // 同步更新本地用户信息，菜单与角色展示立即生效
    if (updated) {
      userStore.setUserInfo({
        id: updated.id,
        username: updated.username,
        name: updated.name,
        roleLevel: updated.roleLevel,
        status: updated.status
      })
    }
    ElMessage.success('资料修改成功')
    loadProfile()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const roleText = computed(() => {
  const map: Record<number, string> = {
    1: '普通用户',
    2: '管理员',
    3: '超级管理员'
  }
  return map[userStore.roleLevel] || '未知'
})

const roleTagType = computed(() => {
  const map: Record<number, 'info' | 'warning' | 'danger'> = {
    1: 'info',
    2: 'warning',
    3: 'danger'
  }
  return map[userStore.roleLevel] || 'info'
})

const permissionTip = computed(() => {
  const map: Record<number, string> = {
    1: '仅可查看本人信息并修改自己的基础资料；无管理权限，不能创建或修改他人账号与部门。',
    2: '可管理用户与部门；不能创建或修改权限高于自身的账号，也不能操作同级账号。',
    3: '拥有全部权限；可管理所有账号与部门，包括设置超级管理员标记。'
  }
  return map[userStore.roleLevel] || ''
})

onMounted(() => {
  userStore.loadFromStorage()
  loadProfile()
})
</script>

<style scoped>
.profile {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.edit-section {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.edit-section h3 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.section-tip {
  margin: 0 0 18px;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}
</style>
