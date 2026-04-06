<template>
  <div class="login-logo">
    <div class="brand-panel">
      <div class="brand-chip">
        <span class="brand-chip-dot"></span>
        light sound archive
      </div>
      <div class="brand-card" aria-hidden="true">
        <div class="brand-card-glow"></div>
        <span class="note-primary">♫</span>
        <span class="note-secondary">♪</span>
      </div>
      <div class="brand-copy">
        <h2 class="brand-title">轻音音乐</h2>
        <p class="brand-subtitle">把心动轻轻收藏，让好歌不期而遇。在轻音音乐，听见与你更合拍的每一首。</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "YinLoginLogo",
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.login-logo {
  min-height: clamp(460px, 62vh, 720px);
  width: 100%;
  position: relative;
  overflow: hidden;
  border-radius: 32px;
  border: 1px solid rgba(132, 148, 175, 0.22);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.28);
  background:
    radial-gradient(circle at 12% 16%, rgba(114, 230, 223, 0.24), transparent 22%),
    radial-gradient(circle at 85% 18%, rgba(255, 177, 110, 0.18), transparent 24%),
    radial-gradient(circle at 50% 100%, rgba(94, 214, 255, 0.18), transparent 30%),
    linear-gradient(145deg, rgba(9, 18, 32, 0.98) 0%, rgba(15, 24, 43, 0.96) 48%, rgba(31, 24, 47, 0.94) 100%);
  backdrop-filter: blur(28px);
}

.brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1.4rem;
  padding: clamp(1.8rem, 4vw, 3rem);
  min-height: inherit;
  position: relative;
  z-index: 1;
}

.brand-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  width: fit-content;
  padding: 0.65rem 1rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(245, 248, 255, 0.92);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.brand-chip-dot {
  width: 0.55rem;
  height: 0.55rem;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--app-accent), var(--app-accent-warm));
  box-shadow: 0 0 0 6px rgba(114, 230, 223, 0.12);
}

.brand-card {
  width: clamp(180px, 18vw, 240px);
  height: clamp(180px, 18vw, 240px);
  border-radius: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(135deg, rgba(114, 230, 223, 0.92) 0%, rgba(94, 214, 255, 0.86) 42%, rgba(255, 177, 110, 0.88) 100%);
  box-shadow:
    0 30px 60px rgba(5, 12, 22, 0.32),
    inset 0 2px 18px rgba(255, 255, 255, 0.18);
}

.brand-card::before {
  content: "";
  position: absolute;
  inset: 12px;
  border-radius: 26px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.03));
}

.brand-card-glow {
  position: absolute;
  width: 60%;
  height: 60%;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  filter: blur(12px);
}

.note-primary,
.note-secondary {
  position: absolute;
  color: rgba(92, 88, 189, 0.98);
  text-shadow: 0 10px 22px rgba(78, 74, 176, 0.18);
  font-weight: 700;
  user-select: none;
}

.note-primary {
  font-size: clamp(5.8rem, 6vw, 8.6rem);
  transform: translate(-8%, 2%) rotate(-7deg);
}

.note-secondary {
  font-size: clamp(3.3rem, 3.6vw, 5.1rem);
  transform: translate(42%, -18%) rotate(8deg);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.brand-title {
  margin: 0;
  font-size: clamp(2rem, 2.4vw, 3rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #ffffff;
  text-shadow: 0 8px 22px rgba(4, 12, 24, 0.32);
}

.brand-subtitle {
  margin: 0;
  max-width: 34rem;
  color: rgba(234, 241, 255, 0.82);
  font-size: clamp(0.98rem, 1.1vw, 1.08rem);
  line-height: 1.8;
}

.brand-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.brand-highlights span {
  display: inline-flex;
  align-items: center;
  padding: 0.72rem 1rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(245, 248, 255, 0.9);
  font-size: 0.92rem;
}

@media screen and (max-width: $sm) {
  .login-logo {
    min-height: 320px;
    border-radius: 28px;
  }

  .brand-panel {
    padding: 1.5rem 1.2rem;
    gap: 0.8rem;
  }

  .brand-card {
    width: 140px;
    height: 140px;
    border-radius: 28px;
  }

  .brand-card::before {
    inset: 9px;
    border-radius: 20px;
  }

  .note-primary {
    font-size: 4.6rem;
  }

  .note-secondary {
    font-size: 2.7rem;
  }

  .brand-highlights span {
    padding: 0.6rem 0.85rem;
    font-size: 0.84rem;
  }
}
</style>
