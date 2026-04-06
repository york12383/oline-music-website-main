<template>
  <div class="music-page-shell singer-page">
    <section class="hero-card music-panel">
      <span class="music-chip"><span class="music-dot"></span> Artist Focus</span>
      <h1 class="hero-title">歌手聚焦</h1>
      <p class="hero-copy">
        在不同的声音里遇见不同的心动，让你喜欢的那个人，
        刚好唱进此刻的情绪里。
      </p>
    </section>

    <section class="content-card music-panel">
      <yin-nav :styleList="singerStyle" :activeName="activeName" @click="handleChangeView"></yin-nav>
      <div class="list-header">
        <h2 class="list-title">{{ activeName }}</h2>
        <span class="list-total">共 {{ allPlayList.length }} 位歌手</span>
      </div>
      <play-list :playList="data" path="singer-detail" card-type="artist"></play-list>
      <el-pagination
        class="pagination"
        background
        layout="total, prev, pager, next"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="allPlayList.length"
        @current-change="handleCurrentChange"
      >
      </el-pagination>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { computed, getCurrentInstance, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import YinNav from "@/components/layouts/YinNav.vue";
import PlayList from "@/components/PlayList.vue";
import { singerStyle } from "@/enums";
import { HttpManager } from "@/api";

const VIEW_ALL_LIMIT = 200;
const { proxy } = getCurrentInstance() as any;
const route = useRoute();
const router = useRouter();

const activeName = ref("全部歌手");
const pageSize = ref(15);
const currentPage = ref(1);
const allPlayList = ref([]);
function normalizeQueryValue(value?: string | string[]) {
  return Array.isArray(value) ? value[0] : value;
}

function parsePositivePage(value?: string | string[]) {
  const page = Number(normalizeQueryValue(value));
  return Number.isInteger(page) && page > 0 ? page : 1;
}

const routeSource = computed(() => normalizeQueryValue(route.query.source));
const routePage = computed(() => parsePositivePage(route.query.page));
const totalCount = computed(() => allPlayList.value.length);

const data = computed(() => {
  return allPlayList.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value);
});

function getMaxPage() {
  return Math.max(1, Math.ceil(totalCount.value / pageSize.value));
}

function showWarning(message: string) {
  proxy?.$message?.({
    message,
    type: "warning",
  });
}

async function getAllSinger() {
  const result = (await HttpManager.getAllSinger()) as ResponseBody;
  allPlayList.value = result.data;
}

async function getHotSingers() {
  const result = (await HttpManager.getHotSingers(VIEW_ALL_LIMIT)) as ResponseBody;
  allPlayList.value = Array.isArray(result?.data) ? result.data : [];
}

async function getRecommendSingers() {
  const result = (await HttpManager.getRecommendSingers(VIEW_ALL_LIMIT)) as ResponseBody;

  if (result?.success === false) {
    allPlayList.value = [];
    showWarning(result?.message || "登录后可查看推荐歌手");
    return;
  }

  allPlayList.value = Array.isArray(result?.data) ? result.data : [];
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

async function loadSingers() {
  if (routeSource.value === "hot") {
    activeName.value = "全部歌手";
    await getHotSingers();
    await applyBoundedRoutePage();
    return;
  }

  if (routeSource.value === "recommend") {
    activeName.value = "全部歌手";
    await getRecommendSingers();
    await applyBoundedRoutePage();
    return;
  }

  if (activeName.value === "全部歌手") {
    await getAllSinger();
    await applyBoundedRoutePage();
    return;
  }

  await getSingerSex(activeName.value === "男歌手" ? 1 : activeName.value === "女歌手" ? 0 : 2);
  await applyBoundedRoutePage();
}

watch(
  [() => route.query.source, () => route.query.page],
  () => {
    void loadSingers();
  },
  { immediate: true }
);

async function handleCurrentChange(val) {
  currentPage.value = val;
  await syncPageQuery(val, routeSource.value);
}

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

  await loadSingers();
}

async function getSingerSex(sex) {
  const result = (await HttpManager.getSingerOfSex(sex)) as ResponseBody;
  allPlayList.value = result.data;
}
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

.singer-page {
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
