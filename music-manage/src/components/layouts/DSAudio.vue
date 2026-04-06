<template>
  <div v-if="url" class="compact-player-shell" :class="{ playing: isPlay }">
    <audio
      ref="audioRef"
      :src="resolvedUrl"
      preload="true"
      @canplay="startPlay"
      @ended="ended"
      @timeupdate="syncProgress"
      @loadedmetadata="syncMeta"
      @play="handleNativePlay"
      @pause="handleNativePause"
      @volumechange="syncVolume"
    ></audio>

    <div class="player-track">
      <div class="player-cover">
        <el-icon><Headset /></el-icon>
      </div>
      <div class="player-meta">
        <div class="player-title">{{ displayTitle }}</div>
        <div class="player-subtitle">{{ displaySubtitle }}</div>
      </div>
    </div>

    <div class="player-controls">
      <button class="player-icon-button" :disabled="!url" @click="togglePlayState">
        <span v-if="isPlay" class="player-pause-glyph" aria-hidden="true">
          <i></i>
          <i></i>
        </span>
        <span v-else class="player-play-glyph" aria-hidden="true"></span>
      </button>
    </div>

    <div class="player-progress">
      <span>{{ formatTime(currentTime) }}</span>
      <input
        class="player-slider"
        type="range"
        min="0"
        :max="duration || 0"
        :value="currentTime"
        :disabled="!url || !duration"
        @input="handleSeek"
      />
      <span>{{ formatTime(duration) }}</span>
    </div>

    <div class="player-volume">
      <el-icon><MuteNotification /></el-icon>
      <input
        class="player-volume-slider"
        type="range"
        min="0"
        max="1"
        step="0.01"
        :value="volume"
        :disabled="!url"
        @input="handleVolumeChange"
      />
    </div>

    <el-dropdown trigger="click" @command="handleCommand">
      <button class="player-more" :disabled="!url">
        <el-icon><MoreFilled /></el-icon>
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="download">下载</el-dropdown-item>
          <el-dropdown-item command="rate-0.75">播放速度 0.75x</el-dropdown-item>
          <el-dropdown-item command="rate-1">播放速度 1.0x</el-dropdown-item>
          <el-dropdown-item command="rate-1.25">播放速度 1.25x</el-dropdown-item>
          <el-dropdown-item command="rate-1.5">播放速度 1.5x</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <button class="player-close" aria-label="关闭播放器" @click="closePlayer">
      <span></span>
      <span></span>
    </button>
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, ref, watch } from "vue";
import { useStore } from "vuex";
import { ElMessage } from "element-plus";
import { Headset, MuteNotification, MoreFilled } from "@element-plus/icons-vue";
import { HttpManager } from "@/api";

