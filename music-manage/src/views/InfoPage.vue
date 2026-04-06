<template>
  <div class="admin-dashboard">
    <div class="page-head">
      <h1 class="page-title">系统首页</h1>
    </div>

    <el-row :gutter="20" class="kpi-row">
      <el-col :xl="4" :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="hover" :body-style="{ padding: '0px' }" class="kpi-card">
          <div
            class="card-content user-theme is-link"
            tabindex="0"
            role="button"
            @click="goToModule('/Consumer')"
            @keydown.enter.prevent="goToModule('/Consumer')"
            @keydown.space.prevent="goToModule('/Consumer')"
          >
            <div class="card-left">
              <el-icon><User /></el-icon>
            </div>
            <div class="card-right">
              <div class="card-label">用户总数</div>
              <div class="card-num">{{ userCount }}</div>
              <div class="card-meta">{{ kpiMeta.user }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xl="4" :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="hover" :body-style="{ padding: '0px' }" class="kpi-card">
          <div
            class="card-content song-theme is-link"
            tabindex="0"
            role="button"
            @click="goToModule('/songs')"
            @keydown.enter.prevent="goToModule('/songs')"
            @keydown.space.prevent="goToModule('/songs')"
          >
            <div class="card-left">
              <el-icon><Headset /></el-icon>
            </div>
            <div class="card-right">
              <div class="card-label">歌曲总数</div>
              <div class="card-num">{{ songCount }}</div>
              <div class="card-meta">{{ kpiMeta.song }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xl="4" :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="hover" :body-style="{ padding: '0px' }" class="kpi-card">
          <div
            class="card-content singer-theme is-link"
            tabindex="0"
            role="button"
            @click="goToModule('/singer')"
            @keydown.enter.prevent="goToModule('/singer')"
            @keydown.space.prevent="goToModule('/singer')"
          >
            <div class="card-left">
              <el-icon><Mic /></el-icon>
            </div>
            <div class="card-right">
              <div class="card-label">歌手数量</div>
              <div class="card-num">{{ singerCount }}</div>
              <div class="card-meta">{{ kpiMeta.singer }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xl="4" :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="hover" :body-style="{ padding: '0px' }" class="kpi-card">
          <div
            class="card-content list-theme is-link"
            tabindex="0"
            role="button"
            @click="goToModule('/SongList')"
            @keydown.enter.prevent="goToModule('/SongList')"
            @keydown.space.prevent="goToModule('/SongList')"
          >
            <div class="card-left">
              <el-icon><Document /></el-icon>
            </div>
            <div class="card-right">
              <div class="card-label">歌单数量</div>
              <div class="card-num">{{ songListCount }}</div>
              <div class="card-meta">{{ kpiMeta.list }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xl="4" :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="hover" :body-style="{ padding: '0px' }" class="kpi-card">
          <div
            class="card-content feedback-theme is-link"
            tabindex="0"
            role="button"
            @click="goToModule('/feedback-manage')"
            @keydown.enter.prevent="goToModule('/feedback-manage')"
            @keydown.space.prevent="goToModule('/feedback-manage')"
          >
            <div class="card-left">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="card-right">
              <div class="card-label">待处理反馈</div>
              <div class="card-num">{{ pendingFeedbackCount }}</div>
              <div class="card-meta">{{ kpiMeta.feedback }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="insight-row">
      <el-col :xl="12" :lg="12" :md="24" :sm="24" :xs="24">
        <el-card class="cav-info insight-card" shadow="hover">
          <div class="chart-title-block">
            <h3>待审核总览</h3>
          </div>
          <div class="audit-grid">
            <button
              v-for="item in auditOverviewItems"
              :key="item.key"
              type="button"
              class="audit-item"
              :class="item.theme"
              @click="goToModule(item.path)"
            >
              <span class="audit-label">{{ item.label }}</span>
              <strong class="audit-value">{{ item.value }}</strong>
              <span class="audit-meta">{{ item.meta }}</span>
            </button>
          </div>
          <div class="audit-preview">
            <div class="audit-preview-head">
              <span>最近待处理内容</span>
              <span class="audit-preview-count">共 {{ auditPendingPreview.length }} 条</span>
            </div>
            <div class="audit-preview-scroll">
              <button
                v-for="item in auditPendingPreview"
                :key="item.key"
                type="button"
                class="audit-preview-item"
                @click="goToModule(item.path)"
              >
                <span class="audit-preview-badge" :class="item.badgeClass">{{ item.badge }}</span>
                <div class="audit-preview-copy">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.subtitle }}</span>
                </div>
                <span class="audit-preview-action">去处理</span>
              </button>
              <div v-if="!auditPendingPreview.length" class="audit-preview-empty">当前没有需要立即处理的审核内容</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xl="12" :lg="12" :md="24" :sm="24" :xs="24">
        <el-card class="cav-info insight-card" shadow="hover">
          <div class="chart-title-block">
            <h3>资源健康度</h3>
          </div>
          <div class="health-list">
            <button
              v-for="item in contentHealthItems"
              :key="item.key"
              type="button"
              class="health-row"
              :class="{ clickable: item.path }"
              @click="goToModule(item.path, item.query)"
            >
              <div class="health-copy">
                <span class="health-name">{{ item.label }}</span>
                <span class="health-tip">{{ item.tip }}</span>
              </div>
              <div class="health-side">
                <span class="health-value" :class="{ warning: item.value > 0 }">{{ item.value }}</span>
                <span class="health-action">{{ item.path ? "查看" : "统计" }}</span>
              </div>
            </button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="cav-info" shadow="hover">
          <div class="chart-head">
            <div class="chart-title-block">
              <h3>{{ userActivityTitle }}</h3>
            </div>
            <el-radio-group v-model="userActivityRange" size="small" @change="initUserActivityChart">
              <el-radio-button label="7d">最近 7 天</el-radio-button>
              <el-radio-button label="30d">最近 30 天</el-radio-button>
              <el-radio-button label="6m">最近半年</el-radio-button>
            </el-radio-group>
          </div>
          <div class="chart-box" id="userActivityChart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="cav-info" shadow="hover">
          <div class="chart-head">
            <div class="chart-title-block">
              <h3>热门歌曲监控</h3>
            </div>
            <el-button class="song-monitor-entry" @click="goToModule('/songs')">进入歌曲管理</el-button>
          </div>
          <div v-if="topSongsPreview.length" class="top-song-monitor-list">
            <button
              v-for="(item, index) in topSongsPreview"
              :key="`${item.songId}-${index}`"
              type="button"
              class="top-song-monitor-item"
              @click="goToModule('/songs', item.manageQuery)"
            >
              <span class="top-song-monitor-rank">{{ index + 1 }}</span>
              <el-image :src="item.cover" fit="cover" class="top-song-monitor-cover">
                <template #error>
                  <div class="top-song-monitor-cover-fallback">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="top-song-monitor-copy">
                <strong>{{ item.songName }}</strong>
                <span>{{ item.singerName }}</span>
                <p>{{ item.summary }}</p>
              </div>
              <div class="top-song-monitor-tags">
                <el-tag size="small" effect="light" :type="item.lyricTagType">{{ item.lyricLabel }}</el-tag>
                <el-tag size="small" effect="light" :type="item.audioTagType">{{ item.audioLabel }}</el-tag>
                <el-tag size="small" effect="light" :type="item.auditTagType">{{ item.auditLabel }}</el-tag>
              </div>
              <div class="top-song-monitor-side">
                <strong>{{ formatPlayCount(item.playCount) }}</strong>
                <span>播放</span>
                <em>去管理</em>
              </div>
            </button>
          </div>
          <el-empty v-else description="暂无热门歌曲数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="cav-info" shadow="hover">
          <div class="chart-head">
            <div class="chart-title-block">
              <h3>{{ newContentTrendTitle }}</h3>
            </div>
            <el-radio-group v-model="newContentTrendRange" size="small" @change="initNewContentTrendChart">
              <el-radio-button label="7d">最近 7 天</el-radio-button>
              <el-radio-button label="30d">最近 30 天</el-radio-button>
              <el-radio-button label="6m">最近半年</el-radio-button>
            </el-radio-group>
          </div>
          <div class="chart-box" id="newContentTrendChart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="cav-info" shadow="hover">
          <div class="chart-head">
            <div class="chart-title-block">
              <h3>{{ userDistributionTitle }}</h3>
            </div>
            <el-radio-group v-model="userDistributionMode" size="small" @change="initUserDistributionChart">
              <el-radio-button label="age">年龄</el-radio-button>
              <el-radio-button label="sex">性别</el-radio-button>
            </el-radio-group>
          </div>
          <div class="distribution-layout">
            <div class="distribution-legend">
              <div v-for="item in userDistributionLegend" :key="item.name" class="legend-row">
                <i :style="{ background: item.color }"></i>
                <span>{{ item.name }}</span>
              </div>
            </div>
            <div class="distribution-chart-box" id="userDistributionChart"></div>
            <div class="distribution-note">
              <strong>{{ userDistributionNote.title }}</strong>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card class="cav-info" shadow="hover">
          <div class="feedback-chart-head">
            <div class="chart-title-block">
              <h3>{{ feedbackTrendTitle }}</h3>
            </div>
            <el-radio-group v-model="feedbackTrendRange" size="small" @change="initRecentFeedbackTrendChart">
              <el-radio-button label="7d">最近 7 天</el-radio-button>
              <el-radio-button label="30d">最近 30 天</el-radio-button>
              <el-radio-button label="6m">最近半年</el-radio-button>
            </el-radio-group>
          </div>
          <div class="chart-box" id="feedbackTrendChart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, onActivated, onBeforeUnmount, nextTick } from "vue";
import { useRouter } from "vue-router";
import * as echarts from "echarts";
import { Mic, Document, User, Headset, ChatDotRound, Picture } from "@element-plus/icons-vue";
import { getLyricStatus, getLyricStatusLabel, getLyricStatusTagType } from "@/utils";

// ⚠️ 假设您的 HttpManager 中已封装以下新接口
import { HttpManager } from "@/api/index"; 

const router = useRouter();

// --- 核心数据统计 ---
const userCount = ref(0);
const songCount = ref(0);
const singerCount = ref(0);
const songListCount = ref(0);
const pendingFeedbackCount = ref(0);
const auditOverview = ref({
  pendingSongCount: 0,
  pendingSongListCount: 0,
  pendingFeedbackCount: 0,
});
const auditPendingPreview = ref<any[]>([]);
const contentHealth = ref({
  missingLyricsCount: 0,
  missingAudioCount: 0,
  missingSingerIntroCount: 0,
  defaultSongListCoverCount: 0,
});
const feedbackTrendRange = ref("7d");
const userDistributionMode = ref("age");
const userActivityRange = ref("30d");
const newContentTrendRange = ref("7d");
type TopSongMonitorRecord = {
  songId: number;
  songName: string;
  singerName: string;
  playCount: number;
  pic?: string;
  introduction?: string;
  lyric?: string;
  url?: string;
  type?: number;
};
const topSongs = ref<TopSongMonitorRecord[]>([]);
const userDistributionRaw = ref<{ value: number; name: string }[]>([]);
const chartIds = ["userActivityChart", "newContentTrendChart", "userDistributionChart", "feedbackTrendChart"] as const;

// --- ECharts 配置 ---

// 1. 近 30 日用户活跃趋势 (Line Chart)
const userActivityOption = reactive({
  title: { text: '活跃用户', left: 'center', show: false },
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: [] as string[] },
  yAxis: { type: 'value' },
  series: [{
    name: '活跃用户',
    type: 'line',
    smooth: true,
    data: [] as number[],
    itemStyle: { color: '#1F6BFF' },
    lineStyle: { width: 4, color: '#1F6BFF' },
    areaStyle: { color: 'rgba(31, 107, 255, 0.12)' }
  }],
});

// 3. 新增内容趋势 (Multi-Line Chart)
const newContentTrendOption = reactive({
  title: { text: '新增内容', left: 'center', show: false },
  tooltip: { trigger: 'axis' },
  legend: { data: ['用户', '歌曲', '歌手', '歌单'], top: 'bottom' },
  grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: [] as string[] },
  yAxis: { type: 'value' },
  series: [
    { name: '用户', type: 'line', smooth: true, data: [] as number[], itemStyle: { color: '#1F6BFF' }, lineStyle: { width: 3, color: '#1F6BFF' } },
    { name: '歌曲', type: 'line', smooth: true, data: [] as number[], itemStyle: { color: '#14B97B' }, lineStyle: { width: 3, color: '#14B97B' } },
    { name: '歌手', type: 'line', smooth: true, data: [] as number[], itemStyle: { color: '#F35B66' }, lineStyle: { width: 3, color: '#F35B66' } },
    { name: '歌单', type: 'line', smooth: true, data: [] as number[], itemStyle: { color: '#FF9F0A' }, lineStyle: { width: 3, color: '#FF9F0A' } },
  ],
});

// 4. 用户年龄/性别分布 (Pie Chart)
const userDistributionOption = reactive({
  title: { text: '年龄分布', left: 'center', show: false },
  tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
  legend: { show: false, data: [] as string[] },
  series: [{
    name: '用户分布',
    type: 'pie',
    radius: '68%',
    center: ['50%', '60%'],
    avoidLabelOverlap: true,
    minShowLabelAngle: 10,
    data: [] as { value: number, name: string }[],
    color: [] as string[],
    label: {
      color: '#5B6B88',
      fontSize: 12,
      fontWeight: 700,
      formatter: (params: any) => (params.value > 0 ? params.name : ""),
    },
    labelLine: {
      show: true,
      length: 10,
      length2: 12,
    },
    labelLayout: {
      hideOverlap: true,
    },
    emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
  }],
});

// 5. 最近 7 天反馈趋势 (Bar Chart)
const feedbackTrendOption = reactive({
  title: { text: '反馈趋势', left: 'center', show: false },
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: [] as string[] },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    name: '反馈数量',
    type: 'line',
    smooth: true,
    data: [] as number[],
    itemStyle: { color: '#A855F7' },
    lineStyle: { width: 3, color: '#A855F7' },
    areaStyle: { color: 'rgba(168, 85, 247, 0.12)' },
  }],
});

