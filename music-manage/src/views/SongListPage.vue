<template>
  <div class="container">
    <div class="handle-box">
      <div class="left-panel">
        <el-button type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="multipleSelection.length === 0">批量删除</el-button>
        <el-input v-model="searchWord" :prefix-icon="Search" placeholder="筛选关键词" class="handle-input"></el-input>
      </div>
      <div class="right-panel">
        <el-button type="primary" :icon="Plus" @click="centerDialogVisible = true">添加歌单</el-button>
        <el-button type="success" :icon="Download" @click="exportPlaylist">导出歌单</el-button>
      </div>
    </div>

    <div class="quick-filter-group">
      <span class="filter-label">资料筛选</span>
      <el-radio-group v-model="coverStatusFilter" size="small">
        <el-radio-button v-for="item in coverStatusOptions" :key="item.value" :label="item.value">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <el-table
      ref="tableRef"
      height="550px"
      border
      size="small"
      stripe
      v-loading="loading"
      :data="paginatedData"
      :row-key="getRowKey"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" :reserve-selection="true" width="40" align="center"></el-table-column>
      <el-table-column label="ID" prop="id" width="60" align="center"></el-table-column>
      <el-table-column label="歌单图片" width="120" align="center">
        <template #default="scope">
          <div class="img-upload-box">
            <el-image :src="attachImageUrl(scope.row.pic)" class="table-img" fit="cover">
              <template #error>
                <div class="image-slot">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <el-upload
              class="upload-mask"
              :action="uploadUrl(scope.row.id)"
              :show-file-list="false"
              :on-success="handleImgSuccess"
              :before-upload="beforeImgUpload"
            >
              <el-icon class="upload-icon"><Upload /></el-icon>
              <span>更新</span>
            </el-upload>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="title" label="标题" width="180"></el-table-column>

      <el-table-column label="资料状态" width="110" align="center">
        <template #default="scope">
          <el-tag :type="getPlaylistHealthType(scope.row)">
            {{ getPlaylistHealthLabel(scope.row) }}
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="简介" min-width="200">
        <template #default="scope">
          <el-tooltip effect="dark" :content="scope.row.introduction" placement="top">
            <p class="introduction-text">{{ scope.row.introduction || '歌单简介整理中' }}</p>
          </el-tooltip>
        </template>
      </el-table-column>
      
      <el-table-column label="风格" prop="style" width="100" align="center">
        <template #default="scope">
           <el-tag effect="plain">{{ scope.row.style || '无' }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="状态" prop="type" width="100" align="center">
        <template #default="scope">
           <el-tag :type="getStatusType(scope.row.type)">
             {{ getStatusLabel(scope.row.type) }}
           </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="详情" width="170" align="center">
        <template #default="scope">
          <div class="detail-action-group">
            <el-button class="content-action-button" size="small" type="primary" @click="goContentPage(scope.row.id)">
              内容管理
            </el-button>
            <el-button class="comment-action-button" size="small" type="success" @click="goCommentPage(scope.row.id)">
              评论管理
            </el-button>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="90" align="center">
        <template #default="scope">
          <el-button size="small" :icon="Edit" @click="editRow(scope.row)">编辑</el-button>

        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-box">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredData.length"
        @current-change="handleCurrentChange"
      >
      </el-pagination>
    </div>

    <el-dialog title="添加歌单" v-model="centerDialogVisible" width="400px" destroy-on-close>
      <el-form label-width="80px" :model="registerForm" ref="addFormRef">
        <el-form-item label="歌单名" prop="title">
          <el-input v-model="registerForm.title"></el-input>
        </el-form-item>
        <el-form-item label="歌单介绍" prop="introduction">
          <el-input type="textarea" v-model="registerForm.introduction"></el-input>
        </el-form-item>
        <el-form-item label="风格" prop="style">
          <el-input v-model="registerForm.style"></el-input>
        </el-form-item>
        </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="centerDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="addSongList">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="编辑歌单" v-model="editVisible" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title"></el-input>
        </el-form-item>
        <el-form-item label="简介">
          <el-input type="textarea" :rows="3" v-model="editForm.introduction"></el-input>
        </el-form-item>
        <el-form-item label="风格">
          <el-input v-model="editForm.style"></el-input>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.type">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="2">审核中</el-radio>
            <el-radio :label="3">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editVisible = false">取 消</el-button>
          <el-button type="primary" @click="saveEdit">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch, getCurrentInstance, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Search, Plus, Download, Edit, Picture, Upload } from '@element-plus/icons-vue';
import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api/index";
import { RouterName } from "@/enums";
import axios from 'axios';
import {getBaseURL} from '@/api/request'
import { isSameRouteQuery, mergeRouteQuery, normalizeQueryValue, parsePositiveIntQuery } from "@/utils/route-query";

