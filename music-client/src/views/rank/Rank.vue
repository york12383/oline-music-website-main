<template>
  <div class="music-page-shell rank-page">
    <section class="rank-hero music-panel">
      <div class="rank-hero-copy">
        <span class="music-chip"><span class="music-dot"></span> Heat Ranking</span>
        <h1 class="hero-title">排行榜</h1>
        <p class="hero-copy">
          把此刻最被偏爱的声音轻轻排成一列，
          让正在升温的旋律、歌手和歌单，都在这里慢慢浮出水面。
        </p>
      </div>

      <div class="rank-toolbar">
        <div class="rank-switch-group">
          <button
            v-for="item in boardOptions"
            :key="item.value"
            type="button"
            class="rank-switch"
            :class="{ active: currentBoard === item.value }"
            @click="handleBoardChange(item.value)"
          >
            <strong>{{ item.label }}</strong>
            <span>{{ item.description }}</span>
          </button>
        </div>

        <div class="rank-limit-group">
          <button
            v-for="limit in topOptions"
            :key="limit"
            type="button"
            class="limit-chip"
            :class="{ active: currentLimit === limit }"
            @click="handleLimitChange(limit)"
          >
            TOP {{ limit }}
          </button>
        </div>
      </div>

      <div class="rank-summary-panel music-panel-soft" v-if="champion">
        <div class="summary-cover-wrap" @click="openRankItem(champion)">
          <img :src="champion.displayCover" :alt="champion.displayTitle" class="summary-cover" />
          <span class="summary-badge">NO.1</span>
        </div>

        <div class="summary-copy">
          <p class="summary-kicker">{{ currentBoardLabel }}</p>
          <h2 class="summary-title">{{ champion.displayTitle }}</h2>
          <p class="summary-subtitle">{{ champion.displaySubtitle }}</p>
          <p class="summary-description">{{ champion.displayDescription }}</p>

          <div class="summary-meta">
            <span>{{ currentBoardMeta }}</span>
            <span>当前展示 {{ rankingItems.length }} 条</span>
            <span>榜单范围 TOP {{ currentLimit }}</span>
          </div>

          <div class="summary-actions">
            <button type="button" class="summary-action primary" @click="openRankItem(champion)">
              {{ primaryActionLabel }}
            </button>
            <button
              v-if="champion.rankType === 'song'"
              type="button"
              class="summary-action secondary"
              @click="goSongLyric(champion)"
            >
              歌词页
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="podium-panel music-panel" v-if="podiumItems.length">
      <div class="music-section-heading">
        <div>
          <h2 class="music-section-title">榜单聚焦</h2>
          <p class="music-section-subtitle">先看前三名，快速抓住这期热度中心。</p>
        </div>
      </div>

      <div class="podium-grid">
        <article
          v-for="item in podiumItems"
          :key="`podium-${item.id}`"
          class="podium-card music-panel-soft"
          :class="`podium-rank-${item.displayRank}`"
          @click="openRankItem(item)"
        >
          <span class="podium-rank-tag">TOP {{ item.displayRank }}</span>
          <img :src="item.displayCover" :alt="item.displayTitle" class="podium-cover" />
          <div class="podium-copy">
            <h3>{{ item.displayTitle }}</h3>
            <p class="podium-subtitle">{{ item.displaySubtitle }}</p>
            <p class="podium-description">{{ item.displayDescription }}</p>
          </div>
        </article>
      </div>
    </section>

    <section class="rank-list-panel music-panel">
      <div class="music-section-heading">
        <div>
          <h2 class="music-section-title">{{ currentBoardLabel }}</h2>
          <p class="music-section-subtitle">{{ currentBoardListDescription }}</p>
        </div>
      </div>

      <div v-loading="loading" class="rank-list-wrap">
        <button
          v-for="item in rankingItems"
          :key="item.id"
          type="button"
          class="rank-row music-panel-soft"
          :class="{ active: isCurrentSong(item) }"
          @click="openRankItem(item)"
        >
          <span class="rank-order">{{ item.displayRank }}</span>
          <img :src="item.displayCover" :alt="item.displayTitle" class="rank-cover" />

          <span class="rank-main">
            <strong>{{ item.displayTitle }}</strong>
            <span>{{ item.displaySubtitle }}</span>
          </span>

          <span class="rank-meta">{{ item.displayDescription }}</span>

          <span class="rank-action">
            <span v-if="item.rankType === 'song' && isCurrentSong(item)" class="playing-indicator">
              <i></i>
              <i></i>
              <i></i>
            </span>
            {{ item.rankType === "song" ? (isCurrentSong(item) ? "正在播放" : "立即播放") : "查看详情" }}
          </span>
        </button>

        <el-empty
          v-if="!loading && !rankingItems.length"
          description="当前榜单还没有可展示的内容"
          :image-size="96"
          class="rank-empty"
        />
      </div>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useStore } from "vuex";

