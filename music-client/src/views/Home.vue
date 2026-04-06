<template>
  <div class="music-page-shell home-page">
    <section class="hero-grid">
      <div class="hero-main music-panel">
        <el-carousel
          v-if="swiperList.length"
          class="hero-carousel"
          height="100%"
          :interval="5000"
          arrow="always"
        >
          <el-carousel-item v-for="item in swiperList" :key="item.id || item.pic">
            <div class="hero-slide">
              <img :src="HttpManager.attachImageUrl(item.pic)" class="hero-image" />
              <div class="hero-image-overlay"></div>
            </div>
          </el-carousel-item>
        </el-carousel>
        <div class="hero-copy">
          <span class="music-chip"><span class="music-dot"></span> For You Mix</span>
          <h1 class="hero-title">猜你喜欢</h1>
          <p class="hero-description">
            晚风替你按下播放键，心动都藏进了这一页轻轻亮起的旋律里。
            愿你点开的这一首，刚好落在想念最柔软的地方。
          </p>
          <div class="hero-actions">
            <button type="button" class="hero-btn hero-btn-primary" @click="playHeroSong">立即播放</button>
            <button type="button" class="hero-btn hero-btn-secondary" @click="openHeroSongs">全部歌曲</button>
          </div>
          <div class="hero-stats">
            <div class="hero-stat">
              <strong>{{ heroSongLibraryMetric }}</strong>
              <span>全站歌曲数</span>
            </div>
            <div class="hero-stat">
              <strong>{{ heroSingerLibraryMetric }}</strong>
              <span>入驻歌手数</span>
            </div>
            <div class="hero-stat">
              <strong>{{ heroPoolCountMetric }}</strong>
              <span>{{ heroPoolCountLabel }}</span>
            </div>
          </div>
        </div>
      </div>

      <aside
        class="hero-side music-panel-soft"
        v-if="spotlightSong"
        role="button"
        tabindex="0"
        @click="playSpotlightSong"
        @keydown.enter.prevent="playSpotlightSong"
        @keydown.space.prevent="playSpotlightSong"
      >
        <div class="hero-side-cover-wrap">
          <el-image class="hero-side-cover" fit="cover" :src="HttpManager.attachImageUrl(spotlightSong.pic)" />
        </div>
        <div class="hero-side-tags">
          <span v-for="tag in spotlightTags" :key="tag" class="hero-side-tag">{{ tag }}</span>
        </div>
        <h2 class="hero-side-title">{{ spotlightSong.songName }}</h2>
        <p class="hero-side-meta">{{ spotlightSong.singerName }}</p>
        <p class="hero-side-description">{{ spotlightSongDescription }}</p>
        <div class="hero-side-actions">
          <button type="button" class="hero-side-action" @click.stop="goSpotlightLyric">查看歌词页</button>
        </div>
      </aside>
    </section>

    <section class="home-section">
      <div class="section-head">
        <div>
          <h2 class="section-title">歌单发现</h2>
          <div class="section-tabs">
            <button
              class="section-tab"
              :class="{ active: songListTab === 'hot' }"
              @click="songListTab = 'hot'"
            >
              热门歌单
            </button>
            <span class="section-divider">/</span>
            <button
              class="section-tab"
              :class="{ active: songListTab === 'recommend', disabled: !isLogin }"
              @click="switchSectionTab('songList', 'recommend')"
            >
              推荐歌单
            </button>
          </div>
        </div>
        <router-link :to="songListViewAllLink" class="section-link">查看全部</router-link>
      </div>
      <play-list
        v-if="songListTab === 'hot' && hotSongLists.length"
        class="surface-list"
        path="song-sheet-detail"
        :playList="hotSongLists"
        card-type="cover"
      ></play-list>
      <div v-else-if="songListTab === 'hot'" class="section-empty music-panel-soft">暂时还没有热门歌单可查看</div>
      <play-list
        v-else-if="recommendSongLists.length"
        class="surface-list"
        path="song-sheet-detail"
        :playList="recommendSongLists"
        card-type="cover"
      ></play-list>
      <div v-else class="section-empty music-panel-soft">{{ isLogin ? "暂时还没有适合你的推荐歌单" : "登录后可查看推荐歌单" }}</div>
    </section>

    <section class="home-section">
      <div class="section-head">
        <div>
          <h2 class="section-title">歌曲与播放列表</h2>
          <div class="section-tabs">
            <button class="section-tab" :class="{ active: songTab === 'hot' }" @click="songTab = 'hot'">热门歌曲</button>
            <span class="section-divider">/</span>
            <button
              class="section-tab"
              :class="{ active: songTab === 'recommend', disabled: !isLogin }"
              @click="switchSectionTab('song', 'recommend')"
            >
              推荐歌曲
            </button>
          </div>
        </div>
        <router-link :to="songViewAllLink" class="section-link">查看全部</router-link>
      </div>
      <song-page
        v-if="songTab === 'hot' && hotSongs.length"
        class="surface-list"
        :playList="hotSongs"
        :fullPlayList="hotSongsFull"
      ></song-page>
      <div v-else-if="songTab === 'hot'" class="section-empty music-panel-soft">暂时还没有热门歌曲可收听</div>
      <song-page
        v-else-if="recommendSongs.length"
        class="surface-list"
        :playList="recommendSongs"
        :fullPlayList="recommendSongsFull"
      ></song-page>
      <div v-else class="section-empty music-panel-soft">{{ isLogin ? "暂时还没有适合你的推荐歌曲" : "登录后可查看推荐歌曲" }}</div>
    </section>

    <section class="home-section">
      <div class="section-head">
        <div>
          <h2 class="section-title">歌手聚焦</h2>
          <div class="section-tabs">
            <button class="section-tab" :class="{ active: singerTab === 'hot' }" @click="singerTab = 'hot'">热门歌手</button>
            <span class="section-divider">/</span>
            <button
              class="section-tab"
              :class="{ active: singerTab === 'recommend', disabled: !isLogin }"
              @click="switchSectionTab('singer', 'recommend')"
            >
              推荐歌手
            </button>
          </div>
        </div>
        <router-link :to="singerViewAllLink" class="section-link">查看全部</router-link>
      </div>
      <play-list
        v-if="singerTab === 'hot' && hotSingers.length"
        class="surface-list"
        path="singer-detail"
        :playList="hotSingers"
        card-type="artist"
      ></play-list>
      <div v-else-if="singerTab === 'hot'" class="section-empty music-panel-soft">暂时还没有热门歌手可查看</div>
      <play-list
        v-else-if="recommendSingers.length"
        class="surface-list"
        path="singer-detail"
        :playList="recommendSingers"
        card-type="artist"
      ></play-list>
      <div v-else class="section-empty music-panel-soft">{{ isLogin ? "暂时还没有适合你的推荐歌手" : "登录后可查看推荐歌手" }}</div>
    </section>

    <el-dialog
      v-model="bootstrapVisible"
      class="bootstrap-dialog"
      width="min(860px, calc(100vw - 32px))"
      :show-close="false"
      :close-on-click-modal="false"
      append-to-body
    >
      <div class="bootstrap-dialog-shell">
        <div class="bootstrap-header">
          <span class="music-chip"><span class="music-dot"></span> 推荐偏好初始化</span>
          <h2 class="bootstrap-title">先告诉轻音音乐，你更想遇见怎样的声音</h2>
          <p class="bootstrap-description">
            选几位喜欢的歌手、几种常听风格，再挑 1-3 首喜欢的歌。
            这会让首页的猜你喜欢更快靠近你的口味。
          </p>
        </div>

        <section class="bootstrap-section">
          <div class="bootstrap-section-head">
            <h3>喜欢的歌手</h3>
            <span>最多 3 个</span>
          </div>
          <div v-if="bootstrapSingers.length" class="bootstrap-grid artist-grid">
            <button
              v-for="item in bootstrapSingers"
              :key="item.id"
              type="button"
              class="bootstrap-pill-card"
              :class="{ active: selectedBootstrapSingerIds.includes(item.id) }"
              @click="toggleBootstrapSinger(item.id)"
            >
              <img :src="HttpManager.attachImageUrl(item.pic)" class="bootstrap-pill-avatar" />
              <span>{{ item.name }}</span>
            </button>
          </div>
          <el-empty v-else description="暂时没有可选歌手" :image-size="72" class="bootstrap-empty" />
        </section>

        <section class="bootstrap-section">
          <div class="bootstrap-section-head">
            <h3>喜欢的风格</h3>
            <span>最多 3 个</span>
          </div>
          <div v-if="bootstrapStyles.length" class="bootstrap-grid style-grid">
            <button
              v-for="style in bootstrapStyles"
              :key="style"
              type="button"
              class="bootstrap-style-chip"
              :class="{ active: selectedBootstrapStyles.includes(style) }"
              @click="toggleBootstrapStyle(style)"
            >
              {{ style }}
            </button>
          </div>
          <el-empty v-else description="暂时没有可选风格" :image-size="72" class="bootstrap-empty" />
        </section>

        <section class="bootstrap-section">
          <div class="bootstrap-section-head">
            <h3>喜欢的歌曲</h3>
            <span>可选，最多 3 首</span>
          </div>
          <div v-if="bootstrapSongs.length" class="bootstrap-grid song-grid">
            <button
              v-for="item in bootstrapSongs"
              :key="item.id"
              type="button"
              class="bootstrap-song-card"
              :class="{ active: selectedBootstrapSongIds.includes(item.id) }"
              @click="toggleBootstrapSong(item.id)"
            >
              <img :src="HttpManager.attachImageUrl(item.pic)" class="bootstrap-song-cover" />
              <div class="bootstrap-song-copy">
                <strong>{{ getSongTitle(item.name) }}</strong>
                <span>{{ getSingerName(item.name) }}</span>
              </div>
            </button>
          </div>
          <el-empty v-else description="暂时没有可选歌曲" :image-size="72" class="bootstrap-empty" />
        </section>

        <div class="bootstrap-actions">
          <button type="button" class="hero-btn hero-btn-secondary" @click="bootstrapVisible = false">稍后再选</button>
          <button
            type="button"
            class="hero-btn hero-btn-primary"
            :disabled="bootstrapSaving"
            @click="saveBootstrapPreferences"
          >
            {{ bootstrapSaving ? "保存中..." : "保存偏好" }}
          </button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useStore } from "vuex";
