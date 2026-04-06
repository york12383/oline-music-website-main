<template>
  <div class="main-wrapper">
    <div class="breadcrumb-bar">
      <el-breadcrumb separator-icon="ArrowRight">
        <el-breadcrumb-item v-for="item in breadcrumbList" :key="item.path"
          :to="{ path: item.path, query: item.query }">
          {{ item.name }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="container">
      <div class="handle-box">
        <div class="left-panel">
          <el-button type="danger" :icon="Delete" @click="handleBatchDelete"
            :disabled="multipleSelection.length === 0">批量删除</el-button>
          <el-input v-model="searchWord" :prefix-icon="Search" placeholder="筛选歌曲" class="handle-input"></el-input>
        </div>
        <div class="right-panel">
          <el-button plain :icon="Upload" @click="openBatchDialog">批量导入LRC</el-button>
          <el-button type="primary" :icon="Plus" @click="isAddVisible = true">添加歌曲</el-button>
        </div>
      </div>

      <div class="quick-filter-group">
        <span class="filter-label">歌词筛选</span>
        <el-radio-group v-model="lyricStatusFilter" size="small">
          <el-radio-button v-for="item in lyricStatusOptions" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <div class="quick-filter-group">
        <span class="filter-label">资源筛选</span>
        <el-radio-group v-model="resourceStatusFilter" size="small">
          <el-radio-button v-for="item in resourceStatusOptions" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <el-table ref="tableRef" border size="small" stripe v-loading="loading" :data="paginatedData" :row-key="getRowKey"
        @selection-change="handleSelectionChange">
        <el-table-column type="selection" :reserve-selection="true" width="40" align="center"></el-table-column>
        <el-table-column label="ID" prop="id" width="60" align="center"></el-table-column>

        <el-table-column label="封面/播放" width="100" align="center">
          <template #default="scope">
            <div class="song-cover-box" @click="togglePlay(scope.row)">
              <el-image :src="attachImageUrl(scope.row.pic)" class="cover-img" fit="cover">
                <template #error>
                  <div class="image-slot"><el-icon>
                      <Picture />
                    </el-icon></div>
                </template>
              </el-image>
              <div class="play-mask" :class="{ 'is-playing': isCurrentSong(scope.row.id) && isPlay }">
                <el-icon class="play-icon">
                  <component :is="isCurrentSong(scope.row.id) && isPlay ? VideoPause : VideoPlay" />
                </el-icon>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="歌名" prop="name" min-width="150"></el-table-column>
        <el-table-column label="歌手" prop="singerName" min-width="100"></el-table-column>
        <el-table-column label="专辑/简介" prop="introduction" min-width="150" show-overflow-tooltip></el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-dropdown trigger="click" @command="(command) => changeSongStatus(scope.row, command)">
              <el-tag :type="getSongStatusType(scope.row.type)" effect="light" style="cursor: pointer;">
                {{ getSongStatusLabel(scope.row.type) }}
                <el-icon class="el-icon--right">
                  <ArrowDown />
                </el-icon>
              </el-tag>

              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :disabled="scope.row.type === 1" :command="1">启用</el-dropdown-item>
                  <el-dropdown-item :disabled="scope.row.type === 2" :command="2">隐藏</el-dropdown-item>
                  <el-dropdown-item :disabled="scope.row.type === 3" :command="3">审核中</el-dropdown-item>
                  <el-dropdown-item :disabled="scope.row.type === 4" :command="4">禁用</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>

        <el-table-column label="歌词" align="center" width="100">
          <template #default="scope">
            <el-popover placement="left" title="歌词预览" :width="300" trigger="hover">
              <template #reference>
                <el-tag>查看歌词</el-tag>
              </template>
              <div class="lyrics-preview">
                {{ scope.row.lyric || '暂无歌词' }}
              </div>
            </el-popover>
          </template>
        </el-table-column>

        <el-table-column label="歌词状态" width="110" align="center">
          <template #default="scope">
            <el-tag :type="getRowLyricTagType(scope.row.lyric)" effect="light">
              {{ getRowLyricLabel(scope.row.lyric) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="资源更新" width="180" align="center">
          <template #default="scope">
            <div class="upload-group">
              <el-tooltip content="更新封面" placement="top">
                <el-upload class="upload-item" :action="updateSongImg(scope.row.id)" :show-file-list="false"
                  :on-success="handleImgSuccess" :before-upload="beforeImgUpload">
                  <el-button class="resource-update-button resource-image-button" circle size="small" :icon="Picture" />
                </el-upload>
              </el-tooltip>

              <el-tooltip content="更新音频文件" placement="top">
                <el-upload class="upload-item" :action="updateSongUrl(scope.row.id)" :show-file-list="false"
                  :on-success="handleSongSuccess" :before-upload="beforeSongUpload">
                  <el-button class="resource-update-button resource-audio-button" circle size="small" :icon="Headset" />
                </el-upload>
              </el-tooltip>

              <el-tooltip content="更新LRC歌词" placement="top">
                <el-upload class="upload-item" :action="updateSongLrc(scope.row.id)" :show-file-list="false"
                  :on-success="handleSongSuccess" :before-upload="beforeSongUpload">
                  <el-button class="resource-update-button resource-lyric-button" circle size="small" :icon="Document" />
                </el-upload>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="评论" width="80" align="center">
          <template #default="scope">
            <el-button class="comment-view-button" size="small" type="primary" @click="goCommentPage(scope.row.id)">查看</el-button>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="90" align="center">
          <template #default="scope">
            <el-button size="small" :icon="Edit" @click="editRow(scope.row)">编辑</el-button>

          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-box">
        <el-pagination background layout="total, prev, pager, next" :current-page="currentPage" :page-size="pageSize"
          :total="total" @current-change="handleCurrentChange">
        </el-pagination>
      </div>
    </div>

    <el-dialog title="添加歌曲" v-model="isAddVisible" width="500px" destroy-on-close>
      <el-form label-width="100px" :model="registerForm" ref="addFormRef">
        <el-form-item label="选择歌手" prop="singerId">
          <el-select v-model="registerForm.singerId" placeholder="请选择歌手" style="width: 100%">
            <el-option v-for="singer in singers" :key="singer.id" :label="singer.name" :value="singer.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="歌曲名" prop="name">
          <el-input v-model="registerForm.name" placeholder="请输入歌曲名"></el-input>
        </el-form-item>
        <el-form-item label="专辑/简介" prop="introduction">
          <el-input v-model="registerForm.introduction" placeholder="专辑名称"></el-input>
        </el-form-item>
        <el-form-item label="歌词文本" prop="lyric">
          <el-input type="textarea" :rows="3" v-model="registerForm.lyric" placeholder="可选：直接粘贴歌词文本"></el-input>
        </el-form-item>

        <el-form-item label="歌词文件(.lrc)">
          <el-upload class="upload-demo" action="" :auto-upload="false" :limit="1" accept=".lrc"
            :on-change="(file) => handleFileChange(file, 'lrc')" :on-remove="() => handleFileRemove('lrc')">
            <el-button type="primary" plain>选择LRC文件</el-button>
          </el-upload>
        </el-form-item>

        <el-form-item label="歌曲文件(.mp3)">
          <el-upload class="upload-demo" action="" :auto-upload="false" :limit="1" accept=".mp3,.flac,.wav"
            :on-change="(file) => handleFileChange(file, 'song')" :on-remove="() => handleFileRemove('song')">
            <el-button type="primary">选择音频文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="isAddVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitAddSong">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="编辑歌曲" v-model="editVisible" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="歌曲名">
          <el-input v-model="editForm.name"></el-input>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.introduction"></el-input>
        </el-form-item>
        <el-form-item label="歌词">
          <el-input type="textarea" :rows="6" v-model="editForm.lyric"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editVisible = false">取 消</el-button>
          <el-button type="primary" @click="saveEdit">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="批量导入LRC" v-model="batchDialogVisible" width="620px" destroy-on-close>
      <div class="batch-hint">
        <p>全部歌曲页支持以下命名方式：</p>
        <p>`songId.lrc`、`歌手-歌名.lrc`</p>
      </div>

      <el-upload class="upload-demo" drag action="" multiple :auto-upload="false" accept=".lrc"
        :file-list="batchLrcFileList" :on-change="handleBatchLrcChange" :on-remove="handleBatchLrcRemove">
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将多个 LRC 文件拖到这里，或 <em>点击上传</em></div>
      </el-upload>

      <div v-if="batchImportResult" class="batch-result">
        <el-alert
          :title="`成功 ${batchImportResult.successCount} 个，未匹配 ${batchImportResult.unmatchedFiles.length} 个，失败 ${batchImportResult.failedFiles.length + batchImportResult.invalidFiles.length} 个`"
          type="info"
          :closable="false"
          show-icon
        />
        <div v-if="batchImportResult.unmatchedFiles.length" class="result-list">
          <strong>未匹配：</strong>{{ batchImportResult.unmatchedFiles.join('，') }}
        </div>
        <div v-if="batchImportResult.invalidFiles.length" class="result-list">
          <strong>无效文件：</strong>{{ batchImportResult.invalidFiles.join('，') }}
        </div>
        <div v-if="batchImportResult.failedFiles.length" class="result-list">
          <strong>导入失败：</strong>{{ batchImportResult.failedFiles.join('，') }}
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchDialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="batchImportLoading" @click="submitBatchImport">开始导入</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, watch, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useStore } from "vuex";
import { ElMessage, ElMessageBox } from "element-plus";
import { 
  Delete, Search, Plus, Edit, Picture, Headset, Document, 
  VideoPlay, VideoPause, ArrowRight, ArrowDown, Upload, UploadFilled
} from '@element-plus/icons-vue';
import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api";
import { RouterName } from "@/enums";
import { getLyricStatus, getLyricStatusLabel, getLyricStatusTagType, isInstrumentalLyric } from "@/utils";
import axios from 'axios';
import { isSameRouteQuery, mergeRouteQuery, normalizeQueryValue, parsePositiveIntQuery } from "@/utils/route-query";

// --- Setup & Hooks ---
const store = useStore();
const route = useRoute();
const router = useRouter();
const { routerManager, beforeImgUpload, beforeSongUpload } = mixin();

// --- State ---
const loading = ref(false);
const tableData = ref([]);
const filteredData = ref([]);
const tableRef = ref();
const searchWord = ref(normalizeQueryValue(route.query.keyword));
const pageSize = ref(7);
const currentPage = ref(parsePositiveIntQuery(route.query.page, 1));
const total = ref(0); // 添加总数据量
const multipleSelection = ref([]);
const isRestoringSelection = ref(false);
const singers = ref([]);
const batchDialogVisible = ref(false);
const batchImportLoading = ref(false);
const batchImportResult = ref(null);
const batchLrcFileList = ref([]);
const batchLrcFiles = ref([]);
const lyricStatusOptions = [
  { label: "全部", value: "all" },
  { label: "待补歌词", value: "health_missing" },
  { label: "仅空歌词", value: "missing" },
  { label: "已收录", value: "ready" },
  { label: "占位歌词", value: "placeholder" },
  { label: "格式异常", value: "abnormal" },
];
const resourceStatusOptions = [
  { label: "全部", value: "all" },
  { label: "缺音频", value: "missing_audio" },
];
const normalizeLyricStatus = (value: unknown) => {
  const normalized = normalizeQueryValue(value as any);
  return lyricStatusOptions.some((item) => item.value === normalized) ? normalized : "all";
};
const normalizeResourceStatus = (value: unknown) => {
  const normalized = normalizeQueryValue(value as any);
  return resourceStatusOptions.some((item) => item.value === normalized) ? normalized : "all";
};
const lyricStatusFilter = ref(normalizeLyricStatus(route.query.lyricStatus));
const resourceStatusFilter = ref(normalizeResourceStatus(route.query.resourceStatus));
const isSyncingRouteState = ref(false);

// 弹窗控制
const isAddVisible = ref(false);
const editVisible = ref(false);

// 表单数据
const registerForm = reactive({
  singerId: "",
  name: "",
  introduction: "",
  lyric: "",
});

// 暂存上传的文件
const tempFiles = reactive({
  lrc: null,
  song: null
});

const editForm = reactive({
  id: "",
  singerId: "",
  name: "",
  introduction: "",
  lyric: "",
  type: "",
  singerName: "",
});

// --- Computed ---
const breadcrumbList = computed(() => {
  return [
    { path: "/info", name: "系统首页" },
    { path: "", name: "所有歌曲" }
  ];
});

const isPlay = computed(() => store.getters.isPlay);
const selectedRowsById = new Map<string, any>();

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredData.value.slice(start, start + pageSize.value);
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

// --- Methods ---
const isCurrentSong = (songId) => {
  return store.getters.id === songId;
};

// 歌曲状态: 启用1 隐藏2 审核3 禁用4
function getSongStatusType(type: number) {
  switch (type) {
    case 1: return 'success'; // 启用
    case 2: return 'info';    // 隐藏
    case 3: return 'warning'; // 审核中
    case 4: return 'danger';  // 禁用
    default: return 'info';
  }
}

function getSongStatusLabel(type: number) {
  switch (type) {
    case 1: return '启用';
    case 2: return '隐藏';
    case 3: return '审核中';
    case 4: return '禁用';
    default: return '未知';
  }
}

function normalizeText(value: unknown) {
  return String(value ?? "").trim();
}

function stripLyricTimestamp(value: string) {
  return value.replace(/\[[0-9:.]+\]/g, "").trim();
}

function isHealthMissingLyric(value: unknown) {
  const normalized = normalizeText(value);
  if (!normalized) {
    return true;
  }
  if (isInstrumentalLyric(normalized)) {
    return false;
  }
  if (normalized.includes("暂无歌词")) {
    return true;
  }
  const stripped = stripLyricTimestamp(normalized).replace(/\s+/g, "");
  return !stripped || /^[?？]+$/.test(stripped);
}

function applyLocalFilters(records: any[]) {
  const keyword = searchWord.value.trim().toLowerCase();

  let nextRecords = records.filter((item) => {
    if (!keyword) {
      return true;
    }
    return [item.name, item.singerName, item.introduction]
      .filter(Boolean)
      .some((field) => String(field).toLowerCase().includes(keyword));
  });

  nextRecords = nextRecords.filter((item) => {
    const lyricStatus = getLyricStatus(item.lyric);
    if (lyricStatusFilter.value === "health_missing") {
      return isHealthMissingLyric(item.lyric);
    }
    if (lyricStatusFilter.value === "all") {
      return true;
    }
    return lyricStatus === lyricStatusFilter.value;
  });

  nextRecords = nextRecords.filter((item) => {
    if (resourceStatusFilter.value === "missing_audio") {
      return !normalizeText(item.url);
    }
    return true;
  });

  filteredData.value = nextRecords;
  total.value = nextRecords.length;
  const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value));
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage;
  }
}

