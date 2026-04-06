<template>
  <div class="music-page-shell song-page">
    <div class="song-background" :style="{ backgroundImage: `url(${attachImageUrl(songPic)})` }"></div>

    <div class="song-content">
      <div class="song-info-container">
        <!-- 移动端优化：歌手信息放在封面顶部 -->
        <div class="mobile-song-meta">
          <h1 class="song-title">{{ songTitle }}</h1>
          <button type="button" class="song-artist song-artist-button" @click="handleOpenSingerDetail">
            {{ singerName }}
          </button>
        </div>

        <div class="cover-rating-wrapper">
          <div class="song-cover-wrapper">
            <el-image class="song-cover" :src="attachImageUrl(songPic)" fit="cover"
              :preview-src-list="[attachImageUrl(songPic)]">
              <template #error>
                <div class="cover-error">
                  <el-icon>
                    <Picture />
                  </el-icon>
                </div>
              </template>
            </el-image>

            <!-- 桌面端歌手信息 -->
            <div class="desktop-song-meta">
              <h1 class="song-title">{{ songTitle }}</h1>
              <button type="button" class="song-artist song-artist-button" @click="handleOpenSingerDetail">
                {{ singerName }}
              </button>
            </div>
          </div>

          <div class="song-rating-card">
            <div class="rating-item">
              <div class="rating-label">综合评分</div>
              <div class="rating-value">
                <span class="score-num">{{ (averageScore * 2).toFixed(1) }}</span>
                <el-rate v-model="averageScore" disabled show-score text-color="#ff9900" score-template="{value}"
                  class="readonly-rate" />
              </div>
            </div>

            <div class="rating-divider"></div>
            <div class="rating-item">
              <div class="rating-label">我要评分</div>
              <div class="rating-action">
                <el-rate v-model="userScore" :disabled="isRated" allow-half @change="handlePushScore"
                  :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
                <div class="rate-controls">
                  <span class="rate-text" v-if="isRated">已评价 ({{ userScore * 2 }}分)</span>
                  <span class="rate-text" v-else>点击星星评分</span>
                  <el-button v-if="isRated" size="small" type="danger" @click="handleDeleteScore" class="delete-rating-btn" plain>
                    删除评分
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="lyric-container music-panel">
        <div class="lyric-header">
          <span>歌词</span>
          <!-- 移动端滚动提示 -->
          <div class="lyric-hint" v-if="isMobile">
            <el-icon><ArrowDownBold /></el-icon>
            <span>向上滑动查看歌词</span>
          </div>
        </div>
        <div class="lyric-viewport" ref="lyricViewport">
          <div class="lyric-content">
            <template v-if="lyricArr.length > 0">
              <div
                v-for="(item, index) in lyricArr"
                :key="`${index}-${item[0]}`"
                :class="{
                  active: isActive(index),
                  passed: isPassed(index),
                  future: isFuture(index),
                  interactive: hasTimestamp(item),
                }"
                class="lyric-line"
                :ref="(el) => setLyricLineRef(el, index)"
                role="button"
                tabindex="0"
                @click="handleLyricSeek(item, index)"
                @keydown.enter.prevent="handleLyricSeek(item, index)"
                @keydown.space.prevent="handleLyricSeek(item, index)"
              >
                {{ item && item.length > 1 ? item[1] : '' }}
              </div>
            </template>

            <div v-else class="no-lyric">
              <el-empty :description="emptyLyricDescription" :image-size="isMobile ? 80 : 100">
                <template #description>
                  <p class="empty-description">{{ emptyLyricDescription }}</p>
                </template>
              </el-empty>
            </div>
          </div>
        </div>
        <button
          v-if="showBackToCurrentButton"
          type="button"
          class="back-to-current-btn"
          @click="handleReturnToCurrentLyric"
        >
          返回当前播放位置
        </button>
      </div>
    </div>

    <div ref="commentSectionRef" class="comment-section music-panel">
      <h2 class="section-title">听友评论</h2>
      <comment :playId="songId" :type="0"></comment>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, computed, ref, watch, onMounted, nextTick, onUnmounted } from "vue";