export default defineComponent({
  components: {
    Headset,
    MuteNotification,
    MoreFilled,
  },
  setup() {
    const store = useStore();
    const audioRef = ref<HTMLAudioElement>();
    const currentTime = ref(0);
    const duration = ref(0);
    const volume = ref(0.8);
    const playbackRate = ref(1);

    const url = computed(() => store.getters.url);
    const id = computed(() => store.getters.id);
    const isPlay = computed(() => store.getters.isPlay);
    const resolvedUrl = computed(() => (url.value ? HttpManager.attachImageUrl(url.value) : ""));
    const displayTitle = computed(() => {
      if (!url.value) {
        return "未选择试听歌曲";
      }

      const filename = decodeURIComponent(String(url.value).split("/").pop() || "");
      const cleaned = filename.replace(/\.[^/.]+$/, "");
      return cleaned || `曲目 #${id.value || "--"}`;
    });
    const displaySubtitle = computed(() => {
      if (!url.value) {
        return "全局播放器已就绪";
      }
      return `后台试听 · ${playbackLabel(playbackRate.value)}`;
    });

    watch(isPlay, () => {
      togglePlay();
    });

    watch(url, () => {
      currentTime.value = 0;
      duration.value = 0;
    });

    function togglePlay() {
      if (!audioRef.value || !url.value) {
        return;
      }

      if (isPlay.value) {
        audioRef.value.play().catch(() => {
          store.commit("setIsPlay", false);
        });
      } else {
        audioRef.value.pause();
      }
    }

    function togglePlayState() {
      if (!url.value) {
        return;
      }
      store.commit("setIsPlay", !isPlay.value);
    }

    function startPlay() {
      if (!audioRef.value) {
        return;
      }
      audioRef.value.volume = volume.value;
      if (isPlay.value) {
        audioRef.value.play().catch(() => {
          store.commit("setIsPlay", false);
        });
      }
    }

    function ended() {
      store.commit("setIsPlay", false);
      currentTime.value = 0;
    }

    function syncProgress() {
      if (!audioRef.value) {
        return;
      }
      currentTime.value = audioRef.value.currentTime;
    }

    function syncMeta() {
      if (!audioRef.value) {
        return;
      }
      duration.value = audioRef.value.duration || 0;
      volume.value = audioRef.value.volume;
      playbackRate.value = audioRef.value.playbackRate || 1;
    }

    function syncVolume() {
      if (!audioRef.value) {
        return;
      }
      volume.value = audioRef.value.volume;
    }

    function handleNativePlay() {
      if (!isPlay.value) {
        store.commit("setIsPlay", true);
      }
    }

    function handleNativePause() {
      if (isPlay.value && currentTime.value < duration.value) {
        store.commit("setIsPlay", false);
      }
    }

    function handleSeek(event: Event) {
      if (!audioRef.value) {
        return;
      }
      const nextTime = Number((event.target as HTMLInputElement).value);
      audioRef.value.currentTime = nextTime;
      currentTime.value = nextTime;
    }

    function handleVolumeChange(event: Event) {
      if (!audioRef.value) {
        return;
      }
      const nextVolume = Number((event.target as HTMLInputElement).value);
      audioRef.value.volume = nextVolume;
      volume.value = nextVolume;
    }

    async function handleDownload() {
      if (!resolvedUrl.value) {
        return;
      }
      try {
        const response = await fetch(resolvedUrl.value);
        if (!response.ok) {
          throw new Error(`download failed: ${response.status}`);
        }

        const audioBlob = await response.blob();
        const objectUrl = window.URL.createObjectURL(audioBlob);
        const link = document.createElement("a");
        const fileExtension = getFileExtension(resolvedUrl.value, audioBlob.type);

        link.href = objectUrl;
        link.download = `${displayTitle.value || "music"}${fileExtension}`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(objectUrl);
        ElMessage.success("歌曲下载已开始");
      } catch (error) {
        ElMessage.error("下载失败，请稍后重试");
      }
    }

    function setPlaybackRate(rate: number) {
      if (!audioRef.value) {
        return;
      }
      audioRef.value.playbackRate = rate;
      playbackRate.value = rate;
      ElMessage.success(`播放速度已切换为 ${playbackLabel(rate)}`);
    }

    function handleCommand(command: string) {
      if (command === "download") {
        handleDownload();
        return;
      }

      if (command.startsWith("rate-")) {
        const nextRate = Number(command.replace("rate-", ""));
        if (!Number.isNaN(nextRate)) {
          setPlaybackRate(nextRate);
        }
      }
    }

    function closePlayer() {
      if (audioRef.value) {
        audioRef.value.pause();
        audioRef.value.currentTime = 0;
      }
      currentTime.value = 0;
      duration.value = 0;
      playbackRate.value = 1;
      store.commit("setIsPlay", false);
      store.commit("setUrl", "");
      store.commit("setId", "");
    }

    function getFileExtension(fileUrl: string, mimeType: string) {
      const pathname = new URL(fileUrl, window.location.origin).pathname;
      const matched = pathname.match(/\.[a-zA-Z0-9]+$/);
      if (matched) {
        return matched[0];
      }

      if (mimeType.includes("mpeg")) {
        return ".mp3";
      }
      if (mimeType.includes("wav")) {
        return ".wav";
      }
      if (mimeType.includes("flac")) {
        return ".flac";
      }
      return ".mp3";
    }

    function formatTime(value: number) {
      if (!value || Number.isNaN(value)) {
        return "0:00";
      }

      const totalSeconds = Math.floor(value);
      const minutes = Math.floor(totalSeconds / 60);
      const seconds = totalSeconds % 60;
      return `${minutes}:${String(seconds).padStart(2, "0")}`;
    }

    function playbackLabel(rate: number) {
      return `${rate.toFixed(rate % 1 === 0 ? 0 : 2)}x`.replace(".00", "");
    }

    return {
      url,
      isPlay,
      resolvedUrl,
      audioRef,
      currentTime,
      duration,
      volume,
      displayTitle,
      displaySubtitle,
      togglePlayState,
      startPlay,
      ended,
      syncProgress,
      syncMeta,
      syncVolume,
      handleSeek,
      handleVolumeChange,
      handleCommand,
      closePlayer,
      formatTime,
    };
  },
});
</script>

