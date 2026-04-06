<template>
  <div class="music-page-shell personal-page">
    <div class="personal-header music-panel">
      <div class="personal-info-wrapper">
        <div class="avatar-box" @click="dialogTableVisible = true">
          <el-image class="user-avatar" fit="cover" :src="attachImageUrl(userPic)">
            <template #error>
              <div class="image-slot">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
          <div class="avatar-hover-mask">
            <el-icon><Camera /></el-icon>
          </div>
        </div>

        <div class="info-box">
          <div class="name-row">
            <span class="username">{{ personalInfo.username }}</span>
            <el-tag size="small" v-if="isSinger">歌手</el-tag>
          </div>
          <div class="introduction">{{ personalInfo.introduction || '暂无个人介绍' }}</div>
          <div class="info-items">
             <span v-if="personalInfo.userSex !== undefined">性别: {{ getUserSex(personalInfo.userSex) }}</span>
             <span v-if="personalInfo.location">地区: {{ personalInfo.location }}</span>
          </div>
        </div>
      </div>

      <div class="action-box">
        <el-button v-if="isSinger" type="primary" round :icon="UploadIcon" @click="openAddDialog">上传歌曲</el-button>
        <el-button v-else type="success" round @click="applySinger">申请成为歌手</el-button>
        <el-button round :icon="Edit" @click="goPage">编辑资料</el-button>
      </div>
    </div>

    <div class="personal-body">
      <section class="body-panel music-panel">
        <div class="section-title">我的歌曲</div>
        <PersonalSongList 
          :songList="currentSongList" 
          :show="true" 
          @changeData="changeData"
          @edit="handleEditSong"
        ></PersonalSongList>
      </section>

      <section class="body-panel music-panel">
        <div class="section-title">最近播放</div>
        <SongList 
          :songList="recentPlayList" 
          :show="false"  ></SongList>
      </section>
    </div>

    <el-dialog v-model="dialogTableVisible" title="修改头像" width="min(760px, 92vw)">
      <upload></upload>
    </el-dialog>

    <el-dialog title="上传新歌曲" v-model="centerDialogVisible" width="500px" :close-on-click-modal="false">
      <el-form label-width="100px" :model="registerForm" ref="addFormRef">
        <el-form-item label="歌曲名" prop="name">
          <el-input v-model="registerForm.name" placeholder="请输入歌名"></el-input>
        </el-form-item>
        <el-form-item label="专辑/介绍" prop="introduction">
          <el-input v-model="registerForm.introduction" placeholder="请输入专辑或歌曲介绍"></el-input>
        </el-form-item>
        <!-- <el-form-item label="歌词文本" prop="lyric">
          <el-input type="textarea" :rows="3" v-model="registerForm.lyric" placeholder="可选：在此粘贴歌词"></el-input>
        </el-form-item> -->
        
        <el-form-item label="歌词文件(.lrc)">
           <el-upload
            class="upload-demo"
            action=""
            :auto-upload="false"
            :limit="1"
            accept=".lrc"
            :on-change="(file) => handleFileChange(file, 'lrc')"
            :on-remove="() => handleFileRemove('lrc')"
            :with-credentials="true"
           >
             <el-button type="info" plain size="small">选择LRC文件</el-button>
           </el-upload>
        </el-form-item>

        <el-form-item label="歌曲文件" required>
           <el-upload
            class="upload-demo"
            action=""
            :auto-upload="false"
            :limit="1"
            accept=".mp3,.flac,.wav"
            :on-change="(file) => handleFileChange(file, 'song')"
            :on-remove="() => handleFileRemove('song')"
           >
             <el-button type="primary" plain size="small">选择音频文件</el-button>
           </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="centerDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitAddSong" :loading="loading">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="编辑歌曲信息" v-model="editDialogVisible" width="500px">
      <el-form label-width="80px" :model="editForm">
        <el-form-item label="歌曲名">
          <el-input v-model="editForm.name"></el-input>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.introduction"></el-input>
        </el-form-item>
        <el-form-item label="歌词">
          <el-input type="textarea" :rows="6" v-model="editForm.lyric"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="saveSongEdit" :loading="loading">保存修改</el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, watch, reactive, onMounted } from "vue";
