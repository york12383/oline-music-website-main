<template>
  <div class="music-assistant" data-assistant-interactive="true">
    <button
      class="assistant-trigger"
      type="button"
      aria-label="打开 AI 助手"
      data-assistant-interactive="true"
      @click="drawerVisible = true"
    >
      <img :src="assistantLogo" alt="" class="assistant-trigger__logo" />
      <span class="assistant-trigger__badge">AI</span>
    </button>

    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      :with-header="false"
      :size="drawerSize"
      append-to-body
      custom-class="assistant-drawer"
    >
      <div class="assistant-shell" data-assistant-interactive="true">
        <div class="assistant-head">
          <div class="assistant-head__brand">
            <img :src="assistantLogo" alt="" class="assistant-head__logo" />
            <div>
              <h3>轻音 AI 小助手</h3>
              <p>能聊天，也能直接帮你操控音乐站</p>
            </div>
          </div>
          <button class="assistant-close" type="button" @click="drawerVisible = false">×</button>
        </div>

        <div class="assistant-quick">
          <button
            v-for="prompt in quickPrompts"
            :key="prompt"
            type="button"
            class="assistant-chip"
            @click="sendPrompt(prompt)"
          >
            {{ prompt }}
          </button>
        </div>

        <div ref="messageListRef" class="assistant-messages">
          <div
            v-for="item in messageList"
            :key="item.id"
            class="assistant-message"
            :class="item.role"
          >
            <div class="assistant-bubble">
              <span v-if="item.pending" class="assistant-thinking">正在思考...</span>
              <span v-else>{{ item.content }}</span>
            </div>

            <div v-if="item.song && !item.pending" class="assistant-song-card">
              <img :src="attachImageUrl(item.song.pic)" alt="" class="assistant-song-card__cover" />
              <div class="assistant-song-card__meta">
                <strong>{{ item.song.songTitle }}</strong>
                <span>{{ item.song.singerName }}</span>
                <small v-if="item.usedImport">本次为你自动补歌</small>
              </div>
              <div class="assistant-song-card__actions">
                <button type="button" @click="playSong(item.song, item.candidates?.length ? item.candidates : [item.song])">播放</button>
                <button type="button" class="ghost" @click="openLyric(item.song)">歌词页</button>
              </div>
            </div>

            <div v-if="item.candidates?.length && !item.pending" class="assistant-candidates">
              <button
                v-for="song in getVisibleCandidates(item)"
                :key="song.id"
                type="button"
                class="assistant-candidate"
                @click="playSong(song, item.candidates || [song])"
              >
                <img :src="attachImageUrl(song.pic)" alt="" />
                <span>{{ song.songTitle }}</span>
                <small>{{ song.singerName }}</small>
              </button>
              <button
                v-if="shouldShowCandidateToggle(item)"
                type="button"
                class="assistant-candidates__toggle"
                @click="toggleCandidateExpand(item)"
              >
                {{ item.showAllCandidates ? "收起歌曲" : "展开全部歌曲" }}
              </button>
            </div>

            <div
              v-if="item.needsConfirmation && !item.pending && activeConfirmationMessageId === item.id"
              class="assistant-confirm"
            >
              <button type="button" class="assistant-confirm__button primary" @click="confirmPendingAction">
                确认执行
              </button>
              <button type="button" class="assistant-confirm__button" @click="cancelPendingAction">
                先别执行
              </button>
            </div>
          </div>
        </div>

        <div class="assistant-footer">
          <div class="assistant-input">
            <textarea
              v-model="inputValue"
              rows="3"
              placeholder="直接说：播放周杰伦的告白气球"
              @keydown.enter.exact.prevent="sendPrompt()"
            ></textarea>
            <button type="button" :disabled="sending || !inputValue.trim()" @click="sendPrompt()">
              {{ sending ? "发送中..." : "发送" }}
            </button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { computed, nextTick, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import { useStore } from "vuex";
import { HttpManager } from "@/api";
import { NavName, RouterName } from "@/enums";
import assistantLogo from "@/assets/images/assistant-logo.svg";

interface AssistantSong {
  id: number;
  name: string;
  singerName: string;
  songTitle: string;
  singerId?: number;
  introduction?: string;
  pic?: string;
  lyric?: string;
  url: string;
  type?: number;
}

interface AssistantSinger {
  id: number | string;
  name: string;
  pic?: string;
  sex?: number | string;
  birth?: string;
  location?: string;
  introduction?: string;
}

interface AssistantMessageItem {
  id: number;
  role: "assistant" | "user";
  content: string;
  pending?: boolean;
  excludeFromHistory?: boolean;
  needsConfirmation?: boolean;
  confirmationAction?: string;
  confirmationTarget?: string;
  confirmationConfidence?: number;
  song?: AssistantSong | null;
  candidates?: AssistantSong[];
  usedImport?: boolean;
  showAllCandidates?: boolean;
}

interface SendPromptOptions {
  excludeFromHistory?: boolean;
}

interface AssistantPlaylist {
  id: number | string;
  title: string;
  introduction?: string;
  style?: string;
  pic?: string;
  type?: number;
}

type PlaylistSemanticTarget = "hotTop" | "hotPage" | "recommend" | "";

const store = useStore();
const router = useRouter();
const isLoggedIn = computed(() => Boolean(store.getters.token));
const userId = computed(() => store.getters.userId);
const currentDetail = computed(() => store.getters.songDetails || null);
const currentSongId = computed(() => store.getters.songId);
const currentSongUrl = computed(() => store.getters.songUrl);
const currentSongTitle = computed(() => store.getters.songTitle);
const currentSingerName = computed(() => store.getters.singerName);
const currentPlayList = computed(() => (Array.isArray(store.getters.currentPlayList) ? store.getters.currentPlayList : []));
const currentPlayIndex = computed(() => Number(store.getters.currentPlayIndex ?? -1));
const drawerVisible = ref(false);
const inputValue = ref("");
const sending = ref(false);
const drawerSize = "min(410px, calc(100vw - 20px))";
const messageListRef = ref<HTMLElement | null>(null);
const quickPrompts = [
  "播放周杰伦的告白气球",
  "帮我找林俊杰的歌",
  "推荐几首适合夜晚听的歌",
  "打开我的歌单",
];
const COLLECTION_CHANGED_EVENT = "music-collection:changed";
const SEARCH_HISTORY_STORAGE_KEY = "light-music-search-history";
const SEARCH_HISTORY_CLEAR_EVENT = "music-search-history:clear";
const PLAYLIST_SONGS_CHANGED_EVENT = "music-playlist:songs-changed";
const CANDIDATE_PREVIEW_LIMIT = 6;
const HOT_TOP_PLAYLIST_HINTS = ["最受欢迎", "人气最高", "热度最高", "最热门", "最火", "排行第一", "排名第一", "榜单第一", "第一名", "top1", "no1"];
const HOT_PAGE_PLAYLIST_HINTS = ["热门", "热榜", "高人气", "流行"];
const RECOMMEND_PLAYLIST_HINTS = ["推荐", "猜你喜欢", "为你推荐"];
const MUSIC_REQUEST_HINT_WORDS = ["播放", "放一首", "放首", "听", "来一首", "来首", "点一首", "点首", "搜歌", "找歌"];
const MUSIC_KEYWORD_STOP_WORDS = [
  "帮我",
  "请",
  "一下",
  "一下子",
  "我想听",
  "想听",
  "给我",
  "可以",
  "能不能",
  "麻烦",
  "直接",
  "帮忙",
  "我要",
  "我想",
  "听听",
  "播一下",
  "播放一下",
  "来点",
  "一首",
  "首歌",
  "这首歌",
  "这个歌",
  "歌曲",
  "音乐",
  "一下吧",
];
const allSongsCache = ref<AssistantSong[] | null>(null);
const allSingersCache = ref<AssistantSinger[] | null>(null);
const activeConfirmationMessageId = ref<number | null>(null);
const messageList = ref<AssistantMessageItem[]>([
  {
    id: Date.now(),
    role: "assistant",
    content: "我是轻音音乐的小助手。你可以和我聊天，也可以直接让我打开页面、控制播放，或者把当前歌曲加入指定歌单。",
  },
]);

watch(drawerVisible, async (visible) => {
  if (visible) {
    await scrollToBottom();
  }
});

async function sendPrompt(prompt?: string, options: SendPromptOptions = {}) {
  const content = (prompt ?? inputValue.value).trim();
  if (!content || sending.value) return;
  const historyPayload = buildHistoryPayload();
  activeConfirmationMessageId.value = null;

  const userMessageId = Date.now();
  messageList.value.push({
    id: userMessageId,
    role: "user",
    content,
    excludeFromHistory: Boolean(options.excludeFromHistory),
  });
  inputValue.value = "";

  const missingSongHint = await buildMissingSongHint(content);
  if (missingSongHint) {
    messageList.value.push({
      id: userMessageId + 1,
      role: "assistant",
      content: missingSongHint,
      excludeFromHistory: true,
    });
  }

  const pendingId = userMessageId + (missingSongHint ? 2 : 1);
  messageList.value.push({ id: pendingId, role: "assistant", content: "", pending: true });
  sending.value = true;
  await scrollToBottom();

  try {
    const result = (await HttpManager.assistantChat({
      message: content,
      history: historyPayload,
      allowImport: true,
    })) as ResponseBody;

    const payload = result?.data || {};
    if (payload.usedImport) {
      allSongsCache.value = null;
      allSingersCache.value = null;
    }
    const actionReply = payload.needsConfirmation ? null : await executeAssistantAction(payload.action, payload, content);
    activeConfirmationMessageId.value = payload.needsConfirmation ? pendingId : null;
    replacePendingMessage(pendingId, {
      id: pendingId,
      role: "assistant",
      content: actionReply || payload.reply || "我已经收到，但这次没整理出结果。",
      excludeFromHistory: Boolean(payload.needsConfirmation),
      needsConfirmation: Boolean(payload.needsConfirmation),
      confirmationAction: String(payload.confirmationAction ?? ""),
      confirmationTarget: String(payload.confirmationTarget ?? ""),
      confirmationConfidence: typeof payload.confidence === "number" ? payload.confidence : undefined,
      song: payload.song || null,
      candidates: Array.isArray(payload.candidates) ? payload.candidates : [],
      usedImport: Boolean(payload.usedImport),
    });
  } catch (error) {
    void error;
    replacePendingMessage(pendingId, {
      id: pendingId,
      role: "assistant",
      content: "我刚刚没有连上服务。你可以稍后再试，或者先用站内搜索。",
    });
  } finally {
    sending.value = false;
    await scrollToBottom();
  }
}

async function confirmPendingAction() {
  await sendPrompt("确认执行", { excludeFromHistory: true });
}

async function cancelPendingAction() {
  await sendPrompt("先别执行", { excludeFromHistory: true });
}

function buildHistoryPayload() {
  return messageList.value
    .filter((item) => !item.pending && !item.excludeFromHistory)
    .slice(-10)
    .map((item) => ({ role: item.role, content: item.content }));
}

function replacePendingMessage(id: number, nextValue: AssistantMessageItem) {
  const index = messageList.value.findIndex((item) => item.id === id);
  if (index >= 0) {
    messageList.value.splice(index, 1, nextValue);
  }
}

function getVisibleCandidates(item: AssistantMessageItem) {
  const candidates = Array.isArray(item.candidates) ? item.candidates : [];
  if (item.showAllCandidates) {
    return candidates;
  }
  return candidates.slice(0, CANDIDATE_PREVIEW_LIMIT);
}

function shouldShowCandidateToggle(item: AssistantMessageItem) {
  const candidates = Array.isArray(item.candidates) ? item.candidates : [];
  return candidates.length > CANDIDATE_PREVIEW_LIMIT;
}

function toggleCandidateExpand(item: AssistantMessageItem) {
  item.showAllCandidates = !item.showAllCandidates;
  void scrollToBottom();
}

async function buildMissingSongHint(content: string) {
  const keyword = extractAssistantMusicKeyword(content);
  if (!keyword) {
    return "";
  }
  if (isBroadAssistantBrowseRequest(content, keyword)) {
    return "";
  }
  if (!looksLikeDirectMusicRequest(content)) {
    return "";
  }

  const songs = await fetchAllSongs();
  const matchedSong = findBestSongMatch(songs, keyword);
  if (matchedSong) {
    return "";
  }
  return "站内暂时没有这首歌，我正在帮您寻找...";
}

function looksLikeDirectMusicRequest(content: string) {
  return MUSIC_REQUEST_HINT_WORDS.some((keyword) => content.includes(keyword));
}

function isBroadAssistantBrowseRequest(content: string, keyword: string) {
  if (!content || !keyword) {
    return false;
  }
  const normalizedContent = normalizeSongText(content);
  const normalizedKeyword = normalizeSongText(keyword);
  const keywordTokens = splitSongTokens(keyword);
  if (!keywordTokens.length || keywordTokens.length > 2) {
    return false;
  }
  return content.includes("的歌")
    || content.includes("的歌曲")
    || normalizedContent.includes("有哪些歌")
    || normalizedContent.includes("都有哪些歌")
    || normalizedContent.includes("都有什么歌")
    || normalizedContent.includes("代表作")
    || normalizedContent.includes("热门歌")
    || normalizedContent.includes("经典歌")
    || normalizedKeyword === normalizeSongText(getSingerName(keyword));
}

function extractAssistantMusicKeyword(content: string) {
  let working = String(content ?? "").trim();
  if (!working) {
    return "";
  }

  for (const stopWord of MUSIC_KEYWORD_STOP_WORDS) {
    working = working.replaceAll(stopWord, " ");
  }
  working = working
    .replaceAll("播放", " ")
    .replaceAll("想听", " ")
    .replaceAll("搜一下", " ")
    .replaceAll("找一下", " ")
    .replaceAll("吧", " ")
    .replaceAll("吗", " ")
    .replaceAll("呀", " ")
    .replaceAll("呢", " ")
    .replaceAll(/[<>/\\|,.!?;:'"，。！？；：、“”‘’（）《》【】]/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  return working;
}

async function executeAssistantAction(action?: string, payload?: Record<string, any>, originalMessage = "") {
  const actionTarget = resolveActionTarget(payload);
  const searchKeyword = String(payload?.searchKeyword ?? "").trim();
  switch (action) {
    case "play_song":
      if (payload?.song) {
        playSong(
          payload.song,
          Array.isArray(payload.candidates) && payload.candidates.length ? payload.candidates : [payload.song],
          false
        );
      }
      return null;
    case "logout":
      return await handleLogout();
    case "navigate_home":
      return await navigateToRoute(RouterName.Home, NavName.Home);
    case "navigate_song_sheet":
      return await navigateToRoute(RouterName.SongSheet, NavName.SongSheet);
    case "navigate_singer":
      return await navigateToRoute(RouterName.Singer, NavName.Singer);
    case "navigate_rank":
      return await navigateToRoute(RouterName.Rank, NavName.Rank);
    case "navigate_collection":
      return await navigateToRoute(RouterName.Collection, NavName.Collection, true);
    case "navigate_my_song_sheet":
      return await navigateToRoute(RouterName.MySongSheet, NavName.MySongSheet, true);
    case "navigate_personal":
      return await navigateToRoute(RouterName.Personal, NavName.Personal, true);
    case "navigate_setting":
      return await navigateToRoute(RouterName.Setting, NavName.Setting, true);
    case "open_singer_detail":
      return await openSingerDetail(actionTarget);
    case "open_playlist_detail":
      return await openPlaylistDetail(actionTarget);
    case "open_lyric":
      return await openCurrentLyricPage();
    case "open_song_comment":
      return await openSongComment(searchKeyword);
    case "clear_search_history":
      return clearSearchHistory();
    case "navigate_collection_playlists":
      return await openCollectionPlaylistTab();
    case "favorite_playlist":
      return await updatePlaylistCollection(true, actionTarget);
    case "unfavorite_playlist":
      return await updatePlaylistCollection(false, actionTarget);
    case "play_playlist":
      return await playPlaylist(actionTarget);
    case "toggle_dark_mode":
      return toggleDarkMode();
    case "player_pause":
      return pauseCurrentSong();
    case "player_resume":
      return resumeCurrentSong();
    case "player_next":
      return playRelativeSong(1);
    case "player_previous":
      return playRelativeSong(-1);
    case "favorite_current_song":
      return await updateCurrentSongCollection(true);
    case "unfavorite_current_song":
      return await updateCurrentSongCollection(false);
    case "add_current_song_to_playlist":
      return await addCurrentSongToPlaylist(actionTarget);
    case "create_playlist":
      return await createPlaylist(actionTarget);
    case "add_song_to_playlist":
      return await addSongToPlaylist(searchKeyword, actionTarget);
    case "remove_current_song_from_playlist":
      return await removeCurrentSongFromPlaylist(actionTarget);
    case "create_recommended_playlist":
      if (!shouldCreateRecommendedPlaylist(originalMessage)) {
        return "这句话我先按“推荐歌曲”理解，不会直接帮你创建歌单。要是你想生成歌单，可以直接说“帮我生成一个夜晚歌单”。";
      }
      return await createRecommendedPlaylist(actionTarget);
    case "delete_my_playlist":
      return await deleteMyPlaylist(actionTarget);
    default:
      return null;
  }
}

function resolveActionTarget(payload?: Record<string, any>) {
  const target = String(payload?.actionTarget ?? payload?.searchKeyword ?? "").trim();
  return target;
}

function shouldCreateRecommendedPlaylist(message?: string) {
  const content = String(message ?? "").trim();
  if (!content) {
    return false;
  }
  const normalizedContent = normalizePlaylistText(content);
  const hasRecommend = content.includes("推荐");
  const hasPlaylist = content.includes("歌单") || content.includes("列表");
  const hasCreateCue = ["生成", "创建", "新建", "做个", "做一个", "做一张", "整一个", "帮我生成", "帮我创建", "帮我做"]
    .some((keyword) => content.includes(keyword));
  return hasRecommend && hasPlaylist && (hasCreateCue
    || normalizedContent.includes(normalizePlaylistText("推荐并生成"))
    || normalizedContent.includes(normalizePlaylistText("推荐并创建"))
    || normalizedContent.includes(normalizePlaylistText("直接生成推荐歌单"))
    || normalizedContent.includes(normalizePlaylistText("直接创建推荐歌单")));
}

async function handleLogout() {
  if (!isLoggedIn.value) {
    return "当前还没有登录账号。";
  }

  HttpManager.logout();
  store.commit("setToken", false);
  store.commit("clearUserInfo");
  store.commit("setUserId", "");
  store.commit("setUsername", "");
  store.commit("setUserPic", "");
  await router.push({ path: "/" });
  return "好的，已经帮你退出登录。";
}

async function navigateToRoute(path: string, navName: string, requiresAuth = false) {
  if (requiresAuth && !isLoggedIn.value) {
    return "你还没登录，暂时不能执行这个操作。";
  }

  store.commit("setActiveNavName", navName);
  await router.push({ path });
  return `好的，已经帮你打开${navName}。`;
}

async function openSingerDetail(actionTarget: string) {
  const singerKeyword = actionTarget || currentSingerName.value;
  if (!singerKeyword) {
    return "你可以直接告诉我歌手名，比如“打开周杰伦的详情页”。";
  }

  const singers = await fetchAllSingers();
  const matchedSinger = findBestSingerMatch(singers, singerKeyword);
  if (!matchedSinger) {
    return `我暂时没找到“${singerKeyword}”这位歌手。`;
  }

  store.commit("setSongDetails", matchedSinger);
  store.commit("setActiveNavName", NavName.Singer);
  await router.push({ name: "singer-detail", params: { id: matchedSinger.id } });
  return `已帮你打开 ${matchedSinger.name} 的详情页。`;
}

async function openPlaylistDetail(actionTarget: string) {
  const fallbackTarget = currentDetail.value?.title ? String(currentDetail.value.title) : "";
  const resolvedTarget = (actionTarget || fallbackTarget).trim();
  if (!resolvedTarget) {
    return "你可以直接告诉我歌单名，比如“打开夜跑歌单”。";
  }

  const semanticTarget = resolvePlaylistSemanticTarget(resolvedTarget);
  if (semanticTarget === "recommend") {
    return await openRecommendPlaylistPage();
  }
  if (semanticTarget === "hotPage") {
    return await openHotPlaylistPage();
  }
  if (semanticTarget === "hotTop") {
    const hottestPlaylist = await fetchTopHotPlaylist();
    if (!hottestPlaylist) {
      return "排行榜里的歌单榜暂时还没有可打开的内容。";
    }
    store.commit("setSongDetails", hottestPlaylist);
    store.commit("setActiveNavName", NavName.Rank);
    await router.push({ path: `/song-sheet-detail/${hottestPlaylist.id}` });
    return `已帮你打开当前人气最高的歌单“${hottestPlaylist.title}”。`;
  }

  if (isLoggedIn.value) {
    const userPlaylists = await fetchUserPlaylists();
    const matchedUserPlaylist = findBestPlaylistMatch(userPlaylists, resolvedTarget);
    if (matchedUserPlaylist) {
      store.commit("setSongDetails", matchedUserPlaylist);
      store.commit("setActiveNavName", NavName.MySongSheet);
      await router.push({ path: `/my-song-sheet-detail/${matchedUserPlaylist.id}` });
      return `已帮你打开我的歌单“${matchedUserPlaylist.title}”。`;
    }
  }

  const publicPlaylists = await fetchPublicPlaylists(resolvedTarget);
  const matchedPublicPlaylist = findBestPlaylistMatch(publicPlaylists, resolvedTarget);
  if (matchedPublicPlaylist) {
    store.commit("setSongDetails", matchedPublicPlaylist);
    store.commit("setActiveNavName", NavName.SongSheet);
    await router.push({ path: `/song-sheet-detail/${matchedPublicPlaylist.id}` });
    return `已帮你打开歌单“${matchedPublicPlaylist.title}”。`;
  }

  return `我暂时没找到名为“${resolvedTarget}”的歌单。你也可以先打开“我的歌单”看看。`;
}

async function openSongComment(searchKeyword: string) {
  if (!searchKeyword) {
    if (!currentSongId.value) {
      return "当前还没有正在播放的歌曲，先播一首歌我再帮你打开评论区。";
    }
    await router.push({ name: "lyric", params: { id: currentSongId.value }, query: { focus: "comment" } });
    return `已帮你打开《${currentSongTitle.value || "当前歌曲"}》的评论区。`;
  }

  const songs = await fetchAllSongs();
  const matchedSong = findBestSongMatch(songs, searchKeyword);
  if (!matchedSong) {
    return `我暂时没找到“${searchKeyword}”这首歌。`;
  }

  setSongContext(matchedSong, [matchedSong], false);
  await router.push({ name: "lyric", params: { id: matchedSong.id }, query: { focus: "comment" } });
  return `已帮你打开《${matchedSong.songTitle}》的评论区。`;
}

function clearSearchHistory() {
  try {
    window.localStorage.removeItem(SEARCH_HISTORY_STORAGE_KEY);
  } catch (error) {
    void error;
  }
  window.dispatchEvent(new CustomEvent(SEARCH_HISTORY_CLEAR_EVENT));
  return "已帮你清空搜索历史。";
}

async function openCollectionPlaylistTab() {
  if (!isLoggedIn.value) {
    return "你还没登录，暂时不能查看收藏歌单。";
  }

  store.commit("setActiveNavName", NavName.Collection);
  await router.push({
    path: RouterName.Collection,
    query: {
      tab: "songList",
    },
  });
  return "已帮你打开收藏里的歌单页签。";
}

async function updatePlaylistCollection(nextCollected: boolean, actionTarget: string) {
  if (!isLoggedIn.value || !userId.value) {
    return "你还没登录，暂时不能操作歌单收藏。";
  }

  const recommendGuardMessage = getRecommendPlaylistAccessMessage(actionTarget, "collect");
  if (recommendGuardMessage) {
    return recommendGuardMessage;
  }

  const matchedPlaylist = await resolvePlaylistTarget(actionTarget);
  if (!matchedPlaylist) {
    return actionTarget
      ? `我暂时没找到“${actionTarget}”这张歌单。`
      : "你可以直接告诉我歌单名，比如“收藏夜跑歌单”。";
  }

  const statusResult = (await HttpManager.isCollection({
    userId: userId.value,
    type: 1,
    songListId: matchedPlaylist.id,
  })) as ResponseBody;
  const currentCollected = Boolean(statusResult?.data);

  if (nextCollected === currentCollected) {
    return nextCollected
      ? `“${matchedPlaylist.title}”已经在你的收藏里了。`
      : `“${matchedPlaylist.title}”本来就没有收藏。`;
  }

  const result = nextCollected
    ? ((await HttpManager.setCollection({
        userId: userId.value,
        type: 1,
        songListId: matchedPlaylist.id,
      })) as ResponseBody)
    : ((await HttpManager.deleteCollection({
        userId: userId.value,
        type: 1,
        songListId: matchedPlaylist.id,
      })) as ResponseBody);

  if (result?.success === false) {
    return result?.message || (nextCollected ? "收藏歌单失败了。" : "取消收藏歌单失败了。");
  }

  return nextCollected
    ? `已帮你收藏歌单“${matchedPlaylist.title}”。`
    : `已帮你取消收藏歌单“${matchedPlaylist.title}”。`;
}

async function playPlaylist(actionTarget: string) {
  const recommendGuardMessage = getRecommendPlaylistAccessMessage(actionTarget, "play");
  if (recommendGuardMessage) {
    return recommendGuardMessage;
  }

  const matchedPlaylist = await resolvePlaylistTarget(actionTarget);
  if (!matchedPlaylist) {
    if (resolvePlaylistSemanticTarget(actionTarget || String(currentDetail.value?.title ?? "")) === "recommend") {
      return "当前还没有适合你的推荐歌单。";
    }
    return actionTarget
      ? `我暂时没找到“${actionTarget}”这张歌单。`
      : "你可以直接告诉我歌单名，比如“播放夜跑歌单”。";
  }

  const songs = await fetchSongsByPlaylistId(matchedPlaylist.id);
  if (!songs.length) {
    return `“${matchedPlaylist.title}”里暂时还没有可播放的歌曲。`;
  }

  store.commit("setSongDetails", matchedPlaylist);
  playSong(songs[0], songs, false);
  return `已开始播放歌单“${matchedPlaylist.title}”。`;
}

async function addCurrentSongToPlaylist(actionTarget: string) {
  if (!isLoggedIn.value || !userId.value) {
    return "你还没登录，暂时不能把歌曲加入歌单。";
  }
  if (!currentSongId.value) {
    return "当前还没有正在播放的歌曲，先播一首歌我再帮你加入歌单。";
  }
  if (!actionTarget) {
    return "你可以直接告诉我歌单名，比如“把这首歌加入夜跑歌单”。";
  }

  const userPlaylists = await fetchUserPlaylists();
  if (!userPlaylists.length) {
    return "你现在还没有创建歌单，先去“我的歌单”建一个吧。";
  }

  const matchedPlaylist = findBestPlaylistMatch(userPlaylists, actionTarget);
  if (!matchedPlaylist) {
    return `我没有在你的歌单里找到“${actionTarget}”。你可以换个更完整的歌单名再试。`;
  }

  const result = (await HttpManager.setListSong({
    songId: currentSongId.value,
    songListId: matchedPlaylist.id,
  })) as ResponseBody;

  if (result?.success === false) {
    return result?.message || "这首歌暂时没能加入歌单。";
  }

  emitPlaylistSongsChanged(matchedPlaylist.id);
  const songTitle = currentSongTitle.value || "当前歌曲";
  return `已帮你把《${songTitle}》加入“${matchedPlaylist.title}”。`;
}

async function createPlaylist(actionTarget: string) {
  if (!isLoggedIn.value || !userId.value) {
    return "你还没登录，暂时不能创建歌单。";
  }
  if (!actionTarget) {
    return "你可以直接告诉我歌单名，比如“创建一个夜跑歌单”。";
  }

  const title = formatPlaylistTitle(actionTarget);
  const existingPlaylists = await fetchUserPlaylists();
  const matchedExisting = existingPlaylists.find((item) => normalizePlaylistText(item.title) === normalizePlaylistText(title));
  if (matchedExisting) {
    store.commit("setSongDetails", matchedExisting);
    store.commit("setActiveNavName", NavName.MySongSheet);
    await router.push({ path: `/my-song-sheet-detail/${matchedExisting.id}` });
    return `你的歌单里已经有“${matchedExisting.title}”了，我直接帮你打开了。`;
  }

  const result = (await HttpManager.addSongList({
    title,
    introduction: `由轻音 AI 小助手帮你创建的歌单《${title}》`,
    style: inferPlaylistStyle(title),
  })) as ResponseBody;

  if (result?.success === false) {
    return result?.message || "创建歌单失败了。";
  }

  const refreshedPlaylists = await fetchUserPlaylists(true);
  const createdPlaylist = findBestPlaylistMatch(refreshedPlaylists, title);
  store.commit("setActiveNavName", NavName.MySongSheet);
  if (createdPlaylist) {
    emitPlaylistSongsChanged(createdPlaylist.id);
    store.commit("setSongDetails", createdPlaylist);
    await router.push({ path: `/my-song-sheet-detail/${createdPlaylist.id}` });
    return `已帮你创建歌单“${createdPlaylist.title}”，并直接打开了。`;
  }

  await router.push({ path: RouterName.MySongSheet });
  return `已帮你创建歌单“${title}”。`;
}

async function addSongToPlaylist(searchKeyword: string, actionTarget: string) {
  if (!isLoggedIn.value || !userId.value) {
    return "你还没登录，暂时不能把歌曲加入歌单。";
  }
  if (!actionTarget) {
    return "你可以直接告诉我歌单名，比如“把稻香加入夜跑歌单”。";
  }

  const userPlaylists = await fetchUserPlaylists();
  if (!userPlaylists.length) {
    return "你现在还没有创建歌单，先去“我的歌单”建一个吧。";
  }
  const matchedPlaylist = findBestPlaylistMatch(userPlaylists, actionTarget);
  if (!matchedPlaylist) {
    return `我没有在你的歌单里找到“${actionTarget}”。你可以换个更完整的歌单名再试。`;
  }

  const songs = await fetchAllSongs();
  const matchedSong = searchKeyword ? findBestSongMatch(songs, searchKeyword) : null;
  if (!matchedSong) {
    return searchKeyword
      ? `我暂时没找到“${searchKeyword}”这首歌。`
      : "你可以直接告诉我要加入哪首歌，比如“把稻香加入夜跑歌单”。";
  }

  const result = (await HttpManager.setListSong({
    songId: matchedSong.id,
    songListId: matchedPlaylist.id,
  })) as ResponseBody;

  if (result?.success === false) {
    return result?.message || "这首歌暂时没能加入歌单。";
  }

  emitPlaylistSongsChanged(matchedPlaylist.id);
  return `已帮你把《${matchedSong.songTitle}》加入“${matchedPlaylist.title}”。`;
}

async function removeCurrentSongFromPlaylist(actionTarget: string) {
  if (!isLoggedIn.value || !userId.value) {
    return "你还没登录，暂时不能从歌单里移除歌曲。";
  }
  if (!currentSongId.value) {
    return "当前还没有正在播放的歌曲，先播一首歌我再帮你移除。";
  }

  const userPlaylists = await fetchUserPlaylists(true);
  if (!userPlaylists.length) {
    return "你现在还没有创建歌单。";
  }

  const fallbackTarget = currentDetail.value?.title ? String(currentDetail.value.title) : "";
  const target = (actionTarget || fallbackTarget).trim();
  if (!target) {
    return "你可以直接告诉我要从哪个歌单移除，比如“把这首歌从夜跑歌单移除”。";
  }

  const matchedPlaylist = findBestPlaylistMatch(userPlaylists, target);
  if (!matchedPlaylist) {
    return `我没有在你的歌单里找到“${target}”。`;
  }

  const songs = await fetchSongsByPlaylistId(matchedPlaylist.id);
  const existsInPlaylist = songs.some((item) => Number(item.id) === Number(currentSongId.value));
  if (!existsInPlaylist) {
    return `《${currentSongTitle.value || "当前歌曲"}》不在“${matchedPlaylist.title}”里。`;
  }

  const result = (await HttpManager.deleteListSongOfId(currentSongId.value, matchedPlaylist.id)) as ResponseBody;
  if (result?.success === false) {
    return result?.message || "这首歌暂时没能从歌单里移除。";
  }

  emitPlaylistSongsChanged(matchedPlaylist.id);
  return `已帮你把《${currentSongTitle.value || "当前歌曲"}》从“${matchedPlaylist.title}”移除。`;
}

async function createRecommendedPlaylist(actionTarget: string) {
  if (!isLoggedIn.value || !userId.value) {
    return "你还没登录，暂时不能生成推荐歌单。";
  }

  const existingPlaylists = await fetchUserPlaylists(true);
  const baseTitle = buildRecommendedPlaylistTitle(actionTarget);
  const title = ensureUniquePlaylistTitle(existingPlaylists, baseTitle);
  const introduction = actionTarget
    ? `由轻音 AI 小助手根据“${actionTarget}”主题，为你生成的推荐歌单《${title}》`
    : `由轻音 AI 小助手为你生成的推荐歌单《${title}》`;

  const createResult = (await HttpManager.addSongList({
    title,
    introduction,
    style: inferPlaylistStyle(title),
  })) as ResponseBody;

  if (createResult?.success === false) {
    return createResult?.message || "推荐歌单创建失败了。";
  }

  const refreshedPlaylists = await fetchUserPlaylists(true);
  const createdPlaylist =
    refreshedPlaylists.find((item) => normalizePlaylistText(item.title) === normalizePlaylistText(title)) ||
    findBestPlaylistMatch(refreshedPlaylists, title);

  if (!createdPlaylist) {
    store.commit("setActiveNavName", NavName.MySongSheet);
    await router.push({ path: RouterName.MySongSheet });
    return `已帮你创建推荐歌单“${title}”，但我暂时没拿到歌单详情。`;
  }

  const recommendResult = (await HttpManager.getRecommendSongList(10)) as ResponseBody;
  const recommendSongs = Array.isArray(recommendResult?.data)
    ? recommendResult.data
        .map((item: any) => normalizeSongRow(item))
        .filter((item: AssistantSong) => Boolean(item?.id))
    : [];

  let addedCount = 0;
  for (const song of recommendSongs) {
    const addResult = (await HttpManager.setListSong({
      songId: song.id,
      songListId: createdPlaylist.id,
    })) as ResponseBody;
    if (addResult?.success !== false) {
      addedCount += 1;
    }
  }

  emitPlaylistSongsChanged(createdPlaylist.id);
  store.commit("setSongDetails", createdPlaylist);
  store.commit("setActiveNavName", NavName.MySongSheet);
  await router.push({ path: `/my-song-sheet-detail/${createdPlaylist.id}` });

  if (!addedCount) {
    return `已帮你创建推荐歌单“${createdPlaylist.title}”，不过这次还没成功加入推荐歌曲。`;
  }
  return `已帮你创建推荐歌单“${createdPlaylist.title}”，并加入了 ${addedCount} 首推荐歌曲。`;
}

async function deleteMyPlaylist(actionTarget: string) {
  if (!isLoggedIn.value || !userId.value) {
    return "你还没登录，暂时不能删除歌单。";
  }

  const userPlaylists = await fetchUserPlaylists(true);
  if (!userPlaylists.length) {
    return "你现在还没有创建歌单。";
  }

  const fallbackTarget = currentDetail.value?.title ? String(currentDetail.value.title) : "";
  const target = actionTarget || fallbackTarget;
  if (!target) {
    return "你可以直接告诉我要删除哪个歌单，比如“删除我的夜跑歌单”。";
  }

  const matchedPlaylist = findBestPlaylistMatch(userPlaylists, target);
  if (!matchedPlaylist) {
    return `我没有在你的歌单里找到“${target}”。`;
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除歌单“${matchedPlaylist.title}”吗？这个操作不可恢复。`,
      "删除歌单",
      {
        confirmButtonText: "删除",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
  } catch (error) {
    void error;
    return "已取消删除歌单。";
  }

  const result = (await HttpManager.deleteSongList(matchedPlaylist.id)) as ResponseBody;
  if (result?.success === false) {
    return result?.message || "删除歌单失败了。";
  }

  const currentRouteName = String(router.currentRoute.value.name ?? "");
  if (currentRouteName === "my-song-sheet-detail") {
    await router.push({ path: RouterName.MySongSheet });
  }
  return `已帮你删除歌单“${matchedPlaylist.title}”。`;
}

async function fetchUserPlaylists(forceRefresh = false) {
  if (!isLoggedIn.value || !userId.value) {
    return [] as AssistantPlaylist[];
  }

  void forceRefresh;

  try {
    const result = (await HttpManager.getSongListByConsumerId(userId.value)) as ResponseBody;
    return Array.isArray(result?.data) ? (result.data as AssistantPlaylist[]) : [];
  } catch (error) {
    void error;
    return [];
  }
}

async function fetchAllSongs(forceRefresh = false) {
  if (!forceRefresh && Array.isArray(allSongsCache.value)) {
    return allSongsCache.value;
  }

  try {
    const result = (await HttpManager.getAllSongs()) as ResponseBody;
    const songs = Array.isArray(result?.data)
      ? result.data.map((item: any) => normalizeSongRow(item))
      : [];
    allSongsCache.value = songs;
    return songs;
  } catch (error) {
    void error;
    return [];
  }
}

async function fetchAllSingers(forceRefresh = false) {
  if (!forceRefresh && Array.isArray(allSingersCache.value)) {
    return allSingersCache.value;
  }

  try {
    const result = (await HttpManager.getAllSinger()) as ResponseBody;
    const singers = Array.isArray(result?.data) ? (result.data as AssistantSinger[]) : [];
    allSingersCache.value = singers;
    return singers;
  } catch (error) {
    void error;
    return [];
  }
}

async function fetchPublicPlaylists(keyword: string) {
  try {
    const result = (await HttpManager.getSongListOfLikeTitle(keyword)) as ResponseBody;
    return Array.isArray(result?.data) ? (result.data as AssistantPlaylist[]) : [];
  } catch (error) {
    void error;
    return [];
  }
}

async function resolvePlaylistTarget(actionTarget: string) {
  const fallbackTarget = currentDetail.value?.title ? String(currentDetail.value.title) : "";
  const target = (actionTarget || fallbackTarget).trim();
  if (!target) {
    return null;
  }

  const semanticTarget = resolvePlaylistSemanticTarget(target);
  if (semanticTarget === "hotTop" || semanticTarget === "hotPage") {
    return await fetchTopHotPlaylist();
  }
  if (semanticTarget === "recommend") {
    return await fetchTopRecommendPlaylist();
  }

  if (isLoggedIn.value) {
    const userPlaylists = await fetchUserPlaylists();
    const matchedUserPlaylist = findBestPlaylistMatch(userPlaylists, target);
    if (matchedUserPlaylist) {
      return matchedUserPlaylist;
    }
  }

  const publicPlaylists = await fetchPublicPlaylists(target);
  return findBestPlaylistMatch(publicPlaylists, target);
}

function getRecommendPlaylistAccessMessage(actionTarget: string, operation: "play" | "collect") {
  const fallbackTarget = currentDetail.value?.title ? String(currentDetail.value.title) : "";
  const semanticTarget = resolvePlaylistSemanticTarget((actionTarget || fallbackTarget).trim());
  if (semanticTarget !== "recommend") {
    return "";
  }
  if (isLoggedIn.value) {
    return "";
  }
  return operation === "play"
    ? "你还没登录，推荐歌单需要登录后才能播放。"
    : "你还没登录，推荐歌单需要登录后才能操作收藏。";
}

async function fetchSongsByPlaylistId(songListId: number | string) {
  try {
    const listSongResult = (await HttpManager.getListSongOfSongId(songListId)) as ResponseBody;
    const songRecords = Array.isArray(listSongResult?.data) ? listSongResult.data : [];
    const songs = await Promise.all(
      songRecords.map(async (item: any) => {
        const songResult = (await HttpManager.getSongOfId(item.songId)) as ResponseBody;
        const song = Array.isArray(songResult?.data) ? songResult.data[0] : songResult?.data;
        return song ? normalizeSongRow(song) : null;
      })
    );
    return songs.filter(Boolean) as AssistantSong[];
  } catch (error) {
    void error;
    return [];
  }
}

function findBestPlaylistMatch(playlists: AssistantPlaylist[], target: string) {
  if (!Array.isArray(playlists) || !playlists.length) {
    return null;
  }
  const ranked = playlists
    .map((playlist) => ({
      playlist,
      score: getPlaylistMatchScore(playlist.title, target),
    }))
    .filter((item) => item.score > 0)
    .sort((left, right) => right.score - left.score);

  if (!ranked.length || ranked[0].score < 42) {
    return null;
  }
  return ranked[0].playlist;
}

function resolvePlaylistSemanticTarget(target?: string): PlaylistSemanticTarget {
  const normalizedTarget = normalizePlaylistText(target);
  if (!normalizedTarget) {
    return "";
  }
  if (hasPlaylistSemanticHint(normalizedTarget, RECOMMEND_PLAYLIST_HINTS)) {
    return "recommend";
  }
  if (hasPlaylistSemanticHint(normalizedTarget, HOT_TOP_PLAYLIST_HINTS)) {
    return "hotTop";
  }
  if (hasPlaylistSemanticHint(normalizedTarget, HOT_PAGE_PLAYLIST_HINTS)) {
    return "hotPage";
  }
  return "";
}

function hasPlaylistSemanticHint(normalizedTarget: string, hints: string[]) {
  return hints.some((hint) => {
    const normalizedHint = normalizePlaylistText(hint);
    return normalizedHint && (normalizedTarget.includes(normalizedHint) || normalizedHint.includes(normalizedTarget));
  });
}

async function fetchTopHotPlaylist() {
  try {
    const result = (await HttpManager.getHotSongLists(10)) as ResponseBody;
    const playlists = Array.isArray(result?.data) ? (result.data as AssistantPlaylist[]) : [];
    return playlists[0] || null;
  } catch (error) {
    void error;
    return null;
  }
}

async function fetchTopRecommendPlaylist() {
  if (!isLoggedIn.value) {
    return null;
  }
  try {
    const result = (await HttpManager.getRecommendSongLists(10)) as ResponseBody;
    const playlists = Array.isArray(result?.data) ? (result.data as AssistantPlaylist[]) : [];
    return playlists[0] || null;
  } catch (error) {
    void error;
    return null;
  }
}

async function openHotPlaylistPage() {
  store.commit("setActiveNavName", NavName.SongSheet);
  await router.push({
    path: RouterName.SongSheet,
    query: {
      source: "hot",
      page: "1",
    },
  });
  return "已帮你打开热门歌单页。";
}

async function openRecommendPlaylistPage() {
  if (!isLoggedIn.value) {
    return "你还没登录，推荐歌单需要登录后才能查看。";
  }
  store.commit("setActiveNavName", NavName.SongSheet);
  await router.push({
    path: RouterName.SongSheet,
    query: {
      source: "recommend",
      page: "1",
    },
  });
  return "已帮你打开推荐歌单页。";
}

function getPlaylistMatchScore(title?: string, target?: string) {
  const normalizedTitle = normalizePlaylistText(title);
  const normalizedTarget = normalizePlaylistText(target);
  if (!normalizedTitle || !normalizedTarget) {
    return 0;
  }
  if (normalizedTitle === normalizedTarget) {
    return 100;
  }
  if (normalizedTitle.includes(normalizedTarget)) {
    return 88;
  }
  if (normalizedTarget.includes(normalizedTitle)) {
    return 72;
  }

  let score = 0;
  const targetTokens = Array.from(new Set(splitMatchTokens(target)));
  for (const token of targetTokens) {
    if (!token) {
      continue;
    }
    if (normalizedTitle.includes(token)) {
      score += token.length >= 2 ? 18 : 8;
    }
  }
  return score;
}

function normalizePlaylistText(value?: string) {
  return String(value ?? "")
    .toLowerCase()
    .replace(/歌单详情|详情页|歌单|列表|页面|页/g, "")
    .replace(/[的\s\u3000_-]/g, "")
    .replace(/\[/g, "")
    .replace(/\]/g, "")
    .replace(/[<>/\\|,.!?;:'"，。！？；：、“”‘’（）《》【】]/g, "")
    .trim();
}

function splitMatchTokens(value?: string) {
  const normalized = String(value ?? "")
    .replace(/歌单详情|详情页|歌单|列表|页面|页/g, " ")
    .replace(/\[/g, " ")
    .replace(/\]/g, " ")
    .replace(/[<>/\\|,.!?;:'"，。！？；：、“”‘’（）《》【】-]/g, " ");
  return normalized
    .split(/[\s\u3000_]+/)
    .map((item) => normalizePlaylistText(item))
    .filter(Boolean);
}

function findBestSingerMatch(singers: AssistantSinger[], target: string) {
  if (!Array.isArray(singers) || !singers.length) {
    return null;
  }

  const ranked = singers
    .map((singer) => ({
      singer,
      score: getSingerMatchScore(singer.name, target),
    }))
    .filter((item) => item.score > 0)
    .sort((left, right) => right.score - left.score);

  if (!ranked.length || ranked[0].score < 42) {
    return null;
  }
  return ranked[0].singer;
}

function getSingerMatchScore(name?: string, target?: string) {
  const normalizedName = normalizeSingerText(name);
  const normalizedTarget = normalizeSingerText(target);
  if (!normalizedName || !normalizedTarget) {
    return 0;
  }
  if (normalizedName === normalizedTarget) {
    return 100;
  }
  if (normalizedName.includes(normalizedTarget)) {
    return 86;
  }
  if (normalizedTarget.includes(normalizedName)) {
    return 70;
  }

  let score = 0;
  const targetTokens = Array.from(new Set(splitMatchTokens(target).map((item) => normalizeSingerText(item)).filter(Boolean)));
  for (const token of targetTokens) {
    if (normalizedName.includes(token)) {
      score += token.length >= 2 ? 18 : 8;
    }
  }
  return score;
}

function normalizeSingerText(value?: string) {
  return String(value ?? "")
    .toLowerCase()
    .replace(/歌手详情|歌手页面|歌手页|歌手主页|歌手资料|歌手|详情|主页|资料/g, "")
    .replace(/[的\s\u3000_-]/g, "")
    .replace(/\[/g, "")
    .replace(/\]/g, "")
    .replace(/[<>/\\|,.!?;:'"，。！？；：、“”‘’（）《》【】]/g, "")
    .trim();
}

function findBestSongMatch(songs: AssistantSong[], target: string) {
  if (!Array.isArray(songs) || !songs.length) {
    return null;
  }

  const ranked = songs
    .map((song) => ({
      song,
      score: getSongMatchScore(song, target),
    }))
    .filter((item) => item.score > 0)
    .sort((left, right) => right.score - left.score);

  if (!ranked.length || ranked[0].score < 46) {
    return null;
  }
  return ranked[0].song;
}

function getSongMatchScore(song: AssistantSong, target: string) {
  const normalizedTarget = normalizeSongText(target);
  const normalizedSong = normalizeSongText(song.name);
  const normalizedTitle = normalizeSongText(song.songTitle);
  const normalizedSinger = normalizeSongText(song.singerName);
  if (!normalizedTarget) {
    return 0;
  }

  let score = 0;
  if (normalizedSong === normalizedTarget) {
    score += 100;
  }
  if (normalizedTitle === normalizedTarget) {
    score += 92;
  }
  if (normalizedSong.includes(normalizedTarget)) {
    score += 72;
  }
  if (normalizedTitle.includes(normalizedTarget)) {
    score += 66;
  }
  if (normalizedSinger && normalizedSinger.includes(normalizedTarget)) {
    score += 36;
  }

  const targetTokens = Array.from(new Set(splitSongTokens(target)));
  for (const token of targetTokens) {
    if (!token) continue;
    if (normalizedSong.includes(token)) {
      score += token.length >= 2 ? 18 : 8;
    } else if (normalizedTitle.includes(token)) {
      score += token.length >= 2 ? 15 : 6;
    } else if (normalizedSinger.includes(token)) {
      score += 10;
    }
  }
  return score;
}

function normalizeSongText(value?: string) {
  return String(value ?? "")
    .toLowerCase()
    .replace(/[的\s\u3000_-]/g, "")
    .replace(/\[/g, "")
    .replace(/\]/g, "")
    .replace(/[<>/\\|,.!?;:'"，。！？；：、“”‘’（）《》【】]/g, "")
    .trim();
}

function splitSongTokens(value?: string) {
  const normalized = String(value ?? "")
    .replace(/\[/g, " ")
    .replace(/\]/g, " ")
    .replace(/[<>/\\|,.!?;:'"，。！？；：、“”‘’（）《》【】-]/g, " ");
  return normalized
    .split(/[\s\u3000_]+/)
    .map((item) => normalizeSongText(item))
    .filter(Boolean);
}

function formatPlaylistTitle(value: string) {
  const trimmed = String(value ?? "").trim();
  if (!trimmed) {
    return "";
  }
  return trimmed.endsWith("歌单") ? trimmed : trimmed;
}

function buildRecommendedPlaylistTitle(theme?: string) {
  const normalizedTheme = String(theme ?? "")
    .replace(/推荐歌单|推荐列表|歌单|列表/g, " ")
    .replace(/推荐|生成|创建|做一个|做个|做一张|整一个|帮我做|帮我生成|帮我创建|直接/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (!normalizedTheme) {
    return "AI 今日推荐";
  }
  if (normalizedTheme.endsWith("推荐") || normalizedTheme.endsWith("精选")) {
    return normalizedTheme;
  }
  return `${normalizedTheme} 推荐`;
}

function ensureUniquePlaylistTitle(playlists: AssistantPlaylist[], baseTitle: string) {
  const normalizedBase = normalizePlaylistText(baseTitle);
  const normalizedTitles = new Set(playlists.map((item) => normalizePlaylistText(item.title)));
  if (!normalizedTitles.has(normalizedBase)) {
    return baseTitle;
  }

  let index = 2;
  while (normalizedTitles.has(normalizePlaylistText(`${baseTitle} ${index}`))) {
    index += 1;
  }
  return `${baseTitle} ${index}`;
}

function inferPlaylistStyle(title: string) {
  if (title.includes("夜跑") || title.includes("跑步") || title.includes("运动")) {
    return "运动";
  }
  if (title.includes("睡") || title.includes("助眠") || title.includes("晚安")) {
    return "治愈";
  }
  if (title.includes("学习") || title.includes("专注")) {
    return "轻音乐";
  }
  return "自定义";
}

async function openCurrentLyricPage() {
  if (!currentSongId.value) {
    return "当前还没有正在播放的歌曲，暂时打不开歌词页。";
  }

  await router.push({ name: "lyric", params: { id: currentSongId.value } });
  return "好的，已经帮你打开歌词页。";
}

function toggleDarkMode() {
  const nextDarkMode = !document.documentElement.classList.contains("dark");
  localStorage.setItem("darkMode", String(nextDarkMode));
  document.documentElement.classList.toggle("dark", nextDarkMode);
  window.dispatchEvent(new CustomEvent("darkModeToggle", { detail: nextDarkMode }));
  return nextDarkMode ? "已切换到暗黑模式。" : "已切换到明亮模式。";
}

function pauseCurrentSong() {
  if (!currentSongId.value) {
    return "当前还没有正在播放的歌曲。";
  }
  if (!store.getters.isPlay) {
    return "当前已经是暂停状态了。";
  }

  store.commit("setIsPlay", false);
  return "好的，已经帮你暂停播放。";
}

function resumeCurrentSong() {
  if (!currentSongId.value || !currentSongUrl.value) {
    return "当前还没有可继续播放的歌曲。";
  }
  if (store.getters.isPlay) {
    return "当前已经在播放了。";
  }

  store.commit("setIsPlay", true);
  window.dispatchEvent(new CustomEvent("music-player:reveal"));
  return "好的，已经继续播放。";
}

function getSafeCurrentIndex() {
  const playList = currentPlayList.value;
  if (!playList.length) {
    return -1;
  }
  if (currentPlayIndex.value >= 0 && currentPlayIndex.value < playList.length) {
    return currentPlayIndex.value;
  }
  const matchedIndex = playList.findIndex((item: any) => {
    if (String(item?.id) !== String(currentSongId.value)) {
      return false;
    }
    if (!currentSongUrl.value || !item?.url) {
      return true;
    }
    return item.url === currentSongUrl.value;
  });
  return matchedIndex >= 0 ? matchedIndex : 0;
}

function normalizePlaylistSong(song: any): AssistantSong {
  const name = song?.name || `${song?.singerName || currentSingerName.value || "未知歌手"}-${song?.songTitle || currentSongTitle.value || "未知歌曲"}`;
  return {
    id: Number(song?.id),
    name,
    singerId: Number(song?.singerId ?? song?.singer_id ?? 0) || undefined,
    singerName: song?.singerName || getSingerName(name),
    songTitle: song?.songTitle || getSongTitle(name),
    introduction: song?.introduction,
    pic: song?.pic,
    lyric: song?.lyric,
    url: song?.url,
    type: song?.type,
  };
}

function playRelativeSong(direction: 1 | -1) {
  const playList = currentPlayList.value;
  if (!playList.length) {
    return "当前播放列表还是空的，暂时没法切歌。";
  }
  if (playList.length === 1) {
    return "当前播放列表只有一首歌，没法继续切换。";
  }

  const currentIndex = getSafeCurrentIndex();
  if (currentIndex < 0) {
    return "当前还没有定位到正在播放的歌曲。";
  }

  const nextIndex = (currentIndex + direction + playList.length) % playList.length;
  const normalizedPlayList = playList.map((item: any) => normalizePlaylistSong(item));
  const targetSong = normalizedPlayList[nextIndex];

  playSong(targetSong, normalizedPlayList, false);
  return direction > 0
    ? `已为你切换到下一首《${targetSong.songTitle}》。`
    : `已为你切换到上一首《${targetSong.songTitle}》。`;
}

function emitCollectionChanged(songId: string | number, collected: boolean) {
  window.dispatchEvent(
    new CustomEvent(COLLECTION_CHANGED_EVENT, {
      detail: {
        songId,
        collected,
      },
    })
  );
}

function emitPlaylistSongsChanged(playlistId: string | number) {
  window.dispatchEvent(
    new CustomEvent(PLAYLIST_SONGS_CHANGED_EVENT, {
      detail: {
        playlistId,
      },
    })
  );
}

async function updateCurrentSongCollection(nextCollected: boolean) {
  if (!isLoggedIn.value || !userId.value) {
    return "你还没登录，暂时不能操作歌曲收藏。";
  }
  if (!currentSongId.value) {
    return "当前还没有正在播放的歌曲。";
  }

  const statusResult = (await HttpManager.isCollection({
    userId: userId.value,
    type: 0,
    songId: currentSongId.value,
  })) as ResponseBody;
  const currentCollected = Boolean(statusResult?.data);

  if (nextCollected === currentCollected) {
    return nextCollected ? "当前这首歌已经在收藏里了。" : "当前这首歌本来就没有收藏。";
  }

  const result = nextCollected
    ? ((await HttpManager.setCollection({ userId: userId.value, type: 0, songId: currentSongId.value })) as ResponseBody)
    : ((await HttpManager.deleteCollection({ userId: userId.value, type: 0, songId: currentSongId.value })) as ResponseBody);

  if (result?.success === false) {
    return result?.message || (nextCollected ? "收藏失败了。" : "取消收藏失败了。");
  }

  emitCollectionChanged(currentSongId.value, nextCollected);
  const songTitle = currentSongTitle.value || "当前歌曲";
  return nextCollected ? `已帮你收藏《${songTitle}》。` : `已帮你取消收藏《${songTitle}》。`;
}

function playSong(song: AssistantSong, playlist: AssistantSong[] = [song], showToast = true) {
  setSongContext(song, playlist, true);
  if (showToast) {
    ElMessage.success(`正在播放 ${song.songTitle || getSongTitle(song.name)}`);
  }
}

function openLyric(song: AssistantSong) {
  if (!song?.id) return;
  setSongContext(song, [song], false);
  void router.push({ name: "lyric", params: { id: song.id } });
}

function normalizeSongRow(song: any): AssistantSong {
  const name = song?.name || `${song?.singerName || "未知歌手"}-${song?.songTitle || "未知歌曲"}`;
  return {
    id: Number(song?.id),
    name,
    singerId: Number(song?.singerId ?? song?.singer_id ?? 0) || undefined,
    singerName: song?.singerName || getSingerName(name),
    songTitle: song?.songTitle || getSongTitle(name),
    introduction: song?.introduction,
    pic: song?.pic,
    lyric: song?.lyric,
    url: song?.url,
    type: song?.type,
  };
}

function setSongContext(song: AssistantSong, playlist: AssistantSong[] = [song], autoPlay = false) {
  const normalizedSong = normalizeSongRow(song);
  const normalizedPlayList = playlist.map((item) => normalizeSongRow(item));
  const currentIndex = Math.max(
    0,
    normalizedPlayList.findIndex((item) => item.id === normalizedSong.id)
  );
  store.dispatch("playMusic", {
    id: normalizedSong.id,
    url: normalizedSong.url,
    pic: normalizedSong.pic,
    index: currentIndex,
    songTitle: normalizedSong.songTitle || getSongTitle(normalizedSong.name),
    singerName: normalizedSong.singerName || getSingerName(normalizedSong.name),
    lyric: normalizedSong.lyric,
    currentSongList: normalizedPlayList,
  });
  store.commit("setIsPlay", autoPlay);
  if (autoPlay) {
    window.dispatchEvent(new CustomEvent("music-player:reveal"));
  }
}

function getSongTitle(name?: string) {
  if (!name) return "未知歌曲";
  const segments = name.split("-");
  return segments.length > 1 ? segments.slice(1).join("-").trim() : name;
}

function getSingerName(name?: string) {
  if (!name) return "未知歌手";
  const index = name.indexOf("-");
  return index > -1 ? name.slice(0, index).trim() : name;
}

function attachImageUrl(url?: string) {
  return HttpManager.attachImageUrl(url);
}

async function scrollToBottom() {
  await nextTick();
  if (!messageListRef.value) return;
  messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
}
</script>

<style lang="scss" scoped>
.assistant-trigger {
  position: fixed;
  right: clamp(18px, 2.4vw, 32px);
  top: clamp(188px, 24vh, 256px);
  z-index: 1200;
  width: 62px;
  height: 62px;
  border: 0;
  border-radius: 999px;
  background:
    radial-gradient(circle at 24% 22%, rgba(255, 255, 255, 0.98), rgba(255, 255, 255, 0.78) 55%, rgba(219, 246, 240, 0.9) 100%);
  box-shadow:
    0 16px 30px rgba(255, 163, 191, 0.28),
    0 0 0 1px rgba(255, 255, 255, 0.72) inset;
  backdrop-filter: blur(18px);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.assistant-trigger:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow:
    0 22px 36px rgba(255, 163, 191, 0.34),
    0 0 0 1px rgba(255, 255, 255, 0.8) inset;
}

.assistant-trigger__logo,
.assistant-head__logo { width: 100%; height: 100%; object-fit: contain; }
.assistant-trigger__badge { position: absolute; right: -2px; bottom: -2px; padding: 2px 7px; border-radius: 999px; background: #f58fb2; color: #fff; font-size: 11px; font-weight: 700; }

.assistant-shell { height: 100%; display: flex; flex-direction: column; gap: 14px; }
.assistant-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.assistant-head__brand { display: flex; align-items: center; gap: 12px; }
.assistant-head__logo {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  box-shadow: 0 12px 24px rgba(255, 169, 195, 0.24);
}
.assistant-head h3 { margin: 0; font-size: 18px; }
.assistant-head p { margin: 4px 0 0; color: #76839a; font-size: 13px; }
.assistant-close { border: 0; background: transparent; font-size: 26px; cursor: pointer; color: #738099; }

.assistant-quick { display: flex; flex-wrap: wrap; gap: 8px; }
.assistant-chip { border: 0; border-radius: 999px; padding: 8px 12px; background: #f5f7fb; color: #405068; cursor: pointer; }

.assistant-messages { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; padding-right: 4px; }
.assistant-message { display: flex; flex-direction: column; gap: 8px; }
.assistant-message.user { align-items: flex-end; }
.assistant-bubble { max-width: 88%; padding: 12px 14px; border-radius: 18px; background: #f6f8fc; color: #243246; line-height: 1.6; }
.assistant-message.user .assistant-bubble { background: linear-gradient(135deg, #ffd9e7, #d7f3ef); }
.assistant-thinking { color: #7b88a1; }

.assistant-song-card { display: grid; grid-template-columns: 62px 1fr; gap: 12px; padding: 12px; border-radius: 18px; background: #f8fbff; }
.assistant-song-card__cover { width: 62px; height: 62px; border-radius: 14px; object-fit: cover; }
.assistant-song-card__meta { display: flex; flex-direction: column; gap: 4px; }
.assistant-song-card__meta span, .assistant-song-card__meta small { color: #75839a; }
.assistant-song-card__actions { grid-column: 1 / -1; display: flex; gap: 10px; }
.assistant-song-card__actions button, .assistant-candidate { border: 0; border-radius: 14px; padding: 10px 12px; cursor: pointer; }
.assistant-song-card__actions button { background: #15284d; color: #fff; }
.assistant-song-card__actions .ghost { background: #eef3fb; color: #30405a; }

.assistant-candidates { display: flex; flex-direction: column; gap: 8px; }
.assistant-candidate { display: grid; grid-template-columns: 44px 1fr; align-items: center; gap: 10px; background: #f7f9fd; text-align: left; }
.assistant-candidate img { width: 44px; height: 44px; border-radius: 12px; object-fit: cover; }
.assistant-candidate span { font-weight: 600; color: #233246; }
.assistant-candidate small { color: #78859b; }
.assistant-candidates__toggle { align-self: center; border: 0; background: transparent; color: #6280a3; font-size: 13px; cursor: pointer; padding: 4px 10px; border-radius: 999px; }
.assistant-candidates__toggle:hover { background: rgba(198, 220, 244, 0.28); color: #365173; }

.assistant-confirm { display: flex; gap: 8px; }
.assistant-confirm__button {
  border: 0;
  border-radius: 999px;
  padding: 8px 14px;
  background: #eef3fb;
  color: #365173;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.assistant-confirm__button.primary {
  background: linear-gradient(135deg, #ffd9e7, #d7f3ef);
  color: #243246;
  box-shadow: 0 10px 18px rgba(245, 143, 178, 0.16);
}
.assistant-confirm__button:hover {
  transform: translateY(-1px);
}

.assistant-footer { display: flex; flex-direction: column; gap: 10px; }
.assistant-input { display: flex; flex-direction: column; gap: 10px; }
.assistant-input textarea { width: 100%; resize: none; border: 1px solid #dde5f0; border-radius: 16px; padding: 12px; font: inherit; box-sizing: border-box; }
.assistant-input button { align-self: flex-end; border: 0; border-radius: 14px; padding: 10px 16px; background: #15284d; color: #fff; cursor: pointer; }
.assistant-input button:disabled { opacity: 0.55; cursor: not-allowed; }

@media (max-width: 768px) {
  .assistant-trigger { top: auto; bottom: 122px; width: 58px; height: 58px; }
}
</style>