import { useStore } from "vuex";
import { useRoute, useRouter } from "vue-router";
import { Picture, ArrowDownBold } from "@element-plus/icons-vue";
import Comment from "@/components/Comment.vue";
import { parseLyric } from "@/utils";
import { HttpManager } from "@/api";
import { ElMessage } from "element-plus";

export default defineComponent({
  components: {
    Comment,
    Picture,
    ArrowDownBold
  },
  setup() {
    const store = useStore();
    const route = useRoute();
    const router = useRouter();
    const lyricViewport = ref<HTMLElement | null>(null);
    const commentSectionRef = ref<HTMLElement | null>(null);
    const lyricLines = ref<HTMLElement[]>([]);
    const lyricScrollTimer = ref<number | null>(null);
    const lyricAutoScrollTimer = ref<number | null>(null);
    const isUserScrollingLyrics = ref(false);
    const isProgrammaticLyricScroll = ref(false);
    const isLyricDriftedFromCurrent = ref(false);
    const manualScrollHoldMs = 3000;
    
    // --- 响应式判断 ---
    const isMobile = ref(false);
    const checkMobile = () => {
      isMobile.value = window.innerWidth <= 768;
    };
    
    // --- 评分相关数据 ---
    const averageScore = ref(0); // 平均分 (0-5)
    const userScore = ref(0);    // 用户评分 (0-5)
    const isRated = ref(false);  // 是否已评分

    // 基础数据
    const songTags = ref(['流行', '华语']); 
    const songDescription = ref('');
    
    // 歌词相关
    const activeLyricIndex = ref(-1);
    // Vuex Getters
    const songId = computed(() => store.getters.songId);
    const lyric = computed(() => store.getters.lyric);
    const curTime = computed(() => store.getters.curTime);
    const songTitle = computed(() => store.getters.songTitle);
    const singerName = computed(() => store.getters.singerName);
    const songPic = computed(() => store.getters.songPic);
    const isPlay = computed(() => store.getters.isPlay);
    const userId = computed(() => store.getters.userId);
    const currentPlayList = computed(() => store.getters.currentPlayList || []);
    const currentPlayIndex = computed(() => Number(store.getters.currentPlayIndex ?? -1));
    const rawLyricText = computed(() => {
      return typeof lyric.value === "string" ? lyric.value.trim() : "";
    });
    const lyricPlainText = computed(() => {
      return rawLyricText.value
        .replace(/\[(?:ti|ar|al|by|offset):[^\]]*\]/gi, "")
        .replace(/\[\d+:\d+(?:\.\d+)?\]/g, "")
        .replace(/\s+/g, "");
    });
    const isPlaceholderLyric = computed(() => {
      if (!rawLyricText.value) {
        return false;
      }
      const plainText = lyricPlainText.value;
      return !plainText || plainText.includes("暂无歌词") || plainText.includes("纯音乐") || /^[?？�]+$/.test(plainText);
    });
    const emptyLyricDescription = computed(() => {
      if (isPlaceholderLyric.value) {
        return "纯音乐，暂无歌词";
      }
      return "暂无歌词或歌词格式不支持";
    });
    const showBackToCurrentButton = computed(
      () =>
        isUserScrollingLyrics.value &&
        isLyricDriftedFromCurrent.value &&
        activeLyricIndex.value >= 0 &&
        lyricArr.value.length > 0
    );

    const isUserAuthError = (message = "") => {
      return ["未登录", "没有权力", "没有权限", "权限不足"].some((keyword) => message.includes(keyword));
    };

    const clearExpiredUserState = (message: string) => {
      store.commit("setToken", false);
      store.commit("clearUserInfo");
      ElMessage.warning(message);
    };

    const handleDeleteScore = async () => {
      if (!userId.value || !songId.value || !userScore.value) {
        ElMessage.warning("缺少必要信息");
        return;
      }

      try {
        const result = (await HttpManager.deleteSongRank({
          songId: songId.value,
          consumerId: userId.value
        })) as any;

        if (!result?.success) {
          if (isUserAuthError(result?.message || "")) {
            clearExpiredUserState("登录状态已失效，请重新登录");
          } else {
            ElMessage.error(result?.message || "删除评分失败");
          }
          return;
        }

        ElMessage.success(result?.message || "删除成功");
        userScore.value = 0;
        isRated.value = false;
        getRank();
      } catch (error) {
        ElMessage.error("删除评分时发生错误");
      }
    };

    // --- 歌词处理 ---
    const lyricArr = computed(() => {
      try {
        const lrc = rawLyricText.value;
        if (!lrc || typeof lrc !== 'string') return [];
        if (isPlaceholderLyric.value) return [];
        const parsed = parseLyric(lrc);
        return Array.isArray(parsed) ? parsed.filter((item) => item && item[1]) : [];
      } catch (e) {
        return [];
      }
    });

    // --- 评分逻辑 ---
    async function getRank() {
      try {
        const result = (await HttpManager.getRankOfSongId(songId.value)) as any;
        const score = typeof result === 'number' ? result : (result.data || 0);
        averageScore.value = score / 2;
      } catch (error) {
        averageScore.value = 0;
      }
    }

    async function getUserRank() {
      if (!songId.value || !userId.value) {
        isRated.value = false;
        userScore.value = 0;
        return;
      }

      try {
        const result = (await HttpManager.getUserRankSong({
          songId: songId.value,
          consumerId: userId.value
        })) as any; 
        
        const score = (result && result.data !== undefined) ? result.data : result;

        if (score > 0) {
          userScore.value = parseFloat((score / 2).toFixed(1));
          isRated.value = true;
        } else {
          userScore.value = 0;
          isRated.value = false;
        }
      } catch (error) {
        userScore.value = 0;
        isRated.value = false;
      }
    }

    async function handlePushScore() {
      if (!userId.value) {
        ElMessage.warning("请先登录");
        userScore.value = 0;
        return;
      }
      
      if (isRated.value) return;

      try {
        const params = {
          songId: songId.value,
          consumerId: userId.value,
          score: userScore.value * 2
        };
        
        const result = (await HttpManager.setSongRank(params)) as any;
        
        if (result.code === 1 || result.success) { 
          ElMessage.success("评分成功");
          isRated.value = true;
          getRank(); 
        } else {
          if (isUserAuthError(result?.message || "")) {
            clearExpiredUserState("登录状态已失效，请重新登录");
          } else {
            ElMessage.error(result.message || "评分失败");
          }
          userScore.value = 0;
        }
      } catch (error) {
        ElMessage.error("评分发生错误");
        userScore.value = 0;
      }
    }

    // --- 歌词滚动逻辑 ---
    const findActiveLyricIndex = (currentTime: number) => {
      const entries = lyricArr.value;
      if (!entries.length) {
        return -1;
      }

      if (currentTime < entries[0][0]) {
        return -1;
      }

      let low = 0;
      let high = entries.length - 1;
      let candidate = entries.length - 1;

      while (low <= high) {
        const mid = Math.floor((low + high) / 2);
        if (entries[mid][0] <= currentTime) {
          candidate = mid;
          low = mid + 1;
        } else {
          high = mid - 1;
        }
      }

      return candidate;
    };

    const updateActiveLyric = () => {
      const nextIndex = findActiveLyricIndex(Number(curTime.value || 0));
      if (activeLyricIndex.value === nextIndex) {
        return;
      }

      activeLyricIndex.value = nextIndex;
      if (!isUserScrollingLyrics.value && nextIndex >= 0) {
        scrollToActiveLine(nextIndex);
      }
    };

    const scrollToActiveLine = (activeIndex: number, behavior: ScrollBehavior = "smooth") => {
      const viewport = lyricViewport.value;
      const targetLine = lyricLines.value[activeIndex];

      if (!viewport || !targetLine) return;

      const viewportHeightValue = viewport.clientHeight || 0;
      const targetTop = Math.max(0, targetLine.offsetTop - viewportHeightValue / 2 + targetLine.offsetHeight / 2);
      isProgrammaticLyricScroll.value = true;
      if (lyricAutoScrollTimer.value) {
        window.clearTimeout(lyricAutoScrollTimer.value);
      }
      viewport.scrollTo({ top: targetTop, behavior });
      lyricAutoScrollTimer.value = window.setTimeout(() => {
        isProgrammaticLyricScroll.value = false;
        updateDriftFromCurrent();
      }, behavior === "smooth" ? 480 : 120);
    };

    const updateDriftFromCurrent = () => {
      const viewport = lyricViewport.value;
      const activeLine = lyricLines.value[activeLyricIndex.value];

      if (!viewport || !activeLine || activeLyricIndex.value < 0) {
        isLyricDriftedFromCurrent.value = false;
        return;
      }

      const viewportCenter = viewport.scrollTop + viewport.clientHeight / 2;
      const activeCenter = activeLine.offsetTop + activeLine.offsetHeight / 2;
      const driftDistance = Math.abs(activeCenter - viewportCenter);
      const visibleSlack = Math.max(96, viewport.clientHeight * 0.18);
      isLyricDriftedFromCurrent.value = driftDistance > visibleSlack;
    };

    const isActive = (i) => i === activeLyricIndex.value;
    const isPassed = (i) => i < activeLyricIndex.value;
    const isFuture = (i) => i > activeLyricIndex.value;

    const setLyricLineRef = (el: Element | null, index: number) => {
      if (el instanceof HTMLElement) {
        lyricLines.value[index] = el;
      }
    };

    const hasTimestamp = (item: [number, string] | undefined) => {
      return Boolean(item && typeof item[0] === "number" && !Number.isNaN(item[0]));
    };

    const resetLyricScrollHold = () => {
      if (lyricScrollTimer.value) {
        window.clearTimeout(lyricScrollTimer.value);
      }

      lyricScrollTimer.value = window.setTimeout(() => {
        isUserScrollingLyrics.value = false;
        isLyricDriftedFromCurrent.value = false;
        if (activeLyricIndex.value >= 0) {
          scrollToActiveLine(activeLyricIndex.value);
        }
      }, manualScrollHoldMs);
    };

    const handleLyricViewportScroll = () => {
      if (isProgrammaticLyricScroll.value) {
        return;
      }
      isUserScrollingLyrics.value = true;
      updateDriftFromCurrent();
      resetLyricScrollHold();
    };

    const handleLyricSeek = (item: [number, string], index: number) => {
      if (!hasTimestamp(item)) return;

      activeLyricIndex.value = index;
      store.commit("setCurTime", item[0]);
      store.commit("setChangeTime", item[0]);
      store.commit("setIsPlay", true);

      if (typeof window !== "undefined") {
        window.dispatchEvent(new CustomEvent("music-player:reveal"));
      }

      isUserScrollingLyrics.value = false;
      isLyricDriftedFromCurrent.value = false;
      scrollToActiveLine(index);
    };

    const handleReturnToCurrentLyric = () => {
      isUserScrollingLyrics.value = false;
      isLyricDriftedFromCurrent.value = false;
      if (lyricScrollTimer.value) {
        window.clearTimeout(lyricScrollTimer.value);
        lyricScrollTimer.value = null;
      }
      if (activeLyricIndex.value >= 0) {
        scrollToActiveLine(activeLyricIndex.value);
      }
    };

    const resolveCurrentSongRow = () => {
      const playList = Array.isArray(currentPlayList.value) ? currentPlayList.value : [];
      const currentId = String(songId.value || "");
      const indexedRow = playList[currentPlayIndex.value];
      if (indexedRow && String(indexedRow.id) === currentId) {
        return indexedRow;
      }
      return playList.find((item: any) => String(item?.id) === currentId) || null;
    };

    const extractSingerId = (songRow: any) => {
      const singerId = Number(songRow?.singerId ?? songRow?.singer_id ?? 0);
      return singerId > 0 ? singerId : 0;
    };

    const handleOpenSingerDetail = async () => {
      if (!songId.value || !singerName.value) {
        return;
      }

      let singerId = extractSingerId(resolveCurrentSongRow());
      if (!singerId) {
        try {
          const result = (await HttpManager.getSongOfId(songId.value)) as any;
          const songRow = Array.isArray(result?.data) ? result.data[0] : result?.data;
          singerId = extractSingerId(songRow);
        } catch (error) {
          singerId = 0;
        }
      }

      if (!singerId) {
        ElMessage.warning("暂时无法定位歌手页面");
        return;
      }

      await router.push({ name: "singer-detail", params: { id: singerId } });
    };

    const scrollToCommentSection = async () => {
      if (route.query.focus !== "comment") {
        return;
      }
      await nextTick();
      commentSectionRef.value?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    };

    watch(curTime, updateActiveLyric);
    watch(lyricArr, async () => {
      await nextTick();
      if (!lyricArr.value.length) {
        activeLyricIndex.value = -1;
        lyricLines.value = [];
        isLyricDriftedFromCurrent.value = false;
        return;
      }
      lyricLines.value = [];
      updateActiveLyric();
    }, { immediate: true });
    
    watch(songId, async (val) => {
      if (val) {
        activeLyricIndex.value = 0;
        userScore.value = 0; 
        isRated.value = false;
        await Promise.all([getRank(), getUserRank()]);
        await nextTick();
        updateActiveLyric();
        updateDriftFromCurrent();
        await scrollToCommentSection();
      }
    });

    watch(() => route.query.focus, () => {
      void scrollToCommentSection();
    });

    onMounted(() => {
      checkMobile();
      window.addEventListener('resize', checkMobile);
      lyricViewport.value?.addEventListener("scroll", handleLyricViewportScroll, { passive: true });
      
      if (songId.value) {
        getRank();
        getUserRank();
        nextTick(() => {
          updateActiveLyric();
          void scrollToCommentSection();
        });
      }
    });

    onUnmounted(() => {
      window.removeEventListener('resize', checkMobile);
      lyricViewport.value?.removeEventListener("scroll", handleLyricViewportScroll);
      if (lyricScrollTimer.value) {
        window.clearTimeout(lyricScrollTimer.value);
      }
      if (lyricAutoScrollTimer.value) {
        window.clearTimeout(lyricAutoScrollTimer.value);
      }
    });

    return {
      lyricViewport,
      commentSectionRef,
      setLyricLineRef,
      handleLyricSeek,
      handleReturnToCurrentLyric,
      handleOpenSingerDetail,
      hasTimestamp,
      handleDeleteScore,
      attachImageUrl: HttpManager.attachImageUrl,
      // 基础数据
      songTitle, singerName, songPic, songTags, songDescription, isPlay, songId,
      // 歌词
      lyricArr, isActive, isPassed, isFuture, emptyLyricDescription, showBackToCurrentButton,
      // 评分
      averageScore, userScore, isRated, handlePushScore,
      // 响应式
      isMobile,
      // 图标
      Picture, ArrowDownBold
    };
  }
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

/* 基础样式 */
.song-page {
  position: relative;
  min-height: auto;
  padding-bottom: 180px;
  color: var(--app-text);
}

.song-background {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background-size: cover;
  background-position: center;
  filter: blur(42px) brightness(0.35);
  z-index: -1;
  opacity: 0.72;
}

.song-content {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 30px;
  
  @media (min-width: 1024px) {
    flex-direction: row;
    gap: 40px;
  }
}

/* 歌曲信息容器 */
.song-info-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  
  @media (min-width: 1024px) {
    flex: 0 0 350px;
  }
}

