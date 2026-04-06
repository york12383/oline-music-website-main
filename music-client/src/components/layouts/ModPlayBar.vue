
<template>
  <!--播放进度栏-->
  <div
    class="play-bar"
    :class="{ show: !barVisible, 'hover-reveal': hoverRevealEnabled }"
    @mouseenter="handleBarMouseEnter"
    @mouseleave="handleBarMouseLeave"
  >

    <!-- 动画进度条 -->
    <div class="animated-progress" aria-hidden="true">
      <span class="animated-progress-track">
        <span class="animated-progress-runner"></span>
      </span>
    </div>

    <div v-if="!hoverRevealEnabled" class="fold" :class="{ turn: toggle }">
      <yin-icon :icon="iconList.ZHEDIE" @click="toggle = !toggle"></yin-icon>
      
    </div>

    <!--原始播放进度-->
    <el-slider class="progress" v-model="nowTime" @change="changeTime" size="small"></el-slider>

    <div class="control-box">
      <div class="info-box">
        <!--歌曲图片-->
        <div class="cover-trigger" @click="goPlayerPage">
          <el-image class="song-bar-img" fit="contain" :src="songPic ? attachImageUrl(songPic) : ''" />
        </div>
        <!--播放开始结束时间-->
        <div v-if="songId" class="song-meta">
          <div class="now-playing-label">Now Playing</div>
          <div class="song-info">{{ this.songTitle }} - {{ this.singerName }}</div>
          <div class="time-info">{{ startTime }} / {{ endTime }}</div>
        </div>
      </div>
      <div class="song-ctr transport-group">

        <button
          type="button"
          class="yin-play-show control-button play-mode-switch"
          :class="`mode-${currentPlayMode}`"
          :title="currentPlayModeLabel"
          @click="changePlayState"
        >
          <span class="mode-shell" :class="`shape-${currentPlayMode}`">
            <template v-if="isOncePlay">
              <span class="once-mode-glyph" aria-hidden="true">
                <span class="once-mode-number">1</span>
                <span class="once-mode-arrow"></span>
                <span class="once-mode-stop"></span>
              </span>
            </template>
            <template v-else>
              <yin-icon class="control-icon mode-icon" :icon="currentPlayStateIcon"></yin-icon>
            </template>
            <span v-if="isSingleLoop" class="mode-inline-badge">1</span>
          </span>
        </button>
        


        <!--上一首-->
        <button type="button" class="yin-play-show control-button transport-btn prev-btn" @click="prev">
          <yin-icon class="control-icon" :icon="iconList.SHANGYISHOU"></yin-icon>
        </button>
        

        <!--播放，播放按钮蓝色-->
        <button type="button" class="control-button play-btn" :class="{ playing: isPlay }" @click="togglePlay">
          <yin-icon class="play-btn-icon" :icon="playBtnIcon"></yin-icon>
        </button>


        <!--下一首-->
        <button type="button" class="yin-play-show control-button transport-btn next-btn" @click="next">
          <yin-icon class="control-icon" :icon="iconList.XIAYISHOU"></yin-icon>
        </button>
        

        <!--音量-->
        <el-dropdown class="yin-play-show control-dropdown" trigger="click">
          <button type="button" class="control-button transport-btn volume-btn">
            <yin-icon class="control-icon" :icon="volume !== 0 ? iconList.YINLIANG : iconList.JINGYIN"></yin-icon>
          </button>

          <template #dropdown>
            <el-dropdown-menu>
              <el-slider class="yin-slider" style="height: 150px; margin: 10px 0" v-model="volume"
                :vertical="true"></el-slider>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="song-ctr song-edit">
        <!--收藏-->
        <yin-icon class="yin-play-show" :class="{ active: isCollection }"
          :icon="isCollection ? iconList.like : iconList.dislike" @click="changeCollection"></yin-icon>

       
        <!--下载-->
        <yin-icon class="yin-play-show" :icon="iconList.download" @click="
            downloadMusic({
              songUrl,
              songName: singerName + '-' + songTitle,
            })
          "></yin-icon>



        <!--歌曲列表-->
        <yin-icon :icon="iconList.LIEBIAO" @click="changeAside"></yin-icon>
        

      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, getCurrentInstance, onBeforeUnmount, onMounted, ref, watch} from "vue";
