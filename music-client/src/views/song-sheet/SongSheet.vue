<template>
  <div class="music-page-shell song-sheet-page">
    <section class="hero-card music-panel">
      <span class="music-chip"><span class="music-dot"></span> Playlist Library</span>
      <h1 class="hero-title">歌单广场</h1>
      <p class="hero-copy">
        把此刻的心情轻轻放进一张歌单里，让熟悉的旋律与新的心动，都在这里慢慢相逢。
      </p>
    </section>

    <section class="content-card music-panel">
      <yin-nav :styleList="songStyle" :activeName="activeName" @click="handleChangeView"></yin-nav>
      <div class="list-header">
        <h2 class="list-title">{{ activeName }}</h2>
        <span class="list-total">Total {{ totalCount }} · 第 {{ currentPage }} 页</span>
      </div>
      <play-list :playList="displayPlayList" path="song-sheet-detail"></play-list>
      <el-pagination
        class="pagination"
        background
        layout="total, prev, pager, next"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="totalCount"
        @current-change="handleCurrentChange"
      >
      </el-pagination>
    </section>
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, getCurrentInstance, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import YinNav from "@/components/layouts/YinNav.vue";
import PlayList from "@/components/PlayList.vue";
import { SONGSTYLE } from "@/enums";
import { HttpManager } from "@/api";

const VIEW_ALL_LIMIT = 200;

export default defineComponent({
  components: {
    YinNav,
    PlayList,
  },
  setup() {
    const { proxy } = getCurrentInstance();
    const route = useRoute();
    const router = useRouter();

    const activeName = ref("全部歌单");
    const pageSize = ref(15);
    const currentPage = ref(1);
    const songStyle = ref(SONGSTYLE);
    const allPlayList = ref([]);
    const allPlayListNum = ref(0);

    function normalizeQueryValue(value?: string | string[]) {
      return Array.isArray(value) ? value[0] : value;
    }

    function parsePositivePage(value?: string | string[]) {
      const page = Number(normalizeQueryValue(value));
      return Number.isInteger(page) && page > 0 ? page : 1;
    }

    const routeSource = computed(() => normalizeQueryValue(route.query.source));
    const routePage = computed(() => parsePositivePage(route.query.page));
    const useClientPagination = computed(() => Boolean(routeSource.value) || activeName.value !== "全部歌单");
    const displayPlayList = computed(() => {
      if (!useClientPagination.value) {
        return allPlayList.value;
      }
      const start = (currentPage.value - 1) * pageSize.value;
      return allPlayList.value.slice(start, start + pageSize.value);
    });
    const totalCount = computed(() => (useClientPagination.value ? allPlayList.value.length : allPlayListNum.value));

    function getMaxPage() {
      return Math.max(1, Math.ceil(totalCount.value / pageSize.value));
    }

    function showWarning(message: string) {
      (proxy as any)?.$message?.({
        message,
        type: "warning",
      });
    }

    async function syncPageQuery(page: number, source?: string) {
      const query = { ...route.query, page: String(page) } as Record<string, string>;
      if (source) {
        query.source = source;
      } else {
        delete query.source;
      }

      await router.replace({
        path: route.path,
        query,
      });
    }

    async function applyBoundedRoutePage() {
      const boundedPage = Math.min(routePage.value, getMaxPage());
      currentPage.value = boundedPage;

      if (route.query.page && boundedPage !== routePage.value) {
        await syncPageQuery(boundedPage, routeSource.value);
      }
    }

    async function getSongListByPage(page: number) {
      const result = ((await HttpManager.getsongListByPage(page)) as ResponseBody).data;
      allPlayList.value = result.records;
      allPlayListNum.value = result.total;
    }

    async function getSongListOfStyle(style: string) {
      allPlayList.value = ((await HttpManager.getSongListOfStyle(style)) as ResponseBody).data;
      allPlayListNum.value = allPlayList.value.length;
    }

    async function getHotSongLists() {
      const result = (await HttpManager.getHotSongLists(VIEW_ALL_LIMIT)) as ResponseBody;
      allPlayList.value = Array.isArray(result?.data) ? result.data : [];
      allPlayListNum.value = allPlayList.value.length;
    }

    async function getRecommendSongLists() {
      const result = (await HttpManager.getRecommendSongLists(VIEW_ALL_LIMIT)) as ResponseBody;
      if (result?.success === false) {
        allPlayList.value = [];
        allPlayListNum.value = 0;
        showWarning(result?.message || "登录后可查看推荐歌单");
        return;
      }
      allPlayList.value = Array.isArray(result?.data) ? result.data : [];
      allPlayListNum.value = allPlayList.value.length;
    }

    async function loadSongLists() {
      currentPage.value = routePage.value;

      if (routeSource.value === "hot") {
        activeName.value = "全部歌单";
        await getHotSongLists();
        await applyBoundedRoutePage();
        return;
      }

      if (routeSource.value === "recommend") {
        activeName.value = "全部歌单";
        await getRecommendSongLists();
        await applyBoundedRoutePage();
        return;
      }

      if (activeName.value === "全部歌单") {
        await getSongListByPage(currentPage.value);
      } else {
        await getSongListOfStyle(activeName.value);
      }

      await applyBoundedRoutePage();
    }

    watch(
      [() => route.query.source, () => route.query.page],
      () => {
        void loadSongLists();
      },
      { immediate: true }
    );

    async function resetListQuery() {
      if (!route.query.source && !route.query.page) {
        return false;
      }

      const query = { ...route.query };
      delete query.source;
      delete query.page;
      await router.replace({
        path: route.path,
        query,
      });
      return true;
    }

    async function handleChangeView(item) {
      activeName.value = item.name;
      currentPage.value = 1;

      const queryReset = await resetListQuery();
      if (queryReset) {
        return;
      }

      await loadSongLists();
    }

    async function handleCurrentChange(val) {
      currentPage.value = val;

      if (useClientPagination.value) {
        await syncPageQuery(val, routeSource.value);
        return;
      }

      await syncPageQuery(val);
      await getSongListByPage(val);
    }

    return {
      activeName,
      songStyle,
      pageSize,
      currentPage,
      displayPlayList,
      totalCount,
      handleChangeView,
      handleCurrentChange,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

.song-sheet-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.hero-card,
.content-card {
  padding: clamp(20px, 3vw, 32px);
}

.hero-title {
  margin: 16px 0 12px;
  color: var(--app-title);
  font-size: clamp(34px, 4vw, 58px);
  line-height: 1;
  font-weight: 900;
}

.hero-copy {
  max-width: 720px;
  margin: 0;
  color: var(--app-text-muted);
  line-height: 1.8;
}

.content-card {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.list-title {
  margin: 0;
  color: var(--app-title);
  font-size: clamp(24px, 2.8vw, 38px);
  font-weight: 900;
}

.list-total {
  color: var(--app-text-muted);
  font-size: 0.98rem;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

@media (max-width: 768px) {
  .list-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