// 引入 mixin
const { routerManager, beforeImgUpload } = mixin();
const { proxy } = getCurrentInstance(); // 仅保留用于 store 操作
const route = useRoute();
const router = useRouter();

// 数据定义
const tableData = ref([]); // 原始数据
const filteredData = ref([]); // 搜索过滤后的数据
const tableRef = ref();
const loading = ref(false);
const pageSize = ref(7); // 每页显示数量
const currentPage = ref(parsePositiveIntQuery(route.query.page, 1));
const searchWord = ref(normalizeQueryValue(route.query.keyword));
const coverStatusOptions = [
  { label: "全部", value: "all" },
  { label: "缺封面", value: "missingCover" },
  { label: "缺简介", value: "missingIntro" },
  { label: "待修复", value: "repair" },
];
const normalizeCoverStatus = (value: unknown) => {
  const normalized = normalizeQueryValue(value as any);
  return coverStatusOptions.some((item) => item.value === normalized) ? normalized : "all";
};
const coverStatusFilter = ref(normalizeCoverStatus(route.query.coverStatus));
const multipleSelection = ref([]);
const isRestoringSelection = ref(false);
const isSyncingRouteState = ref(false);
const selectedRowsById = new Map<string, any>();

// 弹窗控制
const centerDialogVisible = ref(false);
const editVisible = ref(false);

// 表单对象
const registerForm = reactive({
  title: "",
  introduction: "",
  style: "",
  type: 1 // 默认添加为启用状态
});

// ✨ 编辑表单增加 type 字段
const editForm = reactive({
  id: "",
  title: "",
  pic: "",
  introduction: "",
  style: "",
  type: 1,
  consumer: "" 
});

// 计算当前页显示的数据
const paginatedData = computed(() => {
  return filteredData.value.slice(
    (currentPage.value - 1) * pageSize.value,
    currentPage.value * pageSize.value
  );
});

const getRowKey = (row: any) => row.id;

function syncMultipleSelection() {
  multipleSelection.value = Array.from(selectedRowsById.values());
}

function pruneCrossPageSelection(records = tableData.value) {
  const validIds = new Set((records || []).map((item) => String(item.id)));
  Array.from(selectedRowsById.keys()).forEach((id) => {
    if (!validIds.has(id)) {
      selectedRowsById.delete(id);
    }
  });
  syncMultipleSelection();
}

function clearCrossPageSelection() {
  selectedRowsById.clear();
  syncMultipleSelection();

  if (tableRef.value?.clearSelection) {
    isRestoringSelection.value = true;
    tableRef.value.clearSelection();
    isRestoringSelection.value = false;
  }
}

async function restoreCurrentPageSelection() {
  await nextTick();
  if (!tableRef.value?.clearSelection || !tableRef.value?.toggleRowSelection) {
    return;
  }

  isRestoringSelection.value = true;
  tableRef.value.clearSelection();
  paginatedData.value.forEach((row) => {
    if (selectedRowsById.has(String(row.id))) {
      tableRef.value.toggleRowSelection(row, true);
    }
  });
  await nextTick();
  isRestoringSelection.value = false;
}

watch([searchWord, coverStatusFilter], () => {
  if (isSyncingRouteState.value) return;
  clearCrossPageSelection();
  currentPage.value = 1;
  applyFilter();
  syncListQuery();
});

watch(currentPage, () => {
  if (isSyncingRouteState.value) return;
  syncListQuery();
});

watch(
  () => [route.query.page, route.query.keyword, route.query.coverStatus],
  () => {
    const nextPage = parsePositiveIntQuery(route.query.page, 1);
    const nextKeyword = normalizeQueryValue(route.query.keyword);
    const nextCoverStatus = normalizeCoverStatus(route.query.coverStatus);
    const filterContextChanged = nextKeyword !== searchWord.value || nextCoverStatus !== coverStatusFilter.value;

    if (nextPage === currentPage.value && nextKeyword === searchWord.value && nextCoverStatus === coverStatusFilter.value) {
      return;
    }

    isSyncingRouteState.value = true;
    if (filterContextChanged) {
      clearCrossPageSelection();
    }
    currentPage.value = nextPage;
    searchWord.value = nextKeyword;
    coverStatusFilter.value = nextCoverStatus;
    isSyncingRouteState.value = false;
    applyFilter();
  }
);

watch(
  paginatedData,
  () => {
    void restoreCurrentPageSelection();
  },
  { flush: "post" }
);

// ✨ 辅助函数：获取状态对应的 Tag 类型颜色
const getStatusType = (type) => {
  if (type === 1) return 'success'; // 启用 - 绿色
  if (type === 2) return 'warning'; // 审核中 - 橙色
  if (type === 3) return 'danger';  // 禁用 - 红色
  return 'info';
};

