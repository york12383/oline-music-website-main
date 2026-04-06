<template>
  <div class="featured-song-panel">
    <div class="song-rank-list music-panel-soft">
      <button
        v-for="(item, index) in topSongs"
        :key="item.id || index"
        type="button"
        class="rank-row"
        :class="{ active: isCurrentSong(item) }"
        @click="handleClick(item)"
      >
        <span class="rank-index">{{ item.displayRank || index + 1 }}</span>
        <span class="rank-main">
          <strong class="song-name">{{ item.songName }}</strong>
          <span class="song-artist">{{ item.singerName }}</span>
        </span>
        <span class="rank-status-slot" :class="{ active: isCurrentSong(item) }" aria-hidden="true">
          <span v-if="isCurrentSong(item)" class="rank-state">
            <span class="rank-wave" aria-hidden="true">
              <i></i>
              <i></i>
              <i></i>
            </span>
            正在播放
          </span>
        </span>
        <span class="rank-meta">
          <span class="song-album">{{ item.introduction || "单曲" }}</span>
          <span class="song-duration">{{ formatSongDuration(item) }}</span>
        </span>
      </button>
    </div>

    <aside class="song-focus-card music-panel-soft" :class="{ current: isFeatureSongCurrent }" v-if="featureSong">
      <button type="button" class="focus-cover-wrap" @click="playFeatureSong">
        <el-image class="focus-cover" fit="cover" :src="attachImageUrl(featureSong.pic)" />
      </button>
      <div class="focus-content">
        <span class="focus-chip">{{ isFeatureSongCurrent ? "正在播放" : "焦点歌曲" }}</span>
        <h3 class="focus-title">{{ featureSong.songName }}</h3>
        <p class="focus-artist">{{ featureSong.singerName }}</p>
        <div class="focus-tags">
          <button
            type="button"
            class="focus-tag"
            :class="{ active: isCollected }"
            @click="toggleFeatureCollection"
          >
            {{ isCollected ? "已收藏" : "收藏" }}
          </button>
          <button type="button" class="focus-tag" @click="downloadFeatureSong">下载</button>
          <button type="button" class="focus-tag" @click="goFeatureLyric">歌词页</button>
          <button type="button" class="focus-tag" @click="openPlaylistPanel">播放列表</button>
        </div>
        <div class="focus-progress">
          <span class="focus-progress-bar" :style="{ width: `${featureProgressPercent}%` }"></span>
        </div>
        <p class="focus-progress-meta">
          {{ featureProgressStart }} / {{ featureProgressEnd }}
        </p>
      </div>
    </aside>
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, getCurrentInstance, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useStore } from "vuex";

import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api";
import { RouterName } from "@/enums";

interface NormalizedSong {
  id?: string | number;
  url?: string;
  pic?: string;
  name?: string;
  lyric?: string;
  duration?: number | string;
  introduction?: string;
  songName: string;
  singerName: string;
  index: number;
  displayRank?: number;
}

