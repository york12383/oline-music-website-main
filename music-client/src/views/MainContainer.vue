<template>
  <el-container>
    <el-header>
      <d-s-header></d-s-header>
    </el-header>
    <el-main @click.capture="handleMainAreaClick">
      <router-view />
      <d-s-current-play></d-s-current-play>
      <mod-play-bar></mod-play-bar>
      <el-backtop :right="50" :bottom="100" />
      <yin-audio></yin-audio>
      <floating-assistant></floating-assistant>
    </el-main>
    <el-footer>
      <el-affix position="bottom"></el-affix>
      <act-footer></act-footer>
    </el-footer>
  </el-container>
</template>
<script lang="ts" setup>
import { getCurrentInstance, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import YinAudio from "@/components/layouts/YinAudio.vue";
import ActFooter from "@/components/layouts/ActFooter.vue";
import DSHeader from "@/components/layouts/DSHeader.vue";
import DSCurrentPlay from "@/components/layouts/DSCurrentPlay.vue";
import ModPlayBar from "@/components/layouts/ModPlayBar.vue";
import FloatingAssistant from "@/components/layouts/FloatingAssistant.vue";
import { HttpManager } from "@/api";
import { normalizeUserAvatarPath } from "@/constants/defaultAvatars";
const { proxy } = getCurrentInstance() as any;
const route = useRoute();
const PLAYER_HIDE_EVENT = "music-player:hide";
const INTERACTIVE_SELECTOR = [
  "a",
  "button",
  "input",
  "textarea",
  "select",
  "label",
  "[role='button']",
  "[role='link']",
  "[role='menuitem']",
  "[contenteditable='true']",
  ".play-bar",
  ".playlist-container",
  ".el-popper",
  ".el-dropdown-menu",
  ".el-button",
  ".el-slider",
  ".el-pagination",
  ".el-select",
  ".el-input",
  ".el-switch",
  ".el-checkbox",
  ".el-radio",
  ".el-backtop",
  ".music-assistant",
  ".assistant-trigger",
  ".assistant-drawer",
  "[data-assistant-interactive='true']",
].join(", ");
const INTERACTIVE_CLASS_HINTS = [
  "assistant",
  "btn",
  "button",
  "card",
  "item",
  "action",
  "trigger",
  "link",
  "icon",
  "menu",
  "dropdown",
  "slider",
  "switch",
  "checkbox",
  "radio",
  "pagination",
  "cover",
  "avatar",
  "image",
  "img",
  "song",
  "singer",
  "lyric",
  "playlist",
  "collection",
  "tab",
  "hero",
  "spotlight",
  "rank",
];
const savedState = sessionStorage.getItem("dataStore");
if (savedState) {
  proxy.$store.replaceState(Object.assign({}, proxy.$store.state, JSON.parse(savedState)));
  const restoredAvatar = proxy.$store.state?.user?.userPic;
  if (restoredAvatar) {
    proxy.$store.commit("setUserPic", normalizeUserAvatarPath(restoredAvatar));
  }
}
const clearLocalUserState = () => {
  proxy.$store.commit("setToken", false);
  proxy.$store.commit("clearUserInfo");
};
const syncSessionUser = async () => {
  const hasLocalLogin = Boolean(
    proxy.$store.getters.token || proxy.$store.getters.userId || proxy.$store.getters.username
  );
  if (!hasLocalLogin) {
    return;
  }
  try {
    const result = (await HttpManager.getSessionUser()) as ResponseBody;
    if (result?.success && result.data) {
      proxy.$store.commit("setUserId", result.data.id);
      proxy.$store.commit("setUsername", result.data.username);
      proxy.$store.commit("setUserPic", result.data.avator);
      proxy.$store.commit("setToken", true);
      return;
    }
  } catch (error) {
    // 网络波动时静默回退到本地状态清理，避免给用户制造技术噪音
  }
  clearLocalUserState();
  proxy.$message?.warning("Session expired, please sign in again");
};
const hasInteractiveClassHint = (target: Element) => {
  let current: Element | null = target;
  let depth = 0;
  while (current && depth < 5) {
    const className = typeof (current as HTMLElement).className === "string" ? (current as HTMLElement).className.toLowerCase() : "";
    if (INTERACTIVE_CLASS_HINTS.some((hint) => className.includes(hint))) {
      return true;
    }
    current = current.parentElement;
    depth += 1;
  }
  return false;
};
const handleMainAreaClick = (event: MouseEvent) => {
  const target = event.target;
  if (!(target instanceof Element)) {
    return;
  }
  if (target.closest(INTERACTIVE_SELECTOR) || hasInteractiveClassHint(target)) {
    return;
  }
  window.dispatchEvent(new CustomEvent(PLAYER_HIDE_EVENT));
};
onMounted(() => {
  syncSessionUser();
});
watch(
  () => route.fullPath,
  () => {
    syncSessionUser();
  }
);
window.addEventListener("beforeunload", () => {
  sessionStorage.setItem("dataStore", JSON.stringify(proxy.$store.state));
});
</script>
<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.el-footer {
  --el-footer-padding: 0;
  background: transparent;
}

.el-container {
  min-height: 100vh;
}

.el-header {
  padding: 0;
  height: auto;
  background: transparent;
}

.el-main {
  --el-main-padding: 88px 0 0;
  min-height: calc(100vh - 88px);
  padding-inline: 0;
  background: transparent;
  color: var(--app-text);
}

:deep(.el-backtop) {
  background: var(--app-panel-bg);
  border: 1px solid var(--app-panel-border);
  box-shadow: var(--app-panel-shadow-soft);
  color: var(--app-text);
}
</style>
