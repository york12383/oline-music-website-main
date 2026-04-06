<template>
  <div class="container">
    <div class="handle-box">
      <div class="left-panel">
        <el-button
          type="danger"
          :icon="Delete"
          @click="handleDeleteSelected"
          :disabled="multipleSelection.length === 0"
        >
          删除
        </el-button>
        <el-input v-model="searchWord" :prefix-icon="Search" placeholder="筛选歌手" class="handle-input"></el-input>
      </div>
      <div class="right-panel">
        <el-button type="primary" :icon="Plus" @click="centerDialogVisible = true">添加歌手</el-button>
      </div>
    </div>

    <div class="quick-filter-group">
      <span class="filter-label">资料筛选</span>
      <el-radio-group v-model="introStatusFilter" size="small">
        <el-radio-button v-for="item in introStatusOptions" :key="item.value" :label="item.value">
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

      <el-table-column label="歌手图片" width="110" align="center">
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

      <el-table-column label="歌手" prop="name" width="120" align="center"></el-table-column>

      <el-table-column label="性别" prop="sex" width="80" align="center">
        <template #default="scope">
          <el-tag :type="getSexTagType(scope.row.sex)">{{ changeSex(scope.row.sex) || '待补充' }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="成立/出生" prop="birth" width="120" align="center">
        <template #default="scope">
          <div>{{ getSingerBirthText(scope.row) || '待补充' }}</div>
        </template>
      </el-table-column>

      <el-table-column label="地区" prop="location" width="100" align="center">
        <template #default="scope">
          <div>{{ scope.row.location || '待补充' }}</div>
        </template>
      </el-table-column>

      <el-table-column label="简介" min-width="200">
        <template #default="scope">
          <el-tooltip effect="dark" :content="scope.row.introduction" placement="top">
            <p class="introduction-text">{{ scope.row.introduction || '暂无简介' }}</p>
          </el-tooltip>
        </template>
      </el-table-column>

      <el-table-column label="内容管理" width="120" align="center">
        <template #default="scope">
          <el-button class="content-link-button" size="small" type="primary" :icon="Headset" @click="goSongPage(scope.row)">
            歌曲管理
          </el-button>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="120" align="center">
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

    <el-dialog title="添加歌手" v-model="centerDialogVisible" width="500px" destroy-on-close>
      <el-form label-width="80px" :model="registerForm" :rules="singerRule" ref="addFormRef">
        <el-form-item label="歌手名" prop="name">
          <el-input v-model="registerForm.name" placeholder="请输入歌手姓名"></el-input>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="registerForm.sex">
            <el-radio :label="0">女</el-radio>
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">组合</el-radio>
            <el-radio :label="3">不明</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="故乡" prop="location">
          <el-input v-model="registerForm.location" placeholder="请输入国家或地区"></el-input>
        </el-form-item>
        <el-form-item :label="getBirthFieldLabel(registerForm.sex)" prop="birth">
          <el-date-picker type="date" v-model="registerForm.birth" :placeholder="getBirthFieldPlaceholder(registerForm.sex)" style="width: 100%"></el-date-picker>
        </el-form-item>
        <el-form-item label="歌手介绍" prop="introduction">
          <el-input type="textarea" :rows="4" v-model="registerForm.introduction" placeholder="请输入歌手简介"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="centerDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="addSinger">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="编辑歌手" v-model="editVisible" width="500px">
      <el-form label-width="80px" :model="editForm" :rules="singerRule">
        <el-form-item label="歌手名" prop="name">
          <el-input v-model="editForm.name"></el-input>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="editForm.sex">
            <el-radio :label="0">女</el-radio>
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">组合</el-radio>
            <el-radio :label="3">不明</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="getBirthFieldLabel(editForm.sex)" prop="birth">
          <el-date-picker type="date" v-model="editForm.birth" :placeholder="getBirthFieldPlaceholder(editForm.sex)" style="width: 100%"></el-date-picker>
        </el-form-item>
        <el-form-item label="地区" prop="location">
          <el-input v-model="editForm.location"></el-input>
        </el-form-item>
        <el-form-item label="简介" prop="introduction">
          <el-input type="textarea" :rows="4" v-model="editForm.introduction"></el-input>
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
import { ref, reactive, computed, watch, getCurrentInstance, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Search, Plus, Edit, Picture, Upload, Headset } from '@element-plus/icons-vue';
import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api/index";
import { RouterName } from "@/enums";
import { getBirth } from "@/utils";
import { isSameRouteQuery, mergeRouteQuery, normalizeQueryValue, parsePositiveIntQuery } from "@/utils/route-query";

const { changeSex, routerManager, beforeImgUpload } = mixin();
const { proxy } = getCurrentInstance() as any;
const route = useRoute();
const router = useRouter();

const tableRef = ref();
const tableData = ref<any[]>([]);
const filteredData = ref<any[]>([]);
const loading = ref(false);
const pageSize = ref(7);
const currentPage = ref(parsePositiveIntQuery(route.query.page, 1));
const searchWord = ref(normalizeQueryValue(route.query.keyword));
const introStatusOptions = [
  { label: "全部", value: "all" },
  { label: "缺简介", value: "missing" },
];
const normalizeIntroStatus = (value: unknown) => {
  const normalized = normalizeQueryValue(value as any);
  return introStatusOptions.some((item) => item.value === normalized) ? normalized : "all";
};
const introStatusFilter = ref(normalizeIntroStatus(route.query.introStatus));
const multipleSelection = ref<any[]>([]);
const isRestoringSelection = ref(false);
const isSyncingRouteState = ref(false);
const selectedRowsById = new Map<string, any>();

const centerDialogVisible = ref(false);
const editVisible = ref(false);

const addFormRef = ref(null);
const registerForm = reactive({
  name: "",
  sex: 1,
  birth: new Date(),
  location: "",
  introduction: "",
});

const editForm = reactive({
  id: "",
  name: "",
  sex: "",
  pic: "",
  birth: new Date(),
  location: "",
  introduction: "",
});

const singerRule = reactive({
  name: [{ required: true, message: "请输入歌手名字", trigger: "blur" }],
  sex: [{ required: true, message: "请选择性别", trigger: "change" }],
});

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

watch([searchWord, introStatusFilter], () => {
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
  () => [route.query.page, route.query.keyword, route.query.introStatus],
  () => {
    const nextPage = parsePositiveIntQuery(route.query.page, 1);
    const nextKeyword = normalizeQueryValue(route.query.keyword);
    const nextIntroStatus = normalizeIntroStatus(route.query.introStatus);
    const filterContextChanged = nextKeyword !== searchWord.value || nextIntroStatus !== introStatusFilter.value;

    if (nextPage === currentPage.value && nextKeyword === searchWord.value && nextIntroStatus === introStatusFilter.value) {
      return;
    }

    isSyncingRouteState.value = true;
    if (filterContextChanged) {
      clearCrossPageSelection();
    }
    currentPage.value = nextPage;
    searchWord.value = nextKeyword;
    introStatusFilter.value = nextIntroStatus;
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

getData();

function buildListQuery() {
  return {
    page: currentPage.value > 1 ? currentPage.value : undefined,
    keyword: searchWord.value.trim() || undefined,
    introStatus: introStatusFilter.value !== "all" ? introStatusFilter.value : undefined,
  };
}

async function syncListQuery() {
  const nextQuery = mergeRouteQuery(route.query, buildListQuery());
  if (!isSameRouteQuery(route.query, nextQuery)) {
    await router.replace({ path: route.path, query: nextQuery });
  }
}

function applyFilter() {
  const keyword = searchWord.value.trim().toLowerCase();
  if (!keyword) {
    filteredData.value = [...tableData.value];
  } else {
    filteredData.value = tableData.value.filter((item) =>
      String(item.name || "").toLowerCase().includes(keyword)
    );
  }

  if (introStatusFilter.value === "missing") {
    filteredData.value = filteredData.value.filter((item) => !String(item.introduction || "").trim());
  }

  const maxPage = Math.max(1, Math.ceil(filteredData.value.length / pageSize.value));
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage;
  }
}

async function getData() {
  loading.value = true;
  try {
    const result = (await HttpManager.getAllSinger()) as any;
    tableData.value = Array.isArray(result?.data) ? result.data : [];
    applyFilter();
    pruneCrossPageSelection(tableData.value);
  } catch (error) {
    tableData.value = [];
    filteredData.value = [];
    clearCrossPageSelection();
    ElMessage.error("获取歌手列表失败");
  } finally {
    loading.value = false;
  }
}

function handleCurrentChange(val) {
  currentPage.value = val;
}

function uploadUrl(id) {
  return HttpManager.attachImageUrl(`/singer/avatar/update?id=${id}`);
}

function attachImageUrl(url) {
  return HttpManager.attachImageUrl(url);
}

function handleImgSuccess(response) {
  ElMessage({
    message: response.message,
    type: response.type,
  });
  if (response.success) getData();
}

function getSexTagType(sex) {
  const normalizedSex = typeof sex === "string" && sex.trim() !== "" ? Number(sex) : sex;
  if (normalizedSex === 0) return 'danger';
  if (normalizedSex === 1) return '';
  if (normalizedSex === 2) return 'warning';
  return 'info';
}

function isGroupSinger(sex) {
  const normalizedSex = typeof sex === "string" && sex.trim() !== "" ? Number(sex) : sex;
  return normalizedSex === 2;
}

function getBirthFieldLabel(sex) {
  return isGroupSinger(sex) ? "成立日期" : "出生日期";
}

function getBirthFieldPlaceholder(sex) {
  return isGroupSinger(sex) ? "选择组合成立日期" : "选择出生日期";
}

function getSingerBirthText(row) {
  if (!row) return "";
  const birthText = getBirth(row.birth);
  if (!birthText) return "";
  if (isGroupSinger(row.sex)) {
    if (birthText.endsWith("-01-01")) {
      return birthText.slice(0, 4) + "年";
    }
    if (birthText.endsWith("-01")) {
      return birthText.slice(0, 7);
    }
  }
  return birthText;
}

function goSongPage(row) {
  const breadcrumbList = [
    { path: RouterName.Singer, query: buildListQuery(), name: "歌手管理" },
    { path: "", name: "歌曲信息" },
  ];
  proxy.$store.commit("setBreadcrumbList", breadcrumbList);
  routerManager(RouterName.Song, {
    path: RouterName.Song,
    query: { id: row.id, name: row.name },
  });
}

function resetRegisterForm() {
  registerForm.name = "";
  registerForm.sex = 1;
  registerForm.location = "";
  registerForm.introduction = "";
  registerForm.birth = new Date();
}

async function addSinger() {
  if (!registerForm.name.trim()) return ElMessage.warning("请输入歌手姓名");

  const params = {
    name: registerForm.name.trim(),
    sex: registerForm.sex,
    birth: getBirth(registerForm.birth),
    location: registerForm.location,
    introduction: registerForm.introduction,
  };

  const result = (await HttpManager.setSinger(params)) as any;
  ElMessage({
    message: result.message,
    type: result.type,
  });

  if (result.success) {
    await getData();
    resetRegisterForm();
  }
  centerDialogVisible.value = false;
}

function editRow(row) {
  Object.assign(editForm, row);
  editForm.birth = row.birth ? new Date(row.birth) : new Date();
  editVisible.value = true;
}

async function saveEdit() {
  try {
    const params = {
      id: editForm.id,
      name: editForm.name,
      sex: editForm.sex,
      birth: getBirth(new Date(editForm.birth)),
      location: editForm.location,
      introduction: editForm.introduction,
    };

    const result = (await HttpManager.updateSingerMsg(params)) as any;
    ElMessage({
      message: result.message,
      type: result.type,
    });

    if (result.success) await getData();
    editVisible.value = false;
  } catch (error) {
    ElMessage.error("更新失败");
  }
}

function handleSelectionChange(val) {
  if (isRestoringSelection.value) {
    return;
  }

  const currentPageIds = new Set(paginatedData.value.map((item) => String(item.id)));
  currentPageIds.forEach((id) => selectedRowsById.delete(id));
  val.forEach((item) => selectedRowsById.set(String(item.id), item));
  syncMultipleSelection();
}

async function handleSingleDelete(row) {
  await confirmDelete([row]);
}

async function handleDeleteSelected() {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning("请先选择要删除的歌手");
    return;
  }
  await confirmDelete(multipleSelection.value);
}

async function confirmDelete(rows) {
  const ids = rows.map((item) => item.id);
  const single = rows.length === 1;
  const targetName = single ? `“${rows[0].name}”` : `选中的 ${rows.length} 位歌手`;

  try {
    await ElMessageBox.confirm(
      `确定要删除${targetName}吗？删除后其关联歌曲也会一并删除。`,
      single ? '删除歌手' : '批量删除歌手',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
  } catch {
    ElMessage.info('已取消删除');
    return;
  }

  loading.value = true;
  try {
    const results = await Promise.all(
      ids.map(async (id) => {
        try {
          return await HttpManager.deleteSinger(id);
        } catch (error) {
          return { success: false, message: '删除失败' };
        }
      })
    );

    const successCount = results.filter((item: any) => item?.success).length;
    const failCount = ids.length - successCount;

    if (failCount === 0) {
      ElMessage.success(single ? '删除成功' : `已删除 ${successCount} 位歌手`);
    } else if (successCount === 0) {
      ElMessage.error(single ? '删除失败' : '批量删除失败');
    } else {
      ElMessage.warning(`已删除 ${successCount} 位歌手，${failCount} 位删除失败`);
    }

    ids.forEach((id) => selectedRowsById.delete(String(id)));
    syncMultipleSelection();
    await getData();
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.container {
  padding: 28px;
  background-color: rgba(255, 255, 255, 0.94);
  border-radius: 28px;
  border: 1px solid rgba(226, 234, 243, 0.92);
  box-shadow: 0 18px 40px rgba(17, 37, 70, 0.08);
}

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

.table-img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.img-upload-box {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto;
  border-radius: 50%;
  overflow: hidden;
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

.introduction-text {
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0;
  cursor: help;
  color: var(--admin-text-secondary);
}

.content-link-button {
  color: #ffffff !important;
  border-color: rgba(46, 201, 224, 0.94) !important;
  background: linear-gradient(135deg, #41d5e6 0%, #1f8fff 100%) !important;
  box-shadow: 0 10px 20px rgba(31, 107, 255, 0.16);
}

.content-link-button:hover,
.content-link-button:focus-visible {
  color: #ffffff !important;
  border-color: rgba(73, 218, 236, 0.98) !important;
  background: linear-gradient(135deg, #58ddec 0%, #3b9cff 100%) !important;
}

.pagination-box {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