export default defineComponent({
  props: {
    playList: Array,
    fullPlayList: Array,
  },
  setup(props) {
    const { proxy } = getCurrentInstance();
    const store = useStore();
    const { getSongTitle, getSingerName, playMusic, downloadMusic, checkStatus, routerManager } = mixin();
    const isCollected = ref(false);
    const COLLECTION_CHANGED_EVENT = "music-collection:changed";

    const token = computed(() => store.getters.token);
    const userId = computed(() => store.getters.userId);
    const currentSongId = computed(() => store.getters.songId);
    const currentSongUrl = computed(() => store.getters.songUrl);
    const curTime = computed(() => Number(store.getters.curTime || 0));
    const duration = computed(() => Number(store.getters.duration || 0));

    const normalizedPlayList = computed<NormalizedSong[]>(() =>
      ((props.playList || []) as Record<string, any>[]).map((item: Record<string, any>, index: number) => ({
        ...item,
        songName: getSongTitle(item.name),
        singerName: getSingerName(item.name),
        index,
        displayRank: index + 1,
      }))
    );

    const normalizedFullPlayList = computed<NormalizedSong[]>(() => {
      const source = (((props.fullPlayList && props.fullPlayList.length ? props.fullPlayList : props.playList) || []) as Record<string, any>[]);
      return source.map((item: Record<string, any>, index: number) => ({
        ...item,
        songName: getSongTitle(item.name),
        singerName: getSingerName(item.name),
        index,
        displayRank: index + 1,
      }));
    });
    const currentSongInPanel = computed<NormalizedSong | null>(() => {
      if (!normalizedFullPlayList.value.length || !currentSongId.value) {
        return null;
      }

      return (
        normalizedFullPlayList.value.find(
          (item) =>
            String(item.id) === String(currentSongId.value) &&
            (!currentSongUrl.value || !item.url || item.url === currentSongUrl.value)
        ) ||
        normalizedFullPlayList.value.find((item) => String(item.id) === String(currentSongId.value)) ||
        null
      );
    });

    const topSongs = computed<NormalizedSong[]>(() => {
      const baseSongs = normalizedPlayList.value.slice(0, 5);
      const currentSong = currentSongInPanel.value;

      if (!currentSong) {
        return baseSongs;
      }

      const alreadyVisible = baseSongs.some((item) => String(item.id) === String(currentSong.id));
      if (alreadyVisible) {
        return baseSongs;
      }

      return [currentSong, ...baseSongs].slice(0, 5);
    });

    const featureSong = computed<NormalizedSong | null>(() => currentSongInPanel.value || topSongs.value[0] || null);
    const isFeatureSongCurrent = computed(() => Boolean(featureSong.value && isCurrentSong(featureSong.value)));
    const featureProgressPercent = computed(() => {
      if (!isFeatureSongCurrent.value || duration.value <= 0) {
        return 0;
      }
      return Math.min(100, Math.max(0, (curTime.value / duration.value) * 100));
    });
    const featureProgressStart = computed(() =>
      isFeatureSongCurrent.value ? formatDuration(curTime.value) : "00:00"
    );
    const featureProgressEnd = computed(() =>
      isFeatureSongCurrent.value ? formatDuration(duration.value) : formatSongDuration(featureSong.value)
    );

    watch(
      [featureSong, token],
      () => {
        void syncCollectionStatus();
      },
      { immediate: true }
    );

    onMounted(() => {
      if (typeof window !== "undefined") {
        window.addEventListener(COLLECTION_CHANGED_EVENT, handleCollectionChanged as EventListener);
      }
    });

    onBeforeUnmount(() => {
      if (typeof window !== "undefined") {
        window.removeEventListener(COLLECTION_CHANGED_EVENT, handleCollectionChanged as EventListener);
      }
    });

    function handleClick(row: NormalizedSong) {
      const currentSongList = normalizedFullPlayList.value;
      const fullListIndex = currentSongList.findIndex((item) => String(item.id) === String(row.id));
      playMusic({
        id: row.id,
        url: row.url,
        pic: row.pic,
        index: fullListIndex >= 0 ? fullListIndex : row.index ?? 0,
        name: row.name,
        lyric: row.lyric,
        currentSongList,
      });
    }

    function isCurrentSong(item: NormalizedSong | null) {
      if (!item || !currentSongId.value) {
        return false;
      }

      if (String(item.id) !== String(currentSongId.value)) {
        return false;
      }

      if (!currentSongUrl.value || !item.url) {
        return true;
      }

      return item.url === currentSongUrl.value;
    }

    async function syncCollectionStatus() {
      if (!token.value || !featureSong.value?.id) {
        isCollected.value = false;
        return;
      }

      const response = (await HttpManager.isCollection({
        userId: userId.value,
        type: 0,
        songId: featureSong.value.id,
      })) as ResponseBody;

      isCollected.value = Boolean(response?.data);
    }

    async function toggleFeatureCollection() {
      if (!featureSong.value?.id || !checkStatus()) {
        return;
      }

      const request = isCollected.value
        ? HttpManager.deleteCollection({ userId: userId.value, type: 0, songId: featureSong.value.id })
        : HttpManager.setCollection({ userId: userId.value, type: 0, songId: featureSong.value.id });

      const result = (await request) as ResponseBody;
      (proxy as any)?.$message?.({
        message: result?.message || (isCollected.value ? "已取消收藏" : "收藏成功"),
        type: result?.type || "success",
      });

      if (typeof result?.data === "boolean") {
        isCollected.value = result.data;
      } else {
        await syncCollectionStatus();
      }

      emitCollectionChanged(featureSong.value.id, isCollected.value);
    }

    function emitCollectionChanged(songId?: string | number, collected?: boolean) {
      if (typeof window === "undefined" || songId === undefined || typeof collected !== "boolean") {
        return;
      }

      window.dispatchEvent(
        new CustomEvent(COLLECTION_CHANGED_EVENT, {
          detail: {
            songId,
            collected,
          },
        })
      );
    }

    function handleCollectionChanged(event: Event) {
      const customEvent = event as CustomEvent<{ songId?: string | number; collected?: boolean }>;
      const changedSongId = customEvent.detail?.songId;

      if (!featureSong.value?.id || changedSongId === undefined) {
        return;
      }

      if (String(changedSongId) !== String(featureSong.value.id)) {
        return;
      }

      if (typeof customEvent.detail?.collected === "boolean") {
        isCollected.value = customEvent.detail.collected;
      } else {
        void syncCollectionStatus();
      }
    }

    function playFeatureSong() {
      if (!featureSong.value) return;

      const currentSongList = normalizedFullPlayList.value;
      const playIndex = currentSongList.findIndex((item) => String(item.id) === String(featureSong.value.id));

      if (isCurrentSong(featureSong.value)) {
        store.commit("setCurrentPlayList", currentSongList);
        store.commit("setCurrentPlayIndex", playIndex >= 0 ? playIndex : featureSong.value.index ?? 0);
        store.commit("setIsPlay", true);
        if (typeof window !== "undefined") {
          window.dispatchEvent(new CustomEvent("music-player:reveal"));
        }
        return;
      }

      playMusic({
        id: featureSong.value.id,
        url: featureSong.value.url,
        pic: featureSong.value.pic,
        index: playIndex >= 0 ? playIndex : featureSong.value.index ?? 0,
        name: featureSong.value.name,
        lyric: featureSong.value.lyric,
        currentSongList,
      });
    }

    function downloadFeatureSong() {
      if (!featureSong.value) return;
      downloadMusic({
        songUrl: featureSong.value.url,
        songName: featureSong.value.name,
      });
    }

    function goFeatureLyric() {
      if (!featureSong.value) return;

      if (!isCurrentSong(featureSong.value)) {
        playFeatureSong();
      }

      routerManager(RouterName.Lyric, {
        path: `${RouterName.Lyric}/${featureSong.value.id}`,
      });
    }

    function openPlaylistPanel() {
      if (featureSong.value && !isCurrentSong(featureSong.value)) {
        playFeatureSong();
      }

      store.commit("setShowAside", true);
      if (typeof window !== "undefined") {
        window.dispatchEvent(new CustomEvent("music-player:reveal"));
      }
    }

    function formatDuration(rawDuration?: number | string) {
      const totalSeconds = Math.floor(Number(rawDuration || 0));
      if (!totalSeconds) {
        return "00:00";
      }

      const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, "0");
      const seconds = String(totalSeconds % 60).padStart(2, "0");
      return `${minutes}:${seconds}`;
    }

    function formatSongDuration(item?: NormalizedSong | null) {
      if (item?.duration) {
        return formatDuration(item.duration);
      }

      return "04:21";
    }

    return {
      attachImageUrl: HttpManager.attachImageUrl,
      topSongs,
      featureSong,
      isFeatureSongCurrent,
      isCollected,
      featureProgressPercent,
      featureProgressStart,
      featureProgressEnd,
      handleClick,
      formatSongDuration,
      isCurrentSong,
      toggleFeatureCollection,
      downloadFeatureSong,
      openPlaylistPanel,
      playFeatureSong,
      goFeatureLyric,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

.featured-song-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.95fr);
  gap: clamp(16px, 2vw, 24px);
}

.song-rank-list,
.song-focus-card {
  padding: 18px;
}

.song-rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rank-row {
  position: relative;
  display: grid;
  grid-template-columns: 48px minmax(220px, 1fr) 120px minmax(150px, 0.72fr);
  align-items: center;
  gap: 16px;
  width: 100%;
  border: 1px solid transparent;
  border-radius: 18px;
  padding: 14px 16px;
  background: var(--app-chip-bg-muted);
  color: inherit;
  cursor: pointer;
  transition: transform 0.25s ease, border-color 0.25s ease, background-color 0.25s ease;

  &:hover,
  &.active {
    transform: translateY(-1px);
    border-color: var(--app-panel-border-strong);
    background: color-mix(in srgb, var(--app-chip-bg-muted) 75%, var(--app-accent) 8%);
  }
}

.rank-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--app-accent-strong), var(--app-accent));
  color: #08101f;
  font-weight: 800;
}

