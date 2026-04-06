<template>
    <div class="carousel-manager">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>首页轮播图管理</span>
            <el-button type="primary" :icon="Plus" @click="openDialog('add')">
              新增轮播图
            </el-button>
          </div>
        </template>
  
        <el-table :data="pagedCarouselList" style="width: 100%" v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="图片预览" width="120">
            <template #default="scope">
              <el-image
                style="width: 80px; height: 45px"
                :src="scope.row.imageUrl"
                fit="cover"
                :preview-src-list="[scope.row.imageUrl]"
                preview-teleported
              />
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="linkUrl" label="存储位置" width="250" show-overflow-tooltip />
          <!-- <el-table-column prop="sortOrder" label="排序" width="80" sortable /> -->
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">

              <!-- <el-button size="small" @click="openDialog('edit', scope.row)">
                编辑
              </el-button> -->

              <el-button size="small" type="danger" @click="deleteItem(scope.row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
  
        <div class="pagination-wrapper">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            @size-change="fetchCarouselList"
            @current-change="fetchCarouselList"
          />
        </div>
      </el-card>
  
      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="600"
        :before-close="handleClose"
      >
        <el-form
          :model="form"
          :rules="rules"
          ref="formRef"
          label-width="100px"
          v-loading="formLoading"
        >
          <!-- <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入轮播图标题" />
          </el-form-item>
          <el-form-item label="排序值" prop="sortOrder">
            <el-input-number
              v-model="form.sortOrder"
              :min="1"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="跳转链接" prop="linkUrl">
            <el-input v-model="form.linkUrl" placeholder="请输入点击后的跳转链接" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item> -->
          <el-form-item label="轮播图片" prop="imageUrl">
            <el-upload
              class="avatar-uploader"
              :action="customUpload"
              :show-file-list="false"
              :on-success="handleImageUploadSuccess"
              :before-upload="beforeImageUpload"
              
            >
              <img v-if="form.imageUrl" :src="form.imageUrl" class="avatar" alt="轮播图" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              <template #tip>
                <div class="el-upload__tip">
                  仅支持 JPG/PNG 格式，且文件大小不超过 2MB。推荐尺寸：1920x500。
                </div>
              </template>
            </el-upload>
          </el-form-item>
        </el-form>
  
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="handleClose">取消</el-button>
            <el-button type="primary" @click="submitForm">
              保存
            </el-button>
          </span>
        </template>
      </el-dialog>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive, computed, onMounted, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { ElMessage, ElMessageBox } from 'element-plus';
  import { Plus } from '@element-plus/icons-vue';
  import { HttpManager } from "@/api/index";
  import {getBaseURL} from '@/api/request'
  import { isSameRouteQuery, mergeRouteQuery, parsePositiveIntQuery } from "@/utils/route-query";
  
  // --- 数据模型定义 ---
  const BASE_IMAGE_URL = getBaseURL();
  const route = useRoute();
  const router = useRouter();
  // 轮播图数据接口
  const initialFormState = {
    id: null,
    title: '',
    imageUrl: '', // 最终的图片URL
    linkUrl: '',
    sortOrder: 1,
    status: 1, // 1: 启用, 0: 禁用
  };
  const addBannerimgpath = null
  // 轮播图列表数据
  const carouselList = ref([]);
  const total = ref(0);
  const currentPage = ref(parsePositiveIntQuery(route.query.page, 1));
  const pageSize = ref(parsePositiveIntQuery(route.query.pageSize, 10));
  const loading = ref(false); // 列表加载状态
  
  // 弹窗表单数据
  const formRef = ref(null);
  const dialogVisible = ref(false);
  const dialogType = ref('add'); // 'add' 或 'edit'
  const form = reactive({ ...initialFormState });
  const formLoading = ref(false); // 表单提交加载状态
  
  // 计算属性：弹窗标题
  const dialogTitle = computed(() => (dialogType.value === 'add' ? '新增轮播图' : '编辑轮播图'));
  const pagedCarouselList = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return carouselList.value.slice(start, start + pageSize.value);
  });
  
  // --- 验证规则 ---
  const rules = reactive({
    title: [
      { required: true, message: '请输入轮播图标题', trigger: 'blur' },
      { max: 50, message: '标题长度不能超过 50 个字符', trigger: 'blur' }
    ],
    imageUrl: [
      { required: true, message: '请上传轮播图片', trigger: 'change' }
    ],
    linkUrl: [
      { required: true, message: '请输入跳转链接', trigger: 'blur' }
    ],
    sortOrder: [
      { required: true, message: '请输入排序值', trigger: 'change' }
    ],
  });
  
  // --- 列表操作逻辑 ---
  
  // 获取轮播图列表
  const fetchCarouselList = async () => {
    loading.value = true;

    const apiService = await HttpManager.getAllbanner();

    const adaptedList = apiService.data.map((item, index) => ({
    id: item.id,  
    title: `轮播图 ${item.id}`, 
    imageUrl: `${BASE_IMAGE_URL}${item.pic}`,
    linkUrl: `${item.pic}`, 
    sortOrder: index + 1,  // 使用数组下标+1作为排序值
    status: 1, 
    createTime: '未提供', 
}));
    carouselList.value = adaptedList;
    

    // 模拟数据 (请删除并替换为真实的后端数据)
    // await new Promise(resolve => setTimeout(resolve, 500));
    // carouselList.value = [
    //   { id: 1, title: '新年促销季', imageUrl: 'http://localhost:8888/img/swiper/1.jpg', linkUrl: '/promotion/new-year', sortOrder: 1, status: 1 },
    //   { id: 2, title: '新款上市', imageUrl: 'http://localhost:8888/img/swiper/1.jpg', linkUrl: '/product/new-arrivals', sortOrder: 2, status: 1 },
    //   { id: 3, title: '会员专享', imageUrl: 'http://localhost:8888/img/swiper/1.jpg', linkUrl: '/member/exclusive', sortOrder: 3, status: 0 },
    // ].slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value);


    total.value = apiService.data.length;
    const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value));
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage;
    }
    loading.value = false;
  };

  watch([currentPage, pageSize], () => {
    syncListQuery();
  });

  watch(
    () => [route.query.page, route.query.pageSize],
    () => {
      const nextPage = parsePositiveIntQuery(route.query.page, 1);
      const nextPageSize = parsePositiveIntQuery(route.query.pageSize, 10);

      if (nextPage === currentPage.value && nextPageSize === pageSize.value) {
        return;
      }

      currentPage.value = nextPage;
      pageSize.value = nextPageSize;
    }
  );

  const syncListQuery = async () => {
    const nextQuery = mergeRouteQuery(route.query, {
      page: currentPage.value > 1 ? currentPage.value : undefined,
      pageSize: pageSize.value !== 10 ? pageSize.value : undefined,
    });
    if (!isSameRouteQuery(route.query, nextQuery)) {
      await router.replace({ path: route.path, query: nextQuery });
    }
  };
  
  // 删除操作
  const deleteItem = (row) => {
    ElMessageBox.confirm(
      `确定要删除轮播图 **${row.title}** 吗?`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    ).then(async () => {
      
      await HttpManager.deleteBanner(row.id);

      // **注意：这里需要替换为您真实的 API 请求**
      ElMessage.success(`删除成功：ID ${row.id}`);
      fetchCarouselList(); // 刷新列表
    }).catch(() => {
      // 取消删除
    });
  };
  
  // --- 弹窗/表单操作逻辑 ---
  
  // 打开弹窗
  const openDialog = (type, row = null) => {
    dialogType.value = type;
    Object.assign(form, initialFormState); // 重置表单
  
    if (type === 'edit' && row) {
      // 编辑：将行数据复制到表单中
      Object.assign(form, row);
    }
  
    // 重置表单验证状态 (在 $nextTick 或 dialogVisible 设置为 true 之后执行)
    if (formRef.value) {
      formRef.value.resetFields();
    }
  
    dialogVisible.value = true;
  };
  
  // 关闭弹窗
  const handleClose = () => {
    dialogVisible.value = false;
    if (formRef.value) {
      formRef.value.resetFields();
    }
  };
  
  // 提交表单 (新增/编辑)
  const submitForm = () => {
    formRef.value.validate(async (valid) => {
      if (valid) {
        formLoading.value = true;
        // **注意：这里需要替换为您真实的 API 请求**
        // try {
        //   if (dialogType.value === 'add') {
        //     await HttpManager.addBanner(form.linkUrl);
        //     ElMessage.success('新增成功！');
        //   } else {
        //     await apiService.updateCarousel(form);
        //     ElMessage.success('更新成功！');
        //   }
        //   handleClose();
        //   fetchCarouselList();
        // } catch (error) {
        //   ElMessage.error('保存失败');
        // }
        
        const result = await HttpManager.addBanner(form.linkUrl);
        if (!result.code) {
            ElMessage.error('保存失败');
            return;
        }

        handleClose();
        fetchCarouselList(); // 刷新列表

        formLoading.value = false;
      } else {
        ElMessage.warning('表单验证失败，请检查输入项');
        return false;
      }
    });
  };
  
  // --- 图片上传逻辑 ---
