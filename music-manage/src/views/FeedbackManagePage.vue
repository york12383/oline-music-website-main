<template>
  <div class="feedback-manage-page">
    <div class="page-head">
      <h1 class="page-title">反馈意见管理</h1>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" clearable placeholder="筛选标题、内容、用户名或联系方式" class="toolbar-input" />
      <el-select v-model="statusFilter" class="toolbar-select">
        <el-option label="全部状态" value="all" />
        <el-option label="待处理" value="0" />
        <el-option label="已处理" value="1" />
      </el-select>
      <el-select v-model="typeFilter" class="toolbar-select">
        <el-option label="全部类型" value="all" />
        <el-option label="功能问题" value="bug" />
        <el-option label="体验建议" value="suggestion" />
        <el-option label="内容纠错" value="content" />
        <el-option label="其他" value="other" />
      </el-select>
      <el-button @click="loadFeedback">刷新</el-button>
    </div>

    <el-table :data="pagedFeedback" border size="small" height="620px" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-tag effect="plain">{{ feedbackTypeText(row.feedbackType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column label="内容" min-width="320" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.content }}</span>
        </template>
      </el-table-column>
      <el-table-column label="提交人" width="120">
        <template #default="{ row }">
          <span>{{ row.username || "匿名用户" }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="contact" label="联系方式" width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'">
            {{ row.status === 1 ? "已处理" : "待处理" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">
          <span>{{ formatDate(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" align="center">
        <template #default="{ row }">
          <div class="feedback-action-group">
            <el-button
              v-if="row.status !== 1"
              type="success"
              size="small"
              @click="changeStatus(row, 1)"
            >
              标记已处理
            </el-button>
            <el-button
              v-else
              type="warning"
              size="small"
              @click="changeStatus(row, 0)"
            >
              设为待处理
            </el-button>
            <el-button type="danger" size="small" @click="removeFeedback(row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="overview-panel">
      <div class="overview-head">
        <h2>反馈处理概览</h2>
      </div>

      <div class="overview-grid">
        <div class="overview-card">
          <span class="overview-label">待处理反馈</span>
          <strong class="overview-value">{{ feedbackOverview.pending }}</strong>
          <span class="overview-hint">建议优先处理高频问题</span>
        </div>

        <div class="overview-card">
          <span class="overview-label">当前已处理</span>
          <strong class="overview-value">{{ feedbackOverview.processed }}</strong>
          <span class="overview-hint">当前筛选结果范围内</span>
        </div>

        <div class="overview-card">
          <span class="overview-label">功能问题占比</span>
          <strong class="overview-value">{{ feedbackOverview.bugRatio }}</strong>
          <span class="overview-hint">方便快速判断问题类型重心</span>
        </div>

        <div class="overview-card distribution-card">
          <span class="overview-label">类型分布</span>
          <div class="distribution-bar">
            <span
              v-for="item in feedbackOverview.typeDistribution"
              :key="item.type"
              class="distribution-segment"
              :style="{ width: item.percent, background: item.color }"
            ></span>
          </div>
          <div class="distribution-legend">
            <span v-for="item in feedbackOverview.typeDistribution" :key="`${item.type}-legend`" class="legend-item">
              <i :style="{ background: item.color }"></i>
              {{ item.label }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="filteredFeedback.length"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { HttpManager } from "@/api";
import { isSameRouteQuery, mergeRouteQuery, normalizeQueryValue, parsePositiveIntQuery } from "@/utils/route-query";

interface FeedbackItem {
  id: number;
  username?: string;
  feedbackType: string;
  title: string;
  content: string;
  contact?: string;
  pagePath?: string;
  status: number;
  createTime?: string;
}

const feedbackList = ref<FeedbackItem[]>([]);
const loading = ref(false);
const route = useRoute();
const router = useRouter();
const normalizeStatusFilter = (value: unknown) => {
  const normalized = normalizeQueryValue(value as any);
  return ["all", "0", "1"].includes(normalized) ? normalized : "all";
};
const normalizeTypeFilter = (value: unknown) => {
  const normalized = normalizeQueryValue(value as any);
  return ["all", "bug", "suggestion", "content", "other"].includes(normalized) ? normalized : "all";
};
const keyword = ref(normalizeQueryValue(route.query.keyword));
const statusFilter = ref(normalizeStatusFilter(route.query.status));
const typeFilter = ref(normalizeTypeFilter(route.query.type));
const currentPage = ref(parsePositiveIntQuery(route.query.page, 1));
const pageSize = 10;
const isSyncingRouteState = ref(false);

const filteredFeedback = computed(() => {
  const lowerKeyword = keyword.value.trim().toLowerCase();
  return feedbackList.value.filter((item) => {
    const hitStatus = statusFilter.value === "all" || String(item.status) === statusFilter.value;
    const hitType = typeFilter.value === "all" || item.feedbackType === typeFilter.value;
    const hitKeyword =
      !lowerKeyword ||
      [item.title, item.content, item.username, item.contact, item.pagePath]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(lowerKeyword));
    return hitStatus && hitType && hitKeyword;
  });
});

const pagedFeedback = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredFeedback.value.slice(start, start + pageSize);
});

const feedbackOverview = computed(() => {
  const source = filteredFeedback.value;
  const total = source.length;
  const pending = source.filter((item) => item.status !== 1).length;
  const processed = source.filter((item) => item.status === 1).length;
  const bugCount = source.filter((item) => item.feedbackType === "bug").length;
  const bugRatio = total ? `${Math.round((bugCount / total) * 100)}%` : "0%";

  const distributionConfig = [
    { type: "bug", label: "功能问题", color: "#2D7DFF" },
    { type: "suggestion", label: "建议优化", color: "#14B97B" },
    { type: "content", label: "内容纠错", color: "#F59E0B" },
    { type: "other", label: "其他类型", color: "#F35B66" },
  ];

  const typeDistribution = distributionConfig.map((item) => {
    const count = source.filter((feedback) => feedback.feedbackType === item.type).length;
    const width = total ? `${Math.max((count / total) * 100, count ? 10 : 0)}%` : "0%";
    return {
      ...item,
      count,
      percent: width,
    };
  });

  return {
    pending,
    processed,
    bugRatio,
    typeDistribution,
  };
});

watch([keyword, statusFilter, typeFilter], () => {
  if (isSyncingRouteState.value) return;
  currentPage.value = 1;
  syncListQuery();
});

watch(currentPage, () => {
  if (isSyncingRouteState.value) return;
  syncListQuery();
});

watch(filteredFeedback, () => {
  const maxPage = Math.max(1, Math.ceil(filteredFeedback.value.length / pageSize));
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage;
  }
});

watch(
  () => [route.query.page, route.query.keyword, route.query.status, route.query.type],
  () => {
    const nextPage = parsePositiveIntQuery(route.query.page, 1);
    const nextKeyword = normalizeQueryValue(route.query.keyword);
    const nextStatus = normalizeStatusFilter(route.query.status);
    const nextType = normalizeTypeFilter(route.query.type);

    if (
      nextPage === currentPage.value &&
      nextKeyword === keyword.value &&
      nextStatus === statusFilter.value &&
      nextType === typeFilter.value
    ) {
      return;
    }

    isSyncingRouteState.value = true;
    currentPage.value = nextPage;
    keyword.value = nextKeyword;
    statusFilter.value = nextStatus;
    typeFilter.value = nextType;
    isSyncingRouteState.value = false;
  }
);

function buildListQuery() {
  return {
    page: currentPage.value > 1 ? currentPage.value : undefined,
    keyword: keyword.value.trim() || undefined,
    status: statusFilter.value !== "all" ? statusFilter.value : undefined,
    type: typeFilter.value !== "all" ? typeFilter.value : undefined,
  };
}

async function syncListQuery() {
  const nextQuery = mergeRouteQuery(route.query, buildListQuery());
  if (!isSameRouteQuery(route.query, nextQuery)) {
    await router.replace({ path: route.path, query: nextQuery });
  }
}

function feedbackTypeText(type: string) {
  const map: Record<string, string> = {
    bug: "功能问题",
    suggestion: "体验建议",
    content: "内容纠错",
    other: "其他",
  };
  return map[type] || "其他";
}

function formatDate(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  const hour = `${date.getHours()}`.padStart(2, "0");
  const minute = `${date.getMinutes()}`.padStart(2, "0");
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

async function loadFeedback() {
  loading.value = true;
  try {
    const result = (await HttpManager.getAllFeedback()) as ResponseBody;
    if (result.success) {
      feedbackList.value = Array.isArray(result.data) ? result.data : [];
    } else {
      feedbackList.value = [];
      ElMessage.error(result.message || "获取反馈失败");
    }
  } catch (error) {
    feedbackList.value = [];
    ElMessage.error("获取反馈失败，请稍后重试");
  } finally {
    loading.value = false;
  }
}

async function changeStatus(row: FeedbackItem, status: number) {
  try {
    const result = (await HttpManager.updateFeedbackStatus(row.id, status)) as ResponseBody;
    ElMessage({
      message: result.message,
      type: (result.type as any) || (result.success ? "success" : "error"),
    });
    if (result.success) {
      row.status = status;
    }
  } catch (error) {
    ElMessage.error("更新反馈状态失败");
  }
}

async function removeFeedback(id: number) {
  try {
    await ElMessageBox.confirm("确定删除这条反馈吗？删除后不可恢复。", "删除确认", {
      confirmButtonText: "确定删除",
      cancelButtonText: "取消",
      type: "warning",
    });
    const result = (await HttpManager.deleteFeedback(id)) as ResponseBody;
    ElMessage({
      message: result.message,
      type: (result.type as any) || (result.success ? "success" : "error"),
    });
    if (result.success) {
      feedbackList.value = feedbackList.value.filter((item) => item.id !== id);
    }
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("删除反馈失败");
    }
  }
}

function handlePageChange(page: number) {
  currentPage.value = page;
}

onMounted(() => {
  loadFeedback();
});
</script>

<style scoped>
.feedback-manage-page {
  padding: 28px;
}

.page-head {
  margin-bottom: 22px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: var(--admin-text);
  font-weight: 800;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 22px;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(247, 250, 255, 0.98) 0%, rgba(243, 248, 255, 0.94) 100%);
  border: 1px solid rgba(226, 234, 243, 0.92);
}

.toolbar-input {
  width: 320px;
}

.toolbar-select {
  width: 150px;
}

.feedback-action-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.feedback-action-group :deep(.el-button + .el-button) {
  margin-left: 0;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.overview-panel {
  margin-top: 24px;
  padding: 26px 24px 22px;
  border-radius: 24px;
  border: 1px solid rgba(226, 234, 243, 0.92);
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.96) 0%, rgba(255, 255, 255, 0.98) 100%);
}

.overview-head h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: var(--admin-text);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.overview-card {
  display: flex;
  min-height: 138px;
  flex-direction: column;
  justify-content: space-between;
  padding: 22px 20px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid rgba(226, 234, 243, 0.92);
  box-shadow: 0 12px 26px rgba(17, 37, 70, 0.06);
}

.overview-label {
  color: var(--admin-text-secondary);
  font-size: 14px;
  font-weight: 700;
}

.overview-value {
  font-size: 40px;
  font-weight: 800;
  line-height: 1;
  color: var(--admin-text);
}

.overview-hint {
  color: var(--admin-text-tertiary);
  font-size: 12px;
  font-weight: 700;
}

.distribution-card {
  gap: 18px;
}

.distribution-bar {
  display: flex;
  height: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: #edf3fb;
}

.distribution-segment {
  min-width: 0;
  border-radius: 999px;
}

.distribution-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--admin-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.legend-item i {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
