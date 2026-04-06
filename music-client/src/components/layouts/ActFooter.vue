<template>
  <div class="footer">
    <div class="footer-links">
      <router-link to="/about">
        <span class="footer-link-icon">i</span>
        关于本站
      </router-link>
      <router-link to="/resources">
        <span class="footer-link-icon">?</span>
        资源来源
      </router-link>
      <router-link to="/terms">
        <span class="footer-link-icon">T</span>
        使用条款
      </router-link>
      <router-link to="/feedback">
        <span class="footer-link-icon">F</span>
        反馈意见
      </router-link>
    </div>

    <div class="social-icons">
      <button type="button" class="qq" @click="handleThirdPartyLogin('QQ')">
        QQ
      </button>
      <button type="button" class="wechat" @click="handleThirdPartyLogin('微信')">
        微信
      </button>
    </div>

    <p class="copyright">Modification © 2025 Actording</p>
    <p class="copyright">Copyright © 2019 Yin-Hongwei</p>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { ElMessage } from "element-plus";

export default defineComponent({
  name: "ActFooter",
  setup() {
    const handleThirdPartyLogin = (provider: string) => {
      ElMessage.info(`暂未开放${provider}第三方登录`);
    };

    return {
      handleThirdPartyLogin,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.footer {
  width: var(--page-shell-width);
  margin: 0 auto 140px;
  background: var(--app-footer-bg);
  color: var(--app-text-muted);
  padding: 2rem 2rem 1.4rem;
  text-align: center;
  border-radius: 28px;
  border: 1px solid var(--app-panel-border);
  box-shadow: var(--app-panel-shadow-soft);
  backdrop-filter: blur(24px);
}

.footer p {
  margin: 0.45rem 0;
}

.footer-links {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1rem;
}

.footer-links a {
  color: var(--app-text);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
  padding: 0.7rem 1.15rem;
  border-radius: 999px;
  background: var(--app-chip-bg-muted);
  border: 1px solid transparent;
}

.footer-links a:hover {
  background-color: var(--app-panel-bg-strong);
  border-color: var(--app-panel-border);
  transform: translateY(-2px);
}

.footer-link-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 50%;
  background: var(--app-chip-bg);
  font-size: 0.75rem;
  font-weight: 700;
}

.social-icons {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin: 1.25rem 0;
}

.social-icons button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  height: 40px;
  padding: 0 1rem;
  border-radius: 999px;
  color: var(--app-text);
  transition: all 0.3s ease;
  background-color: var(--app-chip-bg-muted);
  border: 1px solid transparent;
  cursor: pointer;
  font: inherit;
}

.social-icons button:hover {
  transform: translateY(-2px);
  border-color: var(--app-panel-border);
}

.social-icons button.qq:hover {
  background-color: #12b7f5;
}

.social-icons button.wechat:hover {
  background-color: #2aae67;
}

.copyright {
  font-size: 0.9rem;
  opacity: 0.78;
}

@media (max-width: 768px) {
  .footer {
    width: calc(100% - 20px);
    margin-bottom: 120px;
    padding: 1.5rem 1rem 1rem;
  }

  .footer-links {
    flex-direction: column;
    align-items: center;
  }
}
</style>
