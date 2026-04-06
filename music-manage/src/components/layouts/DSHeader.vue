<template>
  <div class="header">
    <div class="header-left">
      <div class="collapse-btn" @click="collapseChage">
        <el-icon v-if="!collapse"><fold /></el-icon>
        <el-icon v-else><expand /></el-icon>
      </div>
      <div class="logo-group">
        <div class="logo-badge" aria-hidden="true">
          <span class="badge-glow"></span>
          <span class="badge-note badge-note-primary">&#9835;</span>
          <span class="badge-note badge-note-secondary">&#9834;</span>
        </div>
        <div class="logo-copy">
          <div class="logo">{{ nusicName }}</div>
          <div class="logo-subtitle">音乐内容管理系统</div>
        </div>
      </div>
    </div>
    <div class="header-right">
      <div class="header-user-con">
        <div class="user-status">
          <span class="status-dot"></span>
          <span>在线管理中</span>
        </div>
        <div class="user-avator">
          <img :src="displayAvatar" alt="管理员头像" />
        </div>
        <el-dropdown class="user-name" trigger="click" @command="handleCommand">
          <span class="el-dropdown-link">
            {{ username }}
            <i class="el-icon-caret-bottom" ></i>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="loginout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { defineComponent, computed, ref, onMounted } from "vue";
import { useStore } from "vuex";
import mixin from "@/mixins/mixin";
import { Expand, Fold } from "@element-plus/icons-vue";
import emitter from "@/utils/emitter";
import { HttpManager } from "@/api";
import { RouterName, MUSICNAME } from "@/enums";
import adminAvatarCartoon from "@/assets/images/admin-avatar-cartoon.svg";
import { clearStoredAdminInfo, getStoredAdminInfo } from "@/utils/admin-auth";

export default defineComponent({
  components: {
    Expand,
    Fold,
  },
  setup() {
    const { routerManager } = mixin();
    const store = useStore();

    const collapse = ref(false);
    const userPic = computed(() => store.getters.userPic);
    const displayAvatar = computed(() => {
      const rawAvatar = String(userPic.value || "").trim();
      if (!rawAvatar || rawAvatar === "/img/avatorImages/user.jpg") {
        return adminAvatarCartoon;
      }
      return HttpManager.attachImageUrl(rawAvatar);
    });
    const nusicName = ref(MUSICNAME);
    const username = ref(resolveAdminName());

    onMounted(() => {
      username.value = resolveAdminName();
      if (document.body.clientWidth < 1500) {
        collapseChage();
      }
    });

    function resolveAdminName() {
      const storedAdmin = getStoredAdminInfo();
      return storedAdmin?.username || storedAdmin?.name || "管理员";
    }

    // 侧边栏折叠
    function collapseChage() {
      collapse.value = !collapse.value;
      emitter.emit("collapse", collapse.value);
    }
    // 用户名下拉菜单选择事件
    async function handleCommand(command) {
      if (command === "loginout") {
        // 清除登录状态
        clearStoredAdminInfo();
        await HttpManager.logout();
        // 跳转到登录页
        routerManager(RouterName.SignIn, { path: RouterName.SignIn });

      }
    }
    return {
      nusicName,
      username,
      userPic,
      displayAvatar,
      collapse,
      collapseChage,
      handleCommand,
      attachImageUrl: HttpManager.attachImageUrl,
    };
  },
});
</script>
<style scoped>
.header {
  position: absolute;
  z-index: 1200;
  top: 18px;
  left: 18px;
  right: 18px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  border-radius: 24px;
  font-size: 20px;
  color: var(--admin-text);
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.68);
  box-shadow: 0 16px 40px rgba(21, 39, 69, 0.08);
  backdrop-filter: blur(18px);
}

.header-left,
.header-user-con {
  display: flex;
  align-items: center;
}

.collapse-btn {
  display: flex;
  width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: rgba(15, 23, 41, 0.04);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.collapse-btn:hover {
  color: var(--admin-primary);
  background: rgba(31, 107, 255, 0.1);
}

.logo-group {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-left: 14px;
}

.logo-badge {
  display: flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(114, 230, 223, 0.96) 0%, rgba(62, 171, 255, 0.94) 58%, rgba(31, 107, 255, 0.96) 100%);
  box-shadow: 0 12px 20px rgba(31, 107, 255, 0.16);
}

.logo-badge::before {
  content: "";
  position: absolute;
  inset: 4px;
  border-radius: 11px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.04));
}

.badge-glow {
  position: absolute;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  filter: blur(6px);
}

.badge-note {
  position: absolute;
  z-index: 1;
  line-height: 1;
  user-select: none;
  color: rgba(92, 88, 189, 0.98);
  text-shadow: 0 4px 10px rgba(78, 74, 176, 0.16);
  font-weight: 700;
}

.badge-note-primary {
  font-size: 24px;
  transform: translate(-3px, 1px) rotate(-8deg);
}

.badge-note-secondary {
  font-size: 14px;
  transform: translate(9px, -8px) rotate(8deg);
}

.logo-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header .logo {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.logo-subtitle {
  font-size: 12px;
  color: var(--admin-text-tertiary);
  letter-spacing: 0.06em;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 18px;
  padding: 0 14px;
  height: 38px;
  border-radius: 999px;
  background: rgba(20, 185, 123, 0.08);
  color: var(--admin-success);
  font-size: 12px;
  font-weight: 700;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--admin-success);
  box-shadow: 0 0 0 6px rgba(20, 185, 123, 0.12);
}

.user-name {
  margin-left: 12px;
  color: var(--admin-text);
  font-size: 14px;
  font-weight: 700;
}

.user-avator img {
  display: block;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 22px rgba(17, 37, 70, 0.14);
}

.el-dropdown-link {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
}

.el-dropdown-menu__item {
  text-align: center;
}

@media (max-width: 1200px) {
  .logo-subtitle,
  .user-status {
    display: none;
  }
}
</style>
