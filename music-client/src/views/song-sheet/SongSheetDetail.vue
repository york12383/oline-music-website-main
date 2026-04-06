<template>
  <el-container>
    <el-aside class="album-slide">
      <el-image class="album-img" fit="contain" :src="coverUrl" />
      <h3 class="album-info">{{ songDetails.title }}</h3>
    </el-aside>
    <el-main class="album-main">
      <h1>简介</h1>
      <p>{{ songDetails.introduction || '歌单简介整理中。' }}</p>
      <div class="album-score">
        <div>
          <h3>歌单评分</h3>
          <el-rate v-model="rank" allow-half disabled></el-rate>
        </div>
        <span class="score-value">{{ rank * 2 }}</span>
        <div>
          <h3>{{ assistText }} {{ score * 2 }}</h3>
          <el-rate v-model="score" :disabled="disabledRank" @change="pushValue()"></el-rate>
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
        <button class="collection-btn" type="button" @click="toggleCollection" :disabled="collectionLoading">
          <yin-icon
            class="collection-icon"
            :class="{ active: isCollection }"
            :icon="isCollection ? iconList.like : iconList.dislike"
          ></yin-icon>
          <span>{{ isCollection ? "已收藏歌单" : "收藏歌单" }}</span>
        </button>
      </div>
      <song-list class="album-body" :songList="currentSongList"></song-list>
      <comment :playId="songListId" :type="1"></comment>
    </el-main>
  </el-container>
</template>

