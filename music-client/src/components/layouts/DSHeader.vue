<template>
  <div class="nav-container">
    <!-- 左侧Logo和导航 -->
    <div class="nav-left">
      <div class="logo" @click="goPage()">
        <span class="logo-badge" aria-hidden="true">
          <span class="logo-icon">🎶</span>
        </span>
        <span class="logo-text">{{ musicName }}</span>
      </div>

      <!-- 主导航：PC显示 -->
      <yin-header-nav
        class="main-nav"
        :styleList="headerNavList"
        :activeName="activeNavName"
        @click="goPage"
      ></yin-header-nav>
    </div>

    <!-- 右侧功能区域 -->
    <div class="nav-right">
      <!-- 搜索框 -->
      <div ref="searchBoxRef" class="search-box">
        <button class="search-trigger" type="button" aria-label="搜索" @click="goSearch()">
          <svg viewBox="0 0 24 24" aria-hidden="true" class="search-trigger__icon">
            <circle cx="11" cy="11" r="6.5" />
            <path d="M16 16L21 21" />
          </svg>
        </button>
        <el-input
          placeholder="搜索音乐、歌手、专辑..."
          v-model="keywords"
          @keyup.enter="goSearch()"
          @focus="handleSearchFocus"
          clearable
        />
        <transition name="search-history-fade">
          <div
            v-if="showSearchHistory && filteredSearchHistory.length"
            class="search-history-panel"
          >
            <div class="search-history-head">
              <span>最近搜索</span>
            </div>
            <button
              v-for="item in filteredSearchHistory"
              :key="`${item.keyword}-${item.updatedAt}`"
              type="button"
              class="search-history-item"
              @mousedown.prevent
              @click="applyHistoryKeyword(item.keyword)"
            >
              <span class="search-history-item__keyword">{{ item.keyword }}</span>
              <span class="search-history-item__time">{{ formatHistoryTime(item.updatedAt) }}</span>
            </button>
          </div>
        </transition>
      </div>

      <!-- 暗黑模式切换 -->
      <div class="dark-mode-toggle" @click="toggleDarkMode">
        <el-tooltip :content="isDarkMode ? '切换到明亮模式' : '切换到暗黑模式'" placement="bottom">
          <el-icon :size="22" :color="isDarkMode ? '#FFD700' : '#606266'">
            <component :is="isDarkMode ? Sunny : Moon" />
          </el-icon>
        </el-tooltip>
      </div>

      <!-- 用户操作区域 -->
      <div class="user-actions">
        <yin-header-nav v-if="!token" :styleList="signList" :activeName="activeNavName" @click="goPage" class="auth-nav"></yin-header-nav>

        <el-dropdown v-if="token" trigger="click" class="user-dropdown">
          <div class="user-avatar">
            <el-avatar :size="36" :src="attachImageUrl(userPic)" />
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="(item, index) in menuList"
                :key="index"
                @click.stop="goMenuList(item.path)"
              >
                <el-icon v-if="item.icon" class="menu-item-icon">
                  <component :is="item.icon" />
                </el-icon>
                {{ item.name }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <!-- 汉堡菜单按钮：移动端显示 -->
      <div class="hamburger-menu" @click="toggleMobileNav">
        <span :class="{ active: showMobileNav }"></span>
        <span :class="{ active: showMobileNav }"></span>
        <span :class="{ active: showMobileNav }"></span>
      </div>
    </div>

    <!-- 移动端导航弹出层 -->
    <transition name="slide-down">
      <div v-if="showMobileNav" class="mobile-nav">
        <div
          v-for="item in headerNavList"
          :key="item.name"
          class="mobile-nav-item"
          @click="goPage(item.path, item.name); toggleMobileNav()"
        >
          {{ item.name }}
        </div>
      </div>
    </transition>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted, onBeforeUnmount, getCurrentInstance, markRaw, watch } from "vue";