const feedbackTrendTitleMap: Record<string, string> = {
  "7d": "最近 7 天反馈趋势",
  "30d": "最近 30 天反馈趋势",
  "6m": "最近半年反馈趋势",
};

const newContentTrendTitleMap: Record<string, string> = {
  "7d": "最近 7 天内容趋势",
  "30d": "最近 30 天内容趋势",
  "6m": "最近半年内容趋势",
};

const userActivityTitleMap: Record<string, string> = {
  "7d": "近 7 日用户活跃趋势",
  "30d": "近 30 日用户活跃趋势",
  "6m": "近半年用户活跃趋势",
};

const feedbackTrendTitle = computed(() => feedbackTrendTitleMap[feedbackTrendRange.value] || "反馈趋势");
const newContentTrendTitle = computed(() => newContentTrendTitleMap[newContentTrendRange.value] || "新增内容趋势");
const userDistributionTitle = computed(() =>
  userDistributionMode.value === "sex" ? "用户性别分布" : "用户年龄分布"
);
const userActivityTitle = computed(() => userActivityTitleMap[userActivityRange.value] || "用户活跃趋势");
const topSongsPreview = computed(() => {
  return topSongs.value.slice(0, 10).map((item) => {
    const lyricStatus = getLyricStatus(item.lyric);
    const hasAudio = !!normalizeText(item.url);
    const { displaySongName, displaySingerName } = resolveSongDisplay(item.songName, item.singerName);
    return {
      ...item,
      songName: displaySongName,
      singerName: displaySingerName,
      cover: HttpManager.attachImageUrl(item.pic),
      summary:
        normalizeText(item.introduction) ||
        (hasAudio
          ? `当前播放 ${formatPlayCount(item.playCount)} 次，资源已可播放，建议继续关注热度变化`
          : `当前播放 ${formatPlayCount(item.playCount)} 次，但当前歌曲资料里还没有音频地址，建议尽快补齐`),
      lyricLabel: getLyricStatusLabel(lyricStatus),
      lyricTagType: getLyricStatusTagType(lyricStatus),
      audioLabel: hasAudio ? "音频已入库" : "音频待补",
      audioTagType: hasAudio ? "success" : "danger",
      auditLabel: getSongStatusLabel(Number(item.type)),
      auditTagType: getSongStatusTagType(Number(item.type)),
      manageQuery: {
        keyword: normalizeText(item.songName) || undefined,
      },
    };
  });
});
const kpiMeta = computed(() => ({
  user: userCount.value ? `已同步 ${userCount.value} 条用户档案` : "等待用户数据同步",
  song: songCount.value ? `资源库当前可管理 ${songCount.value} 首` : "等待歌曲数据同步",
  singer: singerCount.value ? `艺人资料已入库 ${singerCount.value} 位` : "等待歌手资料同步",
  list: songListCount.value ? `内容编排已覆盖 ${songListCount.value} 个歌单` : "等待歌单数据同步",
  feedback: pendingFeedbackCount.value ? `当前待处理 ${pendingFeedbackCount.value} 条` : "当前暂无待处理反馈",
}));
const auditOverviewItems = computed(() => [
  {
    key: "song",
    label: "待审核歌曲",
    value: auditOverview.value.pendingSongCount,
    meta: auditOverview.value.pendingSongCount ? "建议优先处理上传内容" : "当前没有待审核歌曲",
    path: "/songs",
    theme: "audit-song",
  },
  {
    key: "songList",
    label: "待审核歌单",
    value: auditOverview.value.pendingSongListCount,
    meta: auditOverview.value.pendingSongListCount ? "歌单资料等待确认" : "歌单审核已清空",
    path: "/SongList",
    theme: "audit-list",
  },
  {
    key: "feedback",
    label: "待处理反馈",
    value: auditOverview.value.pendingFeedbackCount,
    meta: auditOverview.value.pendingFeedbackCount ? "用户反馈仍待回复" : "反馈队列很干净",
    path: "/feedback-manage",
    theme: "audit-feedback",
  },
]);
const contentHealthItems = computed(() => [
  {
    key: "lyric",
    label: "缺歌词歌曲",
    value: contentHealth.value.missingLyricsCount,
    tip: "空歌词或占位歌词",
    path: "/songs",
    query: { lyricStatus: "health_missing" },
  },
  {
    key: "audio",
    label: "缺音频歌曲",
    value: contentHealth.value.missingAudioCount,
    tip: "没有可播放音频地址",
    path: "/songs",
    query: { resourceStatus: "missing_audio" },
  },
  {
    key: "singerIntro",
    label: "缺简介歌手",
    value: contentHealth.value.missingSingerIntroCount,
    tip: "歌手资料仍不完整",
    path: "/singer",
    query: { introStatus: "missing" },
  },
  {
    key: "songListCover",
    label: "默认封面歌单",
    value: contentHealth.value.defaultSongListCoverCount,
    tip: "仍在使用默认歌单封面",
    path: "/SongList",
    query: { coverStatus: "default" },
  },
]);
const distributionColorMap = computed<Record<string, string>>(() => {
  if (userDistributionMode.value === "sex") {
    return {
      男: "#1F6BFF",
      女: "#FF6B8B",
      保密: "#14B97B",
      未知: "#CBD6E8",
    };
  }
  return {
    "18岁以下": "#FFB648",
    "18-30岁": "#1F6BFF",
    "30-40岁": "#14B97B",
    "40-50岁": "#FF6B8B",
    "50-60岁": "#8B5CF6",
    "60岁以上": "#29D2E4",
    未知: "#CBD6E8",
  };
});
const userDistributionLegend = computed(() =>
  userDistributionRaw.value.map((item) => ({
    ...item,
    color: distributionColorMap.value[item.name] || "#CBD6E8",
  }))
);
const userDistributionNote = computed(() => {
  const topItem = [...userDistributionRaw.value].sort((a, b) => b.value - a.value)[0];
  if (!topItem || topItem.value <= 0) {
    return {
      title: "暂无分布数据",
    };
  }
  return {
    title: `${topItem.name}占比最高`,
  };
});