import {mapGetters, useStore} from "vuex";
import mixin from "@/mixins/mixin";
import YinIcon from "./YinIcon.vue";
import {HttpManager} from "@/api";
import {formatSeconds} from "@/utils";
import {Icon, RouterName} from "@/enums";

const PLAYER_HIDE_EVENT = "music-player:hide";

export default defineComponent({
  components: {
    YinIcon,
  },
  setup() {
    const {proxy} = getCurrentInstance();
    const store = useStore();
    const {routerManager, playMusic, checkStatus, downloadMusic} = mixin();

    const isCollection = ref(false); // 是否收藏

    const userIdVO = computed(() => store.getters.userId);
    const songIdVO = computed(() => store.getters.songId);
    const token = computed(() => store.getters.token);
    const COLLECTION_CHANGED_EVENT = "music-collection:changed";

    // --- 新增：播放记录相关状态 ---
    const hasRecorded = ref(false); // 标记当前歌曲是否已记录
    const RECORD_THRESHOLD = 30;    // 记录阈值：30秒
    const curTime = computed(() => store.getters.curTime); // 获取当前播放时间

    watch(songIdVO, () => {
      hasRecorded.value = false;
      initCollection();
    });
    watch(token, (value) => {
      if (!value) isCollection.value = false;
    });


    watch(curTime, (newTime) => {
      // 1. 如果已经记录过，直接跳过
      if (hasRecorded.value) return;
      
      // 2. 检查是否满足记录条件：时间 > 15秒 且 用户已登录 且 有歌曲ID
      if (newTime > RECORD_THRESHOLD && userIdVO.value && songIdVO.value) {
        recordPlayHistory();
      }
    });


// --- 新增：记录播放历史的方法 ---
async function recordPlayHistory() {
    if (!checkStatus(false)) return;
      try {
        hasRecorded.value = true; // 立即标记为已记录，防止重复请求
      
        // 构建请求参数，对应你的 Java 实体类
        const params = {
          userId: userIdVO.value,
          songId: songIdVO.value,
          // playTime: 后端会自动生成 new Date()
          // duration: Math.floor(curTime.value) // 可选：记录触发时的时长
        };

        // 发送静默请求（不需要 await 阻塞后续逻辑，也不需要弹窗提示）
        await HttpManager.addPlayHistory(params); 
        
      } catch (error) {
        // 如果失败，可以选择是否重置 hasRecorded 允许重试，
        // 但为了避免网络问题导致频繁请求，建议这里不重置，这首歌这次就不记了。
      }
    }

    async function initCollection() {
      if (!checkStatus(false)) return;

      const userId = userIdVO.value;
      const type = '0';
      const songId = songIdVO.value;
      isCollection.value = ((await HttpManager.isCollection({userId, type, songId})) as ResponseBody).data;
    }

    async function changeCollection() {
      //检查登陆状态
      if (!checkStatus()) return;

      const userId = userIdVO.value;
      const type = '0'; //这里要看看 不能直接写死
      const songId = songIdVO.value;

      const result = isCollection.value
          ? ((await HttpManager.deleteCollection({ userId: userIdVO.value, type: 0, songId: songIdVO.value })) as ResponseBody)
          : ((await HttpManager.setCollection({userId, type, songId})) as ResponseBody);
      (proxy as any).$message({
        message: result.message,
        type: result.type,
      });

      if (result.data == true || result.data == false) {
        isCollection.value = result.data;
      } else {
        await initCollection();
      }

      emitCollectionChanged(songIdVO.value, isCollection.value);
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

      if (!songIdVO.value || changedSongId === undefined) {
        return;
      }

      if (String(changedSongId) !== String(songIdVO.value)) {
        return;
      }

      if (typeof customEvent.detail?.collected === "boolean") {
        isCollection.value = customEvent.detail.collected;
      } else {
        void initCollection();
      }
    }

    onMounted(() => {
      if (songIdVO.value) initCollection();
      window.addEventListener("keydown", handleKeyDown);
      window.addEventListener(COLLECTION_CHANGED_EVENT, handleCollectionChanged as EventListener);
    });

    onBeforeUnmount(() => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener(COLLECTION_CHANGED_EVENT, handleCollectionChanged as EventListener);
    });

  //空格键播放暂停音乐
  function handleKeyDown(event) {
 // 检查当前焦点是否在输入相关元素上
 const activeElement = document.activeElement;
  const isInputFocused = activeElement instanceof HTMLInputElement || 
                         activeElement instanceof HTMLTextAreaElement ||
                         activeElement?.hasAttribute('contenteditable');
  
  // 排除特定的CSS类名或元素
  const isExcludedElement = activeElement?.classList.contains('comment-input') ||
                            activeElement?.closest('.no-space-toggle');
  
  if (event.code === "Escape" && !isInputFocused) {
    window.dispatchEvent(new CustomEvent(PLAYER_HIDE_EVENT));
    return;
  }

  // 只有当焦点不在输入元素上且不在排除元素上时才触发播放/暂停
  if (event.code === "Space" && !isInputFocused && !isExcludedElement) {
    event.preventDefault(); // 阻止默认行为
    store.commit("setIsPlay", !store.getters.isPlay); // 切换播放状态
  }
  }



    return {
      isCollection,
      playMusic,
      routerManager,
      checkStatus,
      attachImageUrl: HttpManager.attachImageUrl,
      changeCollection,
      downloadMusic,
    };
  },
  data() {
    return {
      startTime: "00:00",
      endTime: "00:00",
      nowTime: 0, // 进度条的位置
      toggle: true,
      hoverRevealEnabled: false,
      isBarVisible: true,
      isHoveringBar: false,
      hideDelay: 5000,
      revealThreshold: 28,
      hideBarTimer: null as ReturnType<typeof setTimeout> | null,
      volume: 50,
      playStateList: [
        { key: "loop", icon: Icon.XUNHUAN },
        { key: "random", icon: Icon.LUANXU },
        { key: "single", icon: Icon.XUNHUAN },
        { key: "once", icon: Icon.XUNHUAN },
      ],
      playStateIndex: 0,
      iconList: {
        download: Icon.XIAZAI,
        ZHEDIE: Icon.ZHEDIE,
        SHANGYISHOU: Icon.SHANGYISHOU,
        XIAYISHOU: Icon.XIAYISHOU,
        YINLIANG: Icon.YINLIANG1,
        JINGYIN: Icon.JINGYIN,
        LIEBIAO: Icon.LIEBIAO,
        dislike: Icon.Dislike,
        like: Icon.Like,
      },
    };
  },
  computed: {
    ...mapGetters([
      "userId",
      "isPlay", // 播放状态
      "playBtnIcon", // 播放状态的图标
      "songId", // 音乐id
      "songUrl", // 音乐地址
      "songTitle", // 歌名
      "singerName", // 歌手名
      "songPic", // 歌曲图片
      "curTime", // 当前音乐的播放位置
      "duration", // 音乐时长
      "currentPlayList",
      "currentPlayIndex", // 当前歌曲在歌曲列表的位置
      "showAside", // 是否显示侧边栏
      "autoNext", // 用于触发自动播放下一首
    ]),
    barVisible() {
      return this.hoverRevealEnabled ? this.isBarVisible : this.toggle;
    },
    currentPlayMode() {
      return this.playStateList[this.playStateIndex]?.key || "loop";
    },
    currentPlayStateIcon() {
      return this.playStateList[this.playStateIndex]?.icon || Icon.XUNHUAN;
    },
    currentPlayModeLabel() {
      switch (this.currentPlayMode) {
        case "random":
          return "随机播放";
        case "single":
          return "单曲循环";
        case "once":
          return "顺序播放一次";
        default:
          return "列表循环";
      }
    },
    isSingleLoop() {
      return this.currentPlayMode === "single";
    },
    isOncePlay() {
      return this.currentPlayMode === "once";
    },
  },
  watch: {
    // 切换播放状态的图标
    isPlay(value) {
      this.$store.commit("setPlayBtnIcon", value ? Icon.ZANTING : Icon.BOFANG);
    },
    volume() {
      this.$store.commit("setVolume", this.volume / 100);
    },
    // 播放时间的开始和结束
    curTime() {
      this.startTime = formatSeconds(this.curTime);
      this.endTime = formatSeconds(this.duration);
      // 移动进度条
      this.nowTime = (this.curTime / this.duration) * 100;
    },
    // 自动播放下一首
    autoNext() {
      if (this.isSingleLoop) {
        this.replayCurrentSong();
      } else if (this.isOncePlay) {
        const playIndex = this.getSequentialPlayIndex(1, false);
        if (playIndex === -1) {
          this.stopPlaybackAfterListEnd();
        } else {
          this.playAtIndex(playIndex);
        }
      } else {
        this.next();
      }
    },
  },
  methods: {
    detectHoverReveal() {
      if (typeof window === "undefined" || !window.matchMedia) {
        this.hoverRevealEnabled = false;
        this.isBarVisible = true;
        return;
      }
      this.hoverRevealEnabled = window.matchMedia("(hover: hover) and (pointer: fine)").matches;
      this.isBarVisible = !this.hoverRevealEnabled;
    },
    clearHideTimer() {
      if (this.hideBarTimer) {
        clearTimeout(this.hideBarTimer);
        this.hideBarTimer = null;
      }
    },
    showBar() {
      if (!this.hoverRevealEnabled) return;
      this.clearHideTimer();
      this.isBarVisible = true;
    },
    scheduleHideBar() {
      if (!this.hoverRevealEnabled) return;
      if (this.isHoveringBar) return;
      this.clearHideTimer();
      this.hideBarTimer = setTimeout(() => {
        if (this.isHoveringBar) return;
        this.isBarVisible = false;
      }, this.hideDelay);
    },
    handleGlobalMouseMove(event: MouseEvent) {
      if (!this.hoverRevealEnabled) return;
      if (this.isHoveringBar) return;
      if (event.clientY >= window.innerHeight - this.revealThreshold) {
        this.showBar();
      } else {
        this.scheduleHideBar();
      }
    },
    handleBarMouseEnter() {
      this.isHoveringBar = true;
      this.showBar();
    },
    handleBarMouseLeave() {
      this.isHoveringBar = false;
      this.scheduleHideBar();
    },
    handleWindowLeave() {
      this.scheduleHideBar();
    },
    handleExternalReveal() {
      this.showBar();
      this.scheduleHideBar();
    },
    hideBarFromBackground() {
      if (!this.hoverRevealEnabled) return;
      this.clearHideTimer();
      this.isHoveringBar = false;
      this.isBarVisible = false;
    },
    handleExternalHide() {
      this.hideBarFromBackground();
    },
    handleViewportModeChange() {
      this.detectHoverReveal();
      this.clearHideTimer();
    },
    changeAside() {
      this.$store.commit("setShowAside", !this.showAside);
    },
    // 控制音乐播放 / 暂停
    togglePlay() {
      this.$store.commit("setIsPlay", this.isPlay ? false : true);
    },
    changeTime() {
      this.$store.commit("setChangeTime", this.duration * (this.nowTime * 0.01));
    },
    changePlayState() {
      this.playStateIndex = (this.playStateIndex + 1) % this.playStateList.length;
    },
    getSafeCurrentIndex() {
      const length = this.currentPlayList.length;
      if (!length) return -1;
      if (this.currentPlayIndex >= 0 && this.currentPlayIndex < length) return this.currentPlayIndex;
      return 0;
    },
    getRandomPlayIndex() {
      const length = this.currentPlayList.length;
      if (!length) return -1;
      if (length === 1) return 0;

      let playIndex = this.getSafeCurrentIndex();
      while (playIndex === this.getSafeCurrentIndex()) {
        playIndex = Math.floor(Math.random() * length);
      }
      return playIndex;
    },
    getSequentialPlayIndex(direction, allowWrap = true) {
      const length = this.currentPlayList.length;
      if (!length) return -1;

      const currentIndex = this.getSafeCurrentIndex();
      if (length === 1) return currentIndex;

      const targetIndex = currentIndex + direction;

      if (allowWrap) {
        return (targetIndex + length) % length;
      }

      if (targetIndex < 0 || targetIndex >= length) {
        return -1;
      }

      return targetIndex;
    },
    playAtIndex(index) {
      const song = this.currentPlayList[index];
      if (!song) return;

      if (song.id === this.songId && song.url === this.songUrl) {
        this.replayCurrentSong();
        return;
      }

      this.$store.commit("setCurrentPlayIndex", index);
      this.playMusic({
        id: song.id,
        url: song.url,
        pic: song.pic,
        index,
        name: song.name,
        lyric: song.lyric,
        currentSongList: this.currentPlayList,
      });
    },
    replayCurrentSong() {
      const currentIndex = this.getSafeCurrentIndex();
      if (currentIndex === -1) return;

      this.$store.commit("setChangeTime", 0);
      this.$store.commit("setCurTime", 0);
      this.$store.commit("setIsPlay", true);
    },
    stopPlaybackAfterListEnd() {
      this.$store.commit("setChangeTime", 0);
      this.$store.commit("setCurTime", 0);
      this.$store.commit("setIsPlay", false);
    },
    // 上一首
    prev() {
      if (!this.currentPlayList.length) return;

      const playIndex =
        this.currentPlayMode === "random"
          ? this.getRandomPlayIndex()
          : this.getSequentialPlayIndex(-1, this.currentPlayMode !== "once");

      if (playIndex !== -1) {
        this.playAtIndex(playIndex);
      }
    },
    // 下一首
    next() {
      if (!this.currentPlayList.length) return;

      const playIndex =
        this.currentPlayMode === "random"
          ? this.getRandomPlayIndex()
          : this.getSequentialPlayIndex(1, this.currentPlayMode !== "once");

      if (playIndex !== -1) {
        this.playAtIndex(playIndex);
      }
    },
    goPlayerPage() {
      this.routerManager(RouterName.Lyric, {path: `${RouterName.Lyric}/${this.songId}`});
    },
  },
  mounted() {
    this.detectHoverReveal();
    window.addEventListener("mousemove", this.handleGlobalMouseMove);
    window.addEventListener("mouseleave", this.handleWindowLeave);
    window.addEventListener("resize", this.handleViewportModeChange);
    window.addEventListener("music-player:reveal", this.handleExternalReveal);
    window.addEventListener(PLAYER_HIDE_EVENT, this.handleExternalHide);
  },
  beforeUnmount() {
    this.clearHideTimer();
    window.removeEventListener("mousemove", this.handleGlobalMouseMove);
    window.removeEventListener("mouseleave", this.handleWindowLeave);
    window.removeEventListener("resize", this.handleViewportModeChange);
    window.removeEventListener("music-player:reveal", this.handleExternalReveal);
    window.removeEventListener(PLAYER_HIDE_EVENT, this.handleExternalHide);
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/yin-play-bar.scss";

/* 进度条样式优化 */
.animated-progress {
  width: 100%;
  padding: 0 20px;
  position: relative;
  top: -8px; /* 调整位置 */
}

.animated-progress-track {
  position: relative;
  display: block;
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background-color: color-mix(in srgb, var(--app-player-divider) 82%, transparent);
  overflow: hidden;
}

.animated-progress-runner {
  position: absolute;
  top: 0;
  left: 0;
  width: 34%;
  height: 100%;
  border-radius: inherit;
  overflow: hidden;
  box-shadow: 0 0 10px rgba(106, 235, 184, 0.2);
  animation: player-progress-loop 4.8s linear infinite, player-progress-glow 5.8s ease-in-out infinite;
}

.animated-progress-runner::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(
    110deg,
    #72e3a0 0%,
    #7fe9d9 7%,
    #78cfff 14%,
    #9297ff 21%,
    #d18df3 28%,
    #f4a1c1 35%,
    #f3c78b 42%,
    #72e3a0 50%,
    #7fe9d9 57%,
    #78cfff 64%,
    #9297ff 71%,
    #d18df3 78%,
    #f4a1c1 85%,
    #f3c78b 92%,
    #72e3a0 100%
  );
  background-size: 200% 100%;
  background-position: 0% 50%;
  transform-origin: center center;
  animation:
    player-progress-breathe 4.8s ease-in-out infinite,
    player-progress-core-pulse 4.8s ease-in-out infinite,
    player-progress-inner-flow 7.4s linear infinite;
}

.animated-progress-runner::after {
  content: "";
  position: relative;
  display: block;
  width: 36%;
  height: 170%;
  margin-top: -2px;
  border-radius: inherit;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 0.08) 28%,
    rgba(255, 255, 255, 0.34) 50%,
    rgba(255, 255, 255, 0.08) 72%,
    rgba(255, 255, 255, 0) 100%
  );
  filter: blur(4px);
  opacity: 0.75;
  transform: translateX(-30%);
  animation: player-progress-sheen 4.8s linear infinite, player-progress-sheen-breathe 4.8s ease-in-out infinite;
}

