<template>
    <div class="rank-management-container">
      <h1 class="page-title">评分管理</h1>
      
      <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
        
        <el-tab-pane label="歌曲评分管理" name="songRank">
          <RankTable 
            :type="0" 
            :data="songRankData" 
            :total="songRankTotal"
            :loading="songLoading"
            :current-page-value="songQuery.currentPage"
            :page-size-value="songQuery.pageSize"
            :search-name="songQuery.name"
            :search-consumer-name="songQuery.consumerName"
            @search="handleRankQueryChange"
            @delete="handleDeleteRank"
            @page-change="handleRankQueryChange"
          />
          <div class="overview-panel">
            <div class="overview-head">
              <h2>评分概览</h2>
            </div>
            <div class="overview-grid">
              <div class="overview-card">
                <span class="overview-label">当前筛选评分数</span>
                <strong class="overview-value">{{ songOverview.total }}</strong>
                <span class="overview-hint">当前查询条件下的总记录数</span>
              </div>
              <div class="overview-card">
                <span class="overview-label">当前页平均分</span>
                <strong class="overview-value">{{ songOverview.average }}</strong>
                <span class="overview-hint">按 5 分制展示</span>
              </div>
              <div class="overview-card">
                <span class="overview-label">最高评分歌曲</span>
                <strong class="overview-value">{{ songOverview.topScore }}</strong>
                <span class="overview-hint">{{ songOverview.topName }}</span>
              </div>
              <div class="overview-card distribution-card">
                <span class="overview-label">评分分布</span>
                <div class="distribution-bar">
                  <span
                    v-for="item in songOverview.distribution"
                    :key="item.label"
                    class="distribution-segment"
                    :style="{ width: item.percent, background: item.color }"
                  ></span>
                </div>
                <div class="distribution-legend">
                  <span v-for="item in songOverview.distribution" :key="`${item.label}-legend`" class="legend-item">
                    <i :style="{ background: item.color }"></i>
                    {{ item.label }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="歌单评分管理" name="listRank">
          <RankTable 
            :type="1" 
            :data="listRankData" 
            :total="listRankTotal"
            :loading="listLoading"
            :current-page-value="listQuery.currentPage"
            :page-size-value="listQuery.pageSize"
            :search-name="listQuery.name"
            :search-consumer-name="listQuery.consumerName"
            @search="handleRankQueryChange"
            @delete="handleDeleteRank"
            @page-change="handleRankQueryChange"
          />
          <div class="overview-panel">
            <div class="overview-head">
              <h2>评分概览</h2>
            </div>
            <div class="overview-grid">
              <div class="overview-card">
                <span class="overview-label">当前筛选评分数</span>
                <strong class="overview-value">{{ listOverview.total }}</strong>
                <span class="overview-hint">当前查询条件下的总记录数</span>
              </div>
              <div class="overview-card">
                <span class="overview-label">当前页平均分</span>
                <strong class="overview-value">{{ listOverview.average }}</strong>
                <span class="overview-hint">按 5 分制展示</span>
              </div>
              <div class="overview-card">
                <span class="overview-label">最高评分歌单</span>
                <strong class="overview-value">{{ listOverview.topScore }}</strong>
                <span class="overview-hint">{{ listOverview.topName }}</span>
              </div>
              <div class="overview-card distribution-card">
                <span class="overview-label">评分分布</span>
                <div class="distribution-bar">
                  <span
                    v-for="item in listOverview.distribution"
                    :key="item.label"
                    class="distribution-segment"
                    :style="{ width: item.percent, background: item.color }"
                  ></span>
                </div>
                <div class="distribution-legend">
                  <span v-for="item in listOverview.distribution" :key="`${item.label}-legend`" class="legend-item">
                    <i :style="{ background: item.color }"></i>
                    {{ item.label }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
      </el-tabs>
    </div>
  </template>

<script lang="ts">
import { computed, defineComponent, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
// 假设您已经有 HttpManager 和 RankTable 组件
import { HttpManager } from '@/api'; 
import RankTable from '@/components/RankTable.vue'; // 这是一个我们稍后要创建的通用子组件
import { isSameRouteQuery, mergeRouteQuery, normalizeQueryValue, parsePositiveIntQuery } from '@/utils/route-query';

// 定义评分数据结构（根据您的后端返回调整）
interface RankItem {
  id: number;
  songId?: number; 
  songListName?: string; 
  consumerId: number;
  consumerName: string;
  score: number;
  createTime: string;
  name: string; // 歌曲名或歌单名
}

export default defineComponent({
  components: {
    RankTable,
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    const normalizeRankTab = (value: unknown) => {
      return normalizeQueryValue(value as any) === 'listRank' ? 'listRank' : 'songRank';
    };
    const activeTab = ref(normalizeRankTab(route.query.tab));
    const songQuery = reactive({
      currentPage: parsePositiveIntQuery(route.query.songPage, 1),
      pageSize: parsePositiveIntQuery(route.query.songPageSize, 10),
      name: normalizeQueryValue(route.query.songName),
      consumerName: normalizeQueryValue(route.query.songConsumer),
    });
    const listQuery = reactive({
      currentPage: parsePositiveIntQuery(route.query.listPage, 1),
      pageSize: parsePositiveIntQuery(route.query.listPageSize, 10),
      name: normalizeQueryValue(route.query.listName),
      consumerName: normalizeQueryValue(route.query.listConsumer),
    });
    const isSyncingRouteState = ref(false);
    
    // 歌曲评分数据
    const songRankData = ref<RankItem[]>([]);
    const songRankTotal = ref(0);
    const songLoading = ref(false);

    // 歌单评分数据
    const listRankData = ref<RankItem[]>([]);
    const listRankTotal = ref(0);
    const listLoading = ref(false);

    const createOverview = (data: RankItem[], total: number) => {
      const currentPageScores = data.map((item) => Number(item.score || 0) / 2).filter((item) => !Number.isNaN(item));
      const average = currentPageScores.length
        ? `${(currentPageScores.reduce((sum, item) => sum + item, 0) / currentPageScores.length).toFixed(1)}分`
        : "--";
      const topItem = data.reduce<RankItem | null>((best, item) => {
        if (!best || Number(item.score) > Number(best.score)) {
          return item;
        }
        return best;
      }, null);

      const buckets = [
        {
          label: "5 分",
          color: "#F5B43A",
          count: currentPageScores.filter((item) => item >= 5).length,
        },
        {
          label: "4-4.5 分",
          color: "#29D2E4",
          count: currentPageScores.filter((item) => item >= 4 && item < 5).length,
        },
        {
          label: "4 分以下",
          color: "#C9D5E5",
          count: currentPageScores.filter((item) => item < 4).length,
        },
      ];

      const base = currentPageScores.length || 1;
      const distribution = buckets.map((bucket) => ({
        ...bucket,
        percent: bucket.count ? `${Math.max((bucket.count / base) * 100, 14)}%` : "0%",
      }));

      return {
        total,
        average,
        topScore: topItem ? `${(Number(topItem.score) / 2).toFixed(1)}分` : "--",
        topName: topItem?.name || "当前页暂无数据",
        distribution,
      };
    };

    const songOverview = computed(() => createOverview(songRankData.value, songRankTotal.value));
    const listOverview = computed(() => createOverview(listRankData.value, listRankTotal.value));

    const getQueryStateByType = (type: 0 | 1) => (type === 0 ? songQuery : listQuery);

    const buildRouteQuery = () => {
      return {
        tab: activeTab.value !== 'songRank' ? activeTab.value : undefined,
        songPage: songQuery.currentPage > 1 ? songQuery.currentPage : undefined,
        songPageSize: songQuery.pageSize !== 10 ? songQuery.pageSize : undefined,
        songName: songQuery.name || undefined,
        songConsumer: songQuery.consumerName || undefined,
        listPage: listQuery.currentPage > 1 ? listQuery.currentPage : undefined,
        listPageSize: listQuery.pageSize !== 10 ? listQuery.pageSize : undefined,
        listName: listQuery.name || undefined,
        listConsumer: listQuery.consumerName || undefined,
      };
    };

    const syncRouteState = async () => {
      const nextQuery = mergeRouteQuery(route.query, buildRouteQuery());
      if (!isSameRouteQuery(route.query, nextQuery)) {
        isSyncingRouteState.value = true;
        await router.replace({ path: route.path, query: nextQuery });
        isSyncingRouteState.value = false;
      }
    };

    /**
     * @param type 0: 歌曲, 1: 歌单
     * @param params 搜索和分页参数
     */
    const fetchRankData = async (type: 0 | 1, params: {
      currentPage: number, 
      pageSize: number, 
      name: string, 
      consumerName: string 
    }) => {
      const isSong = type === 0;
      const loadingRef = isSong ? songLoading : listLoading;
      const dataRef = isSong ? songRankData : listRankData;
      const totalRef = isSong ? songRankTotal : listRankTotal;

      loadingRef.value = true;
      try {
        // 假设后端有一个统一的评分查询接口，通过 type 区分
        // 您可能需要根据您的实际后端接口进行调整
        const result = (await HttpManager.getAllRankByType({
          type: type, // 0 for song, 1 for list
          ...params
        })) as any; 

        if (result.code === 200) {
          dataRef.value = result.data.records.map((item: any) => ({
            ...item,
            // 确保歌曲名/歌单名有一个统一的字段 'name'
            
            name: isSong ? item.songName : item.songListName, 
            consumerName: item.consumerName || '未知用户' ,// 确保有用户名字段
            score: parseFloat(item.score) || 0
          })) as RankItem[];



          totalRef.value = result.data.total;
        } else {
          ElMessage.error(`获取${isSong ? '歌曲' : '歌单'}评分失败：${result.message}`);
          dataRef.value = [];
          totalRef.value = 0;
        }

      } catch (error) {
        ElMessage.error(`网络请求失败，请检查接口！`);
      } finally {
        loadingRef.value = false;
      }
    };

    const handleRankQueryChange = async (
      type: 0 | 1,
      params: {
        currentPage: number;
        pageSize: number;
        name: string;
        consumerName: string;
      }
    ) => {
      Object.assign(getQueryStateByType(type), params);
      await syncRouteState();
      fetchRankData(type, { ...params });
    };
    
    // Tab 切换时触发数据加载
    const handleTabChange = async (name: string) => {
      activeTab.value = name;
      await syncRouteState();
      const type = name === 'songRank' ? 0 : 1;
      fetchRankData(type, { ...getQueryStateByType(type) });
    };

    // 删除/取消评分操作
    const handleDeleteRank = async (id: number, type: 0 | 1) => {
      const isSong = type === 0;
      const rankType = isSong ? '歌曲' : '歌单';

      try {
        await ElMessageBox.confirm(`确定要删除这条 ID 为 ${id} 的${rankType}评分吗？`, '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
        });
        
        // 假设有一个删除评分的接口
        const result = (await HttpManager.deleteRank(type,id)) as any;

        if (result.code === 200) {
          ElMessage.success(`${rankType}评分删除成功！`);
          // 重新加载当前 Tab 的数据
          const currentTabType = activeTab.value === 'songRank' ? 0 : 1;
          fetchRankData(currentTabType, { ...getQueryStateByType(currentTabType) });
        } else {
          ElMessage.error(`删除失败: ${result.message}`);
        }
      } catch (e) {
        if (e !== 'cancel') {
          ElMessage.info('已取消删除操作');
        }
      }
    };

    watch(
      () => [
        route.query.tab,
        route.query.songPage,
        route.query.songPageSize,
        route.query.songName,
        route.query.songConsumer,
        route.query.listPage,
        route.query.listPageSize,
        route.query.listName,
        route.query.listConsumer,
      ],
      () => {
        if (isSyncingRouteState.value) {
          return;
        }

        activeTab.value = normalizeRankTab(route.query.tab);
        songQuery.currentPage = parsePositiveIntQuery(route.query.songPage, 1);
        songQuery.pageSize = parsePositiveIntQuery(route.query.songPageSize, 10);
        songQuery.name = normalizeQueryValue(route.query.songName);
        songQuery.consumerName = normalizeQueryValue(route.query.songConsumer);
        listQuery.currentPage = parsePositiveIntQuery(route.query.listPage, 1);
        listQuery.pageSize = parsePositiveIntQuery(route.query.listPageSize, 10);
        listQuery.name = normalizeQueryValue(route.query.listName);
        listQuery.consumerName = normalizeQueryValue(route.query.listConsumer);

        const currentTabType = activeTab.value === 'songRank' ? 0 : 1;
        fetchRankData(currentTabType, { ...getQueryStateByType(currentTabType) });
      },
      { immediate: true }
    );

    return {
      activeTab,
      songQuery,
      listQuery,
      songRankData,
      songRankTotal,
      songLoading,
      listRankData,
      listRankTotal,
      listLoading,
      songOverview,
      listOverview,
      handleTabChange,
      handleRankQueryChange,
      handleDeleteRank,
    };
  },
});
</script>


<style scoped>
.rank-management-container {
  padding: 28px;
}
.page-title {
  font-size: 28px;
  margin-bottom: 20px;
  color: var(--admin-text);
  font-weight: 800;
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
  font-size: 34px;
  font-weight: 800;
  line-height: 1.1;
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

/* 调整 Tab 样式，使其更符合后台管理界面风格 */
:deep(.el-tabs--border-card) {
  box-shadow: 0 18px 40px rgba(17, 37, 70, 0.08);
  border-radius: 28px;
}

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