import { useStore } from "vuex";
import { Edit, Upload as UploadIcon, Picture, Camera } from "@element-plus/icons-vue";
import PersonalSongList from "@/components/PersonalSongList.vue";
import Upload from "../setting/Upload.vue";
import mixin from "@/mixins/mixin";
import { HttpManager } from "@/api";
import { RouterName } from "@/enums";
import { ElMessage, ElMessageBox } from "element-plus";
import axios from 'axios';
import  SongList  from "@/components/SongList.vue";

interface SingerCheckResponse {
  code: number;
  success: boolean;
  message: string;
  type: string;
  data: number | null;
}

export default defineComponent({
  components: { Upload, PersonalSongList, Picture, Camera, SongList },
  setup() {
    const store = useStore();
    const { routerManager, getUserSex } = mixin();
    
    const recentPlayList = ref([]);//最近播放列表状态
    // 状态定义
    const loading = ref(false);
    const centerDialogVisible = ref(false); // 添加弹窗
    const editDialogVisible = ref(false);   // 编辑弹窗
    const dialogTableVisible = ref(false);  // 头像弹窗
    
    // 用户信息
    const userId = computed(() => store.getters.userId);
    const userPic = computed(() => store.getters.userPic);
    const isSinger = ref(false);
    const singerId = ref<number | null>(null);
    const currentSongList = ref([]);
    
    const personalInfo = reactive({
      username: "",
      userSex: "",
      birth: "",
      location: "",
      introduction: "",
    });

    // 表单数据
    const registerForm = reactive({
      name: "",
      introduction: "",
      lyric: "",
    });
    
    // 暂存上传的文件对象
    const uploadFiles = reactive({
        song: null as File | null,
        lrc: null as File | null
    });

    const editForm = reactive({
      id: "",
      singerId: "",
      name: "",
      introduction: "",
      lyric: "",
    });

    // --- 生命周期 & 监听 ---
    watch(userPic, () => {
      dialogTableVisible.value = false;
    });

    onMounted(() => {
      if (userId.value) {
        initData();
        getRecentPlayList(userId.value); // 在页面加载时获取最近播放
      }
    });

// --- 获取最近播放列表的方法 ---
async function getRecentPlayList(uid: number) {
      try {
        const result = (await HttpManager.getRecentPlayHistory(uid)) as any;
        if (result.success && result.data) {
          // 注意：如果后端返回的是 SongWithPlayTimeDTO，你需要将其映射为 PersonalSongList 可用的格式
          recentPlayList.value = result.data.map(item => ({
              ...item, // 包含 songId, name, pic 等基础信息
              // 如果你的列表组件需要特定的字段，这里进行映射
          }));
        }
      } catch (error) {
        ElMessage.warning("最近播放暂时加载失败");
      }
    }

    async function initData() {
        await getUserInfo(userId.value);
        await getConsumerSong(userId.value);
    }

    // --- 业务逻辑 ---

    function goPage() {
      routerManager(RouterName.Setting, { path: RouterName.Setting });
    }

    async function getUserInfo(id) {
      try {
        const result = (await HttpManager.getUserOfId(id)) as any;
        if (result.data && result.data.length > 0) {
            const data = result.data[0];
            personalInfo.username = data.username;
            personalInfo.userSex = data.sex;
            personalInfo.birth = data.birth;
            personalInfo.introduction = data.introduction;
            personalInfo.location = data.location;
        }
      } catch (e) {
          ElMessage.warning("个人信息加载失败，请稍后重试");
      }
    }

    async function getConsumerSong(uid) {
      try {
        currentSongList.value = [];
        const singerResult = (await HttpManager.getUserSinger(uid)) as SingerCheckResponse;
        // 如果是歌手，获取歌曲
        if (singerResult.data != null) {
            isSinger.value = true;
            singerId.value = singerResult.data;
            const songResult = (await HttpManager.songOfuserSingerId()) as any;
            currentSongList.value = songResult.data;
        } else {
            isSinger.value = false;
            singerId.value = null;
        }
      } catch (error) {
        ElMessage.warning("歌曲列表暂时加载失败");
      }
    }

    function changeData() {
      // 刷新“我的歌曲”列表
      getConsumerSong(userId.value);
      // 刷新“最近播放”列表
      getRecentPlayList(userId.value);
    }

    // --- 上传逻辑 ---

    function resetUploadForm() {
        registerForm.name = "";
        registerForm.introduction = "";
        registerForm.lyric = "";
        uploadFiles.song = null;
        uploadFiles.lrc = null;
    }

    async function applySinger() {
      if (isSinger.value) {
        ElMessage.info("你已经是歌手了");
        return;
      }

      try {
        await ElMessageBox.confirm(
          "申请成为歌手后，你的资料会展示在歌手列表中，并可以上传自己的歌曲，是否继续？",
          "申请成为歌手",
          {
            confirmButtonText: "继续申请",
            cancelButtonText: "取消",
            type: "info",
          }
        );
      } catch {
        return;
      }

      loading.value = true;
      try {
        const singerData = {
          name: personalInfo.username,
          sex: personalInfo.userSex,
          birth: personalInfo.birth,
          location: personalInfo.location,
          introduction: personalInfo.introduction,
          userId: userId.value
        };
        const result = (await HttpManager.becomeSinger(singerData)) as any;
        ElMessage({
          message: result.message || "申请失败",
          type: result.type || (result.success ? "success" : "error"),
        });
        if (result.success || result.type === "success") {
          isSinger.value = true;
          await getConsumerSong(userId.value);
        }
      } catch (error) {
        ElMessage.error("申请成为歌手失败");
      } finally {
        loading.value = false;
      }
    }

    async function openAddDialog() {
        if (!isSinger.value || !singerId.value) {
            ElMessage.warning("请先申请成为歌手后再上传歌曲");
            return;
        }
        resetUploadForm();
        centerDialogVisible.value = true;
    }

    // 处理文件选择
    function handleFileChange(file, type) {
        uploadFiles[type] = file.raw;
    }
    function handleFileRemove(type) {
        uploadFiles[type] = null;
    }

    // 提交新歌曲 (替换了原有的 XMLHttpRequest)
    async function submitAddSong() {
        if (!uploadFiles.song) {
            return ElMessage.warning("请选择音频文件");
        }
        if (!registerForm.name) {
            return ElMessage.warning("请输入歌曲名称");
        }

        loading.value = true;
        const formData = new FormData();
        formData.append("singerId", String(singerId.value));
        // 名字格式：歌手名-歌名
        formData.append("name", `${personalInfo.username}-${registerForm.name}`);
        formData.append("introduction", registerForm.introduction);
        formData.append("lyric", registerForm.lyric || "[00:00:00]纯音乐，暂无歌词");
        
        if (uploadFiles.song) formData.append("file", uploadFiles.song);
        if (uploadFiles.lrc) formData.append("lrcfile", uploadFiles.lrc);

        try {
            // 这里使用了 axios，你也可以封装到 HttpManager 中
            const res = await axios.post(HttpManager.attachImageUrl(`/song/add`), formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            const result = res.data;
            
            if (result.code === 1 || result.success) { // 根据你的后端返回结构判断
                 ElMessage.success(result.message || "上传成功");

              // 清空表单数据
              registerForm.name = "";
              registerForm.introduction = "";
              registerForm.lyric = "";
              uploadFiles.song = null;
              uploadFiles.lrc = null;

                 centerDialogVisible.value = false;
                 changeData(); // 刷新列表
            } else {
                 ElMessage.error(result.message || "上传失败");
            }
        } catch (error) {
            ElMessage.error("上传发生错误");
        } finally {
            loading.value = false;
        }
    }

    // --- 编辑逻辑 (新增) ---

    // 此方法需要子组件 emit 'edit' 事件时触发
    function handleEditSong(row) {
        // 将行数据填充到编辑表单
        editForm.id = row.id;
        editForm.singerId = row.singerId;
        editForm.name = row.name;
        editForm.introduction = row.introduction;
        editForm.lyric = row.lyric;
        
        editDialogVisible.value = true;
    }

    async function saveSongEdit() {
        loading.value = true;
        try {
            editForm.name = personalInfo.username+"-"+editForm.name;
            const result = (await HttpManager.updateSongMsg(editForm)) as any;
            ElMessage({
                message: result.message,
                type: result.type
            });
            if (result.success) {
                editDialogVisible.value = false;
                changeData(); // 刷新列表
            }
        } catch (error) {
            ElMessage.error("修改失败");
        } finally {
            loading.value = false;
        }
    }

    return {
      Edit, UploadIcon, Picture, Camera,
      loading,
      userPic,
      dialogTableVisible,
      centerDialogVisible,
      editDialogVisible,
      personalInfo,
      registerForm,
      editForm,
      currentSongList,
      isSinger,
      recentPlayList,
      attachImageUrl: HttpManager.attachImageUrl,
      getUserSex,
      goPage,
      changeData,
      openAddDialog,
      applySinger,
      submitAddSong,
      handleFileChange,
      handleFileRemove,
      handleEditSong, // 暴露给模板
      saveSongEdit
    };
  },
});
</script>

<style lang="scss" scoped>
@import "@/assets/css/global.scss";

.personal-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.personal-header {
  position: relative;
  padding: clamp(24px, 4vw, 38px);
  display: flex;
  justify-content: space-between;
  align-items: flex-end;

  .personal-info-wrapper {
    display: flex;
    align-items: flex-end;
  }
}

.avatar-box {
  position: relative;
  width: 148px;
  height: 148px;
  border-radius: 36px;
  border: 1px solid var(--app-panel-border);
  box-shadow: var(--app-panel-shadow-soft);
  overflow: hidden;
  cursor: pointer;
  margin-right: 30px;
  flex-shrink: 0;

  .user-avatar {
    width: 100%;
    height: 100%;
  }

  .avatar-hover-mask {
    position: absolute;
    inset: 0;
    background: rgba(8, 12, 24, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    color: #fff;
    font-size: 30px;
    opacity: 0;
    transition: opacity 0.3s;
  }

  &:hover .avatar-hover-mask {
    opacity: 1;
  }
}

.info-box {
  margin-bottom: 10px;

  .name-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;

    .username {
      font-size: clamp(34px, 4vw, 52px);
      font-weight: 800;
      color: var(--app-title);
      margin-right: 0;
    }
  }

  .introduction {
    font-size: 14px;
    color: var(--app-text-muted);
    margin-bottom: 15px;
    max-width: 560px;
    line-height: 1.8;
  }

  .info-items {
    font-size: 14px;
    color: var(--app-text-soft);

    span {
      margin-right: 20px;
    }
  }
}

.action-box {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
}

.personal-body {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.body-panel {
  padding: 24px;
}

.section-title {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 20px;
  color: var(--app-title);
}

@media screen and (max-width: 768px) {
  .personal-header {
    flex-direction: column;
    align-items: center;
    padding: 22px 18px;

    .personal-info-wrapper {
      flex-direction: column;
      align-items: center;
      text-align: center;
    }

    .avatar-box {
      margin-right: 0;
      margin-bottom: 15px;
      width: 120px;
      height: 120px;
      border-radius: 28px;
    }

    .action-box {
      margin-top: 20px;
      width: 100%;
      justify-content: center;
      flex-wrap: wrap;
    }
  }

  .body-panel {
    padding: 18px;
  }
}
</style>
