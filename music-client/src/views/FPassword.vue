<template>
  <div class="auth-page music-page-shell">
    <div class="auth-layout">
      <yin-login-logo class="auth-brand"></yin-login-logo>
      <section class="sign music-panel">
        <div class="sign-head">
          <span class="music-chip">
            <span class="music-dot"></span>
            password reset
          </span>
          <div class="sign-head-copy">
            <h1>修改密码</h1>
            <p>收下验证码，把熟悉的旋律重新接回你的账户里。</p>
          </div>
        </div>
        <el-form class="auth-form auth-form-reset" label-position="top" @submit.prevent="handleSubmit">
          <el-form-item label="邮箱" prop="email">
            <div class="reset-inline">
              <el-input id="email" v-model="email" type="email" placeholder="输入注册邮箱" required />
              <el-button class="auth-btn auth-btn-secondary reset-inline-btn" :loading="sendingCode" :disabled="submitting" @click="sendVerificationCode">
                {{ sendingCode ? "发送中..." : "发送验证码" }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <el-input id="code" v-model="code" type="text" placeholder="输入邮箱验证码" required />
          </el-form-item>
          <el-form-item label="新密码" prop="password">
            <el-input id="password" v-model="password" type="password" placeholder="输入新密码" required />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input id="confirmPassword" v-model="confirmPassword" type="password" placeholder="再次输入新密码" required />
          </el-form-item>
          <el-form-item class="sign-btn sign-btn-single">
            <el-button class="auth-btn auth-btn-primary" type="primary" native-type="submit" :loading="submitting">提交</el-button>
          </el-form-item>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script>
import YinLoginLogo from "@/components/layouts/YinLoginLogo.vue";
import { HttpManager } from "@/api";

export default {
  components: {
    YinLoginLogo,
  },
  data() {
    return {
      email: "",
      code: "",
      password: "",
      confirmPassword: "",
      sendingCode: false,
      submitting: false,
    };
  },
  methods: {
    async sendVerificationCode() {
      if (!this.email) {
        this.$message({
          message: "请先输入注册邮箱",
          type: "warning",
        });
        return;
      }

      this.sendingCode = true;
      try {
        const response = await HttpManager.sendVerificationCode({ email: this.email });
        this.$message({
          message: response.message || "验证码已发送，请留意邮箱",
          type: response.type || "success",
        });
      } catch (error) {
        this.$message({
          message: "验证码发送失败，请稍后重试",
          type: "error",
        });
      } finally {
        this.sendingCode = false;
      }
    },
    async handleSubmit() {
      if (!this.email || !this.code || !this.password || !this.confirmPassword) {
        this.$message({
          message: "请先完整填写信息",
          type: "warning",
        });
        return;
      }

      if (this.password !== this.confirmPassword) {
        this.$message({
          message: "两次输入的密码不一致",
          type: "warning",
        });
        return;
      }

      this.submitting = true;
      try {
        const response = await HttpManager.resetPassword({
          email: this.email,
          code: this.code,
          password: this.password,
          confirmPassword: this.confirmPassword,
        });
        this.$message({
          message: response.message || "密码修改成功",
          type: response.type || "success",
        });
        if (response.success) {
          this.$router.push("/sign-in");
        }
      } catch (error) {
        this.$message({
          message: "密码修改失败，请稍后重试",
          type: "error",
        });
      } finally {
        this.submitting = false;
      }
    },
  },
};
</script>

<style lang="scss" scoped>
@import "@/assets/css/sign.scss";

.reset-inline {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  width: 100%;
}

.reset-inline-btn {
  min-width: 132px;
}

.sign-btn-single:deep(.el-form-item__content) {
  grid-template-columns: minmax(0, 1fr);
}

@media screen and (max-width: 720px) {
  .reset-inline {
    grid-template-columns: minmax(0, 1fr);
  }

  .reset-inline-btn {
    min-width: 0;
  }
}
</style>