// 1. 获取数据
async function getData() {
  loading.value = true;
  try {
    const [songResult, singerResult] = (await Promise.all([
      HttpManager.getAllSong(),
      HttpManager.getAllSinger(),
    ])) as any;
    
    // 建立歌手ID到名称的映射
    const singerMap = {};
    singerResult.data.forEach(singer => {
      singerMap[singer.id] = singer.name;
    });
    
    // 为每首歌曲添加歌手名称
    const songsWithSingerName = (Array.isArray(songResult.data) ? songResult.data : []).map(song => ({
      ...song,
      singerName: singerMap[song.singerId] || '未知歌手'
    }));
    
    tableData.value = songsWithSingerName;
    applyLocalFilters(songsWithSingerName);
    pruneCrossPageSelection(songsWithSingerName);
    
    // 更新歌手列表用于添加歌曲
    singers.value = singerResult.data;
  } catch (e) {
    ElMessage.error("获取数据失败");
  } finally {
    loading.value = false;
  }
}

store.commit("setIsPlay", false);
getData();
watch([searchWord, lyricStatusFilter, resourceStatusFilter], () => {
  if (isSyncingRouteState.value) return;
  clearCrossPageSelection();
  currentPage.value = 1;
  syncListQuery();
});

watch(currentPage, () => {
  if (isSyncingRouteState.value) return;
  syncListQuery();
});

