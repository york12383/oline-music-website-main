<template>
  <div class="content">
    <el-table highlight-current-row :data="dataList" @row-click="handleClick">
      
      <el-table-column label="封面" width="80" align="center">
        <template #default="scope">
          <div class="song-cover-box">
            <el-image 
              :src="attachImageUrl(scope.row.pic)" 
              fit="cover" 
              class="cover-img"
            >
              <template #error>
                <div class="image-slot"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="songName" label="歌曲" />
      <el-table-column prop="singerName" label="歌手" />
      <el-table-column prop="introduction" label="专辑" />

      <el-table-column label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="getSongStatusType(scope.row.type)" effect="light">
            {{ getSongStatusLabel(scope.row.type) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="160" align="center">
        <template #default="scope">
          
          <el-button 
            v-if="scope.row.type === 2" 
            type="primary" 
            link 
            size="small" 
            @click.stop="publishSong(scope.row)"
          >
            发布
          </el-button>

          <el-button 
            type="primary" 
            link 
            size="small" 
            :icon="Edit" 
            @click.stop="emit('edit', scope.row)"
          >
            编辑
          </el-button>

          <el-dropdown trigger="click" @click.stop>
            <el-icon class="more-icon"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                
                <el-dropdown-item class="update-pic-item">
                  <el-upload
                    :action="updateSongImg(scope.row.id)"
                    :show-file-list="false"
                    :on-success="handleImgSuccess"
                    :before-upload="beforeImgUpload"
                  >
                    <span class="dropdown-upload-btn">
                      <el-icon><Picture /></el-icon>
                      更新封面
                    </span>
                  </el-upload>
                </el-dropdown-item>
                
                <!-- <el-dropdown-item
                  :icon="Download"
                  @click="
                    downloadMusic({
                      songUrl: scope.row.url,
                      songName: scope.row.name,
                    })
                  "
                >下载</el-dropdown-item> -->
                
                <el-dropdown-item 
                  :icon="Delete" 
                  v-if="show" 
                  @click="deleteConsumerSong({ id: scope.row.id })"
                  divided
                >
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script lang="ts">
import { defineComponent, getCurrentInstance, toRefs, computed, reactive, inject } from "vue";
import { useStore } from "vuex";
import { MoreFilled, Delete, Download, Edit, Picture } from "@element-plus/icons-vue";

import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api";
import { Icon } from "@/enums";
import { ElMessage } from "element-plus";

export default defineComponent({
  name: "PersonalSongList",
  components: {
    MoreFilled,
    Picture,
  },
  props: {
    songList: Array,
    show: {
      default: false
    }
  },
  emits: ["changeData", "edit"], 
  setup(props, { emit }) { 
    const { getSongTitle, getSingerName, playMusic, checkStatus, downloadMusic } = mixin(); 
    const { proxy } = getCurrentInstance();
    const store = useStore();

    const { songList } = toRefs(props);

    const dataList = computed(() => {
      const list = [];
      songList.value.forEach((item: any, index) => {
        item["songName"] = getSongTitle(item.name);
        item["singerName"] = getSingerName(item.name);
        item["index"] = index;
        // 确保你的后端 Song 实体返回了 type 字段，这里直接使用即可
        list.push(item);
      });
      return list;
    });


    // 检查图片格式和大小
    function beforeImgUpload(file) {
      const isJPGOrPNG = file.type === 'image/jpeg' || file.type === 'image/png';
      const isLt2M = file.size / 1024 / 1024 < 2;

      if (!isJPGOrPNG) {
        ElMessage.error('上传头像图片只能是 JPG 或 PNG 格式!');
      }
      if (!isLt2M) {
        ElMessage.error('上传头像图片大小不能超过 2MB!');
      }
      return isJPGOrPNG && isLt2M;
    }

    
    // 歌曲状态: 启用1 隐藏2 审核3 禁用4
    function getSongStatusLabel(type: number) {
      switch (type) {
        case 1: return '启用';
        case 2: return '隐藏';
        case 3: return '审核中';
        case 4: return '禁用';
        default: return '未知';
      }
    }

    function getSongStatusType(type: number) {
      switch (type) {
        case 1: return 'success'; // 绿色
        case 2: return 'info';    // 灰色
        case 3: return 'warning'; // 橙色
        case 4: return 'danger';  // 红色
        default: return 'info';
      }
    }

    // --- 图片处理逻辑 ---

    function attachImageUrl(url) {
      return HttpManager.attachImageUrl(url);
    }

    function updateSongImg(id) {
      return HttpManager.attachImageUrl(`/song/img/update?id=${id}`);
    }

    function handleImgSuccess(res) {
      (proxy as any).$message({
        message: res.message,
        type: res.type,
      });
      if (res.success) {
        emit("changeData"); 
      }
    }
    
    // --- 原有逻辑 ---

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

    async function deleteConsumerSong({ id }) {
      if (!checkStatus()) return;
      
      const result = (await HttpManager.deleteSong(id)) as any; // 假设已定义 ResponseBody

      (proxy as any).$message({
        message: result.message,
        type: result.type,
      });

      emit("changeData");
    }

    // ✨ 新增：发布歌曲功能
    async function publishSong(row) {
      try {
        const result = (await HttpManager.updateSongStatus({
          songId: row.id,           // 歌曲ID
          singerId: row.singerId // 歌手ID
        })) as any;
        
        (proxy as any).$message({
          message: result.message,
          type: result.type,
        });
        if (result.success) {
          // 更新本地数据状态
          const index = dataList.value.findIndex(item => item.id === row.id);
          if (index !== -1) {
            dataList.value[index].type = 3;
          }
          emit("changeData");
        }
      } catch (error) {
        ElMessage.error("发布失败");
      }
    }


    return {
      dataList,
      Delete,
      Download,
      Edit,
      Picture,
      beforeImgUpload,
      
      attachImageUrl,
      updateSongImg,
      handleImgSuccess,
      
      handleClick,
      downloadMusic,
      deleteConsumerSong,
      emit,
      publishSong, 
      
      getSongStatusLabel,
      getSongStatusType
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

.song-cover-box {
  width: 60px;
  height: 60px;
  margin: 0 auto;
  overflow: hidden;
  border-radius: 16px;
}
.cover-img {
  width: 100%;
  height: 100%;
  display: block;
}
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 20px;
}

.more-icon {
    margin-left: 8px;
    cursor: pointer;
    font-size: 16px;
    color: var(--app-text-soft);
    vertical-align: middle;
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

.update-pic-item {
  padding: 0; 
  & .dropdown-upload-btn {
    display: flex;
    align-items: center;
    padding: 9px 12px;
    width: 100%;
    color: var(--el-text-color-regular);
    
    .el-icon {
      margin-right: 8px;
    }
    
    &:hover {
      background-color: var(--el-dropdown-menu-hover-fill);
      color: var(--el-dropdown-menu-hover-color);
    }
  }
}
</style>
