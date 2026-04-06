<template>
  <!-- 歌曲列表 -->
  <div class="content">
    <el-table highlight-current-row :data="dataList" @row-click="handleClick">
      <el-table-column prop="songName" label="歌曲" />
      <el-table-column prop="singerName" label="歌手" />
      <el-table-column prop="introduction" label="专辑" />
      <el-table-column label="编辑" width="80" align="center">
        <template #default="scope">
          <el-dropdown>
            <el-icon @click="handleEdit(scope.row)"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  :icon="Download"
                  @click="
                    downloadMusic({
                      songUrl: scope.row.url,
                      songName: scope.row.name,
                    })
                  ">下载</el-dropdown-item>


            <el-tooltip content="添加到歌单" placement="top">
            <el-button 
              link 
              :icon="FolderAdd" 
              @click.stop="openAddDialog(scope.row.id)"
            >添加到歌单</el-button>
              </el-tooltip>



                <el-dropdown-item :icon="Delete" v-if="show" @click="deleteCollection({ id: scope.row.id })">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <AddToPlaylist 
      :dialog-visible="addDialogVisible" 
      :current-song-id="selectedSongId"
      @update:dialogVisible="addDialogVisible = $event"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent, getCurrentInstance, toRefs, computed, reactive, ref } from "vue";
import { useStore } from "vuex";
import { MoreFilled, Delete, Download ,FolderAdd} from "@element-plus/icons-vue";
import AddToPlaylist from "@/components/layouts/AddToPlaylist.vue"; // 引入组件
import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api";
import { Icon } from "@/enums";
import { ElMessage } from "element-plus";

export default defineComponent({
  components: {
    MoreFilled,
    AddToPlaylist,
  },
  props: {
    songList: Array,
    show: {
      default: false
    }
  },
  emits: ["changeData"],
  setup(props) {
    const { getSongTitle, getSingerName, playMusic, checkStatus, downloadMusic } = mixin();
    const { proxy } = getCurrentInstance();
    const store = useStore();

    const { songList } = toRefs(props);

    const iconList = reactive({
      dislike: Icon.Dislike,
      like: Icon.Like,
    });

    const songUrl = computed(() => store.getters.songUrl);
    const singerName = computed(() => store.getters.singerName);
    const songTitle = computed(() => store.getters.songTitle);
    const dataList = computed(() => {
      const list = [];
      songList.value.forEach((item: any, index) => {
        item["songName"] = getSongTitle(item.name);
        item["singerName"] = getSingerName(item.name);
        item["index"] = index;
        list.push(item);
      });
      return list;
    });


// 控制弹窗的变量
    const addDialogVisible = ref(false);
    const selectedSongId = ref('');

    // 打开弹窗的方法
    function openAddDialog(id) {
      if (!userId.value) {
        ElMessage.warning("请先登录");
        return;
      }
      selectedSongId.value = id;
      addDialogVisible.value = true;
    }


    function handleClick(row) {
      playMusic({
        id: row.id,
        url: row.url,
        pic: row.pic,
        index: row.index,
        name: row.name,
        lyric: row.lyric,
        currentSongList: songList.value,
      });
    }

    function handleEdit(row) {
      void row;
    }

    const userId = computed(() => store.getters.userId);

    async function deleteCollection({ id }) {
      if (!checkStatus()) return;

      const result = (await HttpManager.deleteCollection({ userId: userId.value, type: 0, songId: id })) as ResponseBody;
      (proxy as any).$message({
        message: result.message,
        type: result.type,
      });

      if (result.data === false) proxy.$emit("changeData", result.data);
    }

    return {
      dataList,
      iconList,
      Delete,
      Download,
      songUrl,
      singerName,
      songTitle,
      handleClick,
      handleEdit,
      downloadMusic,
      deleteCollection,
      FolderAdd,
      
      addDialogVisible,
      selectedSongId,
      openAddDialog
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.content {
  background: transparent;
  border-radius: 24px;
  padding: 0;
}

.content:deep(.el-table__row.current-row) {
  color: var(--app-title);
  font-weight: bold;
}

.content:deep(.el-table__row) {
  cursor: pointer;
}

.content:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: var(--app-table-row);
  --el-table-row-hover-bg-color: var(--app-table-row-hover);
  --el-table-header-bg-color: transparent;
  --el-table-border-color: var(--app-divider);
  color: var(--app-text);
}

.content:deep(.el-table__cell) {
  background: transparent;
}

.content:deep(.el-table .cell) {
  color: var(--app-text);
  padding-top: 4px;
  padding-bottom: 4px;
  line-height: 1.4;
}

.content:deep(.el-table th.el-table__cell .cell) {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.icon {
  @include icon(1.2em, var(--app-text));
}
</style>
