<template>
  <div class="login-shell" :class="{ 'is-submitting': submitting, 'is-success': loginState === 'success' }">
    <div class="login-shell__backdrop"></div>
    <div class="login-shell__noise"></div>
    <div class="login-shell__aurora login-shell__aurora--mint"></div>

    <div class="login-stage">
      <section class="login-panel">
        <div class="login-panel__halo"></div>
        <div class="login-panel__inner">
          <div class="login-panel__header">
            <div class="login-panel__brand">
              <div class="login-brand__mark login-brand__mark--compact">
                <span>♪</span>
              </div>
              <div class="login-panel__brand-copy">
                <p class="login-panel__brand-title">{{ nusicName }}</p>
                <span class="login-panel__brand-subtitle">Admin Console</span>
              </div>
            </div>
            <h2>管理员登录</h2>
            <p class="login-panel__desc">使用管理员账号进入轻音音乐后台。</p>
          </div>

          <div v-if="showStatusMessage" class="login-status" :class="`is-${statusMessage.type}`">
            <el-icon class="login-status__icon">
              <component :is="statusIcon" />
            </el-icon>
            <span>{{ statusMessage.text }}</span>
          </div>

          <el-form ref="formRef" :model="ruleForm" :rules="rules" class="login-form" status-icon>
            <el-form-item prop="username" class="login-form__item">
              <label class="login-form__label">管理员账号</label>
              <el-input v-model="ruleForm.username" placeholder="请输入管理员账号" clearable>
                <template #prefix>
                  <el-icon class="login-input__icon"><UserFilled /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password" class="login-form__item">
              <label class="login-form__label">登录密码</label>
              <el-input
                v-model="ruleForm.password"
                type="password"
                placeholder="请输入登录密码"
                show-password
                @keyup.enter="submitForm"
              >
                <template #prefix>
                  <el-icon class="login-input__icon"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item class="login-form__actions">
              <el-button class="login-btn" type="primary" :loading="submitting" @click="submitForm">
                {{ submitting ? "正在登录..." : "进入后台" }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </section>
    </div>
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, getCurrentInstance, reactive, ref } from "vue";
import { CircleCheckFilled, Lock, Loading, UserFilled, WarningFilled } from "@element-plus/icons-vue";
import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api/index";
import { RouterName, MUSICNAME } from "@/enums";
import { getLastAdminUsername, getStoredAdminInfo, rememberLastAdminUsername } from "@/utils/admin-auth";

export default defineComponent({
  setup() {
    const { proxy } = getCurrentInstance();
    const { routerManager } = mixin();

    const nusicName = ref(MUSICNAME);
    const formRef = ref();
    const submitting = ref(false);
    const loginState = ref<"idle" | "success">("idle");
    const initialAdminName = getLastAdminUsername() || getStoredAdminInfo()?.username || getStoredAdminInfo()?.name || "";
    const statusMessage = reactive({
      type: "info",
      text: "",
    });
    const showStatusMessage = computed(() => statusMessage.type === "error" || statusMessage.type === "success");
    const statusIcon = computed(() => {
      if (submitting.value) {
        return Loading;
      }
      if (statusMessage.type === "success") {
        return CircleCheckFilled;
      }
      return WarningFilled;
    });
    const ruleForm = reactive({
      username: initialAdminName,
      password: "",
    });
    const rules = reactive({
      username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
      password: [{ required: true, message: "请输入密码", trigger: "blur" }],
    });

    async function submitForm() {
      if (submitting.value) {
        return;
      }

      loginState.value = "idle";
      statusMessage.type = "info";
      statusMessage.text = "";
      const form = formRef.value as any;
      if (form?.validate) {
        const valid = await form.validate().catch(() => false);
        if (!valid) {
          statusMessage.type = "error";
          statusMessage.text = "请先完整填写管理员账号和密码。";
          return;
        }
      }

      submitting.value = true;
      try {
        const username = ruleForm.username;
        const password = ruleForm.password;
        const result = (await HttpManager.getLoginStatus({ username, password })) as ResponseBody;
        if (result.success) {
          const sessionResult = (await HttpManager.getAdminSession()) as ResponseBody;
          if (!sessionResult?.success || !sessionResult.data) {
            statusMessage.type = "error";
            statusMessage.text = "登录成功，但管理员会话校验失败，请重新尝试。";
            (proxy as any).$message({
              message: "管理员登录状态校验失败，请重试",
              type: "warning",
            });
            return;
          }

          statusMessage.type = "success";
          statusMessage.text = "管理员身份校验通过，正在进入后台...";
          const adminInfo = {
            username: sessionResult.data.name || username,
            loginTime: new Date().getTime(),
          };
          localStorage.setItem("adminInfo", JSON.stringify(adminInfo));
          rememberLastAdminUsername(adminInfo.username);
          loginState.value = "success";
          (proxy as any).$message({
            message: result.message,
            type: result.type,
          });
          await new Promise((resolve) => setTimeout(resolve, 520));
          routerManager(RouterName.Info, { path: RouterName.Info });
          return;
        }

        loginState.value = "idle";
        statusMessage.type = "error";
        statusMessage.text = result.message || "登录失败，请检查账号和密码后重试。";
        (proxy as any).$message({
          message: result.message,
          type: result.type,
        });
      } catch (error) {
        void error;
        loginState.value = "idle";
        statusMessage.type = "error";
        statusMessage.text = "登录请求失败，请检查后台服务或网络连接。";
        (proxy as any).$message({
          message: "登录请求失败，请稍后重试",
          type: "error",
        });
      } finally {
        submitting.value = false;
      }
    }

    return {
      nusicName,
      formRef,
      ruleForm,
      rules,
      statusIcon,
      statusMessage,
      showStatusMessage,
      loginState,
      submitting,
      submitForm,
      UserFilled,
      Lock,
    };
  },
});
</script>