function normalizeText(value: unknown) {
  return String(value ?? "").trim();
}

function truncateText(value: unknown, max = 24) {
  const normalized = normalizeText(value);
  if (!normalized) {
    return "";
  }
  return normalized.length > max ? `${normalized.slice(0, max)}...` : normalized;
}

function stripLyricTimestamp(value: string) {
  return value.replace(/\[[0-9:.]+\]/g, "").trim();
}

function isMissingLyricValue(value: unknown) {
  const normalized = normalizeText(value);
  if (!normalized) {
    return true;
  }
  if (/纯音乐|轻音乐|钢琴曲|请欣赏/i.test(normalized)) {
    return false;
  }
  if (normalized.includes("暂无歌词")) {
    return true;
  }
  const stripped = stripLyricTimestamp(normalized).replace(/\s+/g, "");
  if (!stripped) {
    return true;
  }
  return /^[?？]+$/.test(stripped);
}

function isDefaultSongListCoverValue(value: unknown) {
  const normalized = normalizeText(value);
  return !normalized || normalized === "/img/songListPic/123.jpg";
}

// --- API 调用与图表初始化函数 ---

function getChartInstance(domId: (typeof chartIds)[number]) {
  const chartDom = document.getElementById(domId) as HTMLDivElement | null;
  if (!chartDom) {
    return null;
  }
  return echarts.getInstanceByDom(chartDom) || echarts.init(chartDom);
}

