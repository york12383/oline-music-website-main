<template>
  <div class="sidebar" :class="{ collapsed: collapse }">
    <el-menu
      class="sidebar-el-menu"
      background-color="transparent"
      text-color="#8ea2c6"
      active-text-color="#ffffff"
      :default-active="activeMenu"
      router
      :collapse="collapse"
    >
      <el-menu-item index="info">
        <!-- <el-icon><pie-chart /></el-icon> -->
        <el-icon><DataAnalysis /></el-icon>
        <span>系统首页</span>
      </el-menu-item>

      

      <el-menu-item index="banner">
        <el-icon><Postcard /></el-icon>
        <span>横幅管理</span>
      </el-menu-item>

      <el-menu-item index="consumer">
        <el-icon><User /></el-icon>
        <span>用户管理</span>
      </el-menu-item>

      <el-menu-item index="admin">
        <el-icon><UserFilled /></el-icon>
        <span> 管 理 员</span>
      </el-menu-item>

      <el-menu-item index="singer">
        <!-- <el-icon><mic /></el-icon> -->
        <el-icon><Microphone /></el-icon>
        <span>歌手管理</span>
      </el-menu-item>

      <el-menu-item index="songList">
        <!-- <el-icon><Document /></el-icon> -->
        <el-icon><Files /></el-icon>
        <span>歌单管理</span>
      </el-menu-item>
      
      <el-menu-item index="songs">
        <el-icon><Headset /></el-icon>
        <span>歌曲管理</span>
      </el-menu-item>

      <el-menu-item index="rank">
        <el-icon><Star /></el-icon>
        <span>评分管理</span>
      </el-menu-item>

      <el-menu-item index="feedback-manage">
        <el-icon><ChatDotRound /></el-icon>
        <span>反馈意见</span>
      </el-menu-item>

    </el-menu>
  </div>
</template>

<script  lang="ts" setup>
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import { User, Postcard, UserFilled, Star, DataAnalysis, Headset, Microphone, Files, ChatDotRound } from "@element-plus/icons-vue";
import emitter from "@/utils/emitter";

const collapse = ref(false);
const route = useRoute();

emitter.on("collapse", (msg) => {
  collapse.value = msg as boolean;
});

const activeMenu = computed(() => {
  const currentPath = route.path.toLowerCase();
  if (currentPath === "/comment") {
    return String(route.query.type) === "1" ? "songList" : "songs";
  }

  const routeMap: Record<string, string> = {
    "/info": "info",
    "/banner": "banner",
    "/consumer": "consumer",
    "/collect": "consumer",
    "/admin": "admin",
    "/singer": "singer",
    "/song": "singer",
    "/songlist": "songList",
    "/listsong": "songList",
    "/songs": "songs",
    "/rank": "rank",
    "/feedback-manage": "feedback-manage",
  };

  return routeMap[currentPath] || "info";
});
</script>

<style scoped>
.sidebar {
  display: block;
  position: absolute;
  left: 18px;
  top: 88px;
  bottom: 24px;
  width: 128px;
  overflow-y: auto;
  border-radius: 28px;
  border: 1px solid var(--admin-sidebar-border);
  background: var(--admin-sidebar-bg);
  box-shadow: 0 22px 38px rgba(10, 20, 39, 0.28);
  transition: width 0.3s ease-in-out, box-shadow 0.3s ease-in-out;
}

.sidebar.collapsed {
  width: 86px;
}

.sidebar::-webkit-scrollbar {
  width: 0;
}

.sidebar > ul {
  height: 100%;
  border-right: none;
  padding: 20px 10px;
}

.sidebar-el-menu:not(.el-menu--collapse) {
  width: 126px;
}

.sidebar-el-menu {
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.08);
  --el-menu-text-color: #8ea2c6;
  --el-menu-active-color: #ffffff;
}

.sidebar :deep(.el-menu-item) {
  position: relative;
  height: 52px;
  margin-bottom: 10px;
  border-radius: 18px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.sidebar :deep(.el-menu-item .el-icon) {
  font-size: 18px;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(41, 210, 228, 0.24) 0%, rgba(31, 107, 255, 0.45) 100%);
  box-shadow: inset 0 0 0 1px rgba(126, 191, 255, 0.22), 0 12px 24px rgba(10, 20, 39, 0.24);
}

.sidebar :deep(.el-menu-item.is-active)::before {
  content: "";
  position: absolute;
  left: 8px;
  top: 12px;
  bottom: 12px;
  width: 4px;
  border-radius: 999px;
  background: linear-gradient(180deg, #29d2e4 0%, #1f6bff 100%);
}

.sidebar :deep(.el-menu-item:hover) {
  color: #ffffff;
}

.sidebar.collapsed :deep(.el-menu-item) {
  justify-content: center;
  padding-left: 0 !important;
  padding-right: 0 !important;
}
</style>
