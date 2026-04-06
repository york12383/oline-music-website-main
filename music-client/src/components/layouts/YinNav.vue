<template>
  <ul class="nav">
    <li v-for="(item, index) in styleList" :key="index" :class="{ active: item.name == activeName }" @click="handleChangeView(item)">
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

    function handleChangeView(val) {
      proxy.$emit("click", val);
    }
    return {
      handleChangeView,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

.nav {
  width: 100%;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 0;
  margin: 0;
  list-style: none;

  li {
    padding: 0.85rem 1.2rem;
    line-height: 1;
    font-size: 0.98rem;
    font-weight: 600;
    color: var(--app-text-muted);
    background: var(--app-chip-bg-muted);
    border: 1px solid transparent;
    border-radius: 999px;
    cursor: pointer;
    transition: all 0.25s ease;

    &:hover {
      color: var(--app-title);
      border-color: var(--app-panel-border);
      transform: translateY(-1px);
    }
  }

  li.active {
    color: #08101f;
    font-weight: 700;
    background: linear-gradient(135deg, var(--app-accent-strong), var(--app-accent));
    box-shadow: 0 12px 28px color-mix(in srgb, var(--app-accent-strong) 24%, transparent);
  }
}

@media screen and (max-width: $sm) {
  .nav {
    li {
      padding: 0.7rem 1rem;
      font-size: 0.92rem;
    }
  }
}
</style>