import { HttpManager } from "@/api";
import mixin from "@/mixins/mixin";
import { NavName } from "@/enums";
import { getSongListCoverUrl } from "@/utils/song-list-cover";

type RankBoard = "song" | "singer" | "songList";
type RankItem = Record<string, any> & {
  rankType: RankBoard;
  displayRank: number;
  displayTitle: string;
  displaySubtitle: string;
  displayDescription: string;
  displayCover: string;
};

const boardOptions: Array<{ label: string; value: RankBoard; description: string }> = [
  { label: "歌曲榜", value: "song", description: "平台热度最高的单曲" },
  { label: "歌手榜", value: "singer", description: "这段时间最受欢迎的歌手" },
  { label: "歌单榜", value: "songList", description: "收藏和互动最强的歌单" },
];
const topOptions = [10, 25, 50, 100];

const route = useRoute();
const router = useRouter();
const store = useStore();
const { changeIndex, getSongTitle, getSingerName, playMusic } = mixin();

const loading = ref(false);
const rankingItems = ref<RankItem[]>([]);
const rankingRequestToken = ref(0);

function normalizeRouteValue(value?: string | string[]) {
  return Array.isArray(value) ? value[0] : value;
}

function normalizeBoard(value?: string | string[]): RankBoard {
  const normalized = normalizeRouteValue(value);
  return boardOptions.some((item) => item.value === normalized) ? (normalized as RankBoard) : "song";
}

function normalizeTop(value?: string | string[]) {
  const parsed = Number(normalizeRouteValue(value));
  return topOptions.includes(parsed) ? parsed : 10;
}

const currentBoard = computed<RankBoard>(() => normalizeBoard(route.query.type));
const currentLimit = computed(() => normalizeTop(route.query.top));
const currentSongId = computed(() => store.getters.songId);

const currentBoardLabel = computed(() => {
  return boardOptions.find((item) => item.value === currentBoard.value)?.label || "排行榜";
});

const currentBoardMeta = computed(() => {
  switch (currentBoard.value) {
    case "song":
      return "综合播放、收藏、评论与评分";
    case "singer":
      return "按歌手名下歌曲热度汇总";
    case "songList":
      return "按歌单整体互动热度排序";
    default:
      return "平台热度榜";
  }
});

const currentBoardListDescription = computed(() => {
  switch (currentBoard.value) {
    case "song":
      return "点任意歌曲即可直接播放，榜单会随着平台热度变化实时刷新。";
    case "singer":
      return "从这里直达歌手详情页，继续看作品与介绍。";
    case "songList":
      return "打开热门歌单，顺着大家正在收藏和讨论的内容继续听下去。";
    default:
      return "平台热度榜";
  }
});

const primaryActionLabel = computed(() => {
  switch (currentBoard.value) {
    case "song":
      return "立即播放";
    case "singer":
      return "查看歌手";
    case "songList":
      return "打开歌单";
    default:
      return "查看详情";
  }
});

const champion = computed(() => rankingItems.value[0] || null);
const podiumItems = computed(() => rankingItems.value.slice(0, 3));

async function syncRankQuery(board = currentBoard.value, top = currentLimit.value) {
  const query = { ...route.query } as Record<string, string>;

  if (board === "song") {
    delete query.type;
  } else {
    query.type = board;
  }

  if (top === 10) {
    delete query.top;
  } else {
    query.top = String(top);
  }

  await router.replace({
    path: route.path,
    query,
  });
}

async function handleBoardChange(board: RankBoard) {
  if (board === currentBoard.value) {
    return;
  }
  await syncRankQuery(board, currentLimit.value);
}

async function handleLimitChange(limit: number) {
  if (limit === currentLimit.value) {
    return;
  }
  await syncRankQuery(currentBoard.value, limit);
}

function getSongDisplayTitle(item: Record<string, any>) {
  const name = String(item.name || "").trim();
  if (!name) {
    return "未命名歌曲";
  }
  return name.includes("-") ? getSongTitle(name) : name;
}