@keyframes player-progress-loop {
  0% {
    transform: translateX(-115%);
  }
  100% {
    transform: translateX(325%);
  }
}

@keyframes player-progress-sheen {
  0% {
    transform: translateX(-35%);
    opacity: 0.08;
  }
  50% {
    transform: translateX(145%);
    opacity: 0.42;
  }
  100% {
    transform: translateX(255%);
    opacity: 0.08;
  }
}

@keyframes player-progress-breathe {
  0%,
  100% {
    filter: saturate(0.9) brightness(0.97);
    opacity: 0.7;
  }
  50% {
    filter: saturate(1) brightness(1.03);
    opacity: 0.82;
  }
}

@keyframes player-progress-core-pulse {
  0%,
  100% {
    transform: scaleX(0.97) scaleY(0.9);
  }
  50% {
    transform: scaleX(1.01) scaleY(1.08);
  }
}

@keyframes player-progress-sheen-breathe {
  0%,
  100% {
    filter: blur(4px) brightness(0.97);
    opacity: 0.34;
  }
  50% {
    filter: blur(4.5px) brightness(1.05);
    opacity: 0.6;
  }
}

@keyframes player-progress-inner-flow {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 100% 50%;
  }
}

@keyframes player-progress-glow {
  0%,
  100% {
    box-shadow: 0 0 8px rgba(114, 227, 160, 0.18);
  }
  25% {
    box-shadow: 0 0 9px rgba(120, 207, 255, 0.2);
  }
  50% {
    box-shadow: 0 0 11px rgba(146, 151, 255, 0.24);
  }
  75% {
    box-shadow: 0 0 10px rgba(209, 141, 243, 0.22);
  }
}

