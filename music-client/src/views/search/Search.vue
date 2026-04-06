<template>
  <div class="search">
    <yin-nav
      v-if="!songsOnlyMode"
      :styleList="searchNavList"
      :activeName="activeName"
      @click="handleChangeView"
    ></yin-nav>
    <component class="search-list" :is="currentView"></component>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import YinNav from "@/components/layouts/YinNav.vue";
import SearchSong from "./SearchSong.vue";
import SearchSongList from "./SearchSongList.vue";

export default defineComponent({
  components: {
    YinNav,
    SearchSong,
    SearchSongList,
  },
  data() {
    return {
      searchNavList: [
        {
          name: "歌曲",
          value: "SearchSong",
        },
        {
          name: "歌单",
          value: "SearchSongList",
        },
      ],
      activeName: "歌曲",
      currentView: "SearchSong",
    };
  },
  computed: {
    songsOnlyMode(this: any) {
      const source = this.$route.query.source;
      return source === "hot" || source === "recommend";
    },
  },
  watch: {
    "$route.query": {
      immediate: true,
      deep: true,
      handler(this: any, query) {
        this.syncViewWithRoute(query.tab, query.source);
      },
    },
  },
  methods: {
    syncViewWithRoute(this: any, tab, source) {
      if (source === "hot" || source === "recommend") {
        this.activeName = "歌曲";
        this.currentView = "SearchSong";

        if (tab === "song-list") {
          this.$router.replace({
            path: this.$route.path,
            query: {
              ...this.$route.query,
              tab: "song",
            },
          });
        }
        return;
      }

      if (tab === "song-list") {
        this.activeName = "歌单";
        this.currentView = "SearchSongList";
        return;
      }

      this.activeName = "歌曲";
      this.currentView = "SearchSong";
    },
    handleChangeView(this: any, item) {
      if (this.songsOnlyMode) {
        return;
      }

      this.activeName = item.name;
      this.currentView = item.value;
      const tab = item.value === "SearchSongList" ? "song-list" : "song";
      this.$router.replace({
        path: this.$route.path,
        query: {
          ...this.$route.query,
          tab,
        },
      });
    },
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.search {
  margin: auto;
  width: 900px;
  
  .search-list {
    min-height: 480px;
  }
}
</style>