import { Sunny, Moon, User, Setting, SwitchButton } from "@element-plus/icons-vue";
import { useStore } from "vuex";
import { useRoute } from "vue-router";
import YinHeaderNav from "./YinHeaderNav.vue";
import mixin from "@/mixins/mixin";
import { SIGNLIST, MENULIST, MUSICNAME, RouterName, NavName, HEADERNAVLIST } from "@/enums";
import { HttpManager } from "@/api";

const SEARCH_HISTORY_STORAGE_KEY = "light-music-search-history";
const SEARCH_HISTORY_LIMIT = 5;
const SEARCH_HISTORY_CLEAR_EVENT = "music-search-history:clear";

interface SearchHistoryItem {
  keyword: string;
  updatedAt: number;
}

export default defineComponent({
  components: { YinHeaderNav, Sunny, Moon },
  setup() {
    const { proxy } = getCurrentInstance();
    const store = useStore();
    const route = useRoute();
    const { changeIndex, routerManager } = mixin();

    const musicName = ref(MUSICNAME);
    const headerNavList = ref(HEADERNAVLIST);
    const signList = ref(SIGNLIST);
    const menuList = ref(
      MENULIST.map((item) => ({
        ...item,
        icon:
          item.name === "个人主页"
            ? markRaw(User)
            : item.name === "设置"
              ? markRaw(Setting)
              : item.name === "退出"
                ? markRaw(SwitchButton)
                : null,
      }))
    );
    const keywords = ref("");
    const searchBoxRef = ref<HTMLElement | null>(null);
    const showSearchHistory = ref(false);
    const searchHistory = ref<SearchHistoryItem[]>([]);
    const activeNavName = computed(() => store.getters.activeNavName);
    const userPic = computed(() => store.getters.userPic);
    const token = computed(() => store.getters.token);
    const isDarkMode = ref(localStorage.getItem("darkMode") === "true");
    const showMobileNav = ref(false);
    const filteredSearchHistory = computed(() => {
      const historyItems = [...searchHistory.value]
        .sort((left, right) => right.updatedAt - left.updatedAt)
        .slice(0, SEARCH_HISTORY_LIMIT);
      const query = keywords.value.trim().toLowerCase();

      if (!query) {
        return historyItems;
      }

      return historyItems.filter((item) => item.keyword.toLowerCase().includes(query));
    });

    function loadSearchHistory() {
      try {
        const rawValue = window.localStorage.getItem(SEARCH_HISTORY_STORAGE_KEY);
        if (!rawValue) {
          searchHistory.value = [];
          return;
        }

        const parsedValue = JSON.parse(rawValue);
        if (!Array.isArray(parsedValue)) {
          searchHistory.value = [];
          return;
        }

        searchHistory.value = parsedValue
          .map((item) => ({
            keyword: typeof item?.keyword === "string" ? item.keyword.trim() : "",
            updatedAt: Number(item?.updatedAt) || 0,
          }))
          .filter((item) => item.keyword)
          .sort((left, right) => right.updatedAt - left.updatedAt)
          .slice(0, SEARCH_HISTORY_LIMIT);
      } catch (error) {
        void error;
        searchHistory.value = [];
      }
    }

    function persistSearchHistory() {
      try {
        window.localStorage.setItem(
          SEARCH_HISTORY_STORAGE_KEY,
          JSON.stringify(searchHistory.value.slice(0, SEARCH_HISTORY_LIMIT))
        );
      } catch (error) {
        void error;
      }
    }

    function saveSearchHistory(keyword: string) {
      const normalizedKeyword = keyword.trim();
      if (!normalizedKeyword) {
        return;
      }

      const nextHistory = searchHistory.value
        .filter((item) => item.keyword.toLowerCase() !== normalizedKeyword.toLowerCase());
      nextHistory.unshift({
        keyword: normalizedKeyword,
        updatedAt: Date.now(),
      });
      searchHistory.value = nextHistory.slice(0, SEARCH_HISTORY_LIMIT);
      persistSearchHistory();
    }

    function syncKeywordsFromRoute() {
      keywords.value = typeof route.query.keywords === "string" ? route.query.keywords : "";
    }

    function executeSearch(rawKeyword: string) {
      const nextKeyword = rawKeyword.trim();
      if (!nextKeyword) {
        (proxy as any).$message({ message: "搜索内容不能为空", type: "error" });
        return;
      }

      keywords.value = nextKeyword;
      showSearchHistory.value = false;
      proxy.$store.commit("setSearchWord", nextKeyword);
      saveSearchHistory(nextKeyword);
      routerManager(RouterName.Search, { path: RouterName.Search, query: { keywords: nextKeyword } });
    }

    function goPage(path?, name?) {
      if (!path && !name) {
        changeIndex(NavName.Home);
        routerManager(RouterName.Home, { path: RouterName.Home });
      } else {
        changeIndex(name);
        routerManager(path, { path });
      }
    }


    // 菜单列表退出登录
    function goMenuList(path) {
      if (path === RouterName.SignOut) {
        proxy.$store.commit("setToken", false);
        HttpManager.logout();
          (proxy as any).$message({ message: "退出成功", type: "success" });
          proxy.$store.commit("clearUserInfo");
              // 重置所有用户相关状态
        proxy.$store.commit("setToken", false);
        proxy.$store.commit("setUserId", null);
        proxy.$store.commit("setUsername", null);
        proxy.$store.commit("setUserPic", null);
        changeIndex(NavName.Home);
        routerManager(RouterName.Home, { path: RouterName.Home });
      
      } else {
        routerManager(path, { path });
      }
    }

    function goSearch() {
      executeSearch(keywords.value);
    }

    function handleSearchFocus() {
      showSearchHistory.value = true;
    }

    function applyHistoryKeyword(keyword: string) {
      executeSearch(keyword);
    }

    function formatHistoryTime(updatedAt: number) {
      if (!updatedAt) {
        return "";
      }

      const diff = Date.now() - updatedAt;
      if (diff < 60 * 1000) {
        return "刚刚";
      }
      if (diff < 60 * 60 * 1000) {
        return `${Math.max(1, Math.floor(diff / (60 * 1000)))} 分钟前`;
      }
      if (diff < 24 * 60 * 60 * 1000) {
        return `${Math.max(1, Math.floor(diff / (60 * 60 * 1000)))} 小时前`;
      }
      return `${Math.max(1, Math.floor(diff / (24 * 60 * 60 * 1000)))} 天前`;
    }

    function handleDocumentClick(event: MouseEvent) {
      const target = event.target;
      if (!(target instanceof Node)) {
        return;
      }
      if (searchBoxRef.value?.contains(target)) {
        return;
      }
      showSearchHistory.value = false;
    }

    function handleDarkModeEvent(event: Event) {
      const customEvent = event as CustomEvent<boolean>;
      const nextDarkMode = Boolean(customEvent.detail);
      isDarkMode.value = nextDarkMode;
      document.documentElement.classList.toggle("dark", nextDarkMode);
    }

    function handleSearchHistoryClear() {
      searchHistory.value = [];
      showSearchHistory.value = false;
      try {
        window.localStorage.removeItem(SEARCH_HISTORY_STORAGE_KEY);
      } catch (error) {
        void error;
      }
    }

    const toggleDarkMode = () => {
      isDarkMode.value = !isDarkMode.value;
      localStorage.setItem("darkMode", isDarkMode.value.toString());
      document.documentElement.classList.toggle("dark", isDarkMode.value);
      window.dispatchEvent(new CustomEvent('darkModeToggle', { detail: isDarkMode.value }));
    };

    const toggleMobileNav = () => { showMobileNav.value = !showMobileNav.value; };

    watch(
      () => route.query.keywords,
      () => {
        syncKeywordsFromRoute();
      },
      { immediate: true }
    );

    onMounted(() => {
      document.documentElement.classList.toggle("dark", isDarkMode.value);
      loadSearchHistory();
      document.addEventListener("click", handleDocumentClick);
      window.addEventListener("darkModeToggle", handleDarkModeEvent as EventListener);
      window.addEventListener(SEARCH_HISTORY_CLEAR_EVENT, handleSearchHistoryClear as EventListener);
    });

    onBeforeUnmount(() => {
      document.removeEventListener("click", handleDocumentClick);
      window.removeEventListener("darkModeToggle", handleDarkModeEvent as EventListener);
      window.removeEventListener(SEARCH_HISTORY_CLEAR_EVENT, handleSearchHistoryClear as EventListener);
    });

    return {
      musicName,
      headerNavList,
      signList,
      menuList,
      keywords,
      searchBoxRef,
      showSearchHistory,
      filteredSearchHistory,
      activeNavName,
      userPic,
      token,
      isDarkMode,
      Sunny,
      Moon,
      showMobileNav,
      goPage,
      goMenuList,
      goSearch,
      handleSearchFocus,
      applyHistoryKeyword,
      formatHistoryTime,
      toggleDarkMode,
      toggleMobileNav,
      attachImageUrl: HttpManager.attachImageUrl,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.nav-container {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  width: min(1440px, calc(100% - clamp(24px, 6vw, 72px)));
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  padding: 0 18px 0 18px;
  background: var(--app-header-bg);
  border: 1px solid var(--app-panel-border);
  box-shadow: var(--app-panel-shadow-soft);
  backdrop-filter: blur(28px);
  border-radius: 28px;
  z-index: 1000;
  transition: all 0.3s ease;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 30px;
  min-width: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.2s ease;
  flex-shrink: 0;

  &:hover {
    transform: translateY(-1px);
  }
}

.logo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--app-accent) 0%, var(--app-accent-strong) 65%, var(--app-accent-warm) 100%);
  box-shadow: 0 12px 28px color-mix(in srgb, var(--app-accent-strong) 30%, transparent);
  flex-shrink: 0;
}

.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  line-height: 1;
  filter: saturate(0.88);
}