/* 移动端歌曲信息（只在移动端显示） */
.mobile-song-meta {
  text-align: center;
  margin-bottom: 15px;
  
  @media (min-width: 768px) {
    display: none;
  }
  
  .song-title {
    font-size: 20px;
    color: var(--app-title);
    margin-bottom: 5px;
    line-height: 1.3;
  }
  
  .song-artist {
    color: var(--app-text-muted);
    font-size: 14px;
  }
}

/* 封面和评分包装器 */
.cover-rating-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
  
  @media (min-width: 768px) {
    gap: 30px;
  }
  
  @media (min-width: 1024px) {
    flex-direction: row;
    flex-wrap: wrap;
  }
}

.song-cover-wrapper {
  @media (min-width: 1024px) {
    flex: 0 0 100%;
  }
}

/* 封面样式 */
.song-cover {
  width: 100%;
  max-width: 300px;
  margin: 0 auto;
  border-radius: 28px;
  aspect-ratio: 1/1;
  box-shadow: 0 28px 64px rgba(0, 0, 0, 0.34);
  display: block;
  
  @media (min-width: 768px) {
    max-width: 350px;
  }
  
  @media (min-width: 1024px) {
    max-width: 100%;
  }
}

/* 桌面端歌曲信息（只在桌面端显示） */
.desktop-song-meta {
  display: none;
  text-align: center;
  margin-top: 15px;
  
  @media (min-width: 768px) {
    display: block;
  }
  
  .song-title {
    font-size: 24px;
    color: var(--app-title);
    margin-bottom: 5px;
  }
  
  .song-artist {
    color: var(--app-text-muted);
  }
}