import { HttpManager } from "@/api";
import PlayList from "@/components/PlayList.vue";
import SongPage from "@/components/SongPage.vue";
import { NavName, RouterName } from "@/enums";
import mixin from "@/mixins/mixin";

const store = useStore();
const { changeIndex, playMusic, routerManager, getSongTitle, getSingerName } = mixin();
const HOME_SONG_LIMIT = 10;
const FULL_SONG_LIMIT = 200;

const swiperList = ref([]);
const hotSongLists = ref([]);
const hotSongs = ref([]);
const hotSongsFull = ref([]);
const hotSingers = ref([]);
const allSongs = ref<any[]>([]);
const allSingers = ref<any[]>([]);
const recommendSongLists = ref([]);
const recommendSongs = ref([]);
const recommendSongsFull = ref([]);
const recommendSingers = ref([]);
const songListTab = ref<"hot" | "recommend">("hot");
const songTab = ref<"hot" | "recommend">("hot");
const singerTab = ref<"hot" | "recommend">("hot");
const bootstrapVisible = ref(false);
const bootstrapSaving = ref(false);
const bootstrapSingers = ref<any[]>([]);
const bootstrapSongs = ref<any[]>([]);
const bootstrapStyles = ref<string[]>([]);
const selectedBootstrapSingerIds = ref<number[]>([]);
const selectedBootstrapSongIds = ref<number[]>([]);
const selectedBootstrapStyles = ref<string[]>([]);

