<template>
  <audio
    :src="attachImageUrl(songUrl)"
    controls="controls"
    :ref="player"
    preload="true"
    @canplay="canplay"
    @timeupdate="timeupdate"
    @ended="ended"
  >
  </audio>
</template>

<script lang="ts">
import { computed, defineComponent, getCurrentInstance, ref, watch } from "vue";
import { useStore } from "vuex";
import { HttpManager } from "@/api";

export default defineComponent({
  setup() {
    const { proxy } = getCurrentInstance();
    const store = useStore();
    const audioRef = ref<HTMLAudioElement | null>(null);
    const playRequested = ref(false);

    const player = (el: HTMLAudioElement | null) => {
      audioRef.value = el;
    };

    const songUrl = computed(() => store.getters.songUrl);
    const isPlay = computed(() => store.getters.isPlay);
    const volume = computed(() => store.getters.volume);
    const changeTime = computed(() => store.getters.changeTime);
    const autoNext = computed(() => store.getters.autoNext);

    watch(songUrl, () => {
      if (!audioRef.value) return;
      playRequested.value = Boolean(songUrl.value);
      proxy.$store.commit("setCurTime", 0);
      proxy.$store.commit("setDuration", 0);
      proxy.$store.commit("setChangeTime", 0);
    });

    watch(isPlay, (value) => {
      if (!audioRef.value) return;
      if (value) {
        playRequested.value = true;
        safelyPlay();
      } else {
        audioRef.value.pause();
      }
    });

    watch(changeTime, (value) => {
      if (!audioRef.value || typeof value !== "number" || Number.isNaN(value)) return;
      audioRef.value.currentTime = value;
    });

    watch(volume, (value) => {
      if (!audioRef.value) return;
      audioRef.value.volume = value;
    });

    async function safelyPlay() {
      if (!audioRef.value || !songUrl.value) return;

      try {
        await audioRef.value.play();
        playRequested.value = false;
      } catch (error) {
        // 音频还没准备好时，等待 canplay 再继续尝试
      }
    }

    function canplay() {
      if (!audioRef.value) return;
      proxy.$store.commit("setDuration", audioRef.value.duration || 0);

      if (isPlay.value || playRequested.value) {
        proxy.$store.commit("setIsPlay", true);
        void safelyPlay();
      }
    }

    function timeupdate() {
      if (!audioRef.value) return;
      proxy.$store.commit("setCurTime", audioRef.value.currentTime);
    }

    function ended() {
      proxy.$store.commit("setIsPlay", false);
      proxy.$store.commit("setCurTime", 0);
      proxy.$store.commit("setAutoNext", !autoNext.value);
    }

    return {
      songUrl,
      player,
      canplay,
      timeupdate,
      ended,
      attachImageUrl: HttpManager.attachImageUrl,
    };
  },
});
</script>

<style scoped>
audio {
  display: none;
}
</style>
