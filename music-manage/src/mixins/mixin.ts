import { getCurrentInstance, ref } from "vue";
import { LocationQueryRaw } from "vue-router";
import { useRouter } from "vue-router";
import { RouterName } from "@/enums";

interface routerOptions {
  path?: string;
  query?: LocationQueryRaw;
}

export default function () {
  const { proxy } = getCurrentInstance();
  const router = useRouter(); // ✅ 使用 useRouter 替代 proxy.$router
  const uploadTypes = ref(["jpg", "jpeg", "png", "gif"]);

  function changeSex(value) {
    const normalizedValue = typeof value === "string" && value.trim() !== "" ? Number(value) : value;

    if (normalizedValue === 0) {
      return "女";
    } else if (normalizedValue === 1) {
      return "男";
    } else if (normalizedValue === 2) {
      return "组合";
    } else if (normalizedValue === 3) {
      return "不明";
    } else if (value === "男" || value === "女") {
      return value;
    } else if (value === "组合" || value === "不明") {
      return value;
    }
  }

  function beforeImgUpload(file) {
    const ltCode = 2;
    const isLt2M = file.size / 1024 / 1024 < ltCode;
    const isExistFileType = uploadTypes.value.includes(file.type.replace(/image\//, ""));

    if (!isExistFileType) {
      (proxy as any).$message.error(`图片只支持 ${uploadTypes.value.join("、")} 格式!`);
    }
    if (!isLt2M) {
      (proxy as any).$message.error(`上传头像图片大小不能超过 ${ltCode}MB!`);
    }
    
    return isExistFileType && isLt2M;
  }

  function beforeSongUpload(file) {
    const ltCode = 10;
    const isLt10M = file.size / 1024 / 1024 < ltCode;
    const testmsg = file.name.substring(file.name.lastIndexOf(".") + 1);
    const extension = testmsg === "mp3" || testmsg === "wma" || testmsg === "lrc" || testmsg === "mp4" || testmsg === "txt";

    if (!extension) {
      (proxy as any).$message({
        message: "上传文件只能是格式有问题",
        type: "error",
      });
    }
    if (!isLt10M) {
      (proxy as any).$message.error(`上传头像图片大小不能超过 ${ltCode}MB!`);
    }

    return extension && isLt10M;
  }

  // 路由管理
  function routerManager(routerName: string | number, options: routerOptions) {
    switch (routerName) {
      case RouterName.Song:
      case RouterName.ListSong:
      case RouterName.Comment:
      case RouterName.Consumer:
      case RouterName.Collect:
        router.push({ path: options.path, query: options.query });
        break;
      case RouterName.Home:
      case RouterName.SignIn:
      case RouterName.SignOut:
      case RouterName.Info:
      case RouterName.Singer:
      case RouterName.SongList:
      case RouterName.Error:
      default:
        router.push({ path: options.path });
        break;
    }
  }

  function goBack(step = -1) {
    router.go(step);
  }

  return { changeSex, routerManager, goBack, beforeImgUpload, beforeSongUpload };
}
