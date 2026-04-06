<template>
  <ul class="header-nav">
    <!-- 主页导航栏内容 -->
    <li :class="{ active: item.name === activeName }" v-for="item in styleList" :key="item.path" @click="handleChangeView(item)">
      {{ item.name }}
    </li>
  </ul>
</template>

<script lang="ts">
import { defineComponent, getCurrentInstance } from "vue";

export default defineComponent({
  props: {
    styleList: Array,
    activeName: String,
  },
  emits: ["click"],
  setup() {
    const { proxy } = getCurrentInstance();

    function handleChangeView(item) {
      proxy.$emit("click", item.path, item.name);
    }
    return {
      handleChangeView,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

.header-nav {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

li {
  position: relative;
  padding: 0.75rem 0.95rem;
  border-radius: 999px;
  line-height: 1;
  color: var(--app-text-muted);
  font-size: 0.98rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.25s ease, background-color 0.25s ease, transform 0.25s ease;

  &:hover {
    color: var(--app-title);
    background: var(--app-chip-bg-muted);
  }
}

li.active {
  color: var(--app-title);

  &::after {
    content: "";
    position: absolute;
    left: 18%;
    right: 18%;
    bottom: 0.15rem;
    height: 4px;
    border-radius: 999px;
    background: linear-gradient(135deg, var(--app-accent-strong), var(--app-accent));
    box-shadow: 0 6px 18px color-mix(in srgb, var(--app-accent-strong) 45%, transparent);
  }
}
</style>
