import { getCurrentInstance, computed } from "vue";
import { useStore } from "vuex";
import { LocationQueryRaw } from "vue-router";
import { RouterName } from "@/enums";
import { HttpManager } from "@/api";
import axios from 'axios'
import { useRouter, useRoute } from "vue-router"; // 
interface routerOptions {
  path?: string;
  query?: LocationQueryRaw;
}

export default function () {
  const { proxy } = getCurrentInstance();
  const router = useRouter(); // 使用 useRouter 获取 $router
  const route = useRoute();   // 使用 useRoute 获取 $rout
  const store = useStore();
  const token = computed(() => store.getters.token);
  const currentSongId = computed(() => store.getters.songId);
  const currentSongUrl = computed(() => store.getters.songUrl);
  const isPlay = computed(() => store.getters.isPlay);



  function getUserSex(sex) {
    if (sex === 0) {
      return "女";
    } else if (sex === 1) {
      return "男";
    } else if (sex === 2) {
      return "保密";
    }
  }

  // 获取歌曲名
  function getSongTitle(str) {
    return str.split("-")[1];
  }

  // 获取歌手名
  function getSingerName(str) {
    return str.split("-")[0];
  }

  // 判断登录状态
  function checkStatus(status?: boolean) {
    if (!token.value) {
      if (status !== false)
        (proxy as any).$message({
          message: "请先登录",
          type: "warning",
        });
      return false;
    }
    return true;
  }

  // 播放
  function playMusic({ id, url, pic, index, name, lyric, currentSongList }) {
    const songTitle = getSongTitle(name);
    const singerName = getSingerName(name);
    const isSameSong = currentSongId.value === id && currentSongUrl.value === url;
    proxy.$store.dispatch("playMusic", {
      id,
      url,
      pic,
      index,
      songTitle,
      singerName,
      lyric,
      currentSongList,
    });

    if (!isSameSong || !isPlay.value) {
      proxy.$store.commit("setIsPlay", true);
    }

    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent("music-player:reveal"));
    }
  }

  function getFileName(path) {
    const cleanPath = path.split("?")[0];
    const parts = cleanPath.split("/");
    return parts[parts.length - 1];
  }

  function getDownloadName(songName, fallbackName) {
    const safeName = (songName || fallbackName || "music").replace(/[\\/:*?"<>|]/g, "_");
    const hasExtension = /\.[a-z0-9]+$/i.test(safeName);
    if (hasExtension) {
      return safeName;
    }
    const fallbackExtension = fallbackName && fallbackName.includes(".") ? fallbackName.substring(fallbackName.lastIndexOf(".")) : ".mp3";
    return `${safeName}${fallbackExtension}`;
  }

  // 下载
  async function downloadMusic({ songUrl, songName }) {

    if (checkStatus() == false) {
      return;
    }

    if (!songUrl) {
      (proxy as any).$message({
        message: "下载链接为空！",
        type: "error",
      });
      return;
    }

    const fileName = getFileName(songUrl);
    const requestUrl = songUrl.startsWith("http") ? songUrl : HttpManager.attachImageUrl(songUrl);

    try {
      const response = await axios.get(requestUrl, {
        responseType: "blob",
      });

      const blob = new Blob([response.data], {
        type: response.headers?.["content-type"] || "application/octet-stream",
      });
      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = blobUrl;
      link.download = getDownloadName(songName, fileName);
      document.body.appendChild(link);
      link.click();
      window.URL.revokeObjectURL(blobUrl);
      document.body.removeChild(link);
    } catch (error) {
      (proxy as any).$message({
        message: "下载失败，请稍后重试",
        type: "error",
      });
    }
  }

  // 导航索引
  function changeIndex(value) {
    proxy.$store.commit("setActiveNavName", value);
  }
  // 路由管理
  function routerManager(routerName: string | number, options: routerOptions) {
    switch (routerName) {
      case RouterName.Search:
        router.push({ path: options.path, query: options.query }); 
        break;
      case RouterName.Home:
      case RouterName.SongSheet:
      case RouterName.SongSheetDetail:
      case RouterName.Singer:
      case RouterName.Rank:
      case RouterName.SingerDetail:
      case RouterName.Personal:
      case RouterName.PersonalData:
      case RouterName.Setting:
      case RouterName.SignIn:
      case RouterName.SignUp:
      case RouterName.SignOut:
      case RouterName.Lyric:
      case RouterName.Error:
      default:
        router.push({ path: options.path });
        break;
    }
  }

  function goBack(step = -1) {
    router.back(); // 替换为 useRouter.back()
  }

  return {
    getUserSex,
    getSongTitle,
    getSingerName,
    changeIndex,
    checkStatus,
    playMusic,
    routerManager,
    goBack,
    downloadMusic,
  };
}