// ✨ 辅助函数：获取状态对应的文字
const getStatusLabel = (type) => {
  if (type === 1) return '启用';
  if (type === 2) return '审核中';
  if (type === 3) return '禁用';
  return '未知';
};

function normalizeText(value) {
  return String(value ?? "").trim();
}

function isDefaultSongListCoverValue(value) {
  const normalized = normalizeText(value);
  return !normalized || normalized === "/img/songListPic/123.jpg";
}

function isMissingSongListIntroValue(value) {
  const normalized = normalizeText(value);
  return !normalized || normalized === "暂无简介";
}

function playlistNeedsRepair(row) {
  return isDefaultSongListCoverValue(row?.pic) || isMissingSongListIntroValue(row?.introduction);
}

function getPlaylistHealthLabel(row) {
  const missingCover = isDefaultSongListCoverValue(row?.pic);
  const missingIntro = isMissingSongListIntroValue(row?.introduction);
  if (missingCover && missingIntro) return "待修复";
  if (missingCover) return "缺封面";
  if (missingIntro) return "缺简介";
  return "完整";
}

function getPlaylistHealthType(row) {
  const missingCover = isDefaultSongListCoverValue(row?.pic);
  const missingIntro = isMissingSongListIntroValue(row?.introduction);
  if (missingCover && missingIntro) return "danger";
  if (missingCover || missingIntro) return "warning";
  return "success";
}

// 初始化获取数据
getData();

function buildListQuery() {
  return {
    page: currentPage.value > 1 ? currentPage.value : undefined,
    keyword: searchWord.value.trim() || undefined,
    coverStatus: coverStatusFilter.value !== "all" ? coverStatusFilter.value : undefined,
  };
}

async function syncListQuery() {
  const nextQuery = mergeRouteQuery(route.query, buildListQuery());
  if (!isSameRouteQuery(route.query, nextQuery)) {
    await router.replace({ path: route.path, query: nextQuery });
  }
}

function applyFilter() {
  const keyword = searchWord.value.trim();
  if (!keyword) {
    filteredData.value = [...tableData.value];
  } else {
    filteredData.value = tableData.value.filter((item) => item.title?.includes(keyword));
  }

  if (coverStatusFilter.value === "missingCover") {
    filteredData.value = filteredData.value.filter((item) => isDefaultSongListCoverValue(item.pic));
  } else if (coverStatusFilter.value === "missingIntro") {
    filteredData.value = filteredData.value.filter((item) => isMissingSongListIntroValue(item.introduction));
  } else if (coverStatusFilter.value === "repair") {
    filteredData.value = filteredData.value.filter((item) => playlistNeedsRepair(item));
  }

  const maxPage = Math.max(1, Math.ceil(filteredData.value.length / pageSize.value));
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage;
  }
}

async function getData() {
  loading.value = true;
  tableData.value = [];
  filteredData.value = [];
  try {
    const result = (await HttpManager.getSongList()) as any;
    tableData.value = Array.isArray(result?.data) ? result.data : [];
    applyFilter();
    pruneCrossPageSelection(tableData.value);
  } catch (error) {
    clearCrossPageSelection();
    ElMessage.error("获取歌单数据失败");
  } finally {
    loading.value = false;
  }
}

// 翻页
function handleCurrentChange(val) {
  currentPage.value = val;
}

// 图片相关
function attachImageUrl(url) {
  return HttpManager.attachImageUrl(url);
}

function uploadUrl(id) {
  return HttpManager.attachImageUrl(`/songList/img/update?id=${id}`);
}

function handleImgSuccess(response, file) {
  ElMessage({
    message: response.message,
    type: response.type,
  });
  if (response.success) getData();
}