function getSongDisplaySubtitle(item: Record<string, any>) {
  const name = String(item.name || "").trim();
  if (name.includes("-")) {
    return getSingerName(name);
  }
  return String(item.singerName || "").trim() || "未知歌手";
}

function buildDescription(rankType: RankBoard, item: Record<string, any>) {
  const text = String(item.introduction || "").trim();
  if (text) {
    return text;
  }

  if (rankType === "song") {
    return "当前热度正在上升，适合直接加入播放队列。";
  }

  if (rankType === "singer") {
    return "主页里还能继续看他的代表作和更多详情。";
  }

  return "这张歌单正在被更多人收藏、评论和评分。";
}

function buildCover(rankType: RankBoard, item: Record<string, any>) {
  if (rankType === "songList") {
    return getSongListCoverUrl(item);
  }
  return HttpManager.attachImageUrl(item.pic);
}

function isCurrentSong(item: Record<string, any>) {
  return item.rankType === "song" && String(item.id) === String(currentSongId.value);
}

function playRankSong(item: Record<string, any>) {
  playMusic({
    id: item.id,
    url: item.url,
    pic: item.pic,
    index: item.displayRank ? item.displayRank - 1 : 0,
    name: item.name,
    lyric: item.lyric,
    currentSongList: rankingItems.value,
  });
}

function goSongLyric(item: Record<string, any>) {
  if (item.rankType !== "song") {
    return;
  }
  router.push({
    name: "lyric",
    params: { id: item.id },
  });
}

function openRankItem(item: Record<string, any>) {
  if (item.rankType === "song") {
    playRankSong(item);
    return;
  }

  if (item.rankType === "singer") {
    router.push({
      name: "singer-detail",
      params: { id: item.id },
    });
    return;
  }

  router.push({
    name: "song-sheet-detail",
    params: { id: item.id },
  });
}

function normalizeRankItems(rankType: RankBoard, source: Record<string, any>[]) {
  return source.map((item, index) => {
    const displayTitle =
      rankType === "song"
        ? getSongDisplayTitle(item)
        : String(item.title || item.name || "").trim() || "未命名内容";
    const displaySubtitle =
      rankType === "song"
        ? getSongDisplaySubtitle(item)
        : rankType === "singer"
          ? String(item.location || item.area || "").trim() || "入驻歌手"
          : String(item.style || "").trim() || "精选歌单";

    return {
      ...item,
      rankType,
      displayRank: index + 1,
      displayTitle,
      displaySubtitle,
      displayDescription: buildDescription(rankType, item),
      displayCover: buildCover(rankType, item),
    };
  });
}

async function loadRankings(rankType = currentBoard.value, limit = currentLimit.value) {
  const requestToken = ++rankingRequestToken.value;
  loading.value = true;
  rankingItems.value = [];
  try {
    let result: ResponseBody;
    if (rankType === "song") {
      result = (await HttpManager.getHotSongs(limit)) as ResponseBody;
    } else if (rankType === "singer") {
      result = (await HttpManager.getHotSingers(limit)) as ResponseBody;
    } else {
      result = (await HttpManager.getHotSongLists(limit)) as ResponseBody;
    }

    if (requestToken !== rankingRequestToken.value) {
      return;
    }

    const source = Array.isArray(result?.data) ? result.data : [];
    rankingItems.value = normalizeRankItems(rankType, source);
  } finally {
    if (requestToken === rankingRequestToken.value) {
      loading.value = false;
    }
  }
}

onMounted(() => {
  changeIndex(NavName.Rank);
});

watch(
  () => [currentBoard.value, currentLimit.value],
  () => {
    changeIndex(NavName.Rank);
    void loadRankings(currentBoard.value, currentLimit.value);
  },
  { immediate: true }
);
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

.rank-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.rank-hero,
.podium-panel,
.rank-list-panel {
  padding: clamp(22px, 3vw, 34px);
}