.song-artist-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  padding: 0;
  margin: 0 auto;
  background: transparent;
  color: var(--app-text-muted);
  cursor: pointer;
  transition: color 0.2s ease, transform 0.2s ease, text-shadow 0.2s ease;

  &:hover,
  &:focus-visible {
    color: var(--app-title);
    transform: translateY(-1px);
    text-shadow: 0 0 14px rgba(123, 225, 255, 0.18);
    outline: none;
  }
}

/* 评分卡片 */
.song-rating-card {
  background: var(--app-panel-bg);
  backdrop-filter: blur(18px);
  border-radius: 24px;
  border: 1px solid var(--app-panel-border);
  box-shadow: var(--app-panel-shadow-soft);
  padding: 20px;
  color: var(--app-text);
  display: flex;
  flex-direction: column;
  gap: 15px;
  
  @media (min-width: 1024px) {
    flex: 1;
    margin-top: 0;
  }
}

.rating-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}

.rating-label {
  font-size: 14px;
  color: var(--app-text-muted);
  margin-bottom: 5px;
  
  @media (min-width: 768px) {
    margin-bottom: 0;
  }
}

.rating-value {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.score-num {
  font-size: 20px;
  font-weight: bold;
  color: var(--app-accent-warm);
  
  @media (min-width: 768px) {
    font-size: 24px;
  }
}

.rating-divider {
  height: 1px;
  background-color: var(--app-divider);
  margin: 5px 0;
}

.rating-action {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  width: 100%;
  
  @media (min-width: 768px) {
    width: auto;
    align-items: flex-end;
  }
}

.rate-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: 10px;
  
  @media (min-width: 768px) {
    justify-content: flex-end;
    gap: 15px;
  }
}

