<template>
  <div class="search-song-list">
    <div v-loading="loading" class="search-song-list-panel">
      <play-list v-if="playList.length" :playList="playList" path="song-sheet-detail"></play-list>
      <el-empty
        v-else
        class="search-empty"
        description="没有找到相关歌单，换个关键词试试"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, watch } from "vue";
import { useStore } from "vuex";
import PlayList from "@/components/PlayList.vue";
import { HttpManager } from "@/api";
import { useRoute } from "vue-router"; 

export default defineComponent({
  components: {
    PlayList,
  },
  setup() {
    const store = useStore();
    const route = useRoute();
    
    const playList = ref([]);
    const loading = ref(false);
    const searchWord = computed(() => store.getters.searchWord);
    const routeKeywords = computed(() => ((route.query.keywords as string) || "").trim());
    const activeKeywords = computed(() => routeKeywords.value || searchWord.value || "");
    let latestRequestId = 0;
    
    watch(activeKeywords, (value) => {
      getSearchList(value);
    }, { immediate: true });

    async function getSearchList(value) {
      if (!value) {
        latestRequestId += 1;
        loading.value = false;
        playList.value = [];
        return;
      }
      const requestId = ++latestRequestId;
      loading.value = true;
      playList.value = [];
      try {
        const result = (await HttpManager.getSongListOfLikeTitle(value)) as ResponseBody;
        if (requestId !== latestRequestId) {
          return;
        }
        playList.value = result.data || [];
      } finally {
        if (requestId === latestRequestId) {
          loading.value = false;
        }
      }
    }

    return {
      playList,
      loading,
    };
  },
});
</script>

<style lang="scss" scoped>
.search-song-list-panel {
  min-height: 420px;
}

.search-empty {
  padding-top: 32px;
}
</style>