.rank-hero {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-title {
  margin: 14px 0 12px;
  color: var(--app-title);
  font-size: clamp(38px, 5vw, 64px);
  line-height: 0.95;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.hero-copy {
  max-width: 760px;
  margin: 0;
  color: var(--app-text-muted);
  line-height: 1.8;
}

.rank-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  justify-content: space-between;
  gap: 16px;
}

.rank-switch-group {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  flex: 1 1 780px;
}

.rank-switch {
  position: relative;
  min-height: 108px;
  padding: 20px 22px;
  border: 1px solid var(--app-panel-border);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--app-text);
  text-align: left;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  transition:
    transform 0.24s ease,
    border-color 0.24s ease,
    box-shadow 0.24s ease,
    background 0.24s ease;
  cursor: pointer;

  strong {
    display: block;
    position: relative;
    margin-bottom: 8px;
    padding-right: 20px;
    font-size: 18px;
    color: var(--app-title);

    &::after {
      content: "";
      position: absolute;
      top: 50%;
      right: 0;
      width: 8px;
      height: 8px;
      border-radius: 999px;
      background: rgba(148, 163, 184, 0.58);
      transform: translateY(-50%);
      transition: background 0.24s ease, transform 0.24s ease;
    }
  }

  span {
    display: block;
    color: var(--app-text-muted);
    line-height: 1.6;
  }

  &:hover,
  &.active {
    transform: translateY(-2px);
    border-color: var(--app-panel-border-strong);
    box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
  }

  &:hover strong::after,
  &.active strong::after {
    background: #66c8ff;
    transform: translateY(-50%) scale(1.08);
  }

  &.active {
    background: linear-gradient(180deg, rgba(237, 249, 255, 0.92) 0%, rgba(244, 250, 255, 0.86) 100%);

    span {
      color: var(--app-text);
    }
  }

  &:focus-visible {
    outline: none;
    border-color: rgba(102, 200, 255, 0.52);
    box-shadow:
      0 0 0 4px rgba(102, 200, 255, 0.14),
      0 14px 28px rgba(15, 23, 42, 0.08);
  }
}

.rank-limit-group {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 5px;
  border: 1px solid var(--app-panel-border);
  border-radius: 999px;
  background: var(--app-panel-bg-soft);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.limit-chip {
  min-width: 96px;
  padding: 12px 18px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--app-text-muted);
  font-weight: 800;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition:
    transform 0.22s ease,
    background 0.22s ease,
    color 0.22s ease,
    box-shadow 0.22s ease;

  &:hover,
  &.active {
    transform: translateY(-1px);
  }

  &:hover {
    color: var(--app-title);
    background: rgba(255, 255, 255, 0.82);
  }

  &.active {
    color: var(--app-button-primary-text);
    background: var(--app-button-primary-bg);
    box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
  }

  &:focus-visible {
    outline: none;
    box-shadow:
      0 0 0 4px rgba(102, 200, 255, 0.14),
      0 10px 22px rgba(15, 23, 42, 0.08);
  }
}

.rank-summary-panel {
  display: grid;
  grid-template-columns: minmax(220px, 300px) minmax(0, 1fr);
  gap: 24px;
  padding: 22px;
}

.summary-cover-wrap {
  position: relative;
  border-radius: 26px;
  overflow: hidden;
  cursor: pointer;
  min-height: 240px;
}

.summary-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.summary-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 82px;
  height: 34px;
  padding: 0 16px;
  border-radius: 999px;
  background: rgba(13, 23, 42, 0.78);
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.summary-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.summary-kicker {
  margin: 0 0 10px;
  color: var(--app-accent-strong);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.summary-title {
  margin: 0;
  color: var(--app-title);
  font-size: clamp(28px, 4vw, 44px);
  line-height: 1.08;
  font-weight: 900;
}

.summary-subtitle {
  margin: 12px 0 0;
  color: var(--app-text);
  font-size: 18px;
  font-weight: 700;
}

.summary-description {
  margin: 16px 0 0;
  color: var(--app-text-muted);
  line-height: 1.8;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;

  span {
    display: inline-flex;
    align-items: center;
    min-height: 34px;
    padding: 0 14px;
    border-radius: 999px;
    background: var(--app-chip-bg-muted);
    color: var(--app-text-soft);
    font-size: 12px;
    font-weight: 700;
  }
}

.summary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.summary-action {
  min-width: 120px;
  height: 46px;
  padding: 0 22px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.24s ease, filter 0.24s ease;

  &:hover {
    transform: translateY(-2px);
    filter: brightness(1.04);
  }

  &.primary {
    background: var(--app-button-primary-bg);
    color: var(--app-button-primary-text);
  }

  &.secondary {
    background: var(--app-button-secondary-bg);
    color: var(--app-button-secondary-text);
    border-color: var(--app-panel-border);
  }
}

.podium-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.podium-card {
  position: relative;
  padding: 16px;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;

  &:hover {
    transform: translateY(-6px);
    border-color: var(--app-panel-border-strong);
    box-shadow: 0 24px 46px rgba(15, 23, 42, 0.14);
  }
}

.podium-rank-tag {
  position: absolute;
  top: 16px;
  right: 16px;
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--app-title);
  font-size: 12px;
  font-weight: 800;
}

