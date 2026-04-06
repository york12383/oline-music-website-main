<template>
    <el-container>
      <el-aside class="album-slide">
        <el-image class="album-img" fit="contain" :src="coverUrl" />
        <h3 class="album-info">{{ songDetails.title }}</h3>
      </el-aside>
      <el-main class="album-main">
        <h1>简介</h1>
        <p>{{ songDetails.introduction }}</p>
        <!--评分-->
        <div class="album-score">
          <div>
            <h3>歌单评分</h3>
            <el-rate v-model="rank" allow-half disabled></el-rate>
          </div>
          <span>{{ rank * 2 }}</span>
          <div>
            <h3>{{ assistText }} {{ score * 2 }}</h3>
            <el-rate v-model="score" :disabled="disabledRank" @change="pushValue()"></el-rate>
            <!-- 添加删除评分按钮 -->
            <el-button 
              v-if="showDeleteButton" 
              type="danger" 
              size="small" 
              @click="deleteRank" 
              :loading="deleteLoading"
              style="margin-top: 10px;"
            >
              删除评分
            </el-button>
          </div>
        </div>
        <!--歌曲-->
        <my-song-list class="album-body" :songList="currentSongList"></my-song-list>
        <comment :playId="songListId" :type="1"></comment>
      </el-main>
    </el-container>
  </template>
  
  <script lang="ts">
  import { defineComponent, ref, computed, getCurrentInstance, onMounted, onBeforeUnmount } from "vue";
  import { useStore } from "vuex";
  import mixin from "@/mixins/mixin";

  import MySongList from "./MySongList.vue";
  import Comment from "@/components/Comment.vue";
  import { HttpManager } from "@/api";
  import { getSongListCoverUrl } from "@/utils/song-list-cover";
  
  export default defineComponent({
    components: {
      Comment,
      MySongList,
    },
    setup() {
      const { proxy } = getCurrentInstance();
      const store = useStore();
      const { checkStatus } = mixin();
      const PLAYLIST_SONGS_CHANGED_EVENT = "music-playlist:songs-changed";
  
      const currentSongList = ref([]); // 存放的音乐
      const nowSongListId = ref(""); // 歌单 ID
      const nowScore = ref(0);
      const nowRank = ref(0);
      const disabledRank = ref(true);
      const assistText = ref("评价");
      const deleteLoading = ref(false); // 删除加载状态
      //const evaluateList = ref(["很差", "较差", "还行", "推荐", "力推"]);
      const songDetails = computed(() => store.getters.songDetails); // 单个歌单信息
      const nowUserId = computed(() => store.getters.userId);
      const coverUrl = computed(() => getSongListCoverUrl(songDetails.value || {}));
    
      // 计算是否显示删除按钮
      const showDeleteButton = computed(() => {
        return disabledRank.value && assistText.value === "已评价" && nowScore.value > 0;
      });
    
      nowSongListId.value = songDetails.value.id; // 给歌单ID赋值
    
      // 收集歌单里面的歌曲
      async function getSongId(id) {
        currentSongList.value = [];
        const result = (await HttpManager.getListSongOfSongId(id)) as ResponseBody;
        // 获取歌单里的歌曲信息
        for (const item of result.data) {
          // 获取单里的歌曲
          const resultSong = (await HttpManager.getSongOfId(item.songId)) as ResponseBody;
          currentSongList.value.push(resultSong.data[0]);
        }
      }

      function handlePlaylistSongsChanged(event: Event) {
        const customEvent = event as CustomEvent<{ playlistId?: string | number }>;
        const changedPlaylistId = String(customEvent.detail?.playlistId ?? "");
        if (!changedPlaylistId || changedPlaylistId !== String(nowSongListId.value)) {
          return;
        }
        getSongId(nowSongListId.value);
      }
      // 获取评分
      async function getRank(id) {
        const result = (await HttpManager.getRankOfSongListId(id)) as ResponseBody;
        nowRank.value = result.data / 2;
      }
  
      // 获取用户对歌单的评分
      async function getUserRank(userId, songListId) {
        if (!checkStatus()) {
          disabledRank.value = true;
          assistText.value = "请登录";
          return;
        }
        
        try {
          const result = (await HttpManager.getUserRank(userId, songListId)) as ResponseBody;
          // 修复 NaN 问题
          if (result.data === null || result.data === undefined || result.data === 0) {
            nowScore.value = 0;
            disabledRank.value = false; // 允许评价
            assistText.value = "评价";
          } else {
            // 确保是有效数字再进行除法运算
            const scoreValue = Number(result.data);
            nowScore.value = !isNaN(scoreValue) ? scoreValue / 2 : 0;
            disabledRank.value = true;
            assistText.value = "已评价";
          }
        } catch (error) {
          nowScore.value = 0;
          disabledRank.value = false;
          assistText.value = "评价";
        }
      }
      
      // 提交评分
      async function pushValue() {
        if (disabledRank.value || !checkStatus()) return;
  
        const songListId = nowSongListId.value;
        var consumerId = nowUserId.value;
        const score = nowScore.value * 2;
        try {
          const result = (await HttpManager.setRank({songListId,consumerId,score})) as ResponseBody;
          (proxy as any).$message({
            message: result.message,
            type: result.type,
          });
  
          if (result.success) {
            getRank(nowSongListId.value);
            disabledRank.value = true;
            assistText.value = "已评价";
          }
        } catch (error) {
          (proxy as any).$message({
            message: "提交评分失败，请稍后重试",
            type: "error",
          });
        }
      }
  
      // 删除评分
      async function deleteRank() {
        if (!checkStatus()) {
          (proxy as any).$message({
            message: "请先登录",
            type: "warning",
          });
          return;
        }
  
        try {
          deleteLoading.value = true;
          
          const songListId = nowSongListId.value;
          const consumerId = nowUserId.value;
          
          const result = (await HttpManager.deleteRankList({ songListId, consumerId })) as ResponseBody;
          
          (proxy as any).$message({
            message: result.message,
            type: result.type,
          });
  
          if (result.success) {
            // 重置评分状态
            nowScore.value = 0;
            disabledRank.value = false;
            assistText.value = "评价";
            
            // 重新获取平均评分
            getRank(nowSongListId.value);
          }
        } catch (error) {
          (proxy as any).$message({
            message: "删除评分失败",
            type: "error",
          });
        } finally {
          deleteLoading.value = false;
        }
      }
  
      onMounted(() => {
        window.addEventListener(PLAYLIST_SONGS_CHANGED_EVENT, handlePlaylistSongsChanged as EventListener);
        getUserRank(nowUserId.value, nowSongListId.value);
        getRank(nowSongListId.value); // 获取评分
        getSongId(nowSongListId.value); // 获取歌单里面的歌曲ID
      });

      onBeforeUnmount(() => {
        window.removeEventListener(PLAYLIST_SONGS_CHANGED_EVENT, handlePlaylistSongsChanged as EventListener);
      });
  
      return {
        songDetails,
        rank: nowRank,
        score: nowScore,
        disabledRank,
        assistText,
        currentSongList,
        songListId: nowSongListId,
        showDeleteButton,
        deleteLoading,
        coverUrl,
        pushValue,
        deleteRank,
      };
    },
  });
  </script>
  
  <style lang="scss" scoped>
  @import "@/assets/css/var.scss";
  
  .album-slide {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 20px;
  
    .album-img {
      height: 250px;
      width: 250px;
      border-radius: 10%;
    }
  
    .album-info {
      width: 70%;
      padding-top: 2rem;
    }
  }
  
  .album-main {
    h1 {
      font-size: 22px;
    }
  
    p {
      color: rgba(0, 0, 0, 0.5);
      margin: 10px 0 20px 0px;
    }
    /*歌单打分*/
    .album-score {
      display: flex;
      align-items: center;
      margin: 1vw;
  
      h3 {
        margin: 10px 0;
      }
      span {
        font-size: 60px;
      }
      & > div:last-child {
        margin-left: 10%;
        display: flex;
        flex-direction: column;
        align-items: flex-start;
      }
    }
  
    .album-body {
      margin: 20px 0 20px 0px;
    }
  }
  
  @media screen and (min-width: $sm) {
    .album-slide {
      position: fixed;
      width: 400px;
    }
    .album-main {
      min-width: 600px;
      padding-right: 10vw;
      margin-left: 400px;
    }
  }
  
  @media screen and (max-width: $sm) {
    .album-slide {
      display: none;
    }
  }
  </style>