.rank-main,
.rank-status-slot,
.rank-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  justify-self: stretch;
}

.rank-status-slot {
  align-items: center;
  justify-content: center;
}

.song-name,
.focus-title {
  color: var(--app-title);
  font-weight: 800;
}

.song-name {
  font-size: 1rem;
  display: block;
  width: 100%;
  text-align: left;
}

.song-artist,
.song-album,
.focus-artist {
  color: var(--app-text-muted);
  font-size: 0.86rem;
  line-height: 1.5;
}

.rank-meta {
  align-items: flex-end;
  text-align: right;
  gap: 4px;
  padding-right: 0;
}

.rank-state {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0.4rem 0.78rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--app-accent) 10%, var(--app-chip-bg));
  border: 1px solid color-mix(in srgb, var(--app-accent) 20%, transparent);
  color: var(--app-accent-strong);
  font-size: 0.76rem;
  font-weight: 800;
}

.rank-wave {
  display: inline-flex;
  align-items: flex-end;
  gap: 3px;
  height: 12px;
}

.rank-wave i {
  display: block;
  width: 3px;
  border-radius: 999px;
  background: currentColor;
  animation: rank-wave 0.9s ease-in-out infinite;
}

.rank-wave i:nth-child(1) {
  height: 7px;
}

.rank-wave i:nth-child(2) {
  height: 12px;
  animation-delay: 0.12s;
}