function resizeDashboardCharts() {
  chartIds.forEach((domId) => {
    const chartDom = document.getElementById(domId) as HTMLDivElement | null;
    if (!chartDom) {
      return;
    }
    echarts.getInstanceByDom(chartDom)?.resize();
  });
}

function disposeDashboardCharts() {
  chartIds.forEach((domId) => {
    const chartDom = document.getElementById(domId) as HTMLDivElement | null;
    if (!chartDom) {
      return;
    }
    echarts.getInstanceByDom(chartDom)?.dispose();
  });
}

// 1. 获取核心统计数据
async function getKpiCounts() {
  try {
    // 假设 HttpManager.getHomeCount() 返回 { userCount: 100, songCount: 500, ... }
    const res = (await HttpManager.getHomeCount()) as ResponseBody;
    if (res.success) {
      userCount.value = res.data.userCount;
      songCount.value = res.data.songCount;
      singerCount.value = res.data.singerCount;
      songListCount.value = res.data.songListCount;
      pendingFeedbackCount.value = res.data.pendingFeedbackCount || 0;
    }
  } catch (error) {
    void error;
  }
}

async function getAuditOverview() {
  try {
    const [songRes, songListRes, feedbackRes] = await Promise.all([
      HttpManager.getAllSong(),
      HttpManager.getSongList(),
      HttpManager.getAllFeedback(),
    ]);

    const songs = Array.isArray((songRes as ResponseBody)?.data) ? (songRes as ResponseBody).data : [];
    const songLists = Array.isArray((songListRes as ResponseBody)?.data) ? (songListRes as ResponseBody).data : [];
    const feedbackList = Array.isArray((feedbackRes as ResponseBody)?.data) ? (feedbackRes as ResponseBody).data : [];

    const pendingSongs = songs.filter((item) => Number(item.type) === 3);
    const pendingSongLists = songLists.filter((item) => Number(item.type) === 2);
    const pendingFeedback = feedbackList.filter((item) => Number(item.status) !== 1);

    auditOverview.value = {
      pendingSongCount: pendingSongs.length,
      pendingSongListCount: pendingSongLists.length,
      pendingFeedbackCount: pendingFeedback.length,
    };
    pendingFeedbackCount.value = pendingFeedback.length;

    auditPendingPreview.value = [
      ...pendingSongs.map((item) => ({
        key: `song-${item.id}`,
        badge: "歌曲",
        badgeClass: "song",
        title: truncateText(item.name || `歌曲 #${item.id}`, 22),
        subtitle: `ID ${item.id}${normalizeText(item.introduction) ? ` · ${truncateText(item.introduction, 18)}` : ""}`,
        path: "/songs",
      })),
      ...pendingSongLists.map((item) => ({
        key: `songList-${item.id}`,
        badge: "歌单",
        badgeClass: "song-list",
        title: truncateText(item.title || `歌单 #${item.id}`, 22),
        subtitle: truncateText(normalizeText(item.style) || "歌单资料待补充", 20),
        path: "/SongList",
      })),
      ...pendingFeedback.map((item) => ({
        key: `feedback-${item.id}`,
        badge: "反馈",
        badgeClass: "feedback",
        title: truncateText(normalizeText(item.title) || normalizeText(item.content) || `反馈 #${item.id}`, 22),
        subtitle: truncateText(
          [normalizeText(item.username) || "匿名用户", normalizeText(item.contact)]
            .filter(Boolean)
            .join(" · "),
          22
        ),
        path: "/feedback-manage",
      })),
    ];
  } catch (error) {
    void error;
    try {
      const res = (await HttpManager.getAuditOverview()) as ResponseBody;
      if (res.success) {
        auditOverview.value = {
          pendingSongCount: res.data.pendingSongCount || 0,
          pendingSongListCount: res.data.pendingSongListCount || 0,
          pendingFeedbackCount: res.data.pendingFeedbackCount || 0,
        };
        pendingFeedbackCount.value = res.data.pendingFeedbackCount || 0;
      }
    } catch (fallbackError) {
      void fallbackError;
    }
  }
}

