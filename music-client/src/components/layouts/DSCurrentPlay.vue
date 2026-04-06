<template>
  <teleport to="body">
    <transition name="slide-fade">
      <div class="playlist-overlay" v-if="showAside" @click.self="closePlaylist">
        <div class="playlist-container" @click.stop>
          <div class="playlist-header">
            <h2 class="playlist-title">
              <el-icon class="title-icon"><List /></el-icon>
              当前播放
            </h2>
            <div class="header-actions">
              <div class="playlist-info">
                <span class="song-count">共 {{ currentPlayList.length || 0 }} 首</span>
                <el-button 
                  type="text" 
                  class="clear-btn"
                  @click="clearPlaylist"
                  :disabled="!currentPlayList.length"
                >
                  <el-icon><Delete /></el-icon>
                  清空列表
                </el-button>
              </div>
              <el-icon class="close-btn" @click="closePlaylist"><Close /></el-icon>
            </div>
          </div>
          
          <el-scrollbar class="playlist-scrollbar" height="calc(100% - 80px)">
            <ul class="song-list">
              <li
                v-for="(item, index) in currentPlayList"
                :key="item.id"
                :class="{ 'active': songId === item.id, 'playing': songId === item.id && isPlaying }"
                @click="handleSongClick(item, index)"
              >
                <div class="song-info">
                  <span class="song-index">{{ index + 1 }}</span>
                  <div class="song-details">
                    <span class="song-name">{{ getSongTitle(item.name) }}</span>
                    <span class="song-artist">{{ getArtistLabel(item) }}</span>
                  </div>
                </div>
                <div class="song-actions">
                  <el-icon class="action-icon" @click.stop="removeFromPlaylist(index)">
                    <Close />
                  </el-icon>
                </div>
              </li>
            </ul>
          </el-scrollbar>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script lang="ts">
import { defineComponent, getCurrentInstance, computed } from "vue";
import { useStore } from "vuex";
import mixin from "@/mixins/mixin";
import { List, Delete, Close } from "@element-plus/icons-vue";

export default defineComponent({
  components: {
    List,
    Delete,
    Close
  },
  setup() {
    const { proxy } = getCurrentInstance();
    const store = useStore();
    const { getSongTitle, getSingerName, playMusic } = mixin();

    const songId = computed(() => store.getters.songId);
    const currentPlayList = computed(() => store.getters.currentPlayList);
    const showAside = computed(() => store.getters.showAside);
    const isPlaying = computed(() => store.getters.isPlaying);

    const handleSongClick = (item: any, index: number) => {
      playMusic({
        id: item.id,
        url: item.url,
        pic: item.pic,
        index: index,
        name: item.name,
        lyric: item.lyric,
        currentSongList: currentPlayList.value,
      });
    };

    const getArtistLabel = (item: any) => {
      if (!item) return "未知歌手";

      const explicitSinger =
        item.singerName ||
        item.artist ||
        item.singer ||
        item.artistName;

      if (explicitSinger) {
        return explicitSinger;
      }

      if (typeof item.name === "string" && item.name.includes("-")) {
        const inferredSinger = getSingerName(item.name);
        if (inferredSinger) {
          return inferredSinger;
        }
      }

      return "未知歌手";
    };

    const clearPlaylist = () => {
      store.commit("clearCurrentPlayList");
    };

    const removeFromPlaylist = (index: number) => {
      store.commit("removeFromCurrentPlayList", index);
    };

    // 关闭播放列表
    const closePlaylist = () => {
      store.commit("setShowAside", false);
    };

    // 监听 ESC 键关闭
    const handleEscKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && showAside.value) {
        closePlaylist();
      }
    };

    // 添加键盘事件监听
    const addKeydownListener = () => {
      document.addEventListener('keydown', handleEscKey);
    };

    // 移除键盘事件监听
    const removeKeydownListener = () => {
      document.removeEventListener('keydown', handleEscKey);
    };

    // 组件挂载时添加事件监听
    addKeydownListener();

    return {
      songId,
      currentPlayList,
      showAside,
      isPlaying,
      getSongTitle,
      getArtistLabel,
      handleSongClick,
      clearPlaylist,
      removeFromPlaylist,
      closePlaylist,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

.playlist-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(3, 7, 18, 0.42);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.playlist-container {
  position: relative;
  width: min(360px, 100%);
  height: 100%;
  background: var(--app-panel-bg-strong);
  border-left: 1px solid var(--app-panel-border);
  box-shadow: -20px 0 60px rgba(0, 0, 0, 0.22);
  z-index: 1001;
  display: flex;
  flex-direction: column;
  transform: translateX(0);
}

.playlist-header {
  padding: 20px;
  border-bottom: 1px solid var(--app-divider);
}

.playlist-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--app-title);
  display: flex;
  align-items: center;
  
  .title-icon {
    margin-right: 8px;
    font-size: 20px;
    color: var(--el-color-primary);
  }
  
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.playlist-info {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: var(--app-text-muted);
  
  .song-count {
    margin-right: 15px;
  }
  
}

.clear-btn {
  padding: 0;
  color: var(--app-text-muted);
  
  &:hover {
    color: var(--el-color-primary);
  }
}

.close-btn {
  font-size: 20px;
  color: var(--app-text-soft);
  cursor: pointer;
  padding: 5px;
  border-radius: 50%;
  transition: all 0.3s;
  
  &:hover {
    color: var(--app-title);
    background-color: var(--app-chip-bg-muted);
  }
}

.playlist-scrollbar {
  flex: 1;
}

.song-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.song-list li {
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border-bottom: 1px solid var(--app-divider);
  
  &:hover {
    background-color: var(--app-table-row-hover);
    
    .song-actions {
      opacity: 1;
    }
  }
  
  &.active {
    background-color: var(--app-chip-bg-muted);
    color: var(--el-color-primary);
    
    .song-name {
      font-weight: 600;
    }
  }
  
  &.playing {
    position: relative;
    
    &::before {
      content: "";
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background-color: var(--el-color-primary);
    }
  }
  
}

.song-info {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.song-index {
  width: 24px;
  color: var(--app-text-soft);
  font-size: 14px;
  text-align: center;
  margin-right: 10px;
  
  .active & {
    color: var(--el-color-primary);
  }
  
}

.song-details {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.song-name {
  font-size: 14px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-artist {
  font-size: 12px;
  color: var(--app-text-muted);
  
  .active & {
    color: var(--el-color-primary-light-3);
  }
  
}

.song-actions {
  opacity: 0;
  transition: opacity 0.3s;
  
  .action-icon {
    color: #999;
    padding: 5px;
    border-radius: 50%;
    
    &:hover {
      color: var(--el-color-danger);
      background-color: var(--app-chip-bg-muted);
    }
  }
}

/* 过渡动画 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  .playlist-container {
    transform: translateX(100%);
  }
  .playlist-overlay {
    opacity: 0;
  }
}

.slide-fade-enter-to,
.slide-fade-leave-from {
  .playlist-container {
    transform: translateX(0);
  }
  .playlist-overlay {
    opacity: 1;
  }
}

@media (max-width: 480px) {
  .playlist-container {
    width: 100%;
  }
}
</style>