.rate-text {
  font-size: 12px;
  color: var(--app-text-soft);
}

.delete-rating-btn {
  margin-left: auto;
  
  @media (min-width: 768px) {
    margin-left: 0;
  }
}

/* 歌词容器 */
.lyric-container {
  background: var(--app-panel-bg);
  border-radius: 28px;
  padding: 20px;
  height: 50vh;
  display: flex;
  flex-direction: column;
  flex: 1;
  position: relative;
  
  @media (min-width: 768px) {
    padding: 30px;
    height: 60vh;
  }
  
  @media (min-width: 1024px) {
    height: calc(100vh - 200px);
  }
}

.lyric-header {
  font-size: 16px;
  font-weight: bold;
  color: var(--app-title);
  margin-bottom: 15px;
  text-align: center;
  opacity: 0.8;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  
  @media (min-width: 768px) {
    font-size: 18px;
    flex-direction: row;
    justify-content: center;
  }
}

.lyric-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--app-text-soft);
  
  @media (min-width: 768px) {
    display: none;
  }
}

.lyric-viewport {
  flex: 1;
  overflow-y: auto;
  position: relative;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch; /* 移动端平滑滚动 */

    /* 隐藏滚动条 - Webkit浏览器 */
    &::-webkit-scrollbar {
    display: none;
  }
  
  /* 隐藏滚动条 - Firefox */
  scrollbar-width: none;
  
  /* 隐藏滚动条 - IE/Edge */
  -ms-overflow-style: none;
  


}