async function getContentHealth() {
  try {
    const [songRes, singerRes, songListRes] = await Promise.all([
      HttpManager.getAllSong(),
      HttpManager.getAllSinger(),
      HttpManager.getSongList(),
    ]);

    const songs = Array.isArray((songRes as ResponseBody)?.data) ? (songRes as ResponseBody).data : [];
    const singers = Array.isArray((singerRes as ResponseBody)?.data) ? (singerRes as ResponseBody).data : [];
    const songLists = Array.isArray((songListRes as ResponseBody)?.data) ? (songListRes as ResponseBody).data : [];

    contentHealth.value = {
      missingLyricsCount: songs.filter((item) => isMissingLyricValue(item.lyric)).length,
      missingAudioCount: songs.filter((item) => !normalizeText(item.url)).length,
      missingSingerIntroCount: singers.filter((item) => !normalizeText(item.introduction)).length,
      defaultSongListCoverCount: songLists.filter((item) => isDefaultSongListCoverValue(item.pic)).length,
    };
  } catch (error) {
    void error;
    try {
      const res = (await HttpManager.getContentHealth()) as ResponseBody;
      if (res.success) {
        contentHealth.value = {
          missingLyricsCount: res.data.missingLyricsCount || 0,
          missingAudioCount: res.data.missingAudioCount || 0,
          missingSingerIntroCount: res.data.missingSingerIntroCount || 0,
          defaultSongListCoverCount: res.data.defaultSongListCoverCount || 0,
        };
      }
    } catch (fallbackError) {
      void fallbackError;
    }
  }
}

// 2. 初始化用户活跃趋势图
async function initUserActivityChart() {
  try {
    // 假设返回 [{ date: '2025-11-01', count: 50 }, ...]
    const res = (await HttpManager.getRecentUserActivity(userActivityRange.value)) as ResponseBody;
    const chart = getChartInstance("userActivityChart");
    if (!chart) {
      return;
    }

    if (res.success && res.data.length > 0) {
      const dates = res.data.map(item => item.date);
      const counts = res.data.map(item => item.count);

      userActivityOption.xAxis.data = dates;
      userActivityOption.series[0].data = counts;
      chart.setOption(userActivityOption, true);
    } else {
      userActivityOption.xAxis.data = [];
      userActivityOption.series[0].data = [];
      chart.setOption(userActivityOption, true);
    }
  } catch (error) {
    void error;
  }
}