const isLogin = computed(() => Boolean(store.getters.token));
const currentSongId = computed(() => store.getters.songId);
const currentSongUrl = computed(() => store.getters.songUrl);
const songListViewAllLink = computed(() =>
  songListTab.value === "hot"
    ? { path: "/song-sheet", query: { source: "hot" } }
    : { path: "/song-sheet", query: { source: "recommend" } }
);
const songViewAllLink = computed(() =>
  songTab.value === "hot"
    ? { path: "/search", query: { tab: "song", source: "hot" } }
    : { path: "/search", query: { tab: "song", source: "recommend" } }
);
const singerViewAllLink = computed(() =>
  singerTab.value === "hot"
    ? { path: "/singer", query: { source: "hot" } }
    : { path: "/singer", query: { source: "recommend" } }
);

const heroUsesRecommend = computed(() => recommendSongsFull.value.length > 0);
const heroSongPool = computed(() => (heroUsesRecommend.value ? recommendSongsFull.value : hotSongsFull.value));
const normalizedHeroSongPool = computed(() =>
  heroSongPool.value.map((item: Record<string, any>, index: number) => ({
    ...item,
    songName: getSongTitle(item.name || ""),
    singerName: getSingerName(item.name || ""),
    index,
  }))
);
const spotlightSong = computed(() => normalizedHeroSongPool.value[1] || normalizedHeroSongPool.value[0] || null);
const spotlightTags = computed(() => {
  if (!spotlightSong.value) {
    return [];
  }

  return [
    heroUsesRecommend.value ? "猜你喜欢" : "热门单曲",
    spotlightSong.value.singerName,
    formatSongDuration(spotlightSong.value.duration),
  ].filter(Boolean).slice(0, 3);
});
const spotlightSongDescription = computed(() => {
  if (heroUsesRecommend.value) {
    return "把最近的偏爱，轻轻放进这一首刚好懂你的旋律里。";
  }
  return "先从这一首开始，让旋律替你把今天慢慢接住。";
});
const heroSongLibraryMetric = computed(() => {
  const count = allSongs.value.length;
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`;
  return String(count || 0);
});
const heroSingerLibraryMetric = computed(() => {
  const count = allSingers.value.length;
  return count ? `${count}` : "0";
});
const heroPoolCountMetric = computed(() => {
  const count = normalizedHeroSongPool.value.length;
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`;
  return String(count || 0);
});
const heroPoolCountLabel = computed(() => (heroUsesRecommend.value ? "当前推荐歌曲" : "当前热门歌曲"));

