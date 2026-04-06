<template>
  <div class="search-song">
    <div v-loading="loading" class="search-song-panel">
      <song-list v-if="currentSongList.length" :songList="currentSongList"></song-list>
      <el-empty v-else :description="emptyDescription" :image-size="96" class="search-empty" />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, watch, getCurrentInstance } from "vue";
import { useStore } from "vuex";
import SongList from "@/components/SongList.vue";
import { HttpManager } from "@/api";
import { useRoute } from "vue-router";

const VIEW_ALL_LIMIT = 200;

export default defineComponent({
  components: {
    SongList,
  },
  setup() {
    const { proxy } = getCurrentInstance();
    const store = useStore();
    const route = useRoute(); 

    const currentSongList = ref([]); // 存放的音乐
    const loading = ref(false);
    const searchWord = computed(() => store.getters.searchWord);
    const routeKeywords = computed(() => ((route.query.keywords as string) || "").trim());
    const activeKeywords = computed(() => routeKeywords.value || searchWord.value || "");
    const routeSource = computed(() => route.query.source as string | undefined);
    let latestRequestId = 0;
    const emptyDescription = computed(() => {
      if (loading.value) return "";
      if (routeSource.value === "recommend") return "暂时还没有可展示的推荐歌曲";
      if (routeSource.value === "hot") return "暂时还没有热门歌曲";
      return "没有找到相关歌曲，换个关键词试试";
    });

    function showWarning(message: string) {
      (proxy as any).$message({
        message,
        type: "warning",
      });
    }

    // 搜索音乐
    async function searchSong(value) {
      if (!value) {
        latestRequestId += 1;
        loading.value = false;
        currentSongList.value = [];
        return;
      }

      const requestId = ++latestRequestId;
      loading.value = true;
      currentSongList.value = [];
      try {
        const result = (await HttpManager.getSongOfSingerName(value)) as ResponseBody;
        if (requestId !== latestRequestId) {
          return;
        }
        if (!result.data.length) {
          currentSongList.value = [];
          showWarning("暂时没有相关歌曲");
        } else {
          currentSongList.value = result.data;
        }
      } finally {
        if (requestId === latestRequestId) {
          loading.value = false;
        }
      }
    }

    async function loadHotSongs() {
      const requestId = ++latestRequestId;
      loading.value = true;
      try {
        const result = (await HttpManager.getHotSongs(VIEW_ALL_LIMIT)) as ResponseBody;
        if (requestId !== latestRequestId) {
          return;
        }
        currentSongList.value = Array.isArray(result?.data) ? result.data : [];
      } finally {
        if (requestId === latestRequestId) {
          loading.value = false;
        }
      }
    }

    async function loadRecommendSongs() {
      const requestId = ++latestRequestId;
      loading.value = true;
      try {
        const result = (await HttpManager.getRecommendSongList(VIEW_ALL_LIMIT)) as ResponseBody;
        if (requestId !== latestRequestId) {
          return;
        }
        if (result?.success === false) {
          currentSongList.value = [];
          showWarning(result?.message || "登录后可查看推荐歌曲");
          return;
        }

        currentSongList.value = Array.isArray(result?.data) ? result.data : [];
      } finally {
        if (requestId === latestRequestId) {
          loading.value = false;
        }
      }
    }

    async function loadSongList() {
      if (routeSource.value === "hot") {
        await loadHotSongs();
        return;
      }

      if (routeSource.value === "recommend") {
        await loadRecommendSongs();
        return;
      }

      await searchSong(activeKeywords.value);
    }

    watch([activeKeywords, routeSource], () => {
      void loadSongList();
    }, { immediate: true });

    return {
      currentSongList,
      loading,
      emptyDescription,
    };
  },
});
</script>

<style lang="scss" scoped>
.search-song-panel {
  min-height: 420px;
}

.search-empty {
  padding-top: 32px;
}
</style>