.logo-text {
  font-size: 1.8rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: var(--app-title);
  background: linear-gradient(135deg, var(--app-accent) 0%, var(--app-accent-warm) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
}

.main-nav {
  min-width: 0;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  width: min(320px, 30vw);
}

.search-history-panel {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  background: color-mix(in srgb, var(--app-panel-bg-strong) 92%, white);
  border: 1px solid var(--app-panel-border);
  border-radius: 22px;
  box-shadow: var(--app-panel-shadow-soft);
  backdrop-filter: blur(22px);
  z-index: 1200;
}

.search-history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 4px 6px;
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.search-history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 11px 12px;
  border: 0;
  border-radius: 16px;
  background: transparent;
  color: var(--app-text);
  cursor: pointer;
  text-align: left;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.search-history-item:hover {
  background: var(--app-chip-bg-muted);
  transform: translateY(-1px);
}

.search-history-item__keyword {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.search-history-item__time {
  flex-shrink: 0;
  color: var(--app-text-muted);
  font-size: 12px;
}

.search-history-fade-enter-active,
.search-history-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.search-history-fade-enter-from,
.search-history-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.search-history-fade-enter-to,
.search-history-fade-leave-from {
  opacity: 1;
  transform: translateY(0);
}

.search-box :deep(.el-input__wrapper) {
  padding: 0 16px 0 46px;
  min-height: 44px;
  background: var(--app-chip-bg-muted);
  border-radius: 999px;
  box-shadow: none;
  border: 1px solid transparent;
}

.search-box :deep(.el-input__wrapper.is-focus) {
  border-color: var(--app-panel-border-strong);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--app-accent-strong) 12%, transparent);
}