@media (prefers-reduced-motion: reduce) {
  .animated-progress-runner {
    animation: none;
    box-shadow: 0 0 10px rgba(94, 220, 136, 0.24);
  }

  .animated-progress-runner::after {
    animation: none;
    opacity: 0;
  }

  .animated-progress-runner::before {
    animation: none;
    background: linear-gradient(110deg, #5edc88 0%, #7be7bf 100%);
    opacity: 0.92;
    filter: none;
    transform: none;
  }
}

/* 播放条整体样式调整 */
// .play-bar {
//   transition: all 0.3s ease;
//   background: linear-gradient(to right, #1a1a2e, #16213e);
//   box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.2);
// }

.cover-trigger {
  display: inline-flex;
}

.song-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  max-width: 280px;
}

.now-playing-label {
  text-transform: uppercase;
  letter-spacing: 0.22em;
  font-size: 10px;
  font-weight: 700;
  color: var(--app-text-soft);
}

.song-info {
  color: var(--app-title);
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300px;
  line-height: 1.35;
}

.time-info {
  color: var(--app-text-soft);
  font-size: 12px;
}

.transport-group {
  gap: 16px;
}

.control-button {
  appearance: none;
  border: none;
  outline: none;
  cursor: pointer;
  padding: 0;
  background: transparent;
}

