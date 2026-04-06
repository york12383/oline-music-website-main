<template>
  <div class="music-page-shell collection-page" v-if="token">
    <aside class="collection-side music-panel">
      <el-image class="singer-img" fit="cover" :src="attachImageUrl(userPic)" />
      <div class="album-info">
        <span class="music-chip"><span class="music-dot"></span> Collection</span>
        <h2>我的收藏</h2>
        <ul>
          <li>用户名：{{ personalInfo.username || "未命名用户" }}</li>
          <li v-if="personalInfo.userSex">
            性别：
            <span v-if="personalInfo.userSex == '1'">男</span>
            <span v-else-if="personalInfo.userSex == '2'">女</span>
            <span v-else-if="personalInfo.userSex == '3'">其他</span>
            <span v-else>未知</span>
          </li>
          <li>生日：{{ personalInfo.birth || "未填写" }}</li>
          <li>故乡：{{ personalInfo.location || "未填写" }}</li>
        </ul>
      </div>
    </aside>

    <section class="collection-main music-panel">
      <div class="main-head">
        <div>
          <h1>{{ personalInfo.username || "我的收藏" }}</h1>
          <p>{{ personalInfo.introduction || "收藏的歌曲和歌单都会在这里统一管理。" }}</p>
        </div>
      </div>
      <el-tabs v-model="activeTab" class="collection-tabs">
        <el-tab-pane label="歌曲收藏" name="song">
          <song-list :songList="collectSongList" :show="true" @changeData="changeData"></song-list>
        </el-tab-pane>
        <el-tab-pane label="歌单收藏" name="songList">
          <play-list v-if="collectPlayList.length" :playList="collectPlayList" path="song-sheet-detail"></play-list>
          <el-empty v-else description="暂无收藏歌单"></el-empty>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>

  <div v-else class="music-page-shell collection-page">
    <section class="login-card music-panel">
      <p>请先 <a @click="goLogin">登录</a> 查看收藏内容</p>
    </section>
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, onBeforeMount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useStore } from "vuex";
import PlayList from "@/components/PlayList.vue";
import SongList from "@/components/SongList.vue";
import { RouterName } from "@/enums/router-name";
import { HttpManager } from "@/api";

export default defineComponent({
  components: {
    SongList,
    PlayList,
  },
  setup() {
    const store = useStore();
    const router = useRouter();
    const route = useRoute();
    const token = computed(() => store.getters.token);
    const userId = computed(() => store.getters.userId);
    const userPic = computed(() => store.getters.userPic);
    const activeTab = ref("song");
    const collectSongList = ref([]);
    const collectPlayList = ref([]);
    const personalInfo = reactive({
      username: "",
      userSex: "",
      birth: "",
      location: "",
      introduction: "",
    });

    const goLogin = () => {
      router.push({
        path: RouterName.SignIn,
        query: { redirect: RouterName.Collection },
      });
    };

    onBeforeMount(() => {
      if (!token.value) {
        goLogin();
      }
    });

    onMounted(async () => {
      syncActiveTabFromRoute();
      if (token.value && userId.value) {
        await initializePage();
      }
    });

    watch(
      () => route.query.tab,
      () => {
        syncActiveTabFromRoute();
      },
      { immediate: true }
    );

    watch([token, userId], ([nextToken, nextUserId]) => {
      if (nextToken && nextUserId) {
        initializePage();
      }
    });

    function syncActiveTabFromRoute() {
      const nextTab = route.query.tab;
      activeTab.value = nextTab === "songList" ? "songList" : "song";
    }

    async function initializePage() {
      await Promise.all([getUserInfo(userId.value), getCollection(userId.value)]);
    }

    async function getUserInfo(id) {
      const result = (await HttpManager.getUserOfId(id)) as ResponseBody;
      const user = result.data?.[0];
      if (!user) {
        return;
      }
      personalInfo.username = user.username;
      personalInfo.userSex = user.sex;
      personalInfo.birth = user.birth;
      personalInfo.introduction = user.introduction;
      personalInfo.location = user.location;
    }

    async function getCollection(currentUserId) {
      collectSongList.value = [];
      collectPlayList.value = [];

      const [songCollectResult, songListCollectResult] = await Promise.all([
        HttpManager.getCollectionOfUser(currentUserId, 0),
        HttpManager.getCollectionOfUser(currentUserId, 1),
      ]);

      const songCollects = (songCollectResult as ResponseBody).data || [];
      const songDetails = await Promise.all(
        songCollects
          .filter((item) => item.songId)
          .map(async (item) => {
            const songResult = (await HttpManager.getSongOfId(item.songId)) as ResponseBody;
            return songResult.data?.[0];
          })
      );
      collectSongList.value = songDetails.filter(Boolean);

      const songListCollects = (songListCollectResult as ResponseBody).data || [];
      const songListDetails = await Promise.all(
        songListCollects
          .filter((item) => item.songListId)
          .map(async (item) => {
            const detailResult = (await HttpManager.getSongListOfId(item.songListId)) as ResponseBody;
            return detailResult.data?.[0];
          })
      );
      collectPlayList.value = songListDetails.filter(Boolean);
    }

    function changeData() {
      getCollection(userId.value);
    }

    return {
      token,
      activeTab,
      goLogin,
      userPic,
      personalInfo,
      changeData,
      collectSongList,
      collectPlayList,
      attachImageUrl: HttpManager.attachImageUrl,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

.collection-page {
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  gap: 22px;
}

.collection-side,
.collection-main,
.login-card {
  padding: clamp(20px, 3vw, 30px);
}

.collection-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.singer-img {
  width: min(240px, 100%);
  aspect-ratio: 1 / 1;
  border-radius: 999px;
  overflow: hidden;
  border: 6px solid rgba(255, 255, 255, 0.36);
}

.album-info {
  width: 100%;

  h2 {
    margin: 16px 0 18px;
    font-size: 2rem;
    color: var(--app-title);
  }

  ul {
    padding: 0;
    margin: 0;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 10px;
    color: var(--app-text-muted);
  }
}

.collection-main {
  display: flex;
  flex-direction: column;
}

.main-head {
  margin-bottom: 16px;

  h1 {
    margin: 0;
    font-size: clamp(32px, 4vw, 52px);
    color: var(--app-title);
  }

  p {
    margin: 10px 0 0;
    color: var(--app-text-muted);
    line-height: 1.8;
  }
}

.collection-tabs {
  margin-top: 8px;
}

.login-card {
  text-align: center;
  color: var(--app-text);
}

.login-card a {
  color: var(--app-accent-strong);
  cursor: pointer;
}

@media (max-width: 992px) {
  .collection-page {
    grid-template-columns: 1fr;
  }
}
</style>