<style scoped>
.compact-player-shell {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  width: 456px;
  min-height: 72px;
  padding: 12px 14px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(14, 23, 41, 0.96) 0%, rgba(21, 39, 69, 0.94) 100%);
  border: 1px solid rgba(120, 164, 231, 0.18);
  box-shadow: 0 20px 40px rgba(11, 20, 37, 0.28);
  color: #f8fbff;
  backdrop-filter: blur(16px);
}

.compact-player-shell.playing {
  box-shadow: 0 24px 48px rgba(13, 24, 46, 0.32);
}

.compact-player-shell audio {
  display: none;
}

.player-track {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1 1 165px;
}

.player-cover {
  display: flex;
  width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(41, 210, 228, 0.26) 0%, rgba(31, 107, 255, 0.36) 100%);
  color: #fff;
  font-size: 18px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.player-meta {
  min-width: 0;
}

.player-title,
.player-subtitle {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.player-title {
  font-size: 13px;
  font-weight: 800;
  color: #ffffff;
}

.player-subtitle {
  margin-top: 4px;
  font-size: 11px;
  color: rgba(222, 234, 255, 0.68);
}

.player-controls {
  display: flex;
  align-items: center;
}

.player-icon-button,
.player-more,
.player-close {
  display: inline-flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, opacity 0.2s ease;
}

.player-icon-button {
  position: relative;
  background: linear-gradient(135deg, rgba(70, 142, 255, 0.94) 0%, rgba(41, 210, 228, 0.9) 100%);
  box-shadow: 0 12px 22px rgba(30, 92, 189, 0.28);
}

.player-icon-button:hover:not(:disabled),
.player-more:hover:not(:disabled),
.player-close:hover {
  transform: translateY(-1px);
  background: rgba(41, 210, 228, 0.2);
}

.player-icon-button:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(84, 156, 255, 0.98) 0%, rgba(59, 218, 236, 0.94) 100%);
}

.player-icon-button:disabled,
.player-more:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.player-close {
  position: absolute;
  top: -12px;
  left: -12px;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  background: rgba(248, 251, 255, 0.98);
  box-shadow: 0 10px 20px rgba(13, 24, 46, 0.2);
}

.player-close span {
  position: absolute;
  width: 14px;
  height: 2px;
  border-radius: 999px;
  background: rgba(19, 32, 51, 0.82);
}

.player-close span:first-child {
  transform: rotate(45deg);
}

.player-close span:last-child {
  transform: rotate(-45deg);
}

.player-icon-button :deep(svg),
.player-more :deep(svg),
.player-cover :deep(svg),
.player-volume :deep(svg) {
  width: 18px;
  height: 18px;
}

.player-icon-button :deep(svg) {
  color: #ffffff;
}

.player-play-glyph {
  display: block;
  width: 0;
  height: 0;
  margin-left: 3px;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-left: 13px solid #ffffff;
}

.player-pause-glyph {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.player-pause-glyph i {
  display: block;
  width: 4px;
  height: 15px;
  border-radius: 999px;
  background: #ffffff;
}

.player-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 0 1 148px;
  color: rgba(222, 234, 255, 0.72);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.player-slider,
.player-volume-slider {
  width: 100%;
  margin: 0;
  height: 4px;
  appearance: none;
  background: rgba(155, 176, 214, 0.28);
  border-radius: 999px;
  outline: none;
}

.player-slider::-webkit-slider-thumb,
.player-volume-slider::-webkit-slider-thumb {
  width: 12px;
  height: 12px;
  appearance: none;
  border-radius: 50%;
  background: #f8fbff;
  box-shadow: 0 0 0 3px rgba(41, 210, 228, 0.16);
  cursor: pointer;
}

.player-volume {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 92px;
  color: rgba(222, 234, 255, 0.74);
}

.player-volume-slider {
  width: 58px;
}

@media (max-width: 1200px) {
  .compact-player-shell {
    width: 400px;
  }

  .player-volume {
    display: none;
  }
}
</style>