.play-mode-switch,
.transport-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  min-width: 38px;
  height: 38px;
  padding: 0;
  border-radius: 50%;
  color: color-mix(in srgb, var(--app-text-soft) 86%, var(--app-title) 14%);
  transition: transform 0.22s ease, color 0.22s ease, opacity 0.22s ease;
  opacity: 0.92;
}

.play-mode-switch {
  width: 66px;
  min-width: 66px;
  height: 42px;
  flex: 0 0 66px;
  background: transparent;
  box-shadow: none;
}

.transport-btn:hover,
.volume-btn:hover {
  transform: translateY(-1px);
  color: var(--app-title);
  opacity: 1;
}

.play-mode-switch:hover {
  opacity: 1;
}

.play-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--app-accent);
  transition: transform 0.24s ease, color 0.24s ease, filter 0.24s ease, opacity 0.24s ease;
}

.play-btn:hover {
  transform: translateY(-1px) scale(1.03);
  filter: saturate(1.08) drop-shadow(0 0 12px color-mix(in srgb, var(--app-accent) 22%, transparent));
}

.control-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.control-icon :deep(svg) {
  width: 1.56rem !important;
  height: 1.56rem !important;
}

.mode-shell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  min-width: 40px;
  height: 40px;
  color: color-mix(in srgb, var(--app-text-soft) 82%, var(--app-title) 18%);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 8px 18px rgba(8, 18, 40, 0.12);
  transition: transform 0.22s ease, filter 0.22s ease, box-shadow 0.22s ease, color 0.22s ease,
    background 0.22s ease, border-color 0.22s ease;
}