function parseDurationToSeconds(duration?: number | string) {
  if (typeof duration === "number") {
    return Number.isFinite(duration) ? Math.max(0, Math.floor(duration)) : 0;
  }

  const raw = String(duration || "").trim();
  if (!raw) {
    return 0;
  }

  if (/^\d+(\.\d+)?$/.test(raw)) {
    return Math.max(0, Math.floor(Number(raw)));
  }

  const parts = raw.split(":").map((part) => Number(part));
  if (!parts.length || parts.some((part) => Number.isNaN(part))) {
    return 0;
  }

  if (parts.length === 2) {
    return Math.max(0, parts[0] * 60 + parts[1]);
  }

  if (parts.length === 3) {
    return Math.max(0, parts[0] * 3600 + parts[1] * 60 + parts[2]);
  }

  return 0;
}

function formatSongDuration(duration?: number | string) {
  const totalSeconds = parseDurationToSeconds(duration);
  if (!totalSeconds) {
    return "单曲";
  }

  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, "0");
  const seconds = String(totalSeconds % 60).padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function toLimitedList(result: PromiseSettledResult<ResponseBody>, limit = 10) {
  if (result.status !== "fulfilled") {
    return [];
  }

  const response = result.value as ResponseBody;
  if (!response || response.success === false || !Array.isArray(response.data)) {
    return [];
  }

  return response.data.slice(0, limit);
}

function resolveResponseData(result: PromiseSettledResult<ResponseBody>) {
  if (result.status !== "fulfilled") {
    return null;
  }

  const response = result.value as ResponseBody;
  if (!response || response.success === false) {
    return null;
  }

  return response.data;
}

function clearRecommendData() {
  recommendSongLists.value = [];
  recommendSongs.value = [];
  recommendSongsFull.value = [];
  recommendSingers.value = [];
}

function clearBootstrapState() {
  bootstrapVisible.value = false;
  bootstrapSaving.value = false;
  bootstrapSingers.value = [];
  bootstrapSongs.value = [];
  bootstrapStyles.value = [];
  selectedBootstrapSingerIds.value = [];
  selectedBootstrapSongIds.value = [];
  selectedBootstrapStyles.value = [];
}

function switchSectionTab(section: "songList" | "song" | "singer", target: "hot" | "recommend") {
  if (target === "recommend" && !isLogin.value) {
    ElMessage.info("登录后可查看推荐内容");
    return;
  }

  if (section === "songList") {
    songListTab.value = target;
    return;
  }

  if (section === "song") {
    songTab.value = target;
    return;
  }

  singerTab.value = target;
}

