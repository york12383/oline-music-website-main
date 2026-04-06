<template>
  <div class="video-player-container" ref="container">
    <!-- 视频元素 -->
    <video ref="video" :src="videoSrc" :poster="poster" @loadedmetadata="onLoadedMetadata" @timeupdate="onTimeUpdate"
      @ended="onEnded" @error="onError" @click="togglePlay" class="video-player"></video>

    <!-- 控制层 -->
    <div class="video-controls" :class="{ 'show-controls': showControls || isHovering }">
      <!-- 进度条 -->
      <div class="progress-container" @click="seek">
        <div class="progress-bar" :style="{ width: `${progress}%` }"></div>
        <div class="progress-buffer" :style="{ width: `${bufferProgress}%` }"></div>
        <div class="progress-handle" :style="{ left: `${progress}%` }" @mousedown.prevent="startDragging"></div>
      </div>

      <!-- 控制按钮组 -->
      <div class="controls-group">
        <!-- 播放/暂停按钮 -->
        <button class="control-btn play-pause-btn" @click.stop="togglePlay">
          <i class="fas" :class="isPlaying ? 'fa-pause' : 'fa-play'"></i>
        </button>

        <!-- 音量控制 -->
        <div class="volume-control" @mouseenter="showVolumeSlider = true" @mouseleave="showVolumeSlider = false">
          <button class="control-btn volume-btn" @click.stop="toggleMute">
            <i class="fas"
              :class="isMuted || volume === 0 ? 'fa-volume-mute' : volume < 0.5 ? 'fa-volume-down' : 'fa-volume-up'"></i>
          </button>
          <div class="volume-slider" v-if="showVolumeSlider">
            <input type="range" min="0" max="1" step="0.05" v-model="volume" @input="handleVolumeChange" @click.stop>
          </div>
        </div>

        <!-- 时间显示 -->
        <div class="time-display">
          {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
        </div>

        <!-- 播放速度 -->
        <div class="playback-rate" @click.stop>
          <button class="control-btn rate-btn">
            {{ playbackRate.toFixed(1) }}x
          </button>
          <div class="rate-options" v-if="showRateOptions">
            <button @click.stop="setPlaybackRate(0.5)">0.5x</button>
            <button @click.stop="setPlaybackRate(1)">1.0x</button>
            <button @click.stop="setPlaybackRate(1.5)">1.5x</button>
            <button @click.stop="setPlaybackRate(2)">2.0x</button>
          </div>
        </div>

        <!-- 全屏按钮 -->
        <button class="control-btn fullscreen-btn" @click.stop="toggleFullscreen">
          <i class="fas" :class="isFullscreen ? 'fa-compress' : 'fa-expand'"></i>
        </button>
      </div>
    </div>

    <!-- 加载中提示 -->
    <div class="loading-indicator" v-if="isLoading">
      <i class="fas fa-spinner fa-spin"></i>
    </div>

    <!-- 错误提示 -->
    <div class="error-message" v-if="hasError">
      <i class="fas fa-exclamation-circle"></i>
      <span>无法加载视频</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'VideoPlayer',
  props: {
    // 视频源URL
    videoSrc: {
      type: String,
      required: true
    },
    // 视频封面图
    poster: {
      type: String,
      default: ''
    },
    // 自动播放
    autoplay: {
      type: Boolean,
      default: false
    },
    // 循环播放
    loop: {
      type: Boolean,
      default: false
    },
    // 静音
    muted: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      // 视频元素引用
      video: null,
      container: null,

      // 播放状态
      isPlaying: false,
      isMuted: this.muted,
      volume: this.muted ? 0 : 1,
      currentTime: 0,
      duration: 0,
      progress: 0,
      bufferProgress: 0,
      playbackRate: 1,

      // UI状态
      showControls: false,
      isHovering: false,
      showVolumeSlider: false,
      showRateOptions: false,
      isFullscreen: false,
      isLoading: true,
      hasError: false,

      // 拖动状态
      isDragging: false,
      controlsTimer: null
    };
  },
  watch: {
    // 监听静音状态变化
    isMuted(newVal) {
      if (this.video) {
        this.video.muted = newVal;
        if (!newVal && this.volume === 0) {
          this.volume = 0.5;
        }
      }
    },
    // 监听音量变化
    volume(newVal) {
      if (this.video) {
        this.video.volume = newVal;
        this.isMuted = newVal === 0;
      }
    },
    // 监听播放速度变化
    playbackRate(newVal) {
      if (this.video) {
        this.video.playbackRate = newVal;
      }
    },
    // 监听自动播放属性变化
    autoplay(newVal) {
      if (newVal && this.video && !this.isPlaying) {
        this.play();
      }
    },
    // 监听循环属性变化
    loop(newVal) {
      if (this.video) {
        this.video.loop = newVal;
      }
    }
  },
  mounted() {
    // 获取DOM元素引用
    this.video = this.$refs.video;
    this.container = this.$refs.container;

    // 设置初始属性
    this.video.loop = this.loop;
    this.video.muted = this.muted;
    this.video.volume = this.volume;

    // 添加事件监听
    this.container.addEventListener('mousemove', this.handleMouseMove);
    document.addEventListener('mousemove', this.handleDrag);
    document.addEventListener('mouseup', this.stopDragging);
    document.addEventListener('fullscreenchange', this.handleFullscreenChange);
    document.addEventListener('webkitfullscreenchange', this.handleFullscreenChange);
    document.addEventListener('mozfullscreenchange', this.handleFullscreenChange);
    document.addEventListener('MSFullscreenChange', this.handleFullscreenChange);

    // 自动播放
    if (this.autoplay) {
      this.play();
    }
  },
  beforeUnmount() {
    // 移除事件监听
    this.container.removeEventListener('mousemove', this.handleMouseMove);
    document.removeEventListener('mousemove', this.handleDrag);
    document.removeEventListener('mouseup', this.stopDragging);
    document.removeEventListener('fullscreenchange', this.handleFullscreenChange);
    document.removeEventListener('webkitfullscreenchange', this.handleFullscreenChange);
    document.removeEventListener('mozfullscreenchange', this.handleFullscreenChange);
    document.removeEventListener('MSFullscreenChange', this.handleFullscreenChange);

    // 清除定时器
    if (this.controlsTimer) {
      clearTimeout(this.controlsTimer);
    }
  },
  methods: {
    // 视频加载完成
    onLoadedMetadata() {
      this.duration = this.video.duration;
      this.isLoading = false;
    },

    // 视频时间更新
    onTimeUpdate() {
      if (!this.isDragging) {
        this.currentTime = this.video.currentTime;
        this.progress = (this.currentTime / this.duration) * 100;
      }

      // 更新缓冲进度
      this.updateBufferProgress();
    },

    // 视频播放结束
    onEnded() {
      this.isPlaying = false;
      this.$emit('ended');
    },

    // 视频错误处理
    onError() {
      this.isLoading = false;
      this.hasError = true;
      this.$emit('error');
    },

    // 更新缓冲进度
    updateBufferProgress() {
      if (this.video.buffered.length > 0) {
        const bufferedEnd = this.video.buffered.end(this.video.buffered.length - 1);
        this.bufferProgress = (bufferedEnd / this.duration) * 100;
      }
    },

    // 播放/暂停切换
    togglePlay() {
      if (this.isPlaying) {
        this.pause();
      } else {
        this.play();
      }
    },

    // 播放视频
    play() {
      if (this.video) {
        this.video.play()
          .then(() => {
            this.isPlaying = true;
            this.$emit('play');
          })
          .catch(err => {
            this.hasError = true;
            this.$emit('error', err);
          });
      }
    },

    // 暂停视频
    pause() {
      if (this.video) {
        this.video.pause();
        this.isPlaying = false;
        this.$emit('pause');
      }
    },

    // 切换静音
    toggleMute() {
      this.isMuted = !this.isMuted;
    },

    // 处理音量变化
    handleVolumeChange() {
      this.$emit('volume-change', this.volume);
    },

    // 跳转播放位置
    seek(e) {
      if (this.video && this.duration) {
        const rect = this.$refs.container.getBoundingClientRect();
        const pos = (e.clientX - rect.left) / rect.width;
        this.currentTime = pos * this.duration;
        this.video.currentTime = this.currentTime;
        this.progress = pos * 100;
      }
    },

    // 开始拖动进度条
    startDragging() {
      this.isDragging = true;
      this.pause();
    },

    // 处理拖动
    handleDrag(e) {
      if (this.isDragging && this.duration) {
        const rect = this.$refs.container.getBoundingClientRect();
        let pos = (e.clientX - rect.left) / rect.width;
        pos = Math.max(0, Math.min(pos, 1)); // 限制在0-1范围内
        this.progress = pos * 100;
      }
    },

    // 停止拖动
    stopDragging() {
      if (this.isDragging) {
        this.isDragging = false;
        this.video.currentTime = (this.progress / 100) * this.duration;
        this.play();
      }
    },

    // 格式化时间（秒 -> MM:SS）
    formatTime(seconds) {
      if (isNaN(seconds)) return '00:00';
      const minutes = Math.floor(seconds / 60);
      const secs = Math.floor(seconds % 60);
      return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    },

    // 处理鼠标移动显示控制栏
    handleMouseMove() {
      this.isHovering = true;
      this.showControls = true;

      // 5秒后隐藏控制栏
      if (this.controlsTimer) {
        clearTimeout(this.controlsTimer);
      }

      this.controlsTimer = setTimeout(() => {
        if (this.isPlaying) {
          this.isHovering = false;
          this.showControls = false;
        }
      }, 5000);
    },

    // 切换全屏
    toggleFullscreen() {
      if (!document.fullscreenElement) {
        this.requestFullscreen();
      } else {
        this.exitFullscreen();
      }
    },

    // 请求全屏
    requestFullscreen() {
      if (this.container.requestFullscreen) {
        this.container.requestFullscreen();
      } else if (this.container.webkitRequestFullscreen) {
        this.container.webkitRequestFullscreen();
      } else if (this.container.msRequestFullscreen) {
        this.container.msRequestFullscreen();
      }
    },

    // 退出全屏
    exitFullscreen() {
      if (document.exitFullscreen) {
        document.exitFullscreen();
      } else if (document.webkitExitFullscreen) {
        document.webkitExitFullscreen();
      } else if (document.msExitFullscreen) {
        document.msExitFullscreen();
      }
    },

    // 处理全屏状态变化
    handleFullscreenChange() {
      this.isFullscreen = !!document.fullscreenElement;
    },

    // 显示/隐藏播放速度选项
    toggleRateOptions() {
      this.showRateOptions = !this.showRateOptions;
    },

    // 设置播放速度
    setPlaybackRate(rate) {
      this.playbackRate = rate;
      this.showRateOptions = false;
    }
  }
};
</script>