.play-mode-switch:hover .mode-shell {
  transform: translateY(-1px);
  filter: brightness(1.05);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 12px 24px rgba(8, 18, 40, 0.16);
}

.mode-shell.shape-loop {
  border-radius: 50%;
  background: radial-gradient(circle at 30% 28%, rgba(136, 223, 255, 0.16), rgba(255, 255, 255, 0.03));
  border-color: rgba(136, 223, 255, 0.18);
}

.mode-shell.shape-loop::before {
  content: "";
  position: absolute;
  inset: 5px;
  border-radius: 50%;
  border: 1px solid rgba(136, 223, 255, 0.18);
  opacity: 0.9;
}

.mode-shell.shape-loop::after {
  content: "";
  position: absolute;
  inset: 10px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.12), transparent 72%);
  pointer-events: none;
}

.mode-shell.shape-random {
  border-radius: 14px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.04), rgba(181, 167, 255, 0.1));
  border-color: rgba(181, 167, 255, 0.2);
}

.mode-shell.shape-random::before {
  content: "";
  position: absolute;
  inset: 6px;
  border-radius: 10px;
  border: 1px solid rgba(181, 167, 255, 0.16);
  transform: rotate(-7deg);
}

.mode-shell.shape-random::after {
  content: "";
  position: absolute;
  inset: 9px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  transform: rotate(6deg);
}

