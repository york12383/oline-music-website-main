<template>
  <div class="avatar-picker">
    <div class="picker-summary">
      <div class="current-avatar">
        <img :src="attachImageUrl(currentAvatarPath)" alt="当前头像" />
      </div>
      <div class="summary-copy">
        <h3>选择你的头像</h3>
        <p>可以直接使用我们准备好的默认头像，也可以继续上传你自己的图片。</p>
      </div>
    </div>

    <div class="avatar-library">
      <div class="section-head">
        <span>默认头像库</span>
      </div>
      <div class="avatar-grid">
        <button
          v-for="avatar in defaultAvatarOptions"
          :key="avatar.id"
          type="button"
          class="avatar-option"
          :class="{ active: avatar.path === currentAvatarPath }"
          :disabled="selectingAvatar"
          @click="applyPresetAvatar(avatar.path)"
        >
          <img :src="attachImageUrl(avatar.path)" :alt="avatar.label" loading="lazy" />
          <span>{{ avatar.label }}</span>
        </button>
      </div>
    </div>

    <div class="upload-divider">或者上传自己的头像</div>

    <el-upload
      drag
      :action="uploadUrl()"
      :show-file-list="false"
      :before-upload="beforeAvatarUpload"
      :on-success="handleAvatarSuccess"
      :on-error="handleAvatarError"
      :disabled="uploadingAvatar || selectingAvatar"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">将文件拖到此处或点击上传</div>
      <template #tip>
        <p class="el-upload__tip">支持 {{ uploadTypes.join("、") }}，大小不超过 2MB</p>
      </template>
    </el-upload>
  </div>
</template>

<script lang="ts">
import { computed, defineComponent, getCurrentInstance, ref } from "vue";
import { useStore } from "vuex";
import { UploadFilled } from "@element-plus/icons-vue";
import { HttpManager } from "@/api";
import {
  DEFAULT_USER_AVATAR_OPTIONS,
  normalizeUserAvatarPath,
} from "@/constants/defaultAvatars";

export default defineComponent({
  components: {
    UploadFilled,
  },
  setup() {
    const { proxy } = getCurrentInstance() as any;
    const store = useStore();

    const uploadTypes = ref(["jpg", "jpeg", "png", "gif", "webp"]);
    const userId = computed(() => store.getters.userId);
    const userPic = computed(() => store.getters.userPic);
    const selectingAvatar = ref(false);
    const uploadingAvatar = ref(false);

    const currentAvatarPath = computed(() => normalizeUserAvatarPath(userPic.value));

    function uploadUrl() {
      return HttpManager.uploadUrl(userId.value);
    }

    function beforeAvatarUpload(file) {
      const maxSizeMb = 2;
      const fileSizeInMb = file.size / 1024 / 1024;
      const fileType = file.type.replace(/image\//, "").toLowerCase();
      const isExistFileType = uploadTypes.value.includes(fileType);

      if (fileSizeInMb > maxSizeMb || fileSizeInMb <= 0) {
        proxy.$message.error(`图片大小范围是 0~${maxSizeMb}MB!`);
        return false;
      }
      if (!isExistFileType) {
        proxy.$message.error(`图片只支持 ${uploadTypes.value.join("、")} 格式!`);
        return false;
      }

      uploadingAvatar.value = true;
      return true;
    }

    function handleAvatarSuccess(response) {
      uploadingAvatar.value = false;
      proxy.$message({
        message: response.message,
        type: response.type,
      });

      if (response.success) {
        proxy.$store.commit("setUserPic", response.data);
      }
    }

    function handleAvatarError() {
      uploadingAvatar.value = false;
      proxy.$message.error("头像上传失败，请稍后重试");
    }

    async function applyPresetAvatar(path: string) {
      if (!userId.value || path === currentAvatarPath.value) {
        return;
      }

      selectingAvatar.value = true;
      try {
        const result = (await HttpManager.updateUserMsg({
          id: userId.value,
          avator: path,
        })) as ResponseBody;

        proxy.$message({
          message: result.message,
          type: result.type,
        });

        if (result.success) {
          proxy.$store.commit("setUserPic", path);
        }
      } catch (error) {
        proxy.$message.error("默认头像更新失败，请稍后重试");
      } finally {
        selectingAvatar.value = false;
      }
    }

    return {
      uploadTypes,
      uploadUrl,
      beforeAvatarUpload,
      handleAvatarSuccess,
      handleAvatarError,
      currentAvatarPath,
      defaultAvatarOptions: DEFAULT_USER_AVATAR_OPTIONS,
      selectingAvatar,
      uploadingAvatar,
      attachImageUrl: HttpManager.attachImageUrl,
      applyPresetAvatar,
    };
  },
});
</script>

<style scoped lang="scss">
.avatar-picker {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.picker-summary {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(180, 196, 220, 0.35);
}

.current-avatar {
  width: 88px;
  height: 88px;
  border-radius: 26px;
  overflow: hidden;
  border: 1px solid rgba(176, 192, 220, 0.45);
  box-shadow: 0 18px 38px rgba(23, 42, 86, 0.08);
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.summary-copy {
  h3 {
    margin: 0 0 6px;
    font-size: 20px;
    font-weight: 700;
    color: #1d2b4f;
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.7;
    color: #6f7f9f;
  }
}

.avatar-library {
  padding: 20px;
  border-radius: 28px;
  background: rgba(248, 251, 255, 0.92);
  border: 1px solid rgba(196, 210, 230, 0.38);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 700;
  color: #233763;
}

.avatar-grid {
  max-height: 360px;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 12px;
  padding-right: 4px;
}

.avatar-option {
  appearance: none;
  border: 1px solid rgba(197, 211, 231, 0.55);
  background: #fff;
  border-radius: 20px;
  padding: 8px 8px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  img {
    width: 72px;
    height: 72px;
    border-radius: 18px;
    object-fit: cover;
    display: block;
  }

  span {
    font-size: 11px;
    color: #66789b;
    line-height: 1.2;
  }

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    border-color: rgba(84, 150, 255, 0.5);
    box-shadow: 0 12px 20px rgba(54, 98, 180, 0.08);
  }

  &.active {
    border-color: rgba(69, 137, 255, 0.85);
    box-shadow: 0 16px 28px rgba(69, 137, 255, 0.18);

    span {
      color: #2f5fd2;
      font-weight: 700;
    }
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.72;
  }
}

.upload-divider {
  font-size: 13px;
  font-weight: 600;
  color: #7b8db0;
  text-align: center;
}

:deep(.el-upload) {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  padding: 18px 0;
  border-radius: 24px;
  border: 1px dashed rgba(137, 178, 244, 0.5);
  background: rgba(250, 252, 255, 0.86);
}

:deep(.el-upload__text) {
  color: #5a6f96;
}

:deep(.el-upload__tip) {
  color: #97a7c5;
}

@media (max-width: 768px) {
  .picker-summary {
    align-items: flex-start;
  }

  .avatar-grid {
    grid-template-columns: repeat(auto-fill, minmax(84px, 1fr));
    max-height: 300px;
  }

  .avatar-option img {
    width: 64px;
    height: 64px;
  }
}
</style>
