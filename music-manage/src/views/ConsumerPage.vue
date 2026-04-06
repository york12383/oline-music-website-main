<template>
  <div class="container">
    <div class="handle-box">
      <el-button type="danger" @click="deleteAll">批量删除</el-button>
      <el-input v-model="searchWord" placeholder="筛选用户" class="handle-input"></el-input>
    </div>

    <el-table ref="tableRef" height="550px" border size="small" :data="data" :row-key="getRowKey" @selection-change="handleSelectionChange">
      <el-table-column type="selection" :reserve-selection="true" width="40" align="center"></el-table-column>
      <el-table-column label="ID" prop="id" width="50" align="center"></el-table-column>
      <el-table-column label="用户头像" width="102" align="center">
        <template v-slot="scope">
          <img :src="attachImageUrl(scope.row.avator)" class="user-avatar" />
        </template>
      </el-table-column>
      <el-table-column label="用户名" prop="username" width="80" align="center"></el-table-column>
      <el-table-column label="性别" width="50" align="center">
        <template v-slot="scope">
          <div>{{ formatUserSex(scope.row.sex) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="手机号码" prop="phoneNum" width="120" align="center"></el-table-column>
      <el-table-column label="邮箱" prop="email" width="120" align="center"></el-table-column>
      <el-table-column label="生日" width="120" align="center">
        <template v-slot="scope">
          <div>{{ getBirth(scope.row.birth) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="签名" prop="introduction"></el-table-column>
      <el-table-column label="地区" prop="location" width="70" align="center"></el-table-column>
      <el-table-column label="收藏列表" width="110" align="center">
        <template v-slot="scope">
          <el-button class="collect-link-button" plain type="primary" @click="goCollectPage(scope.row.id)">查看收藏</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center">
        <template v-slot="scope">
          <el-button type="danger" @click="deleteRow(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
      <el-table-column label="修改" width="90" align="center">
        <template v-slot="scope">
          <el-upload
            :action="'http://localhost:8888/user/avatar/update?id=' + scope.row.id"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeImgUpload"
          >
            <el-button plain>更新头像</el-button>
          </el-upload>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pagination"
      background
      layout="total, prev, pager, next"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="tableData.length"
      @current-change="handleCurrentChange"
    >
    </el-pagination>
  </div>

  <yin-del-dialog :delVisible="delVisible" @confirm="confirm" @cancelRow="delVisible = $event"></yin-del-dialog>
</template>

<script lang="ts">
import { defineComponent, getCurrentInstance, watch, ref, reactive, computed, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessageBox } from "element-plus";
import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api";
import { RouterName } from "@/enums";
import YinDelDialog from "@/components/dialog/YinDelDialog.vue";
import { getBirth } from "@/utils";
import { isSameRouteQuery, mergeRouteQuery, normalizeQueryValue, parsePositiveIntQuery } from "@/utils/route-query";

export default defineComponent({
  components: {
    YinDelDialog,
  },
  setup() {
    const { proxy } = getCurrentInstance() as any;
    const route = useRoute();
    const router = useRouter();
    const { routerManager, beforeImgUpload } = mixin();

    const tableData = ref<any[]>([]);
    const tempDate = ref<any[]>([]);
    const tableRef = ref<any>(null);
    const pageSize = ref(5);
    const currentPage = ref(parsePositiveIntQuery(route.query.page, 1));
    const isSyncingRouteState = ref(false);
    const isRestoringSelection = ref(false);
    const selectedRowsById = new Map<string, any>();

    const data = computed(() => {
      return tableData.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value);
    });

    const getRowKey = (row: any) => row.id;

    function syncMultipleSelection() {
      multipleSelection.value = Array.from(selectedRowsById.values());
    }

    function pruneCrossPageSelection(records = tableData.value) {
      const validIds = new Set((records || []).map((item) => String(item.id)));
      Array.from(selectedRowsById.keys()).forEach((id) => {
        if (!validIds.has(id)) {
          selectedRowsById.delete(id);
        }
      });
      syncMultipleSelection();
    }

    function clearCrossPageSelection() {
      selectedRowsById.clear();
      syncMultipleSelection();

      if (tableRef.value?.clearSelection) {
        isRestoringSelection.value = true;
        tableRef.value.clearSelection();
        isRestoringSelection.value = false;
      }
    }

    async function restoreCurrentPageSelection() {
      await nextTick();
      if (!tableRef.value?.clearSelection || !tableRef.value?.toggleRowSelection) {
        return;
      }

      isRestoringSelection.value = true;
      tableRef.value.clearSelection();
      data.value.forEach((row) => {
        if (selectedRowsById.has(String(row.id))) {
          tableRef.value.toggleRowSelection(row, true);
        }
      });
      await nextTick();
      isRestoringSelection.value = false;
    }

    const searchWord = ref(normalizeQueryValue(route.query.keyword));
    watch(searchWord, () => {
      if (isSyncingRouteState.value) return;
      clearCrossPageSelection();
      currentPage.value = 1;
      applyFilter();
      syncListQuery();
    });

    watch(currentPage, () => {
      if (isSyncingRouteState.value) return;
      syncListQuery();
    });

    watch(
      () => [route.query.page, route.query.keyword],
      () => {
        const nextPage = parsePositiveIntQuery(route.query.page, 1);
        const nextKeyword = normalizeQueryValue(route.query.keyword);
        const filterContextChanged = nextKeyword !== searchWord.value;

        if (nextPage === currentPage.value && nextKeyword === searchWord.value) {
          return;
        }

        isSyncingRouteState.value = true;
        if (filterContextChanged) {
          clearCrossPageSelection();
        }
        currentPage.value = nextPage;
        searchWord.value = nextKeyword;
        isSyncingRouteState.value = false;
        applyFilter();
      }
    );

    watch(
      data,
      () => {
        void restoreCurrentPageSelection();
      },
      { flush: "post" }
    );

    const isAdminAuthError = (message = "") => {
      return ["请先登录管理员账户", "未登录", "权限不足", "当前登录的管理员账户不存在"].some((keyword) =>
        message.includes(keyword)
      );
    };

    const handleAdminAuthFailure = (message = "管理员登录状态已失效，请重新登录") => {
      localStorage.removeItem("adminInfo");
      sessionStorage.removeItem("adminInfo");
      (proxy as any).$message({ message, type: "warning" });
      router.replace("/");
    };

    getData();

    function buildListQuery() {
      return {
        page: currentPage.value > 1 ? currentPage.value : undefined,
        keyword: searchWord.value.trim() || undefined,
      };
    }

    async function syncListQuery() {
      const nextQuery = mergeRouteQuery(route.query, buildListQuery());
      if (!isSameRouteQuery(route.query, nextQuery)) {
        await router.replace({ path: route.path, query: nextQuery });
      }
    }

    function applyFilter() {
      const keyword = searchWord.value.trim();
      if (!keyword) {
        tableData.value = [...tempDate.value];
      } else {
        tableData.value = tempDate.value.filter((item) => item.username?.includes(keyword));
      }

      const maxPage = Math.max(1, Math.ceil(tableData.value.length / pageSize.value));
      if (currentPage.value > maxPage) {
        currentPage.value = maxPage;
      }
    }

    async function getData() {
      tableData.value = [];
      tempDate.value = [];
      try {
        const result = (await HttpManager.getAllUser()) as ResponseBody;
        if (!result?.success) {
          if (isAdminAuthError(result?.message || "")) {
            handleAdminAuthFailure("管理员登录状态已失效，请重新登录");
            return;
          }
          (proxy as any).$message({
            message: result?.message || "获取用户列表失败",
            type: result?.type || "error",
          });
          return;
        }

        tempDate.value = Array.isArray(result.data) ? result.data : [];
        applyFilter();
        pruneCrossPageSelection(tableData.value);
      } catch (error) {
        clearCrossPageSelection();
        (proxy as any).$message({ message: "获取用户列表失败", type: "error" });
      }
    }

    function handleCurrentChange(val) {
      currentPage.value = val;
    }

    function goCollectPage(id) {
      const breadcrumbList = reactive([
        {
          path: RouterName.Consumer,
          query: buildListQuery(),
          name: "用户管理",
        },
        {
          path: "",
          name: "收藏列表",
        },
      ]);
      proxy.$store.commit("setBreadcrumbList", breadcrumbList);
      routerManager(RouterName.Collect, { path: RouterName.Collect, query: { id } });
    }

    const idx = ref(-1);
    const multipleSelection = ref<any[]>([]);
    const delVisible = ref(false);

    async function confirm() {
      const result = (await HttpManager.deleteUser(idx.value)) as ResponseBody;
      (proxy as any).$message({
        message: result.message,
        type: result.type,
      });
      if (result?.success) {
        selectedRowsById.delete(String(idx.value));
        syncMultipleSelection();
        getData();
      }
      delVisible.value = false;
    }
    function deleteRow(id) {
      idx.value = id;
      delVisible.value = true;
    }
    function handleSelectionChange(val) {
      if (isRestoringSelection.value) {
        return;
      }

      const currentPageIds = new Set(data.value.map((item) => String(item.id)));
      currentPageIds.forEach((id) => selectedRowsById.delete(id));
      val.forEach((item) => selectedRowsById.set(String(item.id), item));
      syncMultipleSelection();
    }
    async function deleteAll() {
      if (!multipleSelection.value.length) {
        (proxy as any).$message({ message: "请先选择要删除的用户", type: "warning" });
        return;
      }

      try {
        await ElMessageBox.confirm(`确定删除选中的 ${multipleSelection.value.length} 位用户吗？`, "批量删除", {
          confirmButtonText: "确定删除",
          cancelButtonText: "取消",
          type: "warning",
        });
      } catch {
        return;
      }

      const ids = multipleSelection.value.map((item) => item.id);
      const results = await Promise.all(
        ids.map(async (id) => {
          try {
            return await HttpManager.deleteUser(id);
          } catch (error) {
            return { success: false };
          }
        })
      );
      const successCount = results.filter((item: any) => item?.success).length;
      const failCount = ids.length - successCount;

      ids.forEach((id) => selectedRowsById.delete(String(id)));
      syncMultipleSelection();

      if (failCount === 0) {
        (proxy as any).$message({ message: `已删除 ${successCount} 位用户`, type: "success" });
      } else if (successCount === 0) {
        (proxy as any).$message({ message: "批量删除失败", type: "error" });
      } else {
        (proxy as any).$message({ message: `已删除 ${successCount} 位用户，${failCount} 位删除失败`, type: "warning" });
      }

      await getData();
    }

    function handleAvatarSuccess() {
      location.reload();
    }

    function formatUserSex(value) {
      if (value === 0) return "女";
      if (value === 1) return "男";
      if (value === 2) return "保密";
      return "未知";
    }

    return {
      searchWord,
      tableRef,
      data,
      tableData,
      delVisible,
      pageSize,
      currentPage,
      deleteAll,
      handleSelectionChange,
      handleCurrentChange,
      getRowKey,
      formatUserSex,
      getBirth,
      deleteRow,
      handleAvatarSuccess,
      confirm,
      goCollectPage,
      beforeImgUpload,
      attachImageUrl: HttpManager.attachImageUrl,
    };
  },
});
</script>

<style scoped>
.container {
  padding: 28px;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 28px;
  border: 1px solid rgba(226, 234, 243, 0.92);
  box-shadow: 0 18px 40px rgba(17, 37, 70, 0.08);
}

.handle-box {
  width: auto;
  justify-content: flex-start;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

.handle-input {
  width: 260px;
}

.user-avatar {
  width: 78px;
  height: 78px;
  border-radius: 22px;
  object-fit: cover;
  box-shadow: 0 14px 28px rgba(17, 37, 70, 0.12);
}

.collect-link-button {
  color: #ffffff !important;
  border-color: rgba(58, 128, 255, 0.92) !important;
  background: linear-gradient(135deg, #438fff 0%, #1f6bff 100%) !important;
  box-shadow: 0 10px 20px rgba(31, 107, 255, 0.18);
}

.collect-link-button:hover,
.collect-link-button:focus-visible {
  color: #ffffff !important;
  border-color: rgba(70, 142, 255, 0.96) !important;
  background: linear-gradient(135deg, #589cff 0%, #357cff 100%) !important;
}
</style>