<script lang="ts">
import { computed, defineComponent, getCurrentInstance, nextTick, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { useStore } from "vuex";
import Comment from "@/components/Comment.vue";
import SongList from "@/components/SongList.vue";
import YinIcon from "@/components/layouts/YinIcon.vue";
import { HttpManager } from "@/api";
import mixin from "@/mixins/mixin";
import { Icon } from "@/enums";
import { getSongListCoverUrl } from "@/utils/song-list-cover";

export default defineComponent({
  components: {
    SongList,
    Comment,
    YinIcon,
  },
  setup() {
    const { proxy } = getCurrentInstance();
    const store = useStore();
    const route = useRoute();
    const { checkStatus } = mixin();

    const currentSongList = ref([]);
    const currentSongListId = ref(0);
    const currentScore = ref(0);
    const currentRank = ref(0);
    const disabledRank = ref(true);
    const assistText = ref("评价");
    const deleteLoading = ref(false);
    const collectionLoading = ref(false);
    const isCollection = ref(false);
    const songDetails = ref({
      id: 0,
      title: "",
      pic: "",
      introduction: "",
    });
    const iconList = {
      dislike: Icon.Dislike,
      like: Icon.Like,
    };

    const scrollToTop = async () => {
      if (typeof window === "undefined") {
        return;
      }
      await nextTick();
      window.scrollTo({ top: 0, left: 0, behavior: "auto" });
      requestAnimationFrame(() => {
        window.scrollTo({ top: 0, left: 0, behavior: "auto" });
      });
    };

    const userId = computed(() => store.getters.userId);
    const cachedSongDetails = computed(() => store.getters.songDetails);
    const showDeleteButton = computed(() => disabledRank.value && assistText.value === "已评价" && currentScore.value > 0);
    const songListId = computed(() => currentSongListId.value);
    const coverUrl = computed(() => getSongListCoverUrl(songDetails.value));
    async function loadSongListDetail() {
      const routeId = Number(route.params.id || cachedSongDetails.value?.id || 0);
      if (!routeId) {
        return;
      }

      currentSongListId.value = routeId;
      if (cachedSongDetails.value && Number(cachedSongDetails.value.id) === routeId) {
        songDetails.value = { ...cachedSongDetails.value };
        return;
      }

      const result = (await HttpManager.getSongListOfId(routeId)) as ResponseBody;
      const detail = result.data?.[0];
      if (detail) {
        songDetails.value = detail;
        store.commit("setSongDetails", detail);
      }
    }

    async function loadSongs(songListIdValue) {
      currentSongList.value = [];
      if (!songListIdValue) {
        return;
      }
      const result = (await HttpManager.getListSongOfSongId(songListIdValue)) as ResponseBody;
      const songRecords = result.data || [];
      const songs = await Promise.all(
        songRecords.map(async (item) => {
          const songResult = (await HttpManager.getSongOfId(item.songId)) as ResponseBody;
          return songResult.data?.[0];
        })
      );
      currentSongList.value = songs.filter(Boolean);
    }

    async function loadRank(songListIdValue) {
      if (!songListIdValue) {
        currentRank.value = 0;
        return;
      }
      const result = (await HttpManager.getRankOfSongListId(songListIdValue)) as ResponseBody;
      currentRank.value = Number(result.data || 0) / 2;
    }

    async function loadUserRank(userIdValue, songListIdValue) {
      if (!songListIdValue) {
        currentScore.value = 0;
        disabledRank.value = true;
        assistText.value = "评价";
        return;
      }

      if (!checkStatus(false)) {
        currentScore.value = 0;
        disabledRank.value = true;
        assistText.value = "请登录";
        return;
      }

      try {
        const result = (await HttpManager.getUserRank(userIdValue, songListIdValue)) as ResponseBody;
        const scoreValue = Number(result.data || 0);
        if (!scoreValue) {
          currentScore.value = 0;
          disabledRank.value = false;
          assistText.value = "评价";
        } else {
          currentScore.value = scoreValue / 2;
          disabledRank.value = true;
          assistText.value = "已评价";
        }
      } catch (error) {
        currentScore.value = 0;
        disabledRank.value = false;
        assistText.value = "评价";
      }
    }

    async function initCollection() {
      if (!songListId.value || !checkStatus(false)) {
        isCollection.value = false;
        return;
      }

      const result = (await HttpManager.isCollection({
        userId: userId.value,
        type: 1,
        songListId: songListId.value,
      })) as ResponseBody;
      isCollection.value = Boolean(result.data);
    }

    async function toggleCollection() {
      if (!checkStatus()) {
        return;
      }
      if (!songListId.value) {
        return;
      }

      try {
        collectionLoading.value = true;
        const result = isCollection.value
          ? ((await HttpManager.deleteCollection({
              userId: userId.value,
              type: 1,
              songListId: songListId.value,
            })) as ResponseBody)
          : ((await HttpManager.setCollection({
              userId: userId.value,
              type: 1,
              songListId: songListId.value,
            })) as ResponseBody);

        (proxy as any).$message({
          message: result.message,
          type: result.type,
        });

        if (result.data === true || result.data === false) {
          isCollection.value = result.data;
        }
      } finally {
        collectionLoading.value = false;
      }
    }

    async function pushValue() {
      if (disabledRank.value || !checkStatus()) {
        return;
      }

      try {
        const result = (await HttpManager.setRank({
          songListId: currentSongListId.value,
          consumerId: userId.value,
          score: currentScore.value * 2,
        })) as ResponseBody;

        (proxy as any).$message({
          message: result.message,
          type: result.type,
        });

        if (result.success) {
          await loadRank(currentSongListId.value);
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
        const result = (await HttpManager.deleteRankList({
          songListId: currentSongListId.value,
          consumerId: userId.value,
        })) as ResponseBody;

        (proxy as any).$message({
          message: result.message,
          type: result.type,
        });

        if (result.success) {
          currentScore.value = 0;
          disabledRank.value = false;
          assistText.value = "评价";
          await loadRank(currentSongListId.value);
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

    async function initializePage() {
      await scrollToTop();
      await loadSongListDetail();
      await Promise.all([
        loadSongs(currentSongListId.value),
        loadRank(currentSongListId.value),
        loadUserRank(userId.value, currentSongListId.value),
        initCollection(),
      ]);
    }

    onMounted(() => {
      initializePage();
    });

    watch(() => route.params.id, () => {
      initializePage();
    });

    watch(userId, () => {
      loadUserRank(userId.value, currentSongListId.value);
      initCollection();
    });

    return {
      songDetails,
      rank: currentRank,
      score: currentScore,
      disabledRank,
      assistText,
      currentSongList,
      songListId,
      showDeleteButton,
      deleteLoading,
      collectionLoading,
      isCollection,
      iconList,
      coverUrl,
      pushValue,
      deleteRank,
      toggleCollection,
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

  .album-score {
    display: flex;
    align-items: center;
    gap: 48px;
    margin: 1vw;

    h3 {
      margin: 10px 0;
    }

    .score-value {
      font-size: 60px;
    }

    .collection-btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      border: none;
      background: transparent;
      cursor: pointer;
      color: rgba(0, 0, 0, 0.7);
      padding: 8px 0;
      font-size: 14px;
      font-weight: 400;

      &:disabled {
        cursor: not-allowed;
        opacity: 0.7;
      }

      .collection-icon {
        width: 1.1em;
        height: 1.1em;
        min-width: 1.1em;
        color: rgba(0, 0, 0, 0.45);
        fill: currentColor;
        transition: color 0.2s ease;
      }

      .collection-icon.active {
        color: #ec4141;
      }
    }
  }
}
</style>