watch(
  () => [route.query.page, route.query.keyword, route.query.lyricStatus, route.query.resourceStatus],
  () => {
    const nextPage = parsePositiveIntQuery(route.query.page, 1);
    const nextKeyword = normalizeQueryValue(route.query.keyword);
    const nextLyricStatus = normalizeLyricStatus(route.query.lyricStatus);
    const nextResourceStatus = normalizeResourceStatus(route.query.resourceStatus);
    const filterContextChanged =
      nextKeyword !== searchWord.value ||
      nextLyricStatus !== lyricStatusFilter.value ||
      nextResourceStatus !== resourceStatusFilter.value;

    if (
      nextPage === currentPage.value &&
      nextKeyword === searchWord.value &&
      nextLyricStatus === lyricStatusFilter.value &&
      nextResourceStatus === resourceStatusFilter.value
    ) {
      return;
    }

    isSyncingRouteState.value = true;
    if (filterContextChanged) {
      clearCrossPageSelection();
    }
    currentPage.value = nextPage;
    searchWord.value = nextKeyword;
    lyricStatusFilter.value = nextLyricStatus;
    resourceStatusFilter.value = nextResourceStatus;
    isSyncingRouteState.value = false;
    applyLocalFilters(tableData.value);
  }
);

watch(
  paginatedData,
  () => {
    void restoreCurrentPageSelection();
  },
  { flush: "post" }
);