.mode-shell.shape-single {
  min-width: 52px;
  padding: 0 12px 0 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(255, 202, 136, 0.14), rgba(255, 255, 255, 0.03));
  border-color: rgba(255, 191, 116, 0.2);
}

.mode-shell.shape-single::before {
  content: "";
  position: absolute;
  inset: 4px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.07);
}

.mode-shell.shape-once {
  min-width: 58px;
  padding: 0 13px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(182, 236, 160, 0.14), rgba(255, 255, 255, 0.03));
  border-color: rgba(174, 233, 143, 0.2);
}

.mode-shell.shape-once::before {
  content: "";
  position: absolute;
  inset: 4px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.07);
}

.mode-icon {
  position: relative;
  z-index: 1;
}

.mode-shell.shape-random .mode-icon {
  transform: translateX(1px);
}

.once-mode-glyph {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  line-height: 1;
}

.once-mode-number {
  font-size: 0.82rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: currentColor;
}

.once-mode-arrow {
  position: relative;
  width: 13px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
}

.once-mode-arrow::after {
  content: "";
  position: absolute;
  right: -1px;
  top: 50%;
  width: 6px;
  height: 6px;
  border-top: 2px solid currentColor;
  border-right: 2px solid currentColor;
  transform: translateY(-50%) rotate(45deg);
}

.once-mode-stop {
  width: 2px;
  height: 13px;
  border-radius: 999px;
  background: currentColor;
}

.mode-inline-badge {
  position: absolute;
  right: 4px;
  bottom: 4px;
  min-width: 14px;
  height: 14px;
  padding: 0 3px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--app-player-bg) 88%, white 8%);
  font-size: 0.6rem;
  line-height: 1;
  font-weight: 800;
  color: rgba(255, 191, 116, 0.96);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.14);
  pointer-events: none;
}

.play-btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.play-btn-icon :deep(svg) {
  width: 3rem !important;
  height: 3rem !important;
}

.play-btn.playing {
  filter: drop-shadow(0 0 14px color-mix(in srgb, var(--app-accent) 24%, transparent));
}

.play-btn:not(.playing) .play-btn-icon {
  transform: translateX(1.5px);
}

.control-dropdown {
  display: inline-flex;
}

@media (max-width: 768px) {
  .transport-group {
    gap: 14px;
  }

  .play-mode-switch,
  .transport-btn {
    min-width: 34px;
    height: 34px;
    padding: 0;
  }

  .play-mode-switch {
    width: 58px;
    min-width: 58px;
    height: 38px;
    flex-basis: 58px;
  }

  .mode-shell {
    min-width: 38px;
    height: 38px;
  }

  .mode-shell.shape-single {
    min-width: 48px;
    padding: 0 11px 0 9px;
  }

  .mode-shell.shape-once {
    min-width: 54px;
    padding: 0 12px;
  }

  .once-mode-number {
    font-size: 0.76rem;
  }

  .once-mode-arrow {
    width: 12px;
  }

  .mode-inline-badge {
    right: 5px;
    bottom: 5px;
  }

  .control-icon :deep(svg) {
    width: 1.34rem !important;
    height: 1.34rem !important;
  }

  .play-btn-icon :deep(svg) {
    width: 2.58rem !important;
    height: 2.58rem !important;
  }
}
</style>