function playHeroSong() {
  const targetSong = normalizedHeroSongPool.value[0];
  if (!targetSong) {
    ElMessage.info("暂时没有可播放的歌曲");
    return;
  }

  songTab.value = heroUsesRecommend.value ? "recommend" : "hot";

  if (currentSongId.value === targetSong.id && currentSongUrl.value === targetSong.url) {
    store.commit("setCurrentPlayList", normalizedHeroSongPool.value);
    store.commit("setCurrentPlayIndex", 0);
    store.commit("setIsPlay", true);
    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent("music-player:reveal"));
    }
    return;
  }

  playMusic({
    id: targetSong.id,
    url: targetSong.url,
    pic: targetSong.pic,
    index: 0,
    name: targetSong.name,
    lyric: targetSong.lyric,
    currentSongList: normalizedHeroSongPool.value,
  });
}

function playSpotlightSong() {
  const targetSong = spotlightSong.value;
  if (!targetSong) {
    ElMessage.info("暂时没有可播放的歌曲");
    return;
  }

  songTab.value = heroUsesRecommend.value ? "recommend" : "hot";

  if (currentSongId.value === targetSong.id && currentSongUrl.value === targetSong.url) {
    store.commit("setCurrentPlayList", normalizedHeroSongPool.value);
    store.commit("setCurrentPlayIndex", targetSong.index || 0);
    store.commit("setIsPlay", true);
    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent("music-player:reveal"));
    }
    return;
  }

  playMusic({
    id: targetSong.id,
    url: targetSong.url,
    pic: targetSong.pic,
    index: targetSong.index || 0,
    name: targetSong.name,
    lyric: targetSong.lyric,
    currentSongList: normalizedHeroSongPool.value,
  });
}

function goSpotlightLyric() {
  const targetSong = spotlightSong.value;
  if (!targetSong) {
    ElMessage.info("暂时没有可查看的歌曲");
    return;
  }

  if (currentSongId.value !== targetSong.id || currentSongUrl.value !== targetSong.url) {
    playMusic({
      id: targetSong.id,
      url: targetSong.url,
      pic: targetSong.pic,
      index: targetSong.index || 0,
      name: targetSong.name,
      lyric: targetSong.lyric,
      currentSongList: normalizedHeroSongPool.value,
    });
  } else {
    store.commit("setCurrentPlayList", normalizedHeroSongPool.value);
    store.commit("setCurrentPlayIndex", targetSong.index || 0);
  }

  routerManager(RouterName.Lyric, {
    path: `${RouterName.Lyric}/${targetSong.id}`,
  });
}

function openHeroSongs() {
  songTab.value = heroUsesRecommend.value ? "recommend" : "hot";
  routerManager("/search", {
    path: "/search",
    query: {
      tab: "song",
      source: heroUsesRecommend.value ? "recommend" : "hot",
    },
  });
}

function toggleBootstrapSelection(list: number[], update: (next: number[]) => void, id: number, limit: number, label: string) {
  const next = list.slice();
  const index = next.indexOf(id);
  if (index !== -1) {
    next.splice(index, 1);
    update(next);
    return;
  }

  if (next.length >= limit) {
    ElMessage.info(`${label}最多选择 ${limit} 个`);
    return;
  }

  update([...next, id]);
}

function toggleBootstrapSinger(id: number) {
  toggleBootstrapSelection(selectedBootstrapSingerIds.value, (next) => {
    selectedBootstrapSingerIds.value = next;
  }, id, 3, "歌手");
}

function toggleBootstrapSong(id: number) {
  toggleBootstrapSelection(selectedBootstrapSongIds.value, (next) => {
    selectedBootstrapSongIds.value = next;
  }, id, 3, "歌曲");
}

function toggleBootstrapStyle(style: string) {
  const list = selectedBootstrapStyles.value.slice();
  const index = list.indexOf(style);
  if (index !== -1) {
    list.splice(index, 1);
    selectedBootstrapStyles.value = list;
    return;
  }

  if (list.length >= 3) {
    ElMessage.info("风格最多选择 3 个");
    return;
  }

  selectedBootstrapStyles.value = [...list, style];
}