// 3. 初始化 Top 10 歌曲图表
async function initTopSongsChart() {
  try {
    const res = (await HttpManager.getTop10Songs()) as ResponseBody;
    topSongs.value = res.success && Array.isArray(res.data) ? res.data : [];
  } catch (error) {
    void error;
    topSongs.value = [];
  }
}

// 4. 初始化新增内容趋势图
async function initNewContentTrendChart() {
  try {
    const res = (await HttpManager.getNewContentTrend(newContentTrendRange.value)) as ResponseBody;
    const chart = getChartInstance("newContentTrendChart");
    if (!chart) {
      return;
    }

    if (res.success) {
      newContentTrendOption.xAxis.data = res.data.dates;
      newContentTrendOption.series[0].data = res.data.newUser;
      newContentTrendOption.series[1].data = res.data.newSong;
      newContentTrendOption.series[2].data = res.data.newSinger || new Array(res.data.dates.length).fill(0);
      newContentTrendOption.series[3].data = res.data.newSongList || new Array(res.data.dates.length).fill(0);
      chart.setOption(newContentTrendOption, true);
    } else {
      newContentTrendOption.xAxis.data = [];
      newContentTrendOption.series[0].data = [];
      newContentTrendOption.series[1].data = [];
      newContentTrendOption.series[2].data = [];
      newContentTrendOption.series[3].data = [];
      chart.setOption(newContentTrendOption, true);
    }
  } catch (error) {
    void error;
  }
}

// 5. 初始化用户年龄/性别分布图
async function initUserDistributionChart() {
  await nextTick();
  const chart = getChartInstance("userDistributionChart");
  if (!chart) {
    return;
  }

  try {
    const isSexMode = userDistributionMode.value === "sex";
    const res = (await (isSexMode ? HttpManager.getUserSexDistribution() : HttpManager.getUserAgeDistribution())) as ResponseBody;

    const data = res.success && Array.isArray(res.data) ? res.data : [];
    const pieData = data.filter((item) => Number(item.value) > 0);
    const names = pieData.map(item => item.name);
    const colors = pieData.map((item) => distributionColorMap.value[item.name] || "#CBD6E8");
    userDistributionRaw.value = data;

    userDistributionOption.legend.data = names;
    userDistributionOption.series[0].name = isSexMode ? "用户性别" : "用户年龄";
    userDistributionOption.series[0].data = pieData;
    userDistributionOption.series[0].color = colors;
    chart.setOption(userDistributionOption, true);
  } catch (error) {
    void error;
    userDistributionOption.legend.data = [];
    userDistributionOption.series[0].data = [];
    userDistributionOption.series[0].color = [];
    userDistributionRaw.value = [];
    chart.setOption(userDistributionOption, true);
  }
}

// 6. 初始化最近 7 天反馈趋势图
async function initRecentFeedbackTrendChart() {
  try {
    const res = (await HttpManager.getRecentFeedbackTrend(feedbackTrendRange.value)) as ResponseBody;
    const chart = getChartInstance("feedbackTrendChart");
    if (!chart) {
      return;
    }

    if (res.success && res.data.length > 0) {
      feedbackTrendOption.xAxis.data = res.data.map(item => item.date);
      feedbackTrendOption.series[0].data = res.data.map(item => item.count);
      chart.setOption(feedbackTrendOption, true);
    } else {
      feedbackTrendOption.xAxis.data = [];
      feedbackTrendOption.series[0].data = [];
      chart.setOption(feedbackTrendOption, true);
    }
  } catch (error) {
    void error;
  }
}


// --- 生命周期钩子 ---
const refreshDashboard = () => {
  getKpiCounts();
  getAuditOverview();
  getContentHealth();
  initUserActivityChart();
  initTopSongsChart();
  initNewContentTrendChart();
  initUserDistributionChart();
  initRecentFeedbackTrendChart();
};

onMounted(() => {
  refreshDashboard();
  window.addEventListener("resize", resizeDashboardCharts);
});

onActivated(() => {
  refreshDashboard();
  nextTick(() => resizeDashboardCharts());
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeDashboardCharts);
  disposeDashboardCharts();
});

function goToModule(path: string, query?: Record<string, any>) {
  if (!path) {
    return;
  }
  const currentRoute = router.currentRoute.value;
  const nextQuery = query || {};
  const samePath = currentRoute.path === path;
  const sameQuery = JSON.stringify(currentRoute.query) === JSON.stringify(nextQuery);
  if (samePath && sameQuery) {
    return;
  }
  router.push({ path, query: nextQuery });
}

