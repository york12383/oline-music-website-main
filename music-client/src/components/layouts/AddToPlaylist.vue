<template>
    <el-dialog
      title="添加到歌单"
      v-model="visible"
      width="400px"
      :before-close="handleClose"
      append-to-body
    >
      <div class="playlist-container" v-loading="loading">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索我的歌单"
          prefix-icon="Search"
          clearable
          class="search-input"
        />
  
        <el-scrollbar height="300px">
          <ul v-if="filteredPlaylists.length > 0" class="playlist-list">
            <li
              v-for="item in filteredPlaylists"
              :key="item.id"
              class="playlist-item"
              @click="addToPlaylist(item.id)"
            >
              <el-image class="playlist-cover" :src="getSongListCoverUrl(item)" fit="cover" />
              <div class="playlist-info">
                <div class="playlist-name">{{ item.title }}</div>
                <div class="playlist-count">{{ item.style || '无风格' }}</div>
              </div>
              <el-icon class="add-icon"><Plus /></el-icon>
            </li>
          </ul>
          <el-empty v-else description="暂无创建的歌单" />
        </el-scrollbar>
      </div>
    </el-dialog>
  </template>
  
  <script lang="ts">
  import { defineComponent, ref, computed, watch, getCurrentInstance } from "vue";
  import { useStore } from "vuex";
  import { HttpManager } from "@/api";
  import { ElMessage } from "element-plus";
  import {  Plus } from "@element-plus/icons-vue";
  import { getSongListCoverUrl } from "@/utils/song-list-cover";
  
  export default defineComponent({
    name: "AddToPlaylist",
    components: {  Plus },
    props: {
      dialogVisible: {
        type: Boolean,
        default: false,
      },
      currentSongId: {
        type: [Number, String],
        required: true,
      },
    },
    emits: ["update:dialogVisible"],
    setup(props, { emit }) {
      const store = useStore();
      const { proxy } = getCurrentInstance();
      const userId = computed(() => store.getters.userId);
      
      const visible = ref(false);
      const loading = ref(false);
      const userPlaylists = ref([]); // 原始列表
      const searchKeyword = ref("");
  
      // 监听 props 的变化来控制弹窗显示
      watch(
        () => props.dialogVisible,
        (val) => {
          visible.value = val;
          if (val && userId.value) {
            getUserPlaylists();
          }
        }
      );
  
      // 搜索过滤
      const filteredPlaylists = computed(() => {
        if (!searchKeyword.value) return userPlaylists.value;
        return userPlaylists.value.filter((item: any) =>
          item.title.toLowerCase().includes(searchKeyword.value.toLowerCase())
        );
      });
  
      // 获取用户创建的歌单
      async function getUserPlaylists() {
        loading.value = true;
        try {
          const result = (await HttpManager.getSongListByConsumerId(userId.value)) as any;
          if (result.success) {
            userPlaylists.value = result.data;
          } else {
            userPlaylists.value = [];
          }
        } catch (error) {
          ElMessage.error("获取歌单列表失败，请稍后重试");
        } finally {
          loading.value = false;
        }
      }
  
      // 添加歌曲到指定歌单
      async function addToPlaylist(playlistId: number) {
        try {
          const params = {
            songId: props.currentSongId,
            songListId: playlistId,
          };
          
          // 调用之前后台管理用过的同一个接口 setListSong
          const result = (await HttpManager.setListSong(params)) as any;
          
          if (result.code === 1 || result.success) { // 根据实际后端返回判断
            ElMessage.success("添加成功");
            handleClose();
          } else {
            ElMessage.warning(result.message || "歌曲已存在或添加失败");
          }
        } catch (error) {
          ElMessage.error("添加失败，请稍后重试");
        }
      }
  
      function handleClose() {
        emit("update:dialogVisible", false);
      }
  
      return {
        visible,
        loading,
        searchKeyword,
        filteredPlaylists,
        //Search,
        handleClose,
        addToPlaylist,
        getSongListCoverUrl
      };
    },
  });
  </script>
  
  <style scoped lang="scss">
  @import "@/assets/css/var.scss";
  
  .playlist-container {
    padding: 0 10px;
  }
  
  .search-input {
    margin-bottom: 15px;
  }
  
  .playlist-list {
    list-style: none;
    padding: 0;
    margin: 0;
  }
  
  .playlist-item {
    display: flex;
    align-items: center;
    padding: 10px;
    border-radius: 8px;
    cursor: pointer;
    transition: background-color 0.2s;
  
    &:hover {
      background-color: #f5f7fa;
      
      .add-icon {
          opacity: 1;
          color: $color-blue;
      }
    }
  }
  
  .playlist-cover {
    width: 50px;
    height: 50px;
    border-radius: 4px;
    margin-right: 15px;
    flex-shrink: 0;
  }
  
  .playlist-info {
    flex: 1;
    overflow: hidden;
  }
  
  .playlist-name {
    font-size: 14px;
    color: #333;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .playlist-count {
    font-size: 12px;
    color: #999;
  }
  
  .add-icon {
      font-size: 20px;
      opacity: 0;
      transition: all 0.3s;
  }
  </style>
