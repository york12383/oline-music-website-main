<template>
  <div class="auth-page music-page-shell">
    <div class="auth-layout">
      <yin-login-logo class="auth-brand"></yin-login-logo>
      <section class="sign music-panel">
        <div class="sign-head">
          <span class="music-chip">
            <span class="music-dot"></span>
            create profile
          </span>
          <div class="sign-head-copy">
            <h1>用户注册</h1>
          </div>
        </div>
        <el-form ref="signUpForm" label-width="70px" status-icon :model="registerForm" :rules="SignUpRules" class="auth-form auth-form-signup">
          <el-form-item prop="username" label="用户名">
            <el-input v-model="registerForm.username" placeholder="用户名"></el-input>
          </el-form-item>
          <el-form-item prop="password" label="密码">
            <el-input type="password" placeholder="密码" v-model="registerForm.password"></el-input>
          </el-form-item>
        <el-form-item prop="sex" label="性别">
          <el-radio-group v-model="registerForm.sex">
              <el-radio :value="0">女</el-radio>
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">保密</el-radio>
          </el-radio-group>
        </el-form-item>
          <el-form-item prop="phoneNum" label="手机">
            <el-input placeholder="手机" v-model="registerForm.phoneNum" maxlength="15"></el-input>
          </el-form-item>
          <el-form-item prop="email" label="邮箱">
            <el-input v-model="registerForm.email" placeholder="邮箱" maxlength="30"></el-input>
          </el-form-item>
          <el-form-item prop="birth" label="生日">
            <el-date-picker type="date" placeholder="选择日期" v-model="registerForm.birth" style="width: 100%"></el-date-picker>
          </el-form-item>
          <el-form-item prop="introduction" label="签名">
            <el-input type="textarea" placeholder="签名" v-model="registerForm.introduction"></el-input>
          </el-form-item>
          <el-form-item prop="location" label="地区">
            <el-select v-model="registerForm.location" placeholder="地区" style="width: 100%">
              <el-option v-for="item in AREA" :key="item.value" :label="item.label" :value="item.value"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item class="sign-btn">
            <el-button class="auth-btn auth-btn-secondary" :disabled="submitting" @click="goBackRegist()">取消</el-button>
            <el-button class="auth-btn auth-btn-primary" type="primary" :loading="submitting" @click="handleSignUp">确定</el-button>
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
import { AREA, RouterName, NavName, SignUpRules } from "@/enums";

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
      sex: 2,
      phoneNum: "",
      email: "",
      birth: new Date(),
      introduction: "",
      location: "",
    });
    const submitting = ref(false);

    function normalizeSignUpMessage(message?: string) {
      const rawMessage = (message || "").trim();
      if (!rawMessage) {
        return "注册失败，请稍后重试";
      }

      const normalizedMessage = rawMessage.toLowerCase();

      if (normalizedMessage.includes("phone") || rawMessage.includes("手机号")) {
        if (
          normalizedMessage.includes("exist") ||
          normalizedMessage.includes("duplicate") ||
          rawMessage.includes("已注册") ||
          rawMessage.includes("已存在") ||
          rawMessage.includes("重复")
        ) {
          return "这个手机号已经注册过了，换一个手机号试试";
        }
      }

      if (normalizedMessage.includes("email") || rawMessage.includes("邮箱")) {
        if (
          normalizedMessage.includes("exist") ||
          normalizedMessage.includes("duplicate") ||
          rawMessage.includes("已注册") ||
          rawMessage.includes("已存在") ||
          rawMessage.includes("重复")
        ) {
          return "这个邮箱已经被使用了，换一个邮箱试试";
        }
      }

      if (normalizedMessage.includes("username") || rawMessage.includes("用户名")) {
        if (
          normalizedMessage.includes("exist") ||
          normalizedMessage.includes("duplicate") ||
          rawMessage.includes("已注册") ||
          rawMessage.includes("已存在") ||
          rawMessage.includes("重复")
        ) {
          return "这个用户名已经存在了，换一个用户名试试";
        }
      }

      if (
        normalizedMessage.includes("duplicate") ||
        rawMessage.includes("已注册") ||
        rawMessage.includes("已存在") ||
        rawMessage.includes("重复")
      ) {
        return "这条注册信息已经存在了，请换一组信息后重试";
      }

      return rawMessage;
    }

    async function goBackRegist() {
      routerManager(RouterName.SignIn, { path: RouterName.SignIn });
    }

    async function handleSignUp() {
      let canRun = true;
      (proxy.$refs["signUpForm"] as any).validate((valid) => {
        if (!valid) return (canRun = false);
      });
      if (!canRun) return;

      submitting.value = true;
      try {
        const username = registerForm.username;
        const password = registerForm.password;
        const sex = registerForm.sex;
        const phoneNum = registerForm.phoneNum;
        const email = registerForm.email;
        const birth = registerForm.birth;
        const introduction = registerForm.introduction;
        const location = registerForm.location;
        const result = (await HttpManager.SignUp({ username, password, sex, phoneNum, email, birth, introduction, location })) as ResponseBody;
        (proxy as any).$message({
          message: result.success ? result.message : normalizeSignUpMessage(result.message),
          type: result.success ? result.type : "warning",
        });

        if (result.success) {
          changeIndex(NavName.SignIn);
          routerManager(RouterName.SignIn, { path: RouterName.SignIn });
        }
      } catch (error) {
        const errorMessage =
          (error as any)?.response?.data?.message ||
          (error as any)?.data?.message ||
          (error as any)?.message;
        (proxy as any).$message({
          message: normalizeSignUpMessage(errorMessage),
          type: "error",
        });
      } finally {
        submitting.value = false;
      }
    }

    return {
      SignUpRules,
      AREA,
      registerForm,
      submitting,
      handleSignUp,
      goBackRegist,
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/sign.scss";
</style>