.search-box :deep(.el-input__inner) {
  color: var(--app-text);
}

.search-trigger {
  position: absolute;
  left: 14px;
  top: 50%;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  transform: translateY(-50%);
  border: 1px solid color-mix(in srgb, var(--app-panel-border-strong) 50%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--app-panel-bg-strong) 88%, transparent);
  box-shadow: 0 6px 14px color-mix(in srgb, var(--app-accent-strong) 10%, transparent);
  color: color-mix(in srgb, var(--app-text) 75%, white);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    color: var(--app-text);
    background: color-mix(in srgb, var(--app-accent-strong) 16%, var(--app-panel-bg-strong));
    box-shadow: 0 8px 16px color-mix(in srgb, var(--app-accent-strong) 16%, transparent);
  }

  .search-trigger__icon {
    display: block;
    width: 18px;
    height: 18px;
    stroke: currentColor;
    fill: none;
    stroke-width: 2;
    stroke-linecap: round;
    stroke-linejoin: round;
  }
}

.dark-mode-toggle {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  cursor: pointer;
  transition: background-color 0.25s ease, transform 0.25s ease;
  background: var(--app-chip-bg-muted);

  &:hover {
    transform: translateY(-1px);
  }
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-nav {
  display: flex;
  gap: 8px;
}

.auth-nav :deep(li) {
  padding-inline: 14px;
  background: var(--app-chip-bg-muted);
}

.user-dropdown {
  cursor: pointer;
}

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--app-accent-strong), var(--app-accent-warm));
  box-shadow: 0 12px 28px color-mix(in srgb, var(--app-accent-warm) 22%, transparent);
  transition: transform 0.2s ease;

  &:hover {
    transform: translateY(-1px);
  }
}

