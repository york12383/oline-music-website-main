<template>
  <div class="play-list" :class="[`type-${cardType}`]">
    <div class="play-title" v-if="title">{{ title }}</div>
    <ul class="play-body">
      <li class="card-frame" v-for="(item, index) in playList" :key="index">
        <article class="card-shell music-panel-soft">
          <div class="card" @click="goAblum(item)">
            <el-image class="card-img" fit="cover" :src="getCardCover(item)" />
            <div class="mask" @click="goAblum(item)">
              <span class="mask-button">
                <el-icon class="mask-icon">
                  <VideoPlay />
                </el-icon>
              </span>
            </div>
          </div>
          <div class="card-meta">
            <span v-if="getCardTag(item)" class="card-tag">{{ getCardTag(item) }}</span>
            <p class="card-name">{{ item.name || item.title }}</p>
            <p v-if="getCardSubtitle(item)" class="card-subtitle">{{ getCardSubtitle(item) }}</p>
          </div>
        </article>
      </li>
    </ul>
  </div>
</template>

<script lang="ts">
import { defineComponent, getCurrentInstance, toRefs } from "vue";
import { VideoPlay } from "@element-plus/icons-vue";

import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api";
import { getSongListCoverUrl } from "@/utils/song-list-cover";

export default defineComponent({
  components: {
    VideoPlay,
  },
  props: {
    title: String,
    playList: Array,
    path: String,
    cardType: {
      type: String,
      default: "cover",
    },
  },
  setup(props) {
    const { proxy } = getCurrentInstance();
    const { routerManager } = mixin();

    const { path, cardType } = toRefs(props);

    function goAblum(item) {
      proxy.$store.commit("setSongDetails", item);
      routerManager(path.value, { path: `/${path.value}/${item.id}` });
    }

    function getCardTag(item) {
      if (cardType.value === "artist") {
        return item.location || item.area || "歌手";
      }

      return item.style || item.category || "精选";
    }

    function getCardSubtitle(item) {
      if (cardType.value === "artist") {
        return item.introduction || "";
      }

      return item.introduction || "";
    }

    function getCardCover(item) {
      if (cardType.value === "artist") {
        return HttpManager.attachImageUrl(item.pic);
      }

      return getSongListCoverUrl(item);
    }

    return {
      goAblum,
      getCardTag,
      getCardSubtitle,
      getCardCover,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.play-list {
  width: 100%;

  .play-title {
    margin-bottom: 18px;
    font-size: 1.2rem;
    font-weight: 700;
    color: var(--app-title);
  }

  .play-body {
    @include layout(flex-start, stretch, row, wrap);
    gap: clamp(14px, 2vw, 22px);
    padding: 0;
    margin: 0;
    list-style: none;
  }
}

.card-frame {
  min-width: 0;

  .card-shell {
    height: 100%;
    padding: 14px;
    transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;

    &:hover {
      transform: translateY(-6px);
      border-color: var(--app-panel-border-strong);
      box-shadow: 0 24px 56px color-mix(in srgb, var(--app-title) 18%, transparent);
    }
  }

  .card {
    position: relative;
    aspect-ratio: 1 / 1;
    overflow: hidden;
    border-radius: 22px;

    .card-img {
      width: 100%;
      height: 100%;
      transition: all 0.4s ease;
    }
  }

  .card-meta {
    padding: 14px 4px 4px;
  }

  .card-tag {
    display: inline-flex;
    align-items: center;
    min-height: 28px;
    padding: 0 12px;
    margin-bottom: 10px;
    border-radius: 999px;
    background: var(--app-chip-bg-muted);
    color: var(--app-text-soft);
    font-size: 12px;
    font-weight: 700;
  }

  .card-name {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    margin: 0;
    color: var(--app-title);
    font-size: 1.05rem;
    font-weight: 800;
    line-height: 1.35;
  }

  .card-subtitle {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    margin: 8px 0 0;
    color: var(--app-text-muted);
    font-size: 0.88rem;
    line-height: 1.55;
  }

  &:hover .card-img {
    transform: scale(1.06);
  }
}

.mask {
  position: absolute;
  top: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: 22px;
  background:
    radial-gradient(circle at 50% 45%, rgba(255, 255, 255, 0.16) 0%, transparent 32%),
    linear-gradient(180deg, rgba(7, 12, 26, 0.06) 0%, rgba(7, 12, 26, 0.42) 100%);
  @include layout(center, center);
  transition: opacity 0.28s ease-in-out;
  opacity: 0;
  cursor: pointer;

  .mask-button {
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 88px;
    height: 88px;
    border: none;
    background: transparent;
    box-shadow: none;
    transform: translateY(10px) scale(0.92);
    transition:
      transform 0.28s ease,
      opacity 0.28s ease,
      filter 0.28s ease;
    isolation: isolate;
    opacity: 0.94;
  }

  .mask-button::before {
    display: none;
  }

  .mask-icon {
    position: relative;
    z-index: 1;
    color: rgba(255, 255, 255, 0.98);
    font-size: 54px;
    transform: translateX(3px);
    filter: drop-shadow(0 10px 18px rgba(0, 0, 0, 0.28));
  }
}

.card-shell:hover .mask,
.card-shell:focus-within .mask {
  opacity: 1;
}

.card-shell:hover .mask-button,
.card-shell:focus-within .mask-button {
  transform: translateY(0) scale(1);
  opacity: 1;
  filter: saturate(1.06);
}

@media screen and (min-width: $sm) {
  .card-frame {
    width: calc(20% - 18px);
  }
}

@media screen and (max-width: $sm) {
  .card-frame {
    width: calc(50% - 8px);
  }

  .card-frame .card-shell {
    padding: 10px;
  }

  .card-frame .card {
    border-radius: 18px;
  }
}
</style>
