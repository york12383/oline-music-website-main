<template>
  <!-- 歌手个人页面 -->
  <el-container>
    <el-aside class="album-slide">
      <el-image class="singer-img" fit="contain" :src="attachImageUrl(singerDetails.pic)" />
      <div class="album-info">
        <h2>基本资料</h2>
        <ul>
          <li>性别：{{ singerDetails.sex === 2 ? "组合/合作" : (getUserSex(singerDetails.sex) || "待补充") }}</li>
          <li>{{ singerDetails.sex === 2 ? "成立" : "生日" }}：{{ getSingerBirthText() || "待补充" }}</li>
          <li>故乡：{{ singerDetails.location || "待补充" }}</li>
        </ul>
      </div>
    </el-aside>
    <el-main class="album-main">
      <h1>{{ singerDetails.name || "歌手资料整理中" }}</h1>
      <p>{{ singerDetails.introduction || "歌手资料整理中。" }}</p>
      <div class="song-list-header">
        <h2>歌曲列表</h2>
        <span class="song-list-total">共 {{ totalSongs }} 首 · 第 {{ currentPage }} 页</span>
      </div>
      <song-list :songList="currentSongList"></song-list>
      <el-pagination
        v-if="totalSongs > pageSize"
        class="pagination"
        background
        layout="total, prev, pager, next"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="totalSongs"
        @current-change="handleCurrentChange"
      />
    </el-main>
  </el-container>
</template>

<script lang="ts">
import { computed, defineComponent, nextTick, onMounted, ref, watch } from "vue";
import { useStore } from "vuex";
import { useRoute, useRouter } from "vue-router";
import mixin from "@/mixins/mixin";
import SongList from "@/components/SongList.vue";
import { HttpManager } from "@/api";
import { getBirth } from "@/utils";

export default defineComponent({
  components: {
    SongList,
  },
  setup() {
    const store = useStore();
    const route = useRoute();
    const router = useRouter();
    const { getUserSex } = mixin();

    const currentSongList = ref([]);
    const totalSongs = ref(0);
    const pageSize = 10;
    const currentPage = ref(1);
    const cachedSingerDetails = computed(() => store.getters.songDetails) as any;
    const routeSingerId = computed(() => Number(route.params.id || 0));
    const singerDetails = ref({
      id: 0,
      name: "",
      pic: "",
      sex: "",
      birth: "",
      location: "",
      introduction: "",
    });

    const normalizeQueryValue = (value?: string | string[]) => (Array.isArray(value) ? value[0] : value);
    const parsePositivePage = (value?: string | string[]) => {
      const page = Number(normalizeQueryValue(value));
      return Number.isInteger(page) && page > 0 ? page : 1;
    };
    const routePage = computed(() => parsePositivePage(route.query.page));

    const getSingerBirthText = () => {
      const birthText = getBirth(singerDetails.value.birth);
      if (!birthText) {
        return "";
      }
      if (Number(singerDetails.value.sex) === 2) {
        if (birthText.endsWith("-01-01")) {
          return birthText.slice(0, 4) + "年";
        }
        if (birthText.endsWith("-01")) {
          return birthText.slice(0, 7);
        }
      }
      return birthText;
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

    const hasMeaningfulSingerDetails = (detail: any) => {
      if (!detail || !detail.id) {
        return false;
      }
      return Boolean(
        detail.pic ||
        detail.introduction ||
        detail.location ||
        detail.birth ||
        detail.sex === 0 ||
        detail.sex === 1 ||
        detail.sex === 2
      );
    };

    const loadSingerDetail = async () => {
      const singerId = routeSingerId.value;
      if (!singerId) {
        singerDetails.value = {
          id: 0,
          name: "",
          pic: "",
          sex: "",
          birth: "",
          location: "",
          introduction: "",
        };
        return;
      }

      const cached = cachedSingerDetails.value;
      if (cached?.id && Number(cached.id) === singerId && hasMeaningfulSingerDetails(cached)) {
        singerDetails.value = { ...singerDetails.value, ...cached };
        return;
      }

      try {
        const result = (await HttpManager.getAllSinger()) as ResponseBody;
        const list = Array.isArray(result?.data) ? result.data : [];
        const matched = list.find((item: any) => Number(item?.id) === singerId);
        if (matched) {
          singerDetails.value = { ...singerDetails.value, ...matched };
          store.commit("setSongDetails", matched);
          return;
        }
      } catch (error) {
        if (cached?.id && Number(cached.id) === singerId) {
          singerDetails.value = { ...singerDetails.value, ...cached };
        }
      }

      singerDetails.value = {
        ...singerDetails.value,
        id: singerId,
        name: singerDetails.value.name || "歌手资料整理中",
      };
    };

    const loadSingerSongs = async () => {
      await scrollToTop();
      const singerId = singerDetails.value.id || routeSingerId.value;
      currentPage.value = routePage.value;
      if (!singerId) {
        currentSongList.value = [];
        totalSongs.value = 0;
        return;
      }
      try {
        const result = (await HttpManager.getSongOfSingerIdByPage(singerId, currentPage.value, pageSize)) as ResponseBody;
        const pageData = result?.data || {};
        currentSongList.value = Array.isArray(pageData.records) ? pageData.records : [];
        totalSongs.value = Number(pageData.total || 0);
      } catch (error) {
        currentSongList.value = [];
        totalSongs.value = 0;
      }
    };

    const handleCurrentChange = async (page: number) => {
      if (page === currentPage.value) {
        return;
      }
      await router.replace({
        path: route.path,
        query: {
          ...route.query,
          page: String(page),
        },
      });
    };

    const loadPage = async () => {
      await loadSingerDetail();
      await loadSingerSongs();
    };

    onMounted(() => {
      void loadPage();
    });

    watch(
      () => [route.params.id, route.query.page],
      () => {
        void loadPage();
      }
    );

    return {
      singerDetails,
      currentSongList,
      totalSongs,
      pageSize,
      currentPage,
      handleCurrentChange,
      attachImageUrl: HttpManager.attachImageUrl,
      getSingerBirthText,
      getBirth,
      getUserSex,
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

  .singer-img {
    height: 250px;
    width: 250px;
    border-radius: 10%;
  }

  .album-info {
    width: 60%;
    padding-top: 2rem;
    li {
      width: 100%;
      height: 30px;
      line-height: 30px;
    }
  }
}

.album-main {
  p {
    color: rgba(0, 0, 0, 0.5);
    margin: 10px 0 20px 0px;
  }

  .song-list-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;
  }

  .song-list-total {
    color: rgba(0, 0, 0, 0.45);
    font-size: 14px;
  }

  .pagination {
    margin-top: 20px;
    justify-content: center;
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