function formatPlayCount(value: number) {
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}w`;
  }
  return `${value}`;
}

function getSongStatusLabel(type: number) {
  switch (type) {
    case 1:
      return "启用";
    case 2:
      return "隐藏";
    case 3:
      return "审核中";
    case 4:
      return "禁用";
    default:
      return "未标记";
  }
}

function getSongStatusTagType(type: number) {
  switch (type) {
    case 1:
      return "success";
    case 2:
      return "info";
    case 3:
      return "warning";
    case 4:
      return "danger";
    default:
      return "info";
  }
}

function resolveSongDisplay(songName: unknown, singerName: unknown) {
  const normalizedSongName = normalizeText(songName);
  const normalizedSingerName = normalizeText(singerName);
  const singerUnknown = !normalizedSingerName || normalizedSingerName === "未知歌手";

  if (singerUnknown) {
    const matched = normalizedSongName.match(/^(.+?)[-—](.+)$/);
    if (matched) {
      return {
        displaySongName: matched[2].trim() || normalizedSongName || "未命名歌曲",
        displaySingerName: matched[1].trim() || "歌手待补",
      };
    }
  }

  return {
    displaySongName: normalizedSongName || "未命名歌曲",
    displaySingerName: normalizedSingerName || "歌手待补",
  };
}

</script>

<style scoped>
/* --- 布局和容器样式 --- */
.admin-dashboard {
  padding: 0;
}

.page-head {
  margin-bottom: 22px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: var(--admin-text);
}

.page-subtitle {
  margin: 8px 0 0;
  color: var(--admin-text-tertiary);
  line-height: 1.7;
}

.kpi-row {
  margin-bottom: 20px !important;
}

.chart-row {
  margin-top: 20px;
}

.insight-row {
  margin-bottom: 20px !important;
}

/* --- KPI 卡片样式 --- */
.kpi-card {
  border: none;
  border-radius: 26px;
  overflow: hidden;
  box-shadow: 0 16px 34px rgba(17, 37, 70, 0.08);
}

.card-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 132px;
  padding: 18px 22px;
  color: white;
}

.card-content.is-link {
  cursor: pointer;
  transition: transform 0.22s ease, filter 0.22s ease, box-shadow 0.22s ease;
}

.card-content.is-link:hover {
  transform: translateY(-2px);
  filter: saturate(1.03);
}

.card-content.is-link:focus-visible {
  outline: 3px solid rgba(255, 255, 255, 0.42);
  outline-offset: -6px;
}

.card-left {
  display: flex;
  width: 64px;
  height: 64px;
  align-items: center;
  justify-content: center;
  border-radius: 22px;
  font-size: 2rem;
  background: rgba(255, 255, 255, 0.14);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.card-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding-left: 18px;
}

.card-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
  font-weight: 700;
}

.card-num {
  font-size: 40px;
  font-weight: 800;
  line-height: 1;
}

.card-meta {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  font-weight: 700;
}

/* KPI 主题色 */
.user-theme { background: linear-gradient(135deg, #2d7dff 0%, #1f6bff 100%); }
.song-theme { background: linear-gradient(135deg, #1ed7c4 0%, #14b97b 100%); }
.singer-theme { background: linear-gradient(135deg, #ffb648 0%, #f59e0b 100%); }
.list-theme { background: linear-gradient(135deg, #ff7b92 0%, #f35b66 100%); }
.feedback-theme { background: linear-gradient(135deg, #9176ff 0%, #6c63ff 100%); }

.chart-head,
.feedback-chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0;
}

.chart-title-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.panel-caption {
  margin: 0;
  color: var(--admin-text-tertiary);
  font-size: 13px;
  line-height: 1.7;
}

.song-monitor-entry {
  min-width: 126px;
  height: 38px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid rgba(214, 226, 243, 0.96);
  color: #49617f;
  font-weight: 700;
  background: linear-gradient(135deg, rgba(248, 251, 255, 0.98) 0%, rgba(241, 247, 255, 0.95) 100%);
  box-shadow: 0 8px 20px rgba(17, 37, 70, 0.06);
  transition: transform 0.2s ease, border-color 0.2s ease, color 0.2s ease, background 0.2s ease,
    box-shadow 0.2s ease;
}

.song-monitor-entry:hover,
.song-monitor-entry:focus-visible {
  transform: translateY(-1px);
  border-color: rgba(177, 205, 243, 0.98);
  color: #1f6bff;
  background: linear-gradient(135deg, rgba(246, 250, 255, 1) 0%, rgba(234, 243, 255, 0.98) 100%);
  box-shadow: 0 12px 24px rgba(31, 107, 255, 0.1);
}

/* --- 图表容器样式 --- */
h3 {
  margin: 0;
  text-align: left;
  font-size: 28px;
  font-weight: 800;
  color: var(--admin-text);
}

.cav-info {
  border: 1px solid rgba(226, 234, 243, 0.92);
  border-radius: 28px;
  overflow: hidden;
  height: 420px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 34px rgba(17, 37, 70, 0.08);
}

.cav-info :deep(.el-card__body) {
  height: 100%;
  padding: 26px 28px 20px;
}

.insight-card {
  height: auto;
  min-height: 560px;
}

.insight-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  min-height: 560px;
}

.chart-box {
  /* 为 ECharts 容器设置高度 */
  height: 338px; 
  width: 100%;
}

.audit-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.audit-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  min-height: 138px;
  padding: 16px 18px;
  border: none;
  border-radius: 22px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.22s ease, filter 0.22s ease, box-shadow 0.22s ease;
  color: white;
}

.audit-item:hover {
  transform: translateY(-2px);
  filter: saturate(1.03);
}

.audit-song {
  background: linear-gradient(135deg, #1ed7c4 0%, #14b97b 100%);
}

.audit-list {
  background: linear-gradient(135deg, #ff7b92 0%, #f35b66 100%);
}

.audit-feedback {
  background: linear-gradient(135deg, #9176ff 0%, #6c63ff 100%);
}

.audit-label {
  font-size: 14px;
  font-weight: 700;
  opacity: 0.9;
}

.audit-value {
  font-size: 38px;
  font-weight: 800;
  line-height: 1;
}

.audit-meta {
  font-size: 12px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.5;
}

.audit-preview {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid rgba(226, 234, 243, 0.9);
  flex: 1;
}

.audit-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--admin-text-secondary);
  font-size: 13px;
  font-weight: 800;
}

.audit-preview-count {
  padding: 5px 10px;
  border-radius: 999px;
  background: #eef4ff;
  color: #1f6bff;
  font-size: 12px;
  line-height: 1;
  text-align: center;
}

.audit-preview-scroll {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 260px;
  overflow-y: auto;
  padding-right: 6px;
}

.audit-preview-scroll::-webkit-scrollbar {
  width: 8px;
}

.audit-preview-scroll::-webkit-scrollbar-track {
  border-radius: 999px;
  background: #eef3fb;
}

.audit-preview-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #c3d3ef;
}

.audit-preview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 11px 14px;
  border: 1px solid #e8eef8;
  border-radius: 16px;
  background: #f9fbff;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.audit-preview-item:hover {
  transform: translateY(-1px);
  border-color: #d7e3f7;
  background: #f4f8ff;
}

.audit-preview-badge {
  flex-shrink: 0;
  min-width: 40px;
  padding: 5px 10px;
  border-radius: 999px;
  color: white;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
}

.audit-preview-badge.song {
  background: linear-gradient(135deg, #1ed7c4 0%, #14b97b 100%);
}

.audit-preview-badge.song-list {
  background: linear-gradient(135deg, #ff7b92 0%, #f35b66 100%);
}

.audit-preview-badge.feedback {
  background: linear-gradient(135deg, #9176ff 0%, #6c63ff 100%);
}

.audit-preview-copy {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.audit-preview-copy strong {
  color: var(--admin-text-secondary);
  font-size: 13px;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-preview-copy span {
  color: var(--admin-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-preview-action {
  flex-shrink: 0;
  color: #1f6bff;
  font-size: 12px;
  font-weight: 800;
}

.audit-preview-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 54px;
  border-radius: 16px;
  border: 1px dashed #dbe6f5;
  color: var(--admin-text-tertiary);
  font-size: 13px;
  font-weight: 700;
  background: #fbfdff;
}

.health-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 20px;
}

.health-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 20px;
  background: #f7faff;
  border: 1px solid #e8eef8;
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.health-row.clickable:hover {
  transform: translateY(-1px);
  border-color: #d7e3f7;
  background: #f3f8ff;
}

.health-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.health-side {
  display: flex;
  align-items: center;
  gap: 14px;
}

.health-name {
  color: var(--admin-text-secondary);
  font-size: 14px;
  font-weight: 800;
}

.health-tip {
  color: var(--admin-text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

.health-value {
  min-width: 48px;
  text-align: right;
  color: var(--admin-text);
  font-size: 28px;
  font-weight: 800;
}

.health-value.warning {
  color: #f59e0b;
}

.health-action {
  color: #1f6bff;
  font-size: 12px;
  font-weight: 800;
}

.top-song-monitor-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
  max-height: 338px;
  overflow-y: auto;
  padding-right: 6px;
}

.top-song-monitor-list::-webkit-scrollbar {
  width: 8px;
}

.top-song-monitor-list::-webkit-scrollbar-track {
  border-radius: 999px;
  background: #eef3fb;
}

.top-song-monitor-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #c3d3ef;
}

.top-song-monitor-item {
  display: grid;
  grid-template-columns: 34px 64px minmax(0, 1.2fr) minmax(168px, 0.9fr) 86px;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #e8eef8;
  border-radius: 20px;
  background: #f9fbff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.top-song-monitor-item:hover {
  transform: translateY(-1px);
  border-color: #d7e3f7;
  background: #f4f8ff;
  box-shadow: 0 10px 24px rgba(17, 37, 70, 0.05);
}

.top-song-monitor-rank {
  color: var(--admin-text-tertiary);
  font-size: 18px;
  font-weight: 800;
  text-align: center;
}

.top-song-monitor-cover,
.top-song-monitor-cover-fallback {
  width: 64px;
  height: 64px;
  border-radius: 18px;
}

.top-song-monitor-cover {
  overflow: hidden;
  box-shadow: 0 10px 24px rgba(17, 37, 70, 0.08);
}

.top-song-monitor-cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #90a4c6;
  font-size: 20px;
  background: linear-gradient(135deg, #eef4ff 0%, #f6f9ff 100%);
}

.top-song-monitor-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.top-song-monitor-copy strong {
  color: var(--admin-text-secondary);
  font-size: 15px;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-song-monitor-copy span {
  color: var(--admin-text-tertiary);
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-song-monitor-copy p {
  margin: 0;
  color: var(--admin-text-tertiary);
  font-size: 12px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-song-monitor-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.top-song-monitor-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  color: var(--admin-text-tertiary);
}

.top-song-monitor-side strong {
  color: var(--admin-text-secondary);
  font-size: 22px;
  line-height: 1;
}

.top-song-monitor-side span,
.top-song-monitor-side em {
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.top-song-monitor-side em {
  color: #1f6bff;
}

.distribution-layout {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr) 180px;
  align-items: center;
  gap: 16px;
  height: 338px;
}

.distribution-legend {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--admin-text-secondary);
  font-weight: 700;
}

.legend-row i {
  display: block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.distribution-chart-box {
  width: 100%;
  height: 100%;
}

.distribution-note {
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: var(--admin-text-tertiary);
  line-height: 1.7;
}

.distribution-note strong {
  color: var(--admin-text-secondary);
  font-size: 14px;
}

@media (max-width: 1200px) {
  .insight-card {
    min-height: auto;
  }

  .insight-card :deep(.el-card__body) {
    min-height: auto;
  }

  .audit-grid {
    grid-template-columns: 1fr;
  }

  .distribution-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .distribution-legend {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .distribution-chart-box {
    height: 260px;
  }

  .top-song-monitor-item {
    grid-template-columns: 30px 56px minmax(0, 1fr);
    align-items: flex-start;
  }

  .top-song-monitor-tags,
  .top-song-monitor-side {
    grid-column: 3;
  }

  .top-song-monitor-side {
    align-items: flex-start;
  }
}
</style>