.rank-wave i:nth-child(3) {
  height: 9px;
  animation-delay: 0.24s;
}

.song-duration {
  color: var(--app-text-soft);
  font-size: 0.82rem;
}

.song-album {
  display: block;
  width: 100%;
  text-align: right;
}

.song-focus-card {
  display: flex;
  gap: 18px;
  min-height: 100%;
  align-items: center;
}

.song-focus-card.current {
  border-color: color-mix(in srgb, var(--app-accent) 24%, var(--app-panel-border));
  box-shadow: 0 22px 48px color-mix(in srgb, var(--app-accent) 10%, transparent);
}

.rank-row.active .rank-index {
  box-shadow: 0 12px 28px color-mix(in srgb, var(--app-accent) 24%, transparent);
}

.rank-row.active .song-name {
  color: color-mix(in srgb, var(--app-title) 78%, var(--app-accent) 22%);
}

.focus-cover-wrap {
  flex-shrink: 0;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.focus-cover {
  width: 112px;
  height: 112px;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 16px 36px color-mix(in srgb, var(--app-accent-warm) 20%, transparent);
}

.focus-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.focus-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.4rem 0.72rem;
  border-radius: 999px;
  background: var(--app-chip-bg);
  color: var(--app-chip-text);
  width: fit-content;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.focus-title {
  margin: 16px 0 8px;
  font-size: 1.9rem;
  line-height: 1.1;
}

.focus-artist {
  margin: 0;
}

.focus-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.focus-tag {
  border: 1px solid transparent;
  padding: 0.45rem 0.8rem;
  border-radius: 999px;
  background: var(--app-chip-bg-muted);
  color: var(--app-text-soft);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.24s ease, border-color 0.24s ease, background-color 0.24s ease, color 0.24s ease;
}

.focus-tag:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--app-accent) 26%, var(--app-panel-border));
  color: var(--app-accent-strong);
}

.focus-tag.active {
  border-color: color-mix(in srgb, var(--app-accent-warm) 36%, var(--app-panel-border));
  background: color-mix(in srgb, var(--app-accent-warm) 16%, var(--app-chip-bg-muted));
  color: var(--app-title);
}

.focus-progress {
  margin-top: 18px;
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--app-title) 8%, transparent);
  overflow: hidden;
}

.focus-progress-bar {
  display: block;
  width: 0;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--app-accent-warm), var(--app-accent));
  transition: width 0.24s ease;
}

.focus-progress-meta {
  margin: 10px 0 0;
  color: var(--app-text-soft);
  font-size: 0.82rem;
}

@keyframes rank-wave {
  0%,
  100% {
    transform: scaleY(0.65);
    opacity: 0.72;
  }
  50% {
    transform: scaleY(1);
    opacity: 1;
  }
}

@media (max-width: 992px) {
  .featured-song-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .song-focus-card {
    flex-direction: column;
  }

  .focus-cover {
    width: 92px;
    height: 92px;
  }

  .focus-title {
    font-size: 1.5rem;
  }

  .rank-row {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .rank-status-slot {
    display: none;
  }

  .rank-meta {
    grid-column: 2;
    align-items: flex-start;
    text-align: left;
    margin-top: 4px;
  }

  .song-album {
    text-align: left;
  }

  .rank-state {
    justify-content: flex-start;
  }
}
</style>