.lyric-content {
  padding: 10px 0;
}

.lyric-line {
  text-align: center;
  padding: 10px 0;
  color: var(--app-text-soft);
  transition: color 0.35s ease, opacity 0.35s ease, transform 0.35s ease, background-color 0.35s ease, text-shadow 0.35s ease;
  font-size: 14px;
  line-height: 1.5;
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  border: 0;
  background: transparent;
  
  @media (min-width: 768px) {
    font-size: 16px;
    padding: 12px 0;
    min-height: 50px;
  }
}

.lyric-line.interactive {
  cursor: pointer;
  border-radius: 18px;
}

.lyric-line.interactive:hover,
.lyric-line.interactive:focus-visible {
  color: var(--app-title);
  background: rgba(123, 225, 255, 0.08);
  outline: none;
}

.lyric-line.active {
  color: var(--app-title);
  font-size: 18px;
  font-weight: bold;
  transform: scale(1.035);
  text-shadow:
    0 0 12px rgba(123, 225, 255, 0.18),
    0 0 28px rgba(123, 225, 255, 0.08),
    0 2px 8px rgba(10, 20, 38, 0.12);
  background: linear-gradient(90deg, rgba(123, 225, 255, 0.03), rgba(123, 225, 255, 0.08), rgba(123, 225, 255, 0.03));
  
  @media (min-width: 768px) {
    font-size: 22px;
  }
}