<style scoped>
.video-player-container {
  position: relative;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  background-color: #000;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

.video-player {
  width: 100%;
  height: auto;
  display: block;
}

.video-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8), transparent);
  color: white;
  padding: 1rem;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.video-controls.show-controls {
  opacity: 1;
}

.progress-container {
  position: relative;
  height: 4px;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  margin-bottom: 1rem;
  cursor: pointer;
}

.progress-buffer {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  z-index: 1;
}

.progress-bar {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background-color: #ff3e3e;
  border-radius: 2px;
  z-index: 2;
}

.progress-handle {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  background-color: white;
  border-radius: 50%;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.5);
  z-index: 3;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.progress-container:hover .progress-handle {
  opacity: 1;
}

.controls-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.control-btn {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 1rem;
  transition: color 0.2s ease;
  padding: 0.25rem;
}

.control-btn:hover {
  color: #ff3e3e;
}

.time-display {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
  min-width: 80px;
}

.volume-control {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.volume-slider {
  position: absolute;
  left: 0;
  bottom: 100%;
  margin-bottom: 0.5rem;
  width: 100px;
  background-color: rgba(0, 0, 0, 0.7);
  padding: 0.5rem;
  border-radius: 4px;
  display: none;
  z-index: 10;
}

.volume-control:hover .volume-slider {
  display: block;
}

.volume-slider input {
  width: 100%;
  accent-color: #ff3e3e;
}

.playback-rate {
  position: relative;
}

.rate-options {
  position: absolute;
  bottom: 100%;
  right: 0;
  margin-bottom: 0.5rem;
  background-color: rgba(0, 0, 0, 0.7);
  border-radius: 4px;
  overflow: hidden;
  z-index: 10;
  display: none;
}

.playback-rate:hover .rate-options {
  display: block;
}

.rate-options button {
  display: block;
  width: 100%;
  background: none;
  border: none;
  color: white;
  padding: 0.5rem 0.75rem;
  text-align: center;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.rate-options button:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.loading-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 2rem;
}

.error-message {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.25rem;
  padding: 1rem;
  background-color: rgba(255, 0, 0, 0.5);
  border-radius: 4px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .controls-group {
    gap: 0.5rem;
  }

  .time-display {
    font-size: 0.75rem;
    min-width: 70px;
  }

  .control-btn {
    font-size: 0.875rem;
  }

  .playback-rate {
    display: none;
  }
}
</style>