.user-avatar :deep(.el-avatar) {
  border: 2px solid rgba(255, 255, 255, 0.72);
}

.menu-item-icon {
  margin-right: 8px;
  font-size: 16px;
}

.hamburger-menu {
  display: none;
  flex-direction: column;
  justify-content: space-between;
  width: 22px;
  height: 18px;
  cursor: pointer;
  margin-left: 6px;

  span {
    display: block;
    height: 3px;
    width: 100%;
    background: var(--app-title);
    border-radius: 2px;
    transition: all 0.3s;
  }

  span.active:nth-child(1) {
    transform: rotate(45deg) translate(3px, 3px);
  }

  span.active:nth-child(2) {
    opacity: 0;
  }

  span.active:nth-child(3) {
    transform: rotate(-45deg) translate(3px, -3px);
  }
}

.mobile-nav {
  position: absolute;
  top: calc(100% + 12px);
  left: 0;
  right: 0;
  background: var(--app-panel-bg-strong);
  border: 1px solid var(--app-panel-border);
  border-radius: 24px;
  z-index: 999;
  box-shadow: var(--app-panel-shadow-soft);
  padding: 10px;
}

.mobile-nav-item {
  padding: 12px 16px;
  border-radius: 16px;
  color: var(--app-text);
  cursor: pointer;

  &:hover {
    background: var(--app-chip-bg-muted);
  }
}

.slide-down-enter-active, .slide-down-leave-active { transition: all 0.3s ease; }
.slide-down-enter-from, .slide-down-leave-to { transform: translateY(-100%); opacity:0; }
.slide-down-enter-to, .slide-down-leave-from { transform: translateY(0); opacity:1; }

@media (max-width: 1100px) {
  .nav-container {
    gap: 18px;
    padding-inline: 14px;
  }

  .nav-left {
    gap: 18px;
  }

  .logo-text {
    font-size: 1.45rem;
  }

  .search-box {
    width: 220px;
  }
}

@media (max-width: 860px) {
  .main-nav {
    display: none;
  }

  .hamburger-menu {
    display: flex;
  }
}

@media (max-width: 640px) {
  .nav-container {
    top: 10px;
    width: calc(100% - 20px);
    height: 62px;
    gap: 12px;
  }

  .logo-text {
    font-size: 1.3rem;
  }

  .search-box {
    display: none;
  }
}
</style>