// 修正 customUpload 函数
const customUpload = getBaseURL()+'/banner/uploadImg';



  // 上传成功回调
  const handleImageUploadSuccess = (response) => {
    // 假设后端返回的数据结构中，图片的URL在 response.data.url
    // **请根据您的实际后端接口调整**
     
     form.imageUrl = getBaseURL()+response.data; 
     form.linkUrl = response.data;
    // 模拟成功：随机生成一个图片 URL
    //form.imageUrl = `https://picsum.photos/1920/500?random=${Date.now()}`;
    ElMessage.success('图片上传成功');
    // 触发一次表单验证，更新图片状态
    formRef.value.validateField('imageUrl'); 
  };
  
  // 上传前检查
  const beforeImageUpload = (file) => {
    const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
    const isLt2M = file.size / 1024 / 1024 < 2;
  
    if (!isJpgOrPng) {
      ElMessage.error('上传图片只能是 JPG/PNG 格式!');
    }
    if (!isLt2M) {
      ElMessage.error('上传图片大小不能超过 2MB!');
    }
    return isJpgOrPng && isLt2M;
  };
  
  
  // 组件挂载后加载数据
  onMounted(() => {
    fetchCarouselList();
  });
  </script>
  
  <style scoped>
  .carousel-manager {
    padding: 0;
  }

  :deep(.el-card) {
    border-radius: 28px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-header span {
    font-size: 24px;
    font-weight: 800;
    color: var(--admin-text);
  }
  
  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
  
  /* 图片上传样式 */
  .avatar-uploader .avatar {
    width: 100%;
    max-width: 320px;
    height: 180px;
    border-radius: 18px;
    object-fit: cover;
    display: block;
  }
  </style>
  
  <style>
  /* 覆盖 Element Plus 的默认上传器样式，使其更美观 */
  .avatar-uploader .el-upload {
    border: 1px dashed var(--el-border-color);
    border-radius: 22px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);
    background: rgba(247, 250, 255, 0.96);
  }
  
  .avatar-uploader .el-upload:hover {
    border-color: var(--el-color-primary);
  }
  
  .el-icon.avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 320px;
    height: 180px;
    text-align: center;
  }
  </style>
