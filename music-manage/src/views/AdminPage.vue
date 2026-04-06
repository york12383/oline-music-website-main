<template>
  <div class="admin-manager">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>管理员用户管理</span>
          <el-button type="primary" @click="openDialog('add')">
            <el-icon><i-ep-plus /></el-icon>
            新增管理员
          </el-button>
        </div>
      </template>

      <el-table :data="adminList" style="width: 100%" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="用户名" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button size="small" @click="openDialog('view', scope.row)">
              查看信息
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="formData"
        :rules="formRules"
        ref="formRef"
        label-width="80px"
        :disabled="isView"
      >
        <el-form-item v-if="formData.id" label="ID">
          <el-input v-model="formData.id" disabled />
        </el-form-item>
        <el-form-item label="用户名" prop="name">
          <el-input v-model="formData.name" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            :type="passwordVisible ? 'text' : 'password'"
            placeholder="请输入密码"
          >
            <template #append>
              <el-button @click="togglePasswordVisibility">
                {{ passwordVisible ? '隐藏' : '显示' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ isView ? '关闭' : '取消' }}</el-button>
          <el-button v-if="!isView" type="primary" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '新增' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { HttpManager } from '@/api/index';

const router = useRouter();
const adminList = ref<any[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const isView = ref(false);
const passwordVisible = ref(false);
const formRef = ref();

const initialFormData = {
  id: null,
  name: '',
  password: '',
};
const formData = reactive({ ...initialFormData });

const formRules = {
  name: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, message: '密码至少 3 个字符', trigger: 'blur' },
  ],
};

const dialogTitle = computed(() => {
  if (isView.value) return '查看管理员信息';
  return isEdit.value ? '编辑管理员' : '新增管理员';
});

const resetForm = () => {
  Object.assign(formData, initialFormData);
  passwordVisible.value = false;
};

const isAdminAuthError = (message = '') => {
  return ['请先登录管理员账户', '未登录', '当前登录的管理员账户不存在'].some((keyword) =>
    message.includes(keyword)
  );
};

const clearAdminSession = () => {
  localStorage.removeItem('adminInfo');
  sessionStorage.removeItem('adminInfo');
};

const handleAdminAuthFailure = (message = '管理员登录状态已失效，请重新登录') => {
  clearAdminSession();
  ElMessage.warning(message);
  router.replace('/');
};

const showApiMessage = (result: any, fallbackMessage: string) => {
  ElMessage({
    type: result?.type || 'error',
    message: result?.message || fallbackMessage,
  });
};

async function getAdmin() {
  try {
    const result = await HttpManager.getAdmin();
    if (!result?.success) {
      adminList.value = [];
      if (isAdminAuthError(result?.message || '')) {
        handleAdminAuthFailure('管理员登录状态已失效，请重新登录');
        return;
      }
      showApiMessage(result, '获取管理员列表失败');
      return;
    }

    adminList.value = result.data || [];
  } catch (error) {
    ElMessage.error('获取管理员列表失败');
  }
}

onMounted(() => {
  getAdmin();
});

function togglePasswordVisibility() {
  passwordVisible.value = !passwordVisible.value;
}

function openDialog(type: 'add' | 'edit' | 'view', row: any = null) {
  isEdit.value = type === 'edit';
  isView.value = type === 'view';
  resetForm();

  if ((type === 'edit' || type === 'view') && row) {
    Object.assign(formData, {
      id: row.id ?? null,
      name: row.name ?? row.username ?? '',
      password: '',
    });
  }

  dialogVisible.value = true;
  nextTick(() => {
    if (!isView.value) {
      formRef.value?.clearValidate();
    }
  });
}

async function handleSubmit() {
  if (isView.value) return;

  try {
    await formRef.value?.validate();
  } catch (error) {
    ElMessage.warning('请检查表单输入');
    return;
  }

  if (isEdit.value) {
    ElMessage.warning('当前页面暂未开放编辑管理员功能');
    return;
  }

  try {
    const result = await HttpManager.addAdmin({
      username: formData.name.trim(),
      password: formData.password,
    });

    if (!result?.success) {
      if (isAdminAuthError(result?.message || '')) {
        handleAdminAuthFailure('管理员登录状态已失效，请重新登录');
        return;
      }
      showApiMessage(result, '新增管理员失败');
      return;
    }

    showApiMessage(result, `新增管理员 ${formData.name} 成功`);
    dialogVisible.value = false;
    resetForm();
    await getAdmin();
  } catch (error) {
    ElMessage.error('新增管理员失败');
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除管理员用户 **${row.name}** (ID: ${row.id}) 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true,
      }
    );

    const result = await HttpManager.deleteAdmin(row.id);
    if (!result?.success) {
      if (isAdminAuthError(result?.message || '')) {
        handleAdminAuthFailure('管理员登录状态已失效，请重新登录');
        return;
      }
      showApiMessage(result, '删除失败');
      return;
    }

    showApiMessage(result, `管理员 ${row.name} 删除成功`);
    await getAdmin();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除管理员失败');
    }
  }
}
</script>

<style scoped>
.admin-manager {
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.box-card) {
  border-radius: 28px;
}

.card-header span {
  font-size: 24px;
  font-weight: 800;
  color: var(--admin-text);
}

:deep(.el-icon) {
  margin-right: 4px;
}
</style>
