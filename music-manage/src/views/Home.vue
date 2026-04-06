<template>
  <div class="admin-shell">
    <d-s-header></d-s-header>
    <d-s-aside></d-s-aside>
    <main class="content-box" :class="{ 'content-collapse': collapse }">
      <router-view></router-view>
    </main>

    <div class="global-player-anchor" :class="{ collapsed: collapse }">
      <d-s-audio></d-s-audio>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import DSHeader from "@/components/layouts/DSHeader.vue";
import DSAside from "@/components/layouts/DSAside.vue";
import DSAudio from "@/components/layouts/DSAudio.vue";

import emitter from "@/utils/emitter";

const collapse = ref(false);
emitter.on("collapse", (msg) => {
  collapse.value = msg as boolean;
});
</script>

<style scoped>
.admin-shell {
  position: relative;
  width: 100%;
  height: 100%;
  background: var(--admin-bg);
}

.content-box {
  position: absolute;
  left: 160px;
  right: 0;
  top: 88px;
  bottom: 0;
  overflow-y: auto;
  transition: left 0.3s ease-in-out;
  padding: 26px 28px 132px;
}

.content-collapse {
  left: 118px;
}

.global-player-anchor {
  position: fixed;
  left: 190px;
  bottom: 28px;
  z-index: 1400;
  transition: left 0.3s ease-in-out, transform 0.3s ease-in-out;
}

.global-player-anchor.collapsed {
  left: 142px;
}

@media (max-width: 1200px) {
  .content-box {
    padding-inline: 20px;
  }

  .global-player-anchor {
    left: 172px;
    bottom: 20px;
  }

  .global-player-anchor.collapsed {
    left: 124px;
  }
}
</style>