.lyric-line.passed {
  opacity: 0.8;
}

.lyric-line.future {
  opacity: 0.4;
}

.back-to-current-btn {
  position: absolute;
  top: 18px;
  right: 18px;
  z-index: 2;
  border: 1px solid rgba(123, 225, 255, 0.16);
  background: rgba(255, 255, 255, 0.44);
  color: rgba(26, 40, 67, 0.82);
  border-radius: 999px;
  padding: 9px 14px;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 10px 24px rgba(8, 18, 36, 0.08);
  backdrop-filter: blur(10px);
  opacity: 0.88;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease, opacity 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    background: rgba(255, 255, 255, 0.58);
    box-shadow: 0 14px 28px rgba(8, 18, 36, 0.12);
    opacity: 1;
  }

  &:focus-visible {
    outline: 2px solid rgba(123, 225, 255, 0.35);
    outline-offset: 2px;
  }

  @media (max-width: 767px) {
    top: 62px;
    right: 14px;
    padding: 8px 12px;
    font-size: 12px;
  }
}

/* 评论区域 */
.comment-section {
  padding: 24px;
  margin-top: 30px;
  
  @media (min-width: 768px) {
    padding: 28px;
    margin-top: 40px;
  }
}

.section-title {
  color: var(--app-title);
  border-bottom: 1px solid var(--app-divider);
  padding-bottom: 10px;
  margin-bottom: 20px;
  font-size: 18px;
  
  @media (min-width: 768px) {
    font-size: 20px;
  }
}

/* 全局样式覆盖 */
:deep(.el-rate__icon) {
  font-size: 18px;
  
  @media (min-width: 768px) {
    font-size: 20px;
  }
}

:deep(.readonly-rate .el-rate__icon) {
  font-size: 14px;
  
  @media (min-width: 768px) {
    font-size: 16px;
  }
}

:deep(.el-rate__text) {
  color: var(--app-accent-warm) !important;
  font-size: 12px;
  
  @media (min-width: 768px) {
    font-size: 14px;
  }
}

/* 无歌词状态 */
.no-lyric {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  
  :deep(.el-empty__description p) {
    font-size: 14px;
    
    @media (min-width: 768px) {
      font-size: 16px;
    }
  }
}

.empty-description {
  color: var(--app-text-muted);
}
</style>


