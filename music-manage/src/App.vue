<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>
<script lang="ts" setup>
import { getCurrentInstance, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { HttpManager } from "@/api/index";
import { clearStoredAdminInfo, hasValidStoredAdminInfo } from "@/utils/admin-auth";
const { proxy } = getCurrentInstance() as any;
const router = useRouter();
const route = useRoute();
if (sessionStorage.getItem("dataStore")) {
  proxy.$store.replaceState(Object.assign({}, proxy.$store.state, JSON.parse(sessionStorage.getItem("dataStore"))));
}
const syncAdminSession = async () => {
  const hasLocalAdmin = hasValidStoredAdminInfo();
  if (!hasLocalAdmin) {
    clearStoredAdminInfo();
    return;
  }
  try {
    const result = (await HttpManager.getAdminSession()) as ResponseBody;
    if (result?.success) {
      return;
    }
  } catch (error) {
    void error;
  }
  clearStoredAdminInfo();
  if (route.path !== "/") {
    ElMessage.warning("Admin session expired, please sign in again");
    router.replace("/");
  }
};
onMounted(() => {
  syncAdminSession();
});
watch(
  () => route.fullPath,
  () => {
    syncAdminSession();
  }
);
window.addEventListener("beforeunload", () => {
  sessionStorage.setItem("dataStore", JSON.stringify(proxy.$store.state));
});
</script>