function applyBootstrapOptions(data: Record<string, any> | null) {
  if (!data) {
    clearBootstrapState();
    return;
  }

  bootstrapSingers.value = Array.isArray(data.singers) ? data.singers : [];
  bootstrapSongs.value = Array.isArray(data.songs) ? data.songs : [];
  bootstrapStyles.value = Array.isArray(data.styles) ? data.styles : [];
  selectedBootstrapSingerIds.value = Array.isArray(data.selectedSingerIds) ? data.selectedSingerIds : [];
  selectedBootstrapSongIds.value = Array.isArray(data.selectedSongIds) ? data.selectedSongIds : [];
  selectedBootstrapStyles.value = Array.isArray(data.selectedStyles) ? data.selectedStyles : [];
  bootstrapVisible.value = Boolean(
    data.needsBootstrap &&
      (bootstrapSingers.value.length || bootstrapSongs.value.length || bootstrapStyles.value.length)
  );
}

async function saveBootstrapPreferences() {
  bootstrapSaving.value = true;
  try {
    const response = await HttpManager.saveRecommendBootstrapPreferences({
      likedSingerIds: selectedBootstrapSingerIds.value,
      likedSongIds: selectedBootstrapSongIds.value,
      likedStyles: selectedBootstrapStyles.value,
    });

    if ((response as ResponseBody)?.success === false) {
      ElMessage.error((response as ResponseBody)?.message || "保存偏好失败");
      return;
    }

    bootstrapVisible.value = false;
    ElMessage.success("偏好已保存，正在更新猜你喜欢");
    await loadRecommendData();
  } catch (error) {
    ElMessage.error("保存偏好失败，请稍后再试");
  } finally {
    bootstrapSaving.value = false;
  }
}

async function loadBaseData() {
  const [bannerRes, hotSongListRes, hotSongRes, hotSingerRes, allSongRes, allSingerRes] = await Promise.allSettled([
    HttpManager.getBannerList(),
    HttpManager.getHotSongLists(),
    HttpManager.getHotSongs(FULL_SONG_LIMIT),
    HttpManager.getHotSingers(),
    HttpManager.getAllSongs(),
    HttpManager.getAllSinger(),
  ]);

  swiperList.value = toLimitedList(bannerRes, 20);
  hotSongLists.value = toLimitedList(hotSongListRes);
  hotSongsFull.value = toLimitedList(hotSongRes, FULL_SONG_LIMIT);
  hotSongs.value = hotSongsFull.value.slice(0, HOME_SONG_LIMIT);
  hotSingers.value = toLimitedList(hotSingerRes);
  allSongs.value = toLimitedList(allSongRes, Number.MAX_SAFE_INTEGER);
  allSingers.value = toLimitedList(allSingerRes, Number.MAX_SAFE_INTEGER);
}

async function loadRecommendData() {
  if (!isLogin.value) {
    clearRecommendData();
    clearBootstrapState();
    return;
  }

  const [songListRes, songRes, singerRes, bootstrapRes] = await Promise.allSettled([
    HttpManager.getRecommendSongLists(),
    HttpManager.getRecommendSongList(FULL_SONG_LIMIT),
    HttpManager.getRecommendSingers(),
    HttpManager.getRecommendBootstrapOptions(),
  ]);

  recommendSongLists.value = toLimitedList(songListRes);
  recommendSongsFull.value = toLimitedList(songRes, FULL_SONG_LIMIT);
  recommendSongs.value = recommendSongsFull.value.slice(0, HOME_SONG_LIMIT);
  recommendSingers.value = toLimitedList(singerRes);
  applyBootstrapOptions(resolveResponseData(bootstrapRes) as Record<string, any> | null);
}

watch(isLogin, () => {
  if (!isLogin.value) {
    songListTab.value = "hot";
    songTab.value = "hot";
    singerTab.value = "hot";
  }
  void loadRecommendData();
});

onMounted(() => {
  changeIndex(NavName.Home);
  void loadBaseData();
  void loadRecommendData();
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

.home-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(290px, 0.8fr);
  gap: var(--page-shell-gap);
}

.hero-main {
  position: relative;
  min-height: 420px;
  overflow: hidden;
}

.hero-carousel,
.hero-slide,
.hero-image {
  width: 100%;
  height: 100%;
}

.hero-image {
  object-fit: cover;
}

.hero-image-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(110deg, color-mix(in srgb, var(--app-panel-bg-strong) 72%, transparent) 0%, color-mix(in srgb, var(--app-panel-bg-strong) 46%, transparent) 60%, transparent 100%),
    radial-gradient(circle at 75% 25%, rgba(255, 177, 110, 0.28), transparent 24%),
    radial-gradient(circle at 30% 100%, rgba(94, 214, 255, 0.18), transparent 30%);
}