<style scoped>
.login-shell {
  --login-accent: #6be6dc;
  --login-accent-strong: #4ebae6;
  --login-surface: rgba(11, 18, 28, 0.82);
  --login-border: rgba(255, 255, 255, 0.1);
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 18%, rgba(107, 230, 220, 0.16), transparent 32%),
    linear-gradient(135deg, #05080e 0%, #09121d 48%, #04070d 100%);
  font-family: "Trebuchet MS", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.login-shell__backdrop,
.login-shell__noise,
.login-shell__aurora {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.login-shell__backdrop {
  background:
    linear-gradient(115deg, rgba(3, 8, 14, 0.88) 10%, rgba(6, 14, 22, 0.66) 48%, rgba(7, 12, 20, 0.88) 100%),
    radial-gradient(circle at 22% 18%, rgba(102, 245, 228, 0.12), transparent 24%),
    url("../assets/images/background.jpg") center center / cover no-repeat;
  transform: scale(1.02);
  filter: saturate(0.88) contrast(1.02) brightness(0.72);
  background-blend-mode: screen, normal;
}

.login-shell__noise {
  background:
    linear-gradient(transparent 0, rgba(255, 255, 255, 0.02) 50%, transparent 100%),
    repeating-linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.02) 0,
      rgba(255, 255, 255, 0.02) 1px,
      transparent 1px,
      transparent 14px
    );
  mix-blend-mode: screen;
  opacity: 0.16;
}

.login-shell__aurora {
  filter: blur(72px);
  opacity: 0.34;
  animation: auroraFloat 18s ease-in-out infinite;
}

.login-shell__aurora--mint {
  top: 4%;
  left: -6%;
  right: auto;
  bottom: auto;
  width: 280px;
  height: 280px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(86, 229, 221, 0.22), rgba(86, 229, 221, 0));
}

.login-stage {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: grid;
  place-items: center;
  align-items: center;
  padding: 40px clamp(24px, 4vw, 72px);
  transition: transform 0.55s ease, opacity 0.55s ease;
}

.login-brand__mark {
  width: 72px;
  height: 72px;
  border-radius: 24px;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.2) 38%, transparent 40%),
    linear-gradient(145deg, rgba(107, 230, 220, 0.95), rgba(56, 118, 255, 0.52));
  box-shadow:
    0 18px 40px rgba(8, 17, 30, 0.38),
    inset 0 1px 0 rgba(255, 255, 255, 0.46);
  font-size: 34px;
  color: #103346;
}

.login-panel {
  position: relative;
  width: min(100%, 458px);
  transition: transform 0.55s ease, opacity 0.55s ease;
}

.login-panel__halo {
  position: absolute;
  inset: 22px -10px -14px 10px;
  border-radius: 30px;
  background: radial-gradient(circle at 20% 18%, rgba(107, 230, 220, 0.14), transparent 48%);
  filter: blur(18px);
  opacity: 0.46;
}

.login-panel__inner {
  position: relative;
  padding: 32px 28px 26px;
  border-radius: 28px;
  border: 1px solid var(--login-border);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.04)),
    var(--login-surface);
  backdrop-filter: blur(18px);
  box-shadow:
    0 22px 44px rgba(4, 10, 18, 0.36),
    inset 0 1px 0 rgba(255, 255, 255, 0.14);
  transition:
    transform 0.55s ease,
    box-shadow 0.55s ease,
    border-color 0.55s ease,
    background 0.55s ease;
}

.login-panel__header {
  margin-bottom: 22px;
  color: #f8fbff;
}