function buildListQuery() {
  return {
    page: currentPage.value > 1 ? currentPage.value : undefined,
    keyword: searchWord.value.trim() || undefined,
    lyricStatus: lyricStatusFilter.value !== "all" ? lyricStatusFilter.value : undefined,
    resourceStatus: resourceStatusFilter.value !== "all" ? resourceStatusFilter.value : undefined,
  };
}

async function syncListQuery() {
  const nextQuery = mergeRouteQuery(route.query, buildListQuery());
  if (!isSameRouteQuery(route.query, nextQuery)) {
    await router.replace({ path: route.path, query: nextQuery });
  }
  applyLocalFilters(tableData.value);
}

// 2. 播放控制
function togglePlay(row) {
  const url = row.url;
  
  if (store.getters.url === url && isPlay.value) {
     store.commit("setIsPlay", false);
  } else {
     store.commit("setUrl", url);
     store.commit("setId", row.id);
     store.commit("setIsPlay", true);
  }
}

// 3. 新增歌曲
function handleFileChange(file, type) {
  tempFiles[type] = file.raw;
}

function handleFileRemove(type) {
  tempFiles[type] = null;
}

function openBatchDialog() {
  batchImportResult.value = null;
  batchLrcFileList.value = [];
  batchLrcFiles.value = [];
  batchDialogVisible.value = true;
}