.hero-copy {
  position: absolute;
  inset: 0;
  z-index: 2;
  width: min(60%, 680px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  padding: clamp(26px, 4vw, 40px);
}

.hero-title {
  margin: 0;
  font-size: clamp(42px, 5vw, 72px);
  line-height: 0.98;
  font-weight: 900;
  letter-spacing: -0.06em;
  color: var(--app-title);
}

.hero-description {
  margin: 0;
  max-width: 620px;
  color: var(--app-text-muted);
  font-size: 1.02rem;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-btn {
  min-width: 132px;
  min-height: 48px;
  padding: 0 1.2rem;
  border: 0;
  border-radius: 999px;
  font-size: 0.98rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.25s ease, opacity 0.25s ease, background-color 0.25s ease;

  &:hover {
    transform: translateY(-1px);
  }
}

.hero-btn-primary {
  background: var(--app-button-primary-bg);
  color: var(--app-button-primary-text);
}

.hero-btn-secondary {
  background: var(--app-button-secondary-bg);
  color: var(--app-button-secondary-text);
  border: 1px solid var(--app-panel-border);
}

.hero-stats {
  display: flex;
  gap: 28px;
  padding-top: 6px;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;

  strong {
    color: var(--app-title);
    font-size: 1.8rem;
    font-weight: 800;
  }

  span {
    color: var(--app-text-soft);
    font-size: 0.88rem;
  }
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;
}

.hero-side:hover,
.hero-side:focus-visible {
  transform: translateY(-3px);
  box-shadow: var(--app-panel-shadow);
  border-color: color-mix(in srgb, var(--app-accent) 30%, var(--app-panel-border));
  outline: none;
}

.hero-side-cover {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 24px;
  overflow: hidden;
}

.hero-side-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-side-tag {
  padding: 0.42rem 0.78rem;
  border-radius: 999px;
  background: var(--app-chip-bg-muted);
  color: var(--app-text-soft);
  font-size: 0.78rem;
  font-weight: 600;
}

.hero-side-title,
.section-title {
  margin: 0;
  color: var(--app-title);
  font-weight: 900;
}

.hero-side-title {
  font-size: 2rem;
  line-height: 1.05;
}

.hero-side-meta {
  margin: 0;
  color: var(--app-text-muted);
  font-size: 0.96rem;
  line-height: 1.5;
}

.hero-side-description {
  margin: 0;
  color: var(--app-text-muted);
  line-height: 1.7;
}

.hero-side-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: auto;
}

.hero-side-action {
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--app-panel-border);
  background: color-mix(in srgb, var(--app-panel-bg) 88%, transparent);
  color: var(--app-text);
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.24s ease;
}

.hero-side-action:hover {
  border-color: color-mix(in srgb, var(--app-accent) 26%, var(--app-panel-border));
  color: var(--app-accent-strong);
  background: color-mix(in srgb, var(--app-accent-soft) 16%, var(--app-panel-bg));
}

.home-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.section-title {
  font-size: clamp(24px, 3vw, 34px);
}

.section-tabs {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.section-tab,
.section-link {
  color: var(--app-text-muted);
  font-size: 1rem;
  font-weight: 700;
}

.section-tab {
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;

  &.active {
    color: var(--app-title);
  }

  &.disabled {
    opacity: 0.46;
  }
}

.section-divider {
  color: var(--app-text-soft);
  font-weight: 700;
}

.section-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  background: var(--app-chip-bg-muted);
}

.section-empty {
  padding: 42px 20px;
  text-align: center;
  color: var(--app-text-muted);
}

:deep(.bootstrap-dialog) {
  border-radius: 30px;
  overflow: hidden;
  background: color-mix(in srgb, var(--app-panel-bg) 94%, transparent);
  border: 1px solid var(--app-panel-border);
  box-shadow: var(--app-panel-shadow);
  backdrop-filter: blur(18px);
}