.podium-cover {
  width: 100%;
  aspect-ratio: 1 / 0.76;
  max-height: 340px;
  border-radius: 18px;
  object-fit: cover;
  display: block;
}

.podium-copy {
  padding-top: 14px;

  h3 {
    margin: 0;
    color: var(--app-title);
    font-size: 20px;
    line-height: 1.15;
    font-weight: 900;
  }
}

.podium-subtitle {
  margin: 8px 0 0;
  color: var(--app-text);
  font-weight: 700;
  font-size: 16px;
}

.podium-description {
  margin: 10px 0 0;
  color: var(--app-text-muted);
  line-height: 1.65;
  font-size: 14px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.rank-list-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 240px;
}

.rank-row {
  display: grid;
  grid-template-columns: 58px 86px minmax(0, 1.2fr) minmax(220px, 0.9fr) auto;
  align-items: center;
  gap: 18px;
  width: 100%;
  padding: 14px 16px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition: transform 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease;

  &:hover,
  &.active {
    transform: translateY(-2px);
    border-color: var(--app-panel-border-strong);
    box-shadow: 0 18px 32px rgba(15, 23, 42, 0.1);
  }
}

.rank-order {
  color: var(--app-title);
  font-size: 26px;
  font-weight: 900;
  text-align: center;
}

.rank-cover {
  width: 86px;
  height: 86px;
  border-radius: 20px;
  object-fit: cover;
  display: block;
}

.rank-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;

  strong {
    color: var(--app-title);
    font-size: 19px;
    line-height: 1.25;
    font-weight: 900;
  }

  span {
    color: var(--app-text-muted);
    font-size: 14px;
    line-height: 1.6;
  }
}

.rank-meta {
  color: var(--app-text-muted);
  line-height: 1.7;
}

.rank-action {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  color: var(--app-title);
  font-weight: 800;
  white-space: nowrap;
}

.playing-indicator {
  display: inline-flex;
  align-items: flex-end;
  gap: 3px;
  height: 18px;

  i {
    width: 4px;
    border-radius: 999px;
    background: linear-gradient(180deg, var(--app-accent-strong) 0%, var(--app-accent) 100%);
    animation: rank-wave 1.1s ease-in-out infinite;
  }

  i:nth-child(1) {
    height: 12px;
  }

  i:nth-child(2) {
    height: 18px;
    animation-delay: 0.12s;
  }

  i:nth-child(3) {
    height: 9px;
    animation-delay: 0.24s;
  }
}

.rank-empty {
  padding-top: 32px;
}

@keyframes rank-wave {
  0%, 100% {
    transform: scaleY(0.75);
    opacity: 0.72;
  }
  50% {
    transform: scaleY(1);
    opacity: 1;
  }
}

@media (max-width: 1080px) {
  .rank-switch-group,
  .podium-grid,
  .rank-summary-panel {
    grid-template-columns: 1fr;
  }

  .rank-row {
    grid-template-columns: 48px 72px minmax(0, 1fr);
  }

  .rank-limit-group {
    width: 100%;
    justify-content: space-between;
  }

  .limit-chip {
    flex: 1 1 0;
    min-width: 0;
  }

  .rank-meta,
  .rank-action {
    grid-column: 3;
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .rank-switch {
    min-height: 96px;
    padding: 18px;
    border-radius: 20px;

    strong {
      margin-bottom: 8px;
      font-size: 17px;
      padding-right: 18px;
    }

    span {
      font-size: 14px;
    }
  }

  .rank-limit-group {
    gap: 6px;
    padding: 5px;
  }

  .limit-chip {
    padding: 12px 10px;
    font-size: 14px;
  }

  .rank-row {
    gap: 14px;
    padding: 14px;
  }

  .rank-cover {
    width: 72px;
    height: 72px;
    border-radius: 18px;
  }

  .summary-title {
    font-size: 32px;
  }

  .hero-title {
    font-size: 46px;
  }
}
</style>