// 导出
function exportPlaylist() {
  axios({
    method: 'get',
    url: {getBaseURL}+'excle',
    responseType: 'blob',
  })
    .then((response) => {
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'SongList.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    })
    .catch((error) => {
      ElMessage.error("导出失败，请检查服务器连接");
    });
}

// 路由跳转
function goContentPage(id) {
  updateBreadcrumb("歌单内容");
  routerManager(RouterName.ListSong, { path: RouterName.ListSong, query: { id } });
}

function goCommentPage(id) {
  updateBreadcrumb("评论详情");
  routerManager(RouterName.Comment, { path: RouterName.Comment, query: { id, type: 1 } });
}

function updateBreadcrumb(name) {
  const breadcrumbList = [
    { path: RouterName.SongList, query: buildListQuery(), name: "歌单管理" },
    { path: "", name: name },
  ];
  proxy.$store.commit("setBreadcrumbList", breadcrumbList);
}

// --- CRUD 操作 ---

// 新增
async function addSongList() {
  // 确保包含默认 type
  const params = {
      ...registerForm,
      type: 1 // 默认启用
  };
  const result = (await HttpManager.setSongList(params)) as any;
  ElMessage({
    message: result.message,
    type: result.type,
  });

  if (result.success) {
    getData();
    // 重置表单
    registerForm.title = "";
    registerForm.introduction = "";
    registerForm.style = "";
  }
  centerDialogVisible.value = false;
}

// 编辑
function editRow(row) {
  // ✨ 这里会把 row 里的 type 属性也赋值给 editForm
  Object.assign(editForm, row); 
  editVisible.value = true;
}

async function saveEdit() {
  // ✨ 提交时会带上修改后的 type
  const result = (await HttpManager.updateSongListMsg(editForm)) as any;
  ElMessage({
    message: result.message,
    type: result.type,
  });
  if (result.success) getData();
  editVisible.value = false;
}

// 删除逻辑 (单条 + 批量)
function handleSelectionChange(val) {
  if (isRestoringSelection.value) {
    return;
  }

  const currentPageIds = new Set(paginatedData.value.map((item) => String(item.id)));
  currentPageIds.forEach((id) => selectedRowsById.delete(id));
  val.forEach((item) => selectedRowsById.set(String(item.id), item));
  syncMultipleSelection();
}

// 单条删除
function handleSingleDelete(id) {
  ElMessageBox.confirm('确定要删除该歌单吗？删除后无法恢复。', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await deleteOperation(id);
  }).catch(() => undefined);
}

// 批量删除
function handleBatchDelete() {
  ElMessageBox.confirm(`确定要删除选中的 ${multipleSelection.value.length} 个歌单吗？`, '批量删除', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const promises = multipleSelection.value.map(item => HttpManager.deleteSongList(item.id));
    try {
      await Promise.all(promises);
      ElMessage.success("批量删除成功");
      clearCrossPageSelection();
      getData();
    } catch (e) {
      ElMessage.error("部分删除失败，请刷新重试");
    }
  }).catch(() => undefined);
}

async function deleteOperation(id) {
  const result = await HttpManager.deleteSongList(id) as any;
  if (result.success) {
    selectedRowsById.delete(String(id));
    syncMultipleSelection();
    ElMessage.success(result.message);
    getData();
  } else {
    ElMessage.error(result.message);
  }
}
</script>

<style scoped>
/* 容器样式 */
.container {
  padding: 28px;
  background-color: rgba(255, 255, 255, 0.94);
  border-radius: 28px;
  border: 1px solid rgba(226, 234, 243, 0.92);
  box-shadow: 0 18px 40px rgba(17, 37, 70, 0.08);
}

/* 顶部操作栏 */
.handle-box {
  margin-bottom: 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.quick-filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-label {
  color: #5b6b88;
  font-size: 13px;
  font-weight: 700;
}

.left-panel, .right-panel {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.handle-input {
  width: 240px;
}

/* 表格样式优化 */
.table-img {
  width: 80px;
  height: 80px;
  border-radius: 20px;
}

/* 图片上传覆盖层特效 */
.img-upload-box {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto;
  overflow: hidden;
  border-radius: 20px;
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(17, 37, 70, 0.14);
}

.upload-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, rgba(12, 18, 30, 0.08) 0%, rgba(14, 23, 41, 0.64) 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.img-upload-box:hover .upload-mask {
  opacity: 1;
}

.upload-icon {
  font-size: 20px;
  margin-bottom: 2px;
}

/* 简介文本截断 */
.introduction-text {
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0;
  cursor: pointer;
  color: var(--admin-text-secondary);
}

.detail-action-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.content-action-button,
.comment-action-button {
  min-width: 0;
  height: 28px;
  padding: 0 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  color: #ffffff !important;
}

.content-action-button {
  border-color: rgba(70, 142, 255, 0.96) !important;
  background: linear-gradient(135deg, #438fff 0%, #1f6bff 100%) !important;
  box-shadow: 0 10px 20px rgba(31, 107, 255, 0.16);
}

.content-action-button:hover,
.content-action-button:focus-visible {
  color: #ffffff !important;
  border-color: rgba(92, 156, 255, 0.98) !important;
  background: linear-gradient(135deg, #5a9dff 0%, #3a7eff 100%) !important;
}

.comment-action-button {
  border-color: rgba(31, 201, 135, 0.94) !important;
  background: linear-gradient(135deg, #37cf8f 0%, #14b97b 100%) !important;
  box-shadow: 0 10px 20px rgba(20, 185, 123, 0.14);
}

.comment-action-button:hover,
.comment-action-button:focus-visible {
  color: #ffffff !important;
  border-color: rgba(60, 214, 150, 0.98) !important;
  background: linear-gradient(135deg, #4fda9c 0%, #22c888 100%) !important;
}

.pagination-box {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