:deep(.bootstrap-dialog .el-dialog__header) {
  display: none;
}

:deep(.bootstrap-dialog .el-dialog__body) {
  padding: 0;
}

.bootstrap-dialog-shell {
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 28px;
}

.bootstrap-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bootstrap-title {
  margin: 0;
  color: var(--app-title);
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 900;
}

.bootstrap-description {
  margin: 0;
  color: var(--app-text-muted);
  line-height: 1.8;
}

.bootstrap-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bootstrap-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  h3 {
    margin: 0;
    color: var(--app-title);
    font-size: 1.1rem;
    font-weight: 800;
  }

  span {
    color: var(--app-text-soft);
    font-size: 0.86rem;
  }
}

.bootstrap-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.bootstrap-pill-card,
.bootstrap-song-card,
.bootstrap-style-chip {
  position: relative;
  border: 1px solid var(--app-panel-border);
  background: color-mix(in srgb, var(--app-panel-bg) 90%, transparent);
  color: var(--app-text);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease, background-color 0.22s ease;
}

.bootstrap-pill-card:hover,
.bootstrap-song-card:hover,
.bootstrap-style-chip:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--app-accent) 24%, var(--app-panel-border));
}

.bootstrap-pill-card.active,
.bootstrap-song-card.active,
.bootstrap-style-chip.active {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--app-accent-soft) 68%, var(--app-panel-bg)) 0%,
    color-mix(in srgb, var(--app-panel-bg) 86%, white 8%) 100%
  );
  border-color: color-mix(in srgb, var(--app-accent) 74%, var(--app-panel-border));
  box-shadow:
    0 16px 28px rgba(94, 214, 255, 0.16),
    inset 0 0 0 1px color-mix(in srgb, var(--app-accent) 22%, white 6%);
}

.bootstrap-pill-card.active::after,
.bootstrap-song-card.active::after,
.bootstrap-style-chip.active::after {
  content: "✓";
  position: absolute;
  top: 8px;
  right: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--app-accent) 88%, white 6%);
  color: #ffffff;
  font-size: 0.72rem;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(94, 214, 255, 0.24);
}

.bootstrap-pill-card {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 8px 14px 8px 10px;
  border-radius: 999px;
}

.bootstrap-pill-avatar {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  object-fit: cover;
}

.bootstrap-pill-card.active,
.bootstrap-style-chip.active,
.bootstrap-song-card.active .bootstrap-song-copy strong,
.bootstrap-song-card.active .bootstrap-song-copy span,
.bootstrap-pill-card.active span {
  color: var(--app-title);
}

.bootstrap-pill-card.active .bootstrap-pill-avatar,
.bootstrap-song-card.active .bootstrap-song-cover {
  box-shadow:
    0 0 0 2px color-mix(in srgb, var(--app-accent) 58%, white 12%),
    0 10px 20px rgba(94, 214, 255, 0.2);
}

.bootstrap-style-chip {
  min-height: 42px;
  padding: 0 40px 0 18px;
  border-radius: 999px;
  font-size: 0.94rem;
  font-weight: 700;
}

.bootstrap-song-card {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  width: min(100%, 240px);
  padding: 10px 14px 10px 10px;
  border-radius: 18px;
}

.bootstrap-song-cover {
  width: 54px;
  height: 54px;
  border-radius: 14px;
  object-fit: cover;
}

.bootstrap-song-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;

  strong,
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: var(--app-title);
    font-size: 0.95rem;
  }

  span {
    color: var(--app-text-soft);
    font-size: 0.84rem;
  }
}

.bootstrap-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 6px;
}

.bootstrap-empty {
  border-radius: 20px;
  background: color-mix(in srgb, var(--app-chip-bg-muted) 72%, transparent);
  border: 1px dashed var(--app-panel-border);
}

@media (max-width: 1100px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .hero-main {
    min-height: 360px;
  }
}

@media (max-width: 768px) {
  .hero-copy {
    width: 100%;
    justify-content: flex-end;
  }

  .hero-stats {
    gap: 18px;
    flex-wrap: wrap;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .bootstrap-actions {
    flex-direction: column-reverse;
  }

  .bootstrap-actions .hero-btn {
    width: 100%;
  }
}
</style>