.login-panel__brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.login-brand__mark--compact {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  font-size: 26px;
  box-shadow:
    0 14px 26px rgba(8, 17, 30, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.36);
}

.login-panel__brand-copy {
  min-width: 0;
}

.login-panel__brand-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: rgba(247, 251, 255, 0.96);
}

.login-panel__brand-subtitle {
  display: inline-block;
  margin-top: 4px;
  font-size: 11px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.46);
}

.login-panel__header h2 {
  margin: 0;
  font-size: 34px;
  letter-spacing: -0.03em;
}

.login-panel__desc {
  margin: 10px 0 0;
  color: rgba(236, 242, 248, 0.62);
  line-height: 1.7;
  font-size: 14px;
}

.login-status {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
  padding: 11px 13px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(240, 246, 252, 0.82);
  line-height: 1.6;
  font-size: 13px;
}

.login-status.is-info {
  border-color: rgba(107, 230, 220, 0.2);
  background: rgba(107, 230, 220, 0.08);
  color: rgba(225, 252, 249, 0.9);
}

.login-status.is-success {
  border-color: rgba(129, 226, 165, 0.22);
  background: rgba(129, 226, 165, 0.1);
  color: rgba(235, 255, 240, 0.92);
}

.login-status.is-error {
  border-color: rgba(255, 133, 133, 0.24);
  background: rgba(255, 133, 133, 0.1);
  color: rgba(255, 233, 233, 0.92);
}

.login-status__icon {
  margin-top: 2px;
  font-size: 16px;
  flex-shrink: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.login-form__item {
  margin-bottom: 14px;
}

.login-form__label {
  display: inline-block;
  margin-bottom: 10px;
  color: rgba(246, 250, 255, 0.82);
  font-size: 13px;
  letter-spacing: 0.06em;
}

.login-form__actions {
  margin-top: 6px;
  margin-bottom: 0;
}

.login-btn {
  width: 100%;
  min-height: 50px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--login-accent-strong), #69aaf1);
  box-shadow: 0 14px 26px rgba(42, 135, 220, 0.2);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.08em;
  transition: transform 0.28s ease, box-shadow 0.28s ease, filter 0.28s ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 30px rgba(42, 135, 220, 0.24);
}

.login-btn:active {
  transform: translateY(1px) scale(0.995);
}

:deep(.el-form-item__content) {
  display: block;
  line-height: normal;
}

:deep(.el-input__wrapper) {
  border-radius: 15px;
  padding: 0 16px;
  min-height: 50px;
  background: rgba(255, 255, 255, 0.06);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 0 0 1px rgba(107, 230, 220, 0.42),
    0 0 0 4px rgba(107, 230, 220, 0.06);
}

:deep(.el-input__inner) {
  color: #f9fbff;
  font-size: 15px;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(239, 244, 250, 0.38);
}

:deep(.el-input__prefix-inner) {
  display: flex;
  align-items: center;
}

.login-input__icon {
  color: rgba(220, 235, 248, 0.46);
  font-size: 15px;
}

.login-shell.is-submitting .login-panel__inner {
  transform: translateY(-2px);
}

.login-shell.is-submitting .login-shell__aurora {
  animation-duration: 14s;
}

.login-shell.is-success .login-stage {
  transform: scale(1.005);
}

.login-shell.is-success .login-panel {
  transform: translateY(-6px);
}

.login-shell.is-success .login-panel__inner {
  border-color: rgba(124, 255, 230, 0.22);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.06)),
    rgba(8, 24, 31, 0.82);
  box-shadow:
    0 24px 46px rgba(7, 18, 27, 0.42),
    0 0 0 1px rgba(107, 230, 220, 0.12),
    0 0 0 8px rgba(107, 230, 220, 0.06);
}

.login-shell.is-success .login-shell__backdrop {
  transform: scale(1.04);
  filter: saturate(0.96) contrast(1.04) brightness(0.76);
}

.login-shell.is-success .login-shell__aurora--mint {
  opacity: 0.42;
}

:deep(.el-form-item__error) {
  padding-top: 6px;
}

@keyframes auroraFloat {
  0% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  33% {
    transform: translate3d(16px, -12px, 0) scale(1.06);
  }
  66% {
    transform: translate3d(-12px, 14px, 0) scale(0.96);
  }
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
}

@media (max-width: 1180px) {
  .login-stage {
    padding: 28px 18px 32px;
  }

  .login-panel {
    margin: 0 auto;
  }
}

@media (max-width: 768px) {
  .login-shell {
    min-height: 100svh;
  }

  .login-shell__backdrop {
    background-position: 62% center;
  }

  .login-panel__inner {
    padding: 28px 20px 22px;
    border-radius: 24px;
  }

  .login-panel__header h2 {
    font-size: 30px;
  }
}
</style>
