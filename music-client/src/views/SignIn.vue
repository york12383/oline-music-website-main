<template>
  <div class="auth-page music-page-shell">
    <div class="auth-layout">
      <yin-login-logo class="auth-brand"></yin-login-logo>
      <section class="sign music-panel">
        <div class="sign-head">
          <span class="music-chip">
            <span class="music-dot"></span>
            account access
          </span>
        <div class="sign-head-copy">
          <h1>帐号登录</h1>
          <p>欢迎回来，让喜欢的旋律继续陪你轻轻播放。</p>
        </div>
      </div>
        <el-form ref="signInForm" status-icon :model="registerForm" :rules="SignInRules" class="auth-form auth-form-signin">
          <el-form-item prop="username">
            <el-input placeholder="用户名" v-model="registerForm.username"></el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input type="password" placeholder="密码" v-model="registerForm.password" @keyup.enter="handleLoginIn"></el-input>
          </el-form-item>
          <el-form-item class="sign-btn sign-btn-signin">
            <el-button class="auth-btn auth-btn-secondary" :disabled="submitting" @click="handleSignUp">注册</el-button>
            <el-button class="auth-btn auth-btn-primary" type="primary" :loading="submitting" @click="handleLoginIn">登录</el-button>
            <el-button class="auth-btn auth-btn-ghost" :disabled="submitting" @click="handleForgotPassword">忘记密码</el-button>
          </el-form-item>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, reactive, getCurrentInstance, ref } from "vue";
import mixin from "@/mixins/mixin";
import YinLoginLogo from "@/components/layouts/YinLoginLogo.vue";
import { HttpManager } from "@/api";
import { NavName, RouterName, SignInRules } from "@/enums";

export default defineComponent({
  components: {
    YinLoginLogo,
  },
  setup() {
    const { proxy } = getCurrentInstance();
    const { routerManager, changeIndex } = mixin();

    const registerForm = reactive({
      username: "",
      password: "",
    });
    const submitting = ref(false);
    const SESSION_RETRY_DELAY_MS = 250;
    const SESSION_RETRY_TIMES = 2;

    async function resolveSessionUser() {
      for (let attempt = 0; attempt < SESSION_RETRY_TIMES; attempt += 1) {
        const sessionResult = (await HttpManager.getSessionUser()) as ResponseBody;
        if (sessionResult?.success && sessionResult.data) {
          return sessionResult;
        }

        if (attempt < SESSION_RETRY_TIMES - 1) {
          await new Promise((resolve) => window.setTimeout(resolve, SESSION_RETRY_DELAY_MS));
        }
      }

      return null;
    }

    async function handleLoginIn() {
      let canRun = true;
      (proxy.$refs["signInForm"] as any).validate((valid) => {
        if (!valid) return (canRun = false);
      });
      if (!canRun) return;

      submitting.value = true;
      try {
        const username = registerForm.username;
        const password = registerForm.password;
        const result = (await HttpManager.signIn({ username, password })) as ResponseBody;
        if (result.success) {
          const sessionResult = await resolveSessionUser();
          if (!sessionResult?.data) {
            (proxy as any).$message({
              message: "登录状态校验失败，请重试",
              type: "warning",
            });
            return;
          }

          proxy.$store.commit("setUserId", sessionResult.data.id);
          proxy.$store.commit("setUsername", sessionResult.data.username);
          proxy.$store.commit("setUserPic", sessionResult.data.avator);
          proxy.$store.commit("setToken", true);
          (proxy as any).$message({
            message: result.message,
            type: result.type,
          });
          changeIndex(NavName.Home);
          routerManager(RouterName.Home, { path: RouterName.Home });
          return;
        }

        (proxy as any).$message({
          message: result.message,
          type: result.type,
        });
      } catch (error) {
        (proxy as any).$message({
          message: "登录失败，请稍后重试",
          type: "error",
        });
      } finally {
        submitting.value = false;
      }
    }

    function handleSignUp() {
      routerManager(RouterName.SignUp, { path: RouterName.SignUp });
    }

    function handleForgotPassword() {
      routerManager(RouterName.ForgotPassword, { path: RouterName.ForgotPassword });
    }

    return {
      registerForm,
      submitting,
      SignInRules,
      handleLoginIn,
      handleForgotPassword,
      handleSignUp,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/sign.scss";
</style>