function handleBatchLrcChange(_file, fileList) {
  batchLrcFileList.value = fileList;
  batchLrcFiles.value = fileList
    .map((item) => item.raw)
    .filter(Boolean);
}

function handleBatchLrcRemove(_file, fileList) {
  batchLrcFileList.value = fileList;
  batchLrcFiles.value = fileList
    .map((item) => item.raw)
    .filter(Boolean);
}

async function submitAddSong() {
  if(!registerForm.singerId || !registerForm.name || !tempFiles.song) {
    ElMessage.error("歌手、歌曲名和音频文件为必填项");
    return;
  }

  const formData = new FormData();
  formData.append("singerId", registerForm.singerId);
  formData.append("name", registerForm.name);
  formData.append("introduction", registerForm.introduction);
  formData.append("lyric", registerForm.lyric || "[00:00.00]纯音乐，请欣赏");
  
  if (tempFiles.song) formData.append("file", tempFiles.song);
  if (tempFiles.lrc) formData.append("lrcfile", tempFiles.lrc);

  try {
    const res = await axios.post(HttpManager.attachImageUrl(`/song/add`), formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    
    const result = res.data;
    ElMessage({
      message: result.message,
      type: result.type,
    });

    if (result.success) {
      getData();
      isAddVisible.value = false;
    }
  } catch (error) {
    ElMessage.error("上传失败");
  }
}

async function submitBatchImport() {
  if (!batchLrcFiles.value.length) {
    ElMessage.error("请先选择至少一个LRC文件");
    return;
  }

  const formData = new FormData();
  batchLrcFiles.value.forEach((file) => {
    formData.append("files", file);
  });

  batchImportLoading.value = true;
  try {
    const res = await axios.post(HttpManager.batchUpdateSongLrc(), formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    const result = res.data;
    batchImportResult.value = result.data || null;
    ElMessage({
      message: result.message,
      type: result.type,
    });
    if (result.success) {
      getData();
    }
  } catch (error) {
    ElMessage.error("批量导入失败");
  } finally {
    batchImportLoading.value = false;
  }
}

// 4. 编辑歌曲
function editRow(row) {
  Object.assign(editForm, row);
  // 如果歌曲名包含歌手名前缀，则只显示不包含前缀的部分
  if (editForm.name.startsWith(editForm.singerName + '-')) {
    editForm.name = editForm.name.substring(editForm.singerName.length + 1);
  }
  editVisible.value = true;
}

async function saveEdit() {
  // 名字格式：歌手名-歌名
  editForm.name = `${editForm.singerName}-${editForm.name}`;
  const result = (await HttpManager.updateSongMsg(editForm)) as any;
  ElMessage({
     message: result.message,
     type: result.type,
  });
  if (result.success) getData();
  editVisible.value = false;
}

// 5. 删除 (单条 + 批量)
function handleSelectionChange(val) {
  if (isRestoringSelection.value) {
    return;
  }

  const currentPageIds = new Set(paginatedData.value.map((item) => String(item.id)));
  currentPageIds.forEach((id) => selectedRowsById.delete(id));
  val.forEach((item) => selectedRowsById.set(String(item.id), item));
  syncMultipleSelection();
}

function handleSingleDelete(id) {
  ElMessageBox.confirm('确定删除这首歌吗？', '警告', {
    type: 'warning'
  }).then(async () => {
    const result = await HttpManager.deleteSong(id) as any;
    if (result.success) {
      selectedRowsById.delete(String(id));
      syncMultipleSelection();
      ElMessage.success("删除成功");
      getData();
    } else {
      ElMessage.error("删除失败");
    }
  }).catch(() => {
    ElMessage.error("删除失败");
  });
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${multipleSelection.value.length} 首歌吗？`, '批量删除', {
    type: 'warning'
  }).then(async () => {
    const promises = multipleSelection.value.map(item => HttpManager.deleteSong(item.id));
    await Promise.all(promises);
    ElMessage.success("批量删除执行完毕");
    clearCrossPageSelection();
    getData();
  }).catch(() => {
    ElMessage.error("批量删除失败");
  });
}

// 6. 辅助函数
function attachImageUrl(url) {
  return HttpManager.attachImageUrl(url);
}

function updateSongImg(id) {
  return HttpManager.attachImageUrl(`/song/img/update?id=${id}`);
}

function updateSongUrl(id) {
  return HttpManager.attachImageUrl(`/song/url/update?id=${id}`);
}

function updateSongLrc(id) {
  return HttpManager.attachImageUrl(`/song/lrc/update?id=${id}`);
}

function getRowLyricLabel(lyric) {
  return getLyricStatusLabel(getLyricStatus(lyric));
}

function getRowLyricTagType(lyric) {
  return getLyricStatusTagType(getLyricStatus(lyric));
}

function handleImgSuccess(res) {
  ElMessage({ message: res.message, type: res.type });
  if (res.success) getData();
}

function handleSongSuccess(res) {
  ElMessage({ message: res.message, type: res.type });
  if (res.success) getData();
}

// 翻页处理
function handleCurrentChange(val) {
  currentPage.value = val;
}

async function changeSongStatus(row, newType: number) {
  if (row.type === newType) return;

  ElMessageBox.confirm(`确定将歌曲《${row.name}》的状态修改为【${getSongStatusLabel(newType)}】吗？`, '状态变更', {
    type: 'warning'
  }).then(async () => {
    const result = (await HttpManager.updateSongStatus(row.id, newType)) as any;
    
    ElMessage({
      message: result.message,
      type: result.type,
    });
    
    if (result.success) {
      getData();
    }
  }).catch(() => {
    // 用户取消操作
  });
}

// 路由跳转
function goCommentPage(id) {
  const breadcrumbList = [
    { path: "/info", name: "系统首页" },
    { path: "/songs", query: buildListQuery(), name: "所有歌曲" },
    { path: "", name: "评论详情" },
  ];
  store.commit("setBreadcrumbList", breadcrumbList);
  routerManager(RouterName.Comment, { path: RouterName.Comment, query: { id, type: 0 } });
}
</script>

<style scoped>
/* 全局布局 */
.main-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.breadcrumb-bar {
  padding-left: 6px;
}

.container {
  flex: 1;
  padding: 28px;
  background-color: rgba(255, 255, 255, 0.94);
  border-radius: 28px;
  border: 1px solid rgba(226, 234, 243, 0.92);
  box-shadow: 0 18px 40px rgba(17, 37, 70, 0.08);
  display: flex;
  flex-direction: column;
}

/* 顶部操作栏 */
.handle-box {
  margin-bottom: 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
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

.quick-filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 14px 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(247, 250, 255, 0.98) 0%, rgba(243, 248, 255, 0.94) 100%);
  border: 1px solid rgba(226, 234, 243, 0.92);
}

.filter-label {
  font-size: 13px;
  color: var(--admin-text-secondary);
  white-space: nowrap;
  font-weight: 700;
}

.batch-hint {
  margin-bottom: 16px;
  color: var(--admin-text-secondary);
  line-height: 1.7;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(247, 250, 255, 0.96);
}

.batch-hint p {
  margin: 0 0 6px;
}

.batch-result {
  margin-top: 16px;
}

.result-list {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--admin-text-secondary);
}

/* 封面与播放按钮样式 */
.song-cover-box {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(17, 37, 70, 0.14);
}

.cover-img {
  width: 100%;
  height: 100%;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(145deg, #f2f6fc 0%, #f9fbfe 100%);
  color: #909399;
}

.play-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, rgba(12, 18, 30, 0.08) 0%, rgba(14, 23, 41, 0.64) 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.song-cover-box:hover .play-mask,
.play-mask.is-playing {
  opacity: 1;
}

.play-icon {
  font-size: 24px;
  color: white;
}

/* 上传按钮组 */
.upload-group {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.upload-item {
  display: inline-block;
}

.resource-update-button {
  color: #ffffff !important;
  border: none !important;
  box-shadow: 0 10px 18px rgba(17, 37, 70, 0.14);
}

.resource-image-button {
  background: linear-gradient(135deg, #27cf8b 0%, #14b97b 100%) !important;
}

.resource-audio-button {
  background: linear-gradient(135deg, #4b91ff 0%, #1f6bff 100%) !important;
}

.resource-lyric-button {
  background: linear-gradient(135deg, #f8b44a 0%, #f59e0b 100%) !important;
}

.resource-update-button:hover,
.resource-update-button:focus-visible {
  color: #ffffff !important;
  filter: brightness(1.06);
}

.comment-view-button {
  min-width: 0;
  height: 28px;
  padding: 0 12px;
  border-radius: 10px;
  color: #ffffff !important;
  border-color: rgba(70, 142, 255, 0.96) !important;
  background: linear-gradient(135deg, #438fff 0%, #1f6bff 100%) !important;
  box-shadow: 0 10px 20px rgba(31, 107, 255, 0.16);
}

.comment-view-button:hover,
.comment-view-button:focus-visible {
  color: #ffffff !important;
  border-color: rgba(92, 156, 255, 0.98) !important;
  background: linear-gradient(135deg, #5a9dff 0%, #3a7eff 100%) !important;
}

/* 分页 */
.pagination-box {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 歌词预览 */
.lyrics-preview {
  max-height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  color: var(--admin-text-secondary);
  line-height: 1.8;
}
</style>
