package com.example.music.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.music.common.R;
import com.example.music.mapper.SingerMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.model.domain.Singer;
import com.example.music.model.domain.Song;
import com.example.music.model.request.AssistantChatRequest;
import com.example.music.model.request.AssistantHistoryMessage;
import com.example.music.model.response.AssistantChatResponse;
import com.example.music.model.response.AssistantSongPayload;
import com.example.music.service.AssistantService;
import com.example.music.service.RecommendService;
import com.example.music.service.SongService;
import lombok.Data;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

@Service
public class AssistantServiceImpl implements AssistantService {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("^```(?:json)?\\s*|\\s*```$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_WORD_SPLIT_PATTERN = Pattern.compile("[\\s,，。.!！?？、；;：:'\"“”‘’()（）【】\\[\\]<>《》/\\\\|]+");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern MISSING_MODULE_PATTERN = Pattern.compile("ModuleNotFoundError:\\s+No module named ['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final String ASSISTANT_SESSION_STATE_KEY = "assistantSessionState";
    private static final long RECOMMENDATION_CONTEXT_TTL_MS = 30L * 60L * 1000L;
    private static final int DEFAULT_LOCAL_CANDIDATE_LIMIT = 6;
    private static final int DEFAULT_RECOMMENDATION_CANDIDATE_LIMIT = 12;
    private static final int UNLIMITED_LOCAL_CANDIDATE_LIMIT = 0;
    private static final int BROWSE_REMOTE_SUGGESTION_LIMIT = 8;
    private static final List<String> MUSIC_TRIGGER_WORDS;
    private static final List<String> PLAY_TRIGGER_WORDS;
    private static final List<String> LOGOUT_TRIGGER_WORDS;
    private static final List<String> STOP_WORDS;
    private static final Set<String> AFFIRMATIVE_CONFIRMATION_WORDS;
    private static final Set<String> NEGATIVE_CONFIRMATION_WORDS;
    private static final Set<String> GENERIC_ACTION_TARGETS;

    static {
        List<String> musicTriggers = new ArrayList<>();
        Collections.addAll(musicTriggers,
                "播放", "放一首", "放首", "听", "来一首", "来首", "点一首", "点首", "歌曲", "音乐", "歌", "搜歌", "找歌");
        MUSIC_TRIGGER_WORDS = Collections.unmodifiableList(musicTriggers);

        List<String> playTriggers = new ArrayList<>();
        Collections.addAll(playTriggers,
                "播放", "放一首", "放首", "听", "来一首", "来首", "点一首", "点首", "直接播", "直接播放");
        PLAY_TRIGGER_WORDS = Collections.unmodifiableList(playTriggers);

        List<String> logoutTriggers = new ArrayList<>();
        Collections.addAll(logoutTriggers,
                "退出登录", "退出账号", "退出账户", "登出", "注销登录", "注销账号", "注销账户", "切换账号");
        LOGOUT_TRIGGER_WORDS = Collections.unmodifiableList(logoutTriggers);

        List<String> stopWords = new ArrayList<>();
        Collections.addAll(stopWords,
                "帮我", "请", "一下", "一下子", "我想听", "想听", "给我", "可以", "能不能", "麻烦",
                "直接", "前端", "帮忙", "我要", "我想", "听听", "播一下", "播放一下", "来点", "一首",
                "首歌", "这首歌", "这个歌", "歌曲", "音乐", "歌", "一下吧");
        STOP_WORDS = Collections.unmodifiableList(stopWords);

        Set<String> affirmativeWords = new HashSet<>();
        Collections.addAll(affirmativeWords,
                "是", "是的", "对", "对的", "对没错", "没错", "好", "好的", "好啊", "行", "行啊", "可以",
                "确认", "确认执行", "继续", "继续吧", "执行", "就这个", "按这个", "按这个执行", "就按这个", "就按这个执行", "嗯", "嗯嗯");
        AFFIRMATIVE_CONFIRMATION_WORDS = Collections.unmodifiableSet(affirmativeWords);

        Set<String> negativeWords = new HashSet<>();
        Collections.addAll(negativeWords,
                "不", "不是", "不用", "不用了", "不要", "不要了", "先别", "先别执行", "取消", "取消吧",
                "算了", "算了吧", "别了", "不对", "不是这个", "先不", "不用执行", "别执行");
        NEGATIVE_CONFIRMATION_WORDS = Collections.unmodifiableSet(negativeWords);

        Set<String> genericTargets = new HashSet<>();
        Collections.addAll(genericTargets,
                "这个", "那个", "这个歌单", "那个歌单", "这张", "那张", "这首", "那首",
                "它", "当前", "现在这个", "现在那个", "这个页面", "那个页面", "当前歌单", "本歌单",
                "当前歌手", "这个歌手", "那个歌手");
        GENERIC_ACTION_TARGETS = Collections.unmodifiableSet(genericTargets);
    }

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private SingerMapper singerMapper;

    @Autowired
    private SongService songService;

    @Autowired
    private RecommendService recommendService;

    @Value("${assistant.deepseek.api-key:${DEEPSEEK_API_KEY:}}")
    private String deepSeekApiKey;

    @Value("${assistant.deepseek.base-url:https://api.deepseek.com}")
    private String deepSeekBaseUrl;

    @Value("${assistant.deepseek.model:deepseek-chat}")
    private String deepSeekModel;

    @Value("${assistant.import.enabled:true}")
    private boolean importEnabled;

    @Value("${assistant.import.python-executable:python}")
    private String importPythonExecutable;

    @Value("${assistant.import.script-path:}")
    private String importScriptPath;

    @Value("${assistant.import.sources:gequbao,yyfang,kuwo,jgwav}")
    private String importSources;

    @Value("${assistant.import.limit:6}")
    private int importLimit;

    @Value("${assistant.import.publish-type:1}")
    private int importPublishType;

    @Value("${assistant.import.command-timeout-ms:180000}")
    private long importCommandTimeoutMs;

    private final ExecutorService importAuditExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "assistant-import-audit");
        thread.setDaemon(true);
        return thread;
    });

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    public R chat(AssistantChatRequest request, HttpSession session) {
        String userMessage = sanitizeMessage(request == null ? null : request.getMessage());
        if (userMessage.isEmpty()) {
            return R.error("请输入想和助手说的话");
        }

        List<AssistantHistoryMessage> history = sanitizeHistory(request == null ? null : request.getHistory());
        boolean allowImport = request == null || request.getAllowImport() == null || request.getAllowImport();
        Integer currentUserId = extractCurrentUserId(session);
        AssistantSessionState sessionState = loadAssistantSessionState(session);

        PendingConfirmationResolution pendingResolution = resolvePendingConfirmation(userMessage, sessionState);
        if (pendingResolution.isCancelled()) {
            clearPendingConfirmation(sessionState);
            persistAssistantSessionState(session, sessionState);
            return R.success("助手响应成功", buildCancelledConfirmationResponse());
        }

        IntentPlan plan = pendingResolution.isConfirmed()
                ? pendingResolution.getPlan()
                : resolveIntentPlan(userMessage, history, sessionState);
        if (pendingResolution.isConfirmed()) {
            clearPendingConfirmation(sessionState);
        }
        if (!pendingResolution.isConfirmed()) {
            clearPendingConfirmation(sessionState);
            ConfirmationProposal confirmationProposal = buildConfirmationProposal(plan, userMessage);
            if (confirmationProposal != null) {
                rememberPendingConfirmation(sessionState, confirmationProposal);
                persistAssistantSessionState(session, sessionState);
                return R.success("助手响应成功", buildConfirmationResponse(confirmationProposal));
            }
        }

        AssistantExecution execution = executePlan(plan, allowImport);
        AssistantChatResponse response = buildChatResponse(plan, execution, history, userMessage, currentUserId, sessionState);
        persistAssistantSessionState(session, sessionState);
        return R.success("助手响应成功", response);
    }

    private AssistantChatResponse buildChatResponse(IntentPlan plan,
                                                    AssistantExecution execution,
                                                    List<AssistantHistoryMessage> history,
                                                    String userMessage,
                                                    Integer currentUserId,
                                                    AssistantSessionState sessionState) {
        AssistantChatResponse response = new AssistantChatResponse();
        response.setAction(execution.getAction());
        response.setActionTarget(execution.getActionTarget());
        response.setSearchKeyword(execution.getSearchKeyword());
        response.setUsedImport(execution.getUsedImport());
        response.setNeedsConfirmation(false);
        response.setSong(toSongPayload(execution.getSong()));
        response.setCandidates(execution.getCandidates().stream()
                .map(this::toSongPayload)
                .collect(Collectors.toList()));

        if ("chat".equals(plan.getIntent())) {
            String recommendationPrompt = buildRecommendationPrompt(userMessage, history, sessionState);
            if (!recommendationPrompt.isEmpty()) {
                AssistantExecution recommendationExecution = buildSongRecommendationExecution(recommendationPrompt, userMessage, currentUserId);
                if (!recommendationExecution.getCandidates().isEmpty()) {
                    response.setAction(recommendationExecution.getAction());
                    response.setCandidates(recommendationExecution.getCandidates().stream()
                            .map(this::toSongPayload)
                            .collect(Collectors.toList()));
                    response.setReply(recommendationExecution.getReplyMessage());
                    rememberRecommendationContext(sessionState, recommendationPrompt);
                    return response;
                }
            }
            response.setReply(buildGeneralReply(history, userMessage));
            return response;
        }

        if (isClientActionIntent(execution.getAction())) {
            response.setReply(buildClientActionReply(execution.getAction(), execution.getActionTarget()));
            return response;
        }

        if ("play_song".equals(execution.getAction()) && execution.getSong() != null) {
            String songTitle = extractSongTitle(execution.getSong().getName());
            String singerName = extractSingerName(execution.getSong().getName());
            if (Boolean.TRUE.equals(execution.getUsedImport())) {
                response.setReply(String.format("站内暂时没有这首歌，我已经帮你补充《%s》，现在开始播放%s的版本。", songTitle, singerName));
            } else {
                response.setReply(String.format("已帮你找到《%s》，现在开始播放%s的版本。", songTitle, singerName));
            }
            return response;
        }

        if ("choose_song".equals(execution.getAction()) && !execution.getCandidates().isEmpty()) {
            if (execution.getReplyMessage() != null && !execution.getReplyMessage().isEmpty()) {
                response.setReply(execution.getReplyMessage());
                return response;
            }
            response.setReply(String.format("我先帮你找到了 %d 首相关歌曲，你点下面任意一首就能直接播放。", execution.getCandidates().size()));
            return response;
        }

        if (execution.getErrorMessage() != null && !execution.getErrorMessage().isEmpty()) {
            response.setReply(execution.getErrorMessage());
            return response;
        }

        if ("search_song".equals(plan.getIntent())) {
            response.setReply("我暂时没在站内找到相关歌曲，你可以换个更完整的歌名，或者直接说“播放某某歌曲”。");
            return response;
        }

        response.setReply("我暂时没找到合适的歌曲，你可以再说一次更完整的歌手名和歌名。");
        return response;
    }

    private AssistantExecution buildSongRecommendationExecution(String recommendationPrompt, String userMessage, Integer currentUserId) {
        AssistantExecution execution = new AssistantExecution();
        execution.setAction("none");
        execution.setSearchKeyword("");
        execution.setActionTarget("");
        execution.setUsedImport(false);

        SongRecommendationResult recommendationResult = recommendSongsForScene(recommendationPrompt, currentUserId, DEFAULT_RECOMMENDATION_CANDIDATE_LIMIT);
        if (recommendationResult.getSongs().isEmpty()) {
            execution.setReplyMessage("我先没从站内整理出合适的推荐歌。你可以换个场景再试，比如“推荐几首适合深夜听的歌”。");
            return execution;
        }

        execution.setAction("choose_song");
        execution.setCandidates(recommendationResult.getSongs());
        execution.setReplyMessage(buildRecommendationReply(recommendationResult, userMessage));
        return execution;
    }

    private SongRecommendationResult recommendSongsForScene(String userMessage, Integer currentUserId, int limit) {
        SongRecommendationResult result = new SongRecommendationResult();
        RecommendationCandidatePool candidatePool = loadRecommendationCandidatePool(Math.max(limit * 6, 24), currentUserId);
        result.setPersonalized(candidatePool.isPersonalized());
        if (candidatePool.getSongs().isEmpty()) {
            result.setSongs(Collections.emptyList());
            return result;
        }

        List<Song> selected = selectSongsWithDeepSeek(candidatePool.getSongs(), userMessage, limit);
        if (!selected.isEmpty()) {
            result.setModelSelected(true);
            result.setSongs(selected);
            return result;
        }
        result.setSongs(new ArrayList<>(candidatePool.getSongs().subList(0, Math.min(limit, candidatePool.getSongs().size()))));
        return result;
    }

    private RecommendationCandidatePool loadRecommendationCandidatePool(int limit, Integer currentUserId) {
        RecommendationCandidatePool pool = new RecommendationCandidatePool();
        List<Song> songs = new ArrayList<>();
        if (currentUserId != null && currentUserId > 0) {
            try {
                songs.addAll(recommendService.selectListByRecommend(currentUserId, limit));
            } catch (Exception ignored) {
            }
            songs = deduplicateSongs(songs, limit);
            if (!songs.isEmpty()) {
                pool.setSongs(songs);
                pool.setPersonalized(true);
                return pool;
            }
        }

        try {
            R hotSongsResult = songService.hotSongs(limit);
            Object data = hotSongsResult == null ? null : hotSongsResult.getData();
            if (data instanceof List<?>) {
                for (Object item : (List<?>) data) {
                    if (item instanceof Song) {
                        songs.add((Song) item);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (!songs.isEmpty()) {
            pool.setSongs(deduplicateSongs(songs, limit));
            return pool;
        }

        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", 1);
        queryWrapper.isNotNull("url");
        queryWrapper.ne("url", "");
        queryWrapper.orderByDesc("update_time");
        List<Song> fallbackSongs = songMapper.selectList(queryWrapper);
        if (fallbackSongs == null || fallbackSongs.isEmpty()) {
            pool.setSongs(Collections.emptyList());
            return pool;
        }
        pool.setSongs(deduplicateSongs(fallbackSongs, limit));
        return pool;
    }

    private List<Song> deduplicateSongs(List<Song> songs, int limit) {
        Map<Integer, Song> deduplicated = new LinkedHashMap<>();
        for (Song song : songs) {
            if (song == null || song.getId() == null || deduplicated.containsKey(song.getId())) {
                continue;
            }
            deduplicated.put(song.getId(), song);
            if (deduplicated.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(deduplicated.values());
    }

    private List<Song> selectSongsWithDeepSeek(List<Song> candidatePool, String userMessage, int limit) {
        if (deepSeekApiKey == null || deepSeekApiKey.trim().isEmpty() || candidatePool.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            JSONObject body = new JSONObject();
            body.put("model", deepSeekModel);
            body.put("temperature", 0.3);
            body.put("max_tokens", 500);

            JSONArray messages = new JSONArray();
            messages.add(buildChatMessage("system",
                    "你是站内歌曲推荐助手。请根据用户想听歌的场景，从给定候选歌曲里挑选最多 "
                            + Math.max(limit, 1)
                            + " 首最适合的歌曲。"
                            + "只能从候选列表里选择，不能编造不存在的歌曲。"
                            + "请只输出 JSON，不要输出 markdown。"
                            + "格式固定为 {\"songIds\":[1,2,3],\"reply\":\"一句简短中文推荐语\"}。"));

            JSONArray candidateArray = new JSONArray();
            for (Song song : candidatePool) {
                JSONObject item = new JSONObject();
                item.put("id", song.getId());
                item.put("title", extractSongTitle(song.getName()));
                item.put("singer", extractSingerName(song.getName()));
                item.put("introduction", abbreviateText(song.getIntroduction(), 48));
                candidateArray.add(item);
            }
            messages.add(buildChatMessage("user",
                    "用户需求：" + sanitizeMessage(userMessage) + "\n候选歌曲：" + candidateArray.toJSONString()));
            body.put("messages", messages);

            JSONObject parsed = parseLooseJsonObject(callDeepSeek(body));
            if (parsed == null) {
                return Collections.emptyList();
            }

            JSONArray songIds = parsed.getJSONArray("songIds");
            if (songIds == null || songIds.isEmpty()) {
                return Collections.emptyList();
            }

            Map<Integer, Song> songById = candidatePool.stream()
                    .filter(item -> item != null && item.getId() != null)
                    .collect(Collectors.toMap(Song::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

            List<Song> selected = new ArrayList<>();
            for (int index = 0; index < songIds.size(); index++) {
                Integer songId = songIds.getInteger(index);
                Song song = songById.get(songId);
                if (song == null || selected.contains(song)) {
                    continue;
                }
                selected.add(song);
                if (selected.size() >= limit) {
                    break;
                }
            }
            return selected;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String buildRecommendationThemeLabel(String userMessage) {
        String theme = extractRecommendationTheme(userMessage);
        if (theme.isEmpty()) {
            return "";
        }
        return "在“" + theme + "”这种场景下";
    }

    private String buildRecommendationReply(SongRecommendationResult recommendationResult, String userMessage) {
        String themeLabel = buildRecommendationThemeLabel(userMessage);
        int count = recommendationResult.getSongs().size();
        if (recommendationResult.isModelSelected() && recommendationResult.isPersonalized()) {
            return String.format("我先结合你的站内听歌偏好，给你挑了 %d 首%s适合听的歌，你点下面任意一首就能直接播放。", count, themeLabel);
        }
        if (recommendationResult.isModelSelected()) {
            return String.format("我先从站内曲库里挑了 %d 首%s适合听的歌，你点下面任意一首就能直接播放。", count, themeLabel);
        }
        if (recommendationResult.isPersonalized()) {
            return String.format("我先从你的站内推荐里整理了 %d 首%s可能更对味的歌，你点下面任意一首就能直接播放。", count, themeLabel);
        }
        return String.format("我先从站内热度较高的歌曲里整理了 %d 首%s可能适合你的歌，你点下面任意一首就能直接播放。", count, themeLabel);
    }

    private String extractRecommendationTheme(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        String working = message;
        working = working.replace("推荐几首", " ");
        working = working.replace("推荐一些", " ");
        working = working.replace("推荐几首歌", " ");
        working = working.replace("推荐一些歌", " ");
        working = working.replace("推荐", " ");
        working = working.replace("适合", " ");
        working = working.replace("听的歌", " ");
        working = working.replace("听的歌曲", " ");
        working = working.replace("歌曲", " ");
        working = working.replace("几首歌", " ");
        working = working.replace("几首", " ");
        working = working.replace("歌", " ");
        working = working.replace("吧", " ");
        working = MULTI_SPACE_PATTERN.matcher(working).replaceAll(" ").trim();
        return working;
    }

    private String buildRecommendationPrompt(String userMessage,
                                             List<AssistantHistoryMessage> history,
                                             AssistantSessionState sessionState) {
        if (isSongRecommendationRequest(userMessage)) {
            return userMessage;
        }
        if (!isSongRecommendationFollowUp(userMessage, history, sessionState)) {
            return "";
        }
        String latestRecommendationRequest = findLatestRecommendationRequest(history, sessionState);
        if (latestRecommendationRequest.isEmpty()) {
            return userMessage;
        }
        return latestRecommendationRequest + "；补充要求：" + sanitizeMessage(userMessage);
    }

    private boolean isSongRecommendationFollowUp(String userMessage,
                                                 List<AssistantHistoryMessage> history,
                                                 AssistantSessionState sessionState) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return false;
        }
        if (history == null || history.isEmpty()) {
            return hasRecentRecommendationContext(history, sessionState) && looksLikeRecommendationFollowUpMessage(message);
        }
        if (!hasRecentRecommendationContext(history, sessionState)) {
            return false;
        }
        return looksLikeRecommendationFollowUpMessage(message);
    }

    private boolean hasRecentRecommendationContext(List<AssistantHistoryMessage> history, AssistantSessionState sessionState) {
        if (sessionState != null
                && sessionState.getLastRecommendationAt() > 0
                && !sessionState.getLastRecommendationRequest().isEmpty()
                && System.currentTimeMillis() - sessionState.getLastRecommendationAt() <= RECOMMENDATION_CONTEXT_TTL_MS) {
            return true;
        }
        if (history == null || history.isEmpty()) {
            return false;
        }
        int start = Math.max(0, history.size() - 4);
        for (int index = history.size() - 1; index >= start; index--) {
            AssistantHistoryMessage message = history.get(index);
            if (message == null || message.getContent() == null) {
                continue;
            }
            String content = sanitizeMessage(message.getContent());
            if ("user".equals(message.getRole()) && isSongRecommendationRequest(content)) {
                return true;
            }
            if ("assistant".equals(message.getRole()) && content.contains("适合听的歌")) {
                return true;
            }
        }
        return false;
    }

    private String findLatestRecommendationRequest(List<AssistantHistoryMessage> history, AssistantSessionState sessionState) {
        if (sessionState != null
                && sessionState.getLastRecommendationAt() > 0
                && !sessionState.getLastRecommendationRequest().isEmpty()
                && System.currentTimeMillis() - sessionState.getLastRecommendationAt() <= RECOMMENDATION_CONTEXT_TTL_MS) {
            return sessionState.getLastRecommendationRequest();
        }
        if (history == null || history.isEmpty()) {
            return "";
        }
        for (int index = history.size() - 1; index >= 0; index--) {
            AssistantHistoryMessage message = history.get(index);
            if (message == null || !"user".equals(message.getRole())) {
                continue;
            }
            String content = sanitizeMessage(message.getContent());
            if (isSongRecommendationRequest(content)) {
                return content;
            }
        }
        return "";
    }

    private boolean looksLikeRecommendationFollowUpMessage(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        String normalized = normalizeForCompare(message);
        return containsAnyText(message, "再来", "再来点", "还有吗", "还有没有", "换成", "换点", "换一批", "换一些", "别太", "不要太", "更", "偏")
                || containsAnyNormalized(normalized,
                "安静一点", "更安静", "更轻松", "更治愈", "更伤感", "适合开车", "适合学习", "适合睡前",
                "女生唱", "女声", "男生唱", "男声", "节奏快一点", "节奏慢一点", "不那么伤感", "别那么伤感", "还有别的吗");
    }

    private Integer extractCurrentUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object userId = session.getAttribute("userId");
        if (userId instanceof Integer) {
            return (Integer) userId;
        }
        if (userId instanceof Number) {
            return ((Number) userId).intValue();
        }
        if (userId instanceof String) {
            try {
                return Integer.parseInt(((String) userId).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private AssistantSessionState loadAssistantSessionState(HttpSession session) {
        if (session == null) {
            return new AssistantSessionState();
        }
        Object stored = session.getAttribute(ASSISTANT_SESSION_STATE_KEY);
        if (stored instanceof AssistantSessionState) {
            return (AssistantSessionState) stored;
        }
        AssistantSessionState state = new AssistantSessionState();
        session.setAttribute(ASSISTANT_SESSION_STATE_KEY, state);
        return state;
    }

    private void persistAssistantSessionState(HttpSession session, AssistantSessionState state) {
        if (session == null || state == null) {
            return;
        }
        session.setAttribute(ASSISTANT_SESSION_STATE_KEY, state);
    }

    private PendingConfirmationResolution resolvePendingConfirmation(String userMessage, AssistantSessionState sessionState) {
        PendingConfirmationResolution resolution = new PendingConfirmationResolution();
        if (sessionState == null || sessionState.getPendingConfirmation() == null) {
            return resolution;
        }

        PendingConfirmation pendingConfirmation = sessionState.getPendingConfirmation();
        if (pendingConfirmation.getCreatedAt() <= 0
                || System.currentTimeMillis() - pendingConfirmation.getCreatedAt() > RECOMMENDATION_CONTEXT_TTL_MS) {
            clearPendingConfirmation(sessionState);
            return resolution;
        }

        String normalized = normalizeForCompare(userMessage);
        if (isAffirmativeConfirmationMessage(userMessage, normalized)) {
            IntentPlan plan = new IntentPlan();
            plan.setIntent(pendingConfirmation.getIntent());
            plan.setSearchKeyword(pendingConfirmation.getSearchKeyword());
            plan.setActionTarget(pendingConfirmation.getActionTarget());
            plan.setShouldAutoPlay(pendingConfirmation.isShouldAutoPlay());
            plan.setAllowImportIfMissing(pendingConfirmation.isAllowImportIfMissing());
            plan.setOriginalMessage(pendingConfirmation.getOriginalMessage());
            resolution.setConfirmed(true);
            resolution.setPlan(plan);
            return resolution;
        }
        if (isNegativeConfirmationMessage(normalized)) {
            resolution.setCancelled(true);
            return resolution;
        }
        return resolution;
    }

    private boolean isAffirmativeConfirmationMessage(String message, String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isEmpty()) {
            return false;
        }
        String sanitizedMessage = sanitizeMessage(message);
        return AFFIRMATIVE_CONFIRMATION_WORDS.contains(normalizedMessage)
                || containsAnyText(sanitizedMessage,
                "确认，按这个执行", "确认按这个执行", "确认一下按这个", "就按这个", "就按这个执行", "按这个执行", "照这个执行")
                || containsAnyNormalized(normalizedMessage,
                "确认执行", "确认按这个", "确认按这个执行", "是按这个", "对按这个", "对就是这个", "嗯按这个", "好按这个", "继续执行", "就按这个", "按这个执行");
    }

    private boolean isNegativeConfirmationMessage(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isEmpty()) {
            return false;
        }
        return NEGATIVE_CONFIRMATION_WORDS.contains(normalizedMessage)
                || containsAnyNormalized(normalizedMessage,
                "先别执行", "不是这个", "不用这个", "先不要这个", "换一个吧");
    }

    private void rememberRecommendationContext(AssistantSessionState sessionState, String recommendationPrompt) {
        if (sessionState == null) {
            return;
        }
        String sanitizedPrompt = sanitizeMessage(recommendationPrompt);
        if (sanitizedPrompt.isEmpty()) {
            return;
        }
        sessionState.setLastRecommendationRequest(sanitizedPrompt);
        sessionState.setLastRecommendationAt(System.currentTimeMillis());
    }

    private void clearPendingConfirmation(AssistantSessionState sessionState) {
        if (sessionState == null) {
            return;
        }
        sessionState.setPendingConfirmation(null);
    }

    private void rememberPendingConfirmation(AssistantSessionState sessionState, ConfirmationProposal proposal) {
        if (sessionState == null || proposal == null) {
            return;
        }
        PendingConfirmation pendingConfirmation = new PendingConfirmation();
        pendingConfirmation.setIntent(proposal.getIntent());
        pendingConfirmation.setSearchKeyword(proposal.getSearchKeyword());
        pendingConfirmation.setActionTarget(proposal.getActionTarget());
        pendingConfirmation.setShouldAutoPlay(proposal.isShouldAutoPlay());
        pendingConfirmation.setAllowImportIfMissing(proposal.isAllowImportIfMissing());
        pendingConfirmation.setOriginalMessage(proposal.getOriginalMessage());
        pendingConfirmation.setPrompt(proposal.getPrompt());
        pendingConfirmation.setConfidence(proposal.getConfidence());
        pendingConfirmation.setCreatedAt(System.currentTimeMillis());
        sessionState.setPendingConfirmation(pendingConfirmation);
    }

    private AssistantChatResponse buildConfirmationResponse(ConfirmationProposal proposal) {
        AssistantChatResponse response = new AssistantChatResponse();
        response.setAction("none");
        response.setActionTarget("");
        response.setSearchKeyword("");
        response.setUsedImport(false);
        response.setNeedsConfirmation(true);
        response.setConfidence(proposal.getConfidence());
        response.setConfirmationPrompt(proposal.getPrompt());
        response.setConfirmationAction(proposal.getIntent());
        response.setConfirmationTarget(proposal.getActionTarget());
        response.setReply(proposal.getPrompt());
        response.setCandidates(Collections.emptyList());
        return response;
    }

    private AssistantChatResponse buildCancelledConfirmationResponse() {
        AssistantChatResponse response = new AssistantChatResponse();
        response.setAction("none");
        response.setActionTarget("");
        response.setSearchKeyword("");
        response.setUsedImport(false);
        response.setNeedsConfirmation(false);
        response.setReply("好的，这次我先不执行。你可以直接再告诉我更具体一点的意思。");
        response.setCandidates(Collections.emptyList());
        return response;
    }

    private String abbreviateText(String text, int limit) {
        String sanitized = sanitizeMessage(text);
        if (sanitized.length() <= limit) {
            return sanitized;
        }
        return sanitized.substring(0, Math.max(limit, 0)) + "...";
    }

    private String buildClientActionReply(String action, String actionTarget) {
        if ("logout".equals(action)) {
            return "好的，已经帮你退出登录。";
        }
        if ("navigate_home".equals(action)) {
            return "好的，已经帮你回到首页。";
        }
        if ("navigate_song_sheet".equals(action)) {
            return "好的，已经帮你打开歌单页面。";
        }
        if ("navigate_singer".equals(action)) {
            return "好的，已经帮你打开歌手页面。";
        }
        if ("navigate_rank".equals(action)) {
            return "好的，已经帮你打开排行榜。";
        }
        if ("navigate_collection".equals(action)) {
            return "好的，已经帮你打开收藏。";
        }
        if ("navigate_my_song_sheet".equals(action)) {
            return "好的，已经帮你打开我的歌单。";
        }
        if ("navigate_personal".equals(action)) {
            return "好的，已经帮你打开个人中心。";
        }
        if ("navigate_setting".equals(action)) {
            return "好的，已经帮你打开设置。";
        }
        if ("open_singer_detail".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你打开 %s 的详情页。", actionTarget);
            }
            return "好的，我来帮你打开当前歌手详情。";
        }
        if ("open_playlist_detail".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你打开“%s”歌单。", actionTarget);
            }
            return "好的，我来帮你打开对应的歌单。";
        }
        if ("open_lyric".equals(action)) {
            return "好的，已经帮你打开歌词页。";
        }
        if ("open_song_comment".equals(action)) {
            return "好的，我来帮你打开歌曲评论区。";
        }
        if ("clear_search_history".equals(action)) {
            return "好的，我来帮你清空搜索历史。";
        }
        if ("navigate_collection_playlists".equals(action)) {
            return "好的，我来帮你打开收藏里的歌单页签。";
        }
        if ("favorite_playlist".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你收藏“%s”歌单。", actionTarget);
            }
            return "好的，我来帮你收藏当前歌单。";
        }
        if ("unfavorite_playlist".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你取消收藏“%s”歌单。", actionTarget);
            }
            return "好的，我来帮你取消收藏当前歌单。";
        }
        if ("play_playlist".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你播放“%s”歌单。", actionTarget);
            }
            return "好的，我来帮你播放当前歌单。";
        }
        if ("toggle_dark_mode".equals(action)) {
            return "好的，已经帮你切换界面模式。";
        }
        if ("player_pause".equals(action)) {
            return "好的，已经帮你暂停播放。";
        }
        if ("player_resume".equals(action)) {
            return "好的，已经帮你继续播放。";
        }
        if ("player_next".equals(action)) {
            return "好的，已经帮你切到下一首。";
        }
        if ("player_previous".equals(action)) {
            return "好的，已经帮你切回上一首。";
        }
        if ("favorite_current_song".equals(action)) {
            return "好的，我来帮你收藏当前这首歌。";
        }
        if ("unfavorite_current_song".equals(action)) {
            return "好的，我来帮你取消收藏当前这首歌。";
        }
        if ("add_current_song_to_playlist".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你把当前歌曲加入“%s”歌单。", actionTarget);
            }
            return "好的，我来帮你把当前歌曲加入歌单。";
        }
        if ("create_playlist".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你创建“%s”歌单。", actionTarget);
            }
            return "好的，我来帮你创建歌单。";
        }
        if ("add_song_to_playlist".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你把那首歌加入“%s”歌单。", actionTarget);
            }
            return "好的，我来帮你把歌曲加入歌单。";
        }
        if ("remove_current_song_from_playlist".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你把当前歌曲从“%s”歌单移除。", actionTarget);
            }
            return "好的，我来帮你把当前歌曲从歌单里移除。";
        }
        if ("create_recommended_playlist".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你生成一张“%s”推荐歌单。", actionTarget);
            }
            return "好的，我来帮你生成一张推荐歌单。";
        }
        if ("delete_my_playlist".equals(action)) {
            if (actionTarget != null && !actionTarget.isEmpty()) {
                return String.format("好的，我来帮你删除“%s”歌单。", actionTarget);
            }
            return "好的，我来帮你删除这个歌单。";
        }
        return "好的，我来帮你处理。";
    }

    private ConfirmationProposal buildConfirmationProposal(IntentPlan plan, String userMessage) {
        if (plan == null || plan.getIntent() == null || plan.getIntent().isEmpty()) {
            return null;
        }
        if (!isClientActionIntent(plan.getIntent()) || "logout".equals(plan.getIntent())) {
            return null;
        }
        if (("add_current_song_to_playlist".equals(plan.getIntent())
                || "add_song_to_playlist".equals(plan.getIntent())
                || "create_playlist".equals(plan.getIntent()))
                && (plan.getActionTarget() == null || plan.getActionTarget().isEmpty() || isGenericActionTarget(plan.getActionTarget()))) {
            return null;
        }

        double confidence = estimateActionConfidence(plan, userMessage);
        if (confidence >= 0.72d) {
            return null;
        }

        ConfirmationProposal proposal = new ConfirmationProposal();
        proposal.setIntent(plan.getIntent());
        proposal.setSearchKeyword(plan.getSearchKeyword());
        proposal.setActionTarget(resolveConfirmationActionTarget(plan));
        proposal.setShouldAutoPlay(plan.isShouldAutoPlay());
        proposal.setAllowImportIfMissing(plan.isAllowImportIfMissing());
        proposal.setOriginalMessage(plan.getOriginalMessage());
        proposal.setConfidence(confidence);
        proposal.setPrompt(buildConfirmationPrompt(plan));
        return proposal;
    }

    private double estimateActionConfidence(IntentPlan plan, String userMessage) {
        double confidence = 0.94d;
        String sanitizedMessage = sanitizeMessage(userMessage);
        String normalizedMessage = normalizeForCompare(sanitizedMessage);

        if (sanitizedMessage.length() <= 8) {
            confidence -= 0.08d;
        }
        if (containsAmbiguousReferenceCue(sanitizedMessage, normalizedMessage)) {
            confidence -= 0.32d;
        }
        if (requiresCurrentContext(plan)) {
            confidence -= 0.18d;
        }
        if (isGenericActionTarget(plan.getActionTarget())) {
            confidence -= 0.22d;
        }
        if (containsAnyText(sanitizedMessage, "一下", "帮我弄", "帮我整", "给我弄")) {
            confidence -= 0.06d;
        }
        if ("delete_my_playlist".equals(plan.getIntent())) {
            confidence -= 0.08d;
        }

        if (confidence < 0.35d) {
            return 0.35d;
        }
        if (confidence > 0.98d) {
            return 0.98d;
        }
        return confidence;
    }

    private boolean containsAmbiguousReferenceCue(String message, String normalizedMessage) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        return containsAnyText(message, "这个", "那个", "这张", "那张", "它", "当前这个", "现在这个")
                || containsAnyNormalized(normalizedMessage,
                "打开那个歌单", "打开这个歌单", "播放那个歌单", "播放这个歌单", "收藏这个歌单", "取消收藏这个歌单",
                "打开那个歌手", "打开这个歌手", "打开这个评论区", "打开那个评论区", "把这首歌加进去", "把这首歌移出去");
    }

    private boolean requiresCurrentContext(IntentPlan plan) {
        if (plan == null) {
            return false;
        }
        if ("open_song_comment".equals(plan.getIntent())) {
            return plan.getSearchKeyword() == null || plan.getSearchKeyword().isEmpty();
        }
        if ("open_singer_detail".equals(plan.getIntent())
                || "open_playlist_detail".equals(plan.getIntent())
                || "favorite_playlist".equals(plan.getIntent())
                || "unfavorite_playlist".equals(plan.getIntent())
                || "play_playlist".equals(plan.getIntent())
                || "remove_current_song_from_playlist".equals(plan.getIntent())
                || "delete_my_playlist".equals(plan.getIntent())) {
            return plan.getActionTarget() == null || plan.getActionTarget().isEmpty() || isGenericActionTarget(plan.getActionTarget());
        }
        return false;
    }

    private String resolveConfirmationActionTarget(IntentPlan plan) {
        if (plan == null) {
            return "";
        }
        if (requiresCurrentContext(plan) || isGenericActionTarget(plan.getActionTarget())) {
            return "";
        }
        return plan.getActionTarget();
    }

    private boolean isGenericActionTarget(String actionTarget) {
        String normalizedTarget = normalizeForCompare(actionTarget);
        return !normalizedTarget.isEmpty() && GENERIC_ACTION_TARGETS.contains(normalizedTarget);
    }

    private String buildConfirmationPrompt(IntentPlan plan) {
        String action = plan.getIntent();
        String actionTarget = sanitizeMessage(plan.getActionTarget());
        String searchKeyword = sanitizeMessage(plan.getSearchKeyword());
        boolean genericTarget = isGenericActionTarget(actionTarget);

        if ("open_playlist_detail".equals(action)) {
            return genericTarget || actionTarget.isEmpty()
                    ? "我先理解成你想打开当前这个歌单，确认要继续吗？如果不是，也可以直接把歌单名告诉我。"
                    : String.format("我先理解成你想打开“%s”歌单，确认要继续吗？", actionTarget);
        }
        if ("play_playlist".equals(action)) {
            return genericTarget || actionTarget.isEmpty()
                    ? "我先理解成你想播放当前这个歌单，确认要继续吗？如果不是，也可以直接把歌单名告诉我。"
                    : String.format("我先理解成你想播放“%s”歌单，确认要继续吗？", actionTarget);
        }
        if ("favorite_playlist".equals(action)) {
            return genericTarget || actionTarget.isEmpty()
                    ? "我先理解成你想收藏当前这个歌单，确认要继续吗？"
                    : String.format("我先理解成你想收藏“%s”歌单，确认要继续吗？", actionTarget);
        }
        if ("unfavorite_playlist".equals(action)) {
            return genericTarget || actionTarget.isEmpty()
                    ? "我先理解成你想取消收藏当前这个歌单，确认要继续吗？"
                    : String.format("我先理解成你想取消收藏“%s”歌单，确认要继续吗？", actionTarget);
        }
        if ("open_singer_detail".equals(action)) {
            return genericTarget || actionTarget.isEmpty()
                    ? "我先理解成你想打开当前这位歌手的详情页，确认要继续吗？如果不是，也可以直接把歌手名告诉我。"
                    : String.format("我先理解成你想打开 %s 的详情页，确认要继续吗？", actionTarget);
        }
        if ("open_song_comment".equals(action)) {
            return searchKeyword.isEmpty()
                    ? "我先理解成你想打开当前这首歌的评论区，确认要继续吗？"
                    : String.format("我先理解成你想打开“%s”的评论区，确认要继续吗？", searchKeyword);
        }
        if ("add_current_song_to_playlist".equals(action)) {
            return genericTarget || actionTarget.isEmpty()
                    ? "我先理解成你想把当前歌曲加入当前这个歌单，确认要继续吗？如果不是，也可以直接告诉我歌单名。"
                    : String.format("我先理解成你想把当前歌曲加入“%s”歌单，确认要继续吗？", actionTarget);
        }
        if ("remove_current_song_from_playlist".equals(action)) {
            return genericTarget || actionTarget.isEmpty()
                    ? "我先理解成你想把当前歌曲从当前这个歌单移除，确认要继续吗？"
                    : String.format("我先理解成你想把当前歌曲从“%s”歌单移除，确认要继续吗？", actionTarget);
        }
        if ("delete_my_playlist".equals(action)) {
            return genericTarget || actionTarget.isEmpty()
                    ? "我先理解成你想删除当前这个歌单，确认要继续吗？删除后就不能恢复了。"
                    : String.format("我先理解成你想删除“%s”歌单，确认要继续吗？删除后就不能恢复了。", actionTarget);
        }
        return buildClientActionReply(action, actionTarget) + " 先和你确认一下，是否继续？";
    }

    private AssistantExecution executePlan(IntentPlan plan, boolean allowImport) {
        AssistantExecution execution = new AssistantExecution();
        execution.setAction("none");
        execution.setActionTarget(plan.getActionTarget());
        execution.setSearchKeyword(plan.getSearchKeyword());
        execution.setUsedImport(false);

        if ("logout".equals(plan.getIntent())) {
            execution.setAction("logout");
            return execution;
        }

        if (isClientActionIntent(plan.getIntent())) {
            execution.setAction(plan.getIntent());
            return execution;
        }

        if (!isMusicIntent(plan)) {
            return execution;
        }

        String keyword = normalizeSearchKeyword(plan.getSearchKeyword(), plan.getOriginalMessage());
        execution.setSearchKeyword(keyword);
        if (keyword.isEmpty()) {
            execution.setErrorMessage("你可以直接说“播放周杰伦的告白气球”或者“帮我找张学友的歌”。");
            return execution;
        }

        boolean preferChoosing = isBroadSongBrowseRequest(plan.getOriginalMessage(), keyword);
        int localCandidateLimit = preferChoosing ? UNLIMITED_LOCAL_CANDIDATE_LIMIT : DEFAULT_LOCAL_CANDIDATE_LIMIT;
        List<RankedSong> rankedSongs = searchLocalSongs(keyword, localCandidateLimit);
        if (!rankedSongs.isEmpty()) {
            execution.setCandidates(rankedSongs.stream().map(RankedSong::getSong).collect(Collectors.toList()));
            if (!preferChoosing && plan.isShouldAutoPlay() && isConfidentMatch(rankedSongs)) {
                execution.setAction("play_song");
                execution.setSong(rankedSongs.get(0).getSong());
            } else {
                execution.setAction("choose_song");
            }
            return execution;
        }

        if (preferChoosing) {
            RemoteSearchBridgeResult remoteSearch = searchRemoteSuggestions(keyword, BROWSE_REMOTE_SUGGESTION_LIMIT);
            if (!remoteSearch.getSuggestions().isEmpty()) {
                execution.setErrorMessage(buildRemoteSuggestionReply(keyword, remoteSearch.getSuggestions()));
            } else if (remoteSearch.getErrorMessage() != null && !remoteSearch.getErrorMessage().isEmpty()) {
                execution.setErrorMessage(remoteSearch.getErrorMessage());
            } else {
                execution.setErrorMessage("我先没找到这位歌手的候选歌曲，你也可以直接告诉我更具体的歌名。");
            }
            return execution;
        }

        if ("play_song".equals(plan.getIntent()) && plan.isAllowImportIfMissing() && allowImport && importEnabled) {
            ImportBridgeResult importResult = importSong(keyword);
            if (importResult.getSong() != null) {
                execution.setAction("play_song");
                execution.setSong(importResult.getSong());
                execution.setCandidates(Collections.singletonList(importResult.getSong()));
                execution.setUsedImport(true);
                return execution;
            }
            execution.setErrorMessage(importResult.getErrorMessage());
            return execution;
        }

        execution.setErrorMessage("站内暂时没有找到这首歌。");
        return execution;
    }

    private boolean isMusicIntent(IntentPlan plan) {
        return "play_song".equals(plan.getIntent()) || "search_song".equals(plan.getIntent());
    }

    private boolean isBroadSongBrowseRequest(String userMessage, String keyword) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return false;
        }

        List<String> tokens = tokenizeKeyword(keyword);
        if (tokens.isEmpty() || tokens.size() > 2) {
            return false;
        }

        String normalizedMessage = normalizeForCompare(message);
        return containsAnyText(message, "的歌", "的歌曲")
                || containsAnyNormalized(normalizedMessage, "有哪些歌", "都有哪些歌", "都有什么歌", "代表作", "热门歌", "经典歌");
    }

    private boolean isClientActionIntent(String intent) {
        return "logout".equals(intent)
                || "navigate_home".equals(intent)
                || "navigate_song_sheet".equals(intent)
                || "navigate_singer".equals(intent)
                || "navigate_rank".equals(intent)
                || "navigate_collection".equals(intent)
                || "navigate_my_song_sheet".equals(intent)
                || "navigate_personal".equals(intent)
                || "navigate_setting".equals(intent)
                || "open_singer_detail".equals(intent)
                || "open_playlist_detail".equals(intent)
                || "open_lyric".equals(intent)
                || "open_song_comment".equals(intent)
                || "clear_search_history".equals(intent)
                || "navigate_collection_playlists".equals(intent)
                || "favorite_playlist".equals(intent)
                || "unfavorite_playlist".equals(intent)
                || "play_playlist".equals(intent)
                || "toggle_dark_mode".equals(intent)
                || "player_pause".equals(intent)
                || "player_resume".equals(intent)
                || "player_next".equals(intent)
                || "player_previous".equals(intent)
                || "favorite_current_song".equals(intent)
                || "unfavorite_current_song".equals(intent)
                || "add_current_song_to_playlist".equals(intent)
                || "create_playlist".equals(intent)
                || "add_song_to_playlist".equals(intent)
                || "remove_current_song_from_playlist".equals(intent)
                || "create_recommended_playlist".equals(intent)
                || "delete_my_playlist".equals(intent);
    }

    private boolean isConfidentMatch(List<RankedSong> rankedSongs) {
        if (rankedSongs.isEmpty()) {
            return false;
        }
        RankedSong top = rankedSongs.get(0);
        if (top.getScore() >= 92) {
            return true;
        }
        if (rankedSongs.size() == 1) {
            return top.getScore() >= 70;
        }
        RankedSong second = rankedSongs.get(1);
        return top.getScore() >= 78 && top.getScore() - second.getScore() >= 12;
    }

    private List<RankedSong> searchLocalSongs(String keyword, int limit) {
        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("url");
        queryWrapper.ne("url", "");
        List<Song> allSongs = songMapper.selectList(queryWrapper);
        if (allSongs == null || allSongs.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedKeyword = normalizeForCompare(keyword);
        List<String> tokens = tokenizeKeyword(keyword);
        if (tokens.isEmpty()) {
            tokens = Collections.singletonList(normalizedKeyword);
        }

        List<RankedSong> rankedSongs = new ArrayList<>();
        for (Song song : allSongs) {
            int score = computeSongMatchScore(song, keyword, normalizedKeyword, tokens);
            if (score <= 0) {
                continue;
            }
            rankedSongs.add(new RankedSong(song, score));
        }

        rankedSongs.sort(Comparator
                .comparingInt(RankedSong::getScore).reversed()
                .thenComparing((RankedSong item) -> item.getSong().getType() != null && item.getSong().getType() == 1 ? 0 : 1)
                .thenComparing(item -> item.getSong().getUpdateTime(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> item.getSong().getId(), Comparator.nullsLast(Comparator.reverseOrder())));
        if (limit > 0 && rankedSongs.size() > limit) {
            return new ArrayList<>(rankedSongs.subList(0, limit));
        }
        return rankedSongs;
    }

    private int computeSongMatchScore(Song song, String rawKeyword, String normalizedKeyword, List<String> tokens) {
        String songName = Objects.toString(song.getName(), "");
        if (songName.trim().isEmpty()) {
            return 0;
        }

        String normalizedSong = normalizeForCompare(songName);
        String normalizedTitle = normalizeForCompare(extractSongTitle(songName));
        String normalizedSinger = normalizeForCompare(extractSingerName(songName));
        int score = 0;

        if (!normalizedKeyword.isEmpty()) {
            if (normalizedSong.equals(normalizedKeyword)) {
                score += 100;
            }
            if (normalizedTitle.equals(normalizedKeyword)) {
                score += 92;
            }
            if (normalizedSong.contains(normalizedKeyword)) {
                score += 68;
            }
            if (normalizedTitle.contains(normalizedKeyword)) {
                score += 62;
            }
            if (normalizedSinger.contains(normalizedKeyword)) {
                score += 38;
            }
        }

        int matchedTokenCount = 0;
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (normalizedSong.contains(token)) {
                score += token.length() >= 3 ? 18 : 10;
                matchedTokenCount++;
            } else if (normalizedTitle.contains(token)) {
                score += token.length() >= 3 ? 15 : 8;
                matchedTokenCount++;
            } else if (normalizedSinger.contains(token)) {
                score += 10;
                matchedTokenCount++;
            }
        }

        if (!tokens.isEmpty() && matchedTokenCount == tokens.size()) {
            score += 26;
        } else if (matchedTokenCount >= 2) {
            score += 12;
        }

        String rawSongName = songName.toLowerCase(Locale.ROOT);
        if (rawSongName.contains(rawKeyword.toLowerCase(Locale.ROOT))) {
            score += 12;
        }

        return score;
    }

    private IntentPlan resolveIntentPlan(String userMessage,
                                         List<AssistantHistoryMessage> history,
                                         AssistantSessionState sessionState) {
        IntentPlan fallbackPlan = buildHeuristicPlan(userMessage);
        if ("logout".equals(fallbackPlan.getIntent())) {
            return fallbackPlan;
        }
        if (deepSeekApiKey == null || deepSeekApiKey.trim().isEmpty()) {
            return fallbackPlan;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("model", deepSeekModel);
            body.put("temperature", 0.1);
            body.put("max_tokens", 320);

            JSONArray messages = new JSONArray();
            messages.add(buildChatMessage("system",
                    "你是音乐站助手的意图规划器。请只输出 JSON，不要输出 markdown。"
                            + "字段必须包含 intent、searchKeyword、actionTarget、shouldAutoPlay、allowImportIfMissing。"
                        + "intent 只能是 chat、play_song、search_song、logout、navigate_home、navigate_song_sheet、navigate_singer、navigate_rank、navigate_collection、navigate_my_song_sheet、navigate_personal、navigate_setting、open_singer_detail、open_playlist_detail、open_lyric、open_song_comment、clear_search_history、navigate_collection_playlists、favorite_playlist、unfavorite_playlist、play_playlist、toggle_dark_mode、player_pause、player_resume、player_next、player_previous、favorite_current_song、unfavorite_current_song、add_current_song_to_playlist、create_playlist、add_song_to_playlist、remove_current_song_from_playlist、create_recommended_playlist、delete_my_playlist。"
                            + "如果用户想直接听歌或播放歌曲，intent=play_song。"
                            + "如果用户只是想找歌、查歌曲、看有哪些歌，intent=search_song。"
                            + "如果用户是在让你推荐几首歌、推荐适合某种场景/情绪听的歌，例如“推荐几首适合夜晚听的歌”“推荐几首通勤时听的歌”，这是歌曲推荐请求，intent=chat，不要创建歌单。"
                            + "如果用户只是在问某个歌手的歌、代表作、有哪些歌，不要当作精确单曲播放，优先返回 search_song。"
                            + "如果用户明确要退出登录、登出、注销账号，intent=logout。"
                            + "如果用户要打开首页、歌单、歌手、排行榜、收藏、我的歌单、个人中心、设置等页面，就返回对应 navigate intent。"
                            + "如果用户要打开歌手详情页，intent=open_singer_detail，并把歌手名放到 actionTarget；如果是当前歌手可返回空字符串。"
                            + "如果用户要打开某个具体歌单详情，intent=open_playlist_detail，并把歌单标题放到 actionTarget。"
                            + "如果用户要打开歌词页，就返回 open_lyric。"
                        + "如果用户要打开歌曲评论区，intent=open_song_comment；如果是指定歌曲，把歌手名和歌名放到 searchKeyword，否则 searchKeyword 返回空字符串。"
                        + "如果用户要清空搜索历史，intent=clear_search_history。"
                        + "如果用户要打开收藏里的歌单页签，intent=navigate_collection_playlists。"
                        + "如果用户要收藏、取消收藏或播放某个歌单，就返回 favorite_playlist、unfavorite_playlist、play_playlist，并把歌单标题放到 actionTarget；如果用户说的是当前歌单，可返回空字符串。"
                        + "如果用户要暂停、继续播放、下一首、上一首，就返回对应 player intent。"
                        + "如果用户要收藏或取消收藏当前歌曲，就返回 favorite_current_song 或 unfavorite_current_song。"
                        + "如果用户要把当前歌曲加入某个歌单，intent=add_current_song_to_playlist，并把歌单标题放到 actionTarget。"
                        + "如果用户要新建歌单，intent=create_playlist，并把歌单标题放到 actionTarget。"
                        + "如果用户要把指定歌曲加入某个歌单，intent=add_song_to_playlist，把歌手名和歌名放到 searchKeyword，把歌单标题放到 actionTarget。"
                        + "如果用户要把当前正在播放的歌曲从某个歌单移除，intent=remove_current_song_from_playlist；如果用户明确说了歌单名，就放到 actionTarget，否则 actionTarget 为空字符串。"
                        + "只有当用户明确说“生成歌单”“创建歌单”“做个歌单”“新建歌单”这类创建意图时，才返回 create_recommended_playlist；如果只是让你推荐歌，不要返回 create_recommended_playlist。"
                        + "如果用户要推荐并直接生成一个新歌单，intent=create_recommended_playlist；如果用户给了主题，例如夜晚、通勤，就把主题放到 actionTarget，否则返回空字符串。"
                        + "如果用户要删除自己的某个歌单，intent=delete_my_playlist，并把歌单标题放到 actionTarget。"
                        + "普通闲聊就是 chat。"
                        + "searchKeyword 只保留歌手名和歌名，不要保留“帮我”“播放”“我想听”等口语。"
                        + "actionTarget 只保留歌单标题或歌单主题，不要保留“打开”“加入”“歌单”“列表”“推荐”“生成”等口语；如果不需要就返回空字符串。"
                        + "allowImportIfMissing 在 intent=play_song 且用户明显要听歌时为 true。"));
            appendHistoryMessages(messages, history, 6);
            appendRecommendationContextMessage(messages, sessionState);
            messages.add(buildChatMessage("user", userMessage));
            body.put("messages", messages);

            String content = callDeepSeek(body);
            JSONObject parsed = parseLooseJsonObject(content);
            if (parsed == null) {
                return fallbackPlan;
            }

            IntentPlan plan = new IntentPlan();
            String intent = parsed.getString("intent");
            if (!"play_song".equals(intent)
                    && !"search_song".equals(intent)
                    && !"chat".equals(intent)
                    && !"logout".equals(intent)
                    && !isClientActionIntent(intent)) {
                return fallbackPlan;
            }
            plan.setIntent(intent);
            plan.setSearchKeyword(sanitizeMessage(parsed.getString("searchKeyword")));
            plan.setActionTarget(normalizeActionTarget(parsed.getString("actionTarget")));
            if ("open_singer_detail".equals(intent)) {
                plan.setActionTarget(normalizeSingerTarget(parsed.getString("actionTarget")));
            }
            plan.setShouldAutoPlay(Boolean.TRUE.equals(parsed.getBoolean("shouldAutoPlay")));
            plan.setAllowImportIfMissing(Boolean.TRUE.equals(parsed.getBoolean("allowImportIfMissing")));
            plan.setOriginalMessage(userMessage);
            if (requiresActionTarget(intent) && plan.getActionTarget().isEmpty()) {
                if ("open_playlist_detail".equals(intent)) {
                    plan.setActionTarget(extractPlaylistDetailTarget(userMessage));
                } else if ("add_current_song_to_playlist".equals(intent)) {
                    plan.setActionTarget(extractAddToPlaylistTarget(userMessage));
                } else if ("create_playlist".equals(intent)) {
                    plan.setActionTarget(extractCreatePlaylistTarget(userMessage));
                } else if ("add_song_to_playlist".equals(intent)) {
                    SongPlaylistActionParts parts = extractSpecificSongPlaylistAction(userMessage);
                    plan.setSearchKeyword(parts.getSongKeyword());
                    plan.setActionTarget(parts.getPlaylistTitle());
                } else if ("delete_my_playlist".equals(intent)) {
                    plan.setActionTarget(extractDeleteMyPlaylistTarget(userMessage));
                }
            }
            if ("open_singer_detail".equals(intent) && plan.getActionTarget().isEmpty()) {
                plan.setActionTarget(extractSingerDetailTarget(userMessage));
            }
            if ("open_song_comment".equals(intent) && plan.getSearchKeyword().isEmpty()) {
                plan.setSearchKeyword(extractSongCommentTarget(userMessage));
            }
            if ("remove_current_song_from_playlist".equals(intent) && plan.getActionTarget().isEmpty()) {
                plan.setActionTarget(extractRemoveCurrentSongFromPlaylistTarget(userMessage));
            }
            if ("create_recommended_playlist".equals(intent) && plan.getActionTarget().isEmpty()) {
                plan.setActionTarget(extractCreateRecommendedPlaylistTheme(userMessage));
            }
            if ("create_recommended_playlist".equals(intent) && !isExplicitRecommendedPlaylistCreationRequest(userMessage)) {
                if (isSongRecommendationRequest(userMessage)) {
                    plan.setIntent("chat");
                    plan.setActionTarget("");
                    plan.setSearchKeyword("");
                    plan.setShouldAutoPlay(false);
                    plan.setAllowImportIfMissing(false);
                } else {
                    return fallbackPlan;
                }
            }
            if (isMusicIntent(plan) && isBroadSongBrowseRequest(userMessage, plan.getSearchKeyword())) {
                plan.setIntent("search_song");
                plan.setShouldAutoPlay(false);
                plan.setAllowImportIfMissing(false);
            }
            if (plan.getSearchKeyword().isEmpty()
                    && !"chat".equals(intent)
                    && !"logout".equals(intent)
                    && !isClientActionIntent(intent)) {
                return fallbackPlan;
            }
            if (requiresActionTarget(intent) && plan.getActionTarget().isEmpty()) {
                return fallbackPlan;
            }
            return plan;
        } catch (Exception exception) {
            return fallbackPlan;
        }
    }

    private String buildGeneralReply(List<AssistantHistoryMessage> history, String userMessage) {
        if (deepSeekApiKey == null || deepSeekApiKey.trim().isEmpty()) {
            return "我现在还没连上 DeepSeek，所以更适合帮你点歌。等配置好 API Key 后，我就能正常聊天了。";
        }

        try {
            JSONObject body = new JSONObject();
            body.put("model", deepSeekModel);
            body.put("temperature", 0.7);
            body.put("max_tokens", 700);

            JSONArray messages = new JSONArray();
            messages.add(buildChatMessage("system",
                    "你是“轻音音乐”的内嵌 AI 小助手，语气自然、简洁、友好，默认用中文回答。"
                            + "你可以聊天，也可以引导用户直接说出歌手名和歌名来点歌。"
                            + "如果用户是在让你推荐几首适合某种场景、时间、情绪听的歌，请直接给出 3 到 6 首歌曲建议，先不要擅自创建歌单。"
                            + "回答不要太长，2-5 句优先。"));
            appendHistoryMessages(messages, history, 8);
            messages.add(buildChatMessage("user", userMessage));
            body.put("messages", messages);
            String content = callDeepSeek(body);
            return sanitizeAssistantReply(content);
        } catch (Exception exception) {
            return "我刚刚有点走神了，你可以再说一次，或者直接告诉我歌手名和歌名，我来帮你播放。";
        }
    }

    private void appendHistoryMessages(JSONArray messages, List<AssistantHistoryMessage> history, int limit) {
        if (history == null || history.isEmpty()) {
            return;
        }
        int start = Math.max(0, history.size() - limit);
        for (int i = start; i < history.size(); i++) {
            AssistantHistoryMessage item = history.get(i);
            if (item == null) {
                continue;
            }
            String role = Objects.toString(item.getRole(), "").trim().toLowerCase(Locale.ROOT);
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            String content = sanitizeMessage(item.getContent());
            if (content.isEmpty()) {
                continue;
            }
            messages.add(buildChatMessage(role, content));
        }
    }

    private void appendRecommendationContextMessage(JSONArray messages, AssistantSessionState sessionState) {
        if (messages == null || sessionState == null) {
            return;
        }
        if (sessionState.getLastRecommendationAt() <= 0
                || sessionState.getLastRecommendationRequest().isEmpty()
                || System.currentTimeMillis() - sessionState.getLastRecommendationAt() > RECOMMENDATION_CONTEXT_TTL_MS) {
            return;
        }
        messages.add(buildChatMessage("system",
                "最近一次已确认的歌曲推荐主题是：" + sessionState.getLastRecommendationRequest()
                        + "。如果用户这一句像“再来点”“换成女生唱的”这种追问，请优先把它理解成对这次推荐的补充。"));
    }

    private JSONObject buildChatMessage(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String callDeepSeek(JSONObject body) throws IOException {
        String requestBody = body.toJSONString();
        String endpoint = deepSeekBaseUrl.endsWith("/")
                ? deepSeekBaseUrl + "chat/completions"
                : deepSeekBaseUrl + "/chat/completions";

        Request request = new Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer " + deepSeekApiKey.trim())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.body() == null) {
                throw new IOException("DeepSeek 返回了空响应");
            }
            String rawResponse = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("DeepSeek 请求失败: " + rawResponse);
            }
            JSONObject payload = JSON.parseObject(rawResponse);
            JSONArray choices = payload.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IOException("DeepSeek 没有返回候选结果");
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            if (message == null) {
                throw new IOException("DeepSeek 响应格式异常");
            }
            return message.getString("content");
        }
    }

    private ImportBridgeResult importSong(String keyword) {
        ImportBridgeResult result = new ImportBridgeResult();
        result.setErrorMessage("站内没找到这首歌，而且自动补歌暂时失败了。");

        if (importScriptPath == null || importScriptPath.trim().isEmpty()) {
            result.setErrorMessage("自动补歌脚本路径还没有配置。");
            return result;
        }

        Path scriptPath = Paths.get(importScriptPath).toAbsolutePath().normalize();
        if (!scriptPath.toFile().exists()) {
            result.setErrorMessage("自动补歌脚本不存在，请检查配置。");
            return result;
        }

        List<String> command = new ArrayList<>();
        command.add(importPythonExecutable);
        command.add(scriptPath.toString());
        command.add("import");
        command.add("--keyword");
        command.add(keyword);
        command.add("--source");
        command.add(importSources);
        command.add("--limit");
        command.add(String.valueOf(Math.max(importLimit, 1)));
        command.add("--index");
        command.add("1");
        command.add("--publish-type");
        command.add(String.valueOf(importPublishType));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PYTHONIOENCODING", "UTF-8");
        Path projectRoot = scriptPath.getParent() == null ? scriptPath : scriptPath.getParent().getParent();
        if (projectRoot != null && projectRoot.toFile().exists()) {
            processBuilder.directory(projectRoot.toFile());
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Process process = processBuilder.start();
            Future<String> outputFuture = executorService.submit(new StreamReader(process.getInputStream()));

            boolean finished = process.waitFor(importCommandTimeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.setErrorMessage("自动补歌超时了，你可以稍后再试。");
                return result;
            }

            String output = safeFutureGet(outputFuture);
            if (process.exitValue() != 0) {
                result.setErrorMessage(buildImportErrorMessage(output));
                return result;
            }

            JSONObject payload = parseLastJsonObject(output);
            if (payload == null) {
                result.setErrorMessage("自动补歌返回结果解析失败。");
                return result;
            }

            Integer songId = payload.getInteger("song_id");
            if (songId == null) {
                result.setErrorMessage("自动补歌完成了，但没有返回歌曲编号。");
                return result;
            }

            Song song = songMapper.selectById(songId);
            if (song == null) {
                result.setErrorMessage("自动补歌完成了，但歌曲还没有入库成功。");
                return result;
            }

            repairSingerProfileAfterImport(song);
            schedulePostImportAudit(song);
            result.setSong(song);
            result.setErrorMessage(null);
            return result;
        } catch (IOException exception) {
            result.setErrorMessage("自动补歌脚本启动失败了，请检查 Python 路径。");
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            result.setErrorMessage("自动补歌过程中断了，请稍后再试。");
            return result;
        } finally {
            executorService.shutdownNow();
        }
    }

    private void repairSingerProfileAfterImport(Song song) {
        if (song == null) {
            return;
        }

        String singerName = sanitizeMessage(resolveSingerName(song));
        if (singerName.isEmpty()) {
            return;
        }

        runImportMaintenanceCommand(buildSingerProfileRepairCommand(song, singerName), importCommandTimeoutMs);
    }

    private String resolveSingerName(Song song) {
        if (song == null) {
            return "";
        }
        if (song.getSingerId() != null && song.getSingerId() > 0 && singerMapper != null) {
            Singer singer = singerMapper.selectById(song.getSingerId());
            if (singer != null && singer.getName() != null && !singer.getName().trim().isEmpty()) {
                return singer.getName().trim();
            }
        }
        return extractSingerName(song.getName());
    }

    private void schedulePostImportAudit(Song song) {
        if (song == null || song.getId() == null || song.getId() <= 0) {
            return;
        }
        importAuditExecutor.submit(() -> runPostImportAudit(song));
    }

    private void runPostImportAudit(Song song) {
        String singerName = sanitizeMessage(resolveSingerName(song));
        if (singerName.isEmpty() || song.getId() == null || song.getId() <= 0) {
            return;
        }

        long auditTimeoutMs = Math.max(45000L, Math.min(importCommandTimeoutMs, 90000L));
        runImportMaintenanceCommand(buildSongAssetRepairCommand(song.getId(), singerName), auditTimeoutMs);
        runImportMaintenanceCommand(buildLyricRepairCommand(song.getId()), auditTimeoutMs);
        runImportMaintenanceCommand(buildSingerProfileRepairCommand(song, singerName), auditTimeoutMs);
    }

    private List<String> buildSingerProfileRepairCommand(Song song, String singerName) {
        List<String> command = buildImportCommandPrefix();
        if (command.isEmpty()) {
            return command;
        }
        command.add("repair-singer-profiles");
        command.add("--sources");
        command.add(importSources);
        if (song != null && song.getSingerId() != null && song.getSingerId() > 0) {
            command.add("--singer-id-min");
            command.add(String.valueOf(song.getSingerId()));
        }
        command.add("--artist-like");
        command.add(singerName);
        return command;
    }

    private List<String> buildSongAssetRepairCommand(Integer songId, String singerName) {
        List<String> command = buildImportCommandPrefix();
        if (command.isEmpty() || songId == null || songId <= 0) {
            return command;
        }
        command.add("repair-song-assets");
        command.add("--sources");
        command.add(importSources);
        command.add("--song-ids");
        command.add(String.valueOf(songId));
        if (singerName != null && !singerName.trim().isEmpty()) {
            command.add("--artist-like");
            command.add(singerName.trim());
        }
        command.add("--limit");
        command.add("1");
        return command;
    }

    private List<String> buildLyricRepairCommand(Integer songId) {
        List<String> command = buildImportCommandPrefix();
        if (command.isEmpty() || songId == null || songId <= 0) {
            return command;
        }
        command.add("repair-lyric-sync");
        command.add("--sources");
        command.add(importSources);
        command.add("--song-ids");
        command.add(String.valueOf(songId));
        command.add("--limit");
        command.add("1");
        return command;
    }

    private List<String> buildImportCommandPrefix() {
        if (importScriptPath == null || importScriptPath.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Path scriptPath = Paths.get(importScriptPath).toAbsolutePath().normalize();
        if (!scriptPath.toFile().exists()) {
            return Collections.emptyList();
        }

        List<String> command = new ArrayList<>();
        command.add(importPythonExecutable);
        command.add(scriptPath.toString());
        return command;
    }

    private void runImportMaintenanceCommand(List<String> command, long timeoutMs) {
        if (command == null || command.isEmpty()) {
            return;
        }

        Path scriptPath = Paths.get(importScriptPath).toAbsolutePath().normalize();
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PYTHONIOENCODING", "UTF-8");
        Path projectRoot = scriptPath.getParent() == null ? scriptPath : scriptPath.getParent().getParent();
        if (projectRoot != null && projectRoot.toFile().exists()) {
            processBuilder.directory(projectRoot.toFile());
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Process process = processBuilder.start();
            Future<String> outputFuture = executorService.submit(new StreamReader(process.getInputStream()));
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return;
            }
            safeFutureGet(outputFuture);
        } catch (IOException exception) {
            return;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            executorService.shutdownNow();
        }
    }

    private RemoteSearchBridgeResult searchRemoteSuggestions(String keyword, int limit) {
        RemoteSearchBridgeResult result = new RemoteSearchBridgeResult();
        if (importScriptPath == null || importScriptPath.trim().isEmpty()) {
            result.setErrorMessage("我先没在站内找到这位歌手的歌，你也可以直接告诉我更具体的歌名。");
            return result;
        }

        Path scriptPath = Paths.get(importScriptPath).toAbsolutePath().normalize();
        if (!scriptPath.toFile().exists()) {
            result.setErrorMessage("我先没在站内找到这位歌手的歌，你也可以直接告诉我更具体的歌名。");
            return result;
        }

        List<String> command = new ArrayList<>();
        command.add(importPythonExecutable);
        command.add(scriptPath.toString());
        command.add("search");
        command.add("--keyword");
        command.add(keyword);
        command.add("--source");
        command.add(importSources);
        command.add("--limit");
        command.add(String.valueOf(Math.max(limit, 1)));
        command.add("--json");

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PYTHONIOENCODING", "UTF-8");
        Path projectRoot = scriptPath.getParent() == null ? scriptPath : scriptPath.getParent().getParent();
        if (projectRoot != null && projectRoot.toFile().exists()) {
            processBuilder.directory(projectRoot.toFile());
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Process process = processBuilder.start();
            Future<String> outputFuture = executorService.submit(new StreamReader(process.getInputStream()));

            boolean finished = process.waitFor(importCommandTimeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.setErrorMessage("我去外部来源找歌时超时了，你可以直接告诉我更具体的歌名。");
                return result;
            }

            String output = safeFutureGet(outputFuture);
            if (process.exitValue() != 0) {
                result.setErrorMessage(buildRemoteSearchErrorMessage(output));
                return result;
            }

            JSONArray payload = parseLastJsonArray(output);
            if (payload == null) {
                result.setErrorMessage("我去外部来源找了一下，但候选结果暂时解析失败了。");
                return result;
            }

            Map<String, RemoteSongSuggestion> deduplicated = new LinkedHashMap<>();
            for (int index = 0; index < payload.size(); index++) {
                JSONObject item = payload.getJSONObject(index);
                if (item == null) {
                    continue;
                }
                String title = sanitizeMessage(item.getString("title"));
                if (title.isEmpty()) {
                    continue;
                }
                String artist = sanitizeMessage(item.getString("artist"));
                String dedupKey = normalizeForCompare(artist + "-" + title);
                if (dedupKey.isEmpty() || deduplicated.containsKey(dedupKey)) {
                    continue;
                }
                RemoteSongSuggestion suggestion = new RemoteSongSuggestion();
                suggestion.setTitle(title);
                suggestion.setArtist(artist);
                suggestion.setSource(sanitizeMessage(item.getString("source")));
                deduplicated.put(dedupKey, suggestion);
                if (deduplicated.size() >= Math.max(limit, 1)) {
                    break;
                }
            }

            result.setSuggestions(new ArrayList<>(deduplicated.values()));
            if (result.getSuggestions().isEmpty()) {
                result.setErrorMessage("我先没在外部来源里找到这位歌手的候选歌曲，你也可以直接告诉我更具体的歌名。");
            }
            return result;
        } catch (IOException exception) {
            result.setErrorMessage("我去外部来源找歌时没有启动成功，请检查补歌环境。");
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            result.setErrorMessage("我去外部来源找歌时被中断了，你可以稍后再试。");
            return result;
        } finally {
            executorService.shutdownNow();
        }
    }

    private String buildImportErrorMessage(String output) {
        if (output == null || output.trim().isEmpty()) {
            return "自动补歌失败了，请换个关键词试试。";
        }
        String trimmed = output.trim();
        Matcher missingModuleMatcher = MISSING_MODULE_PATTERN.matcher(trimmed);
        if (missingModuleMatcher.find()) {
            return String.format("自动补歌脚本缺少 Python 依赖包：%s。请先安装补歌依赖。", missingModuleMatcher.group(1));
        }
        String[] lines = trimmed.split("\\r?\\n");
        String lastLine = lines[lines.length - 1].trim();
        if (lastLine.startsWith("ERROR:")) {
            return lastLine.substring("ERROR:".length()).trim();
        }
        if (trimmed.contains("No search results")) {
            return "外部来源里也没有找到这首歌。";
        }
        return "自动补歌失败了，请换个更完整的歌名再试。";
    }

    private String buildRemoteSearchErrorMessage(String output) {
        if (output == null || output.trim().isEmpty()) {
            return "我去外部来源找了一下，但还没拿到可用候选。";
        }
        String trimmed = output.trim();
        Matcher missingModuleMatcher = MISSING_MODULE_PATTERN.matcher(trimmed);
        if (missingModuleMatcher.find()) {
            return String.format("远程候选搜索缺少 Python 依赖包：%s。请先安装补歌依赖。", missingModuleMatcher.group(1));
        }
        if (trimmed.contains("No results found") || trimmed.contains("No search results")) {
            return "我先没在外部来源里找到这位歌手的候选歌曲，你也可以直接告诉我更具体的歌名。";
        }
        String[] lines = trimmed.split("\\r?\\n");
        String lastLine = lines[lines.length - 1].trim();
        if (lastLine.startsWith("ERROR:")) {
            return lastLine.substring("ERROR:".length()).trim();
        }
        return "我去外部来源找了一下，但还没拿到可用候选。";
    }

    private String buildRemoteSuggestionReply(String keyword, List<RemoteSongSuggestion> suggestions) {
        List<String> labels = suggestions.stream()
                .limit(5)
                .map(item -> formatRemoteSuggestionItem(keyword, item))
                .filter(label -> !label.isEmpty())
                .collect(Collectors.toList());
        if (labels.isEmpty()) {
            return "我先没在外部来源里找到合适的候选歌曲，你也可以直接告诉我更具体的歌名。";
        }
        return String.format("你现在是想听这个歌手的哪首歌呀？我先帮你找到几首：%s。你直接说歌名，我就能继续帮你补并播放。", String.join("、", labels));
    }

    private String formatRemoteSuggestionItem(String keyword, RemoteSongSuggestion suggestion) {
        if (suggestion == null || suggestion.getTitle() == null || suggestion.getTitle().trim().isEmpty()) {
            return "";
        }
        String title = suggestion.getTitle().trim();
        String artist = suggestion.getArtist() == null ? "" : suggestion.getArtist().trim();
        if (artist.isEmpty()) {
            return String.format("《%s》", title);
        }
        String normalizedKeyword = normalizeForCompare(keyword);
        String normalizedArtist = normalizeForCompare(artist);
        if (!normalizedKeyword.isEmpty()
                && !normalizedArtist.isEmpty()
                && (normalizedArtist.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedArtist))) {
            return String.format("《%s》", title);
        }
        return String.format("%s 的《%s》", artist, title);
    }

    private String safeFutureGet(Future<String> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | java.util.concurrent.TimeoutException exception) {
            return "";
        }
    }

    private IntentPlan buildHeuristicPlan(String userMessage) {
        IntentPlan plan = new IntentPlan();
        plan.setOriginalMessage(userMessage);
        String normalizedMessage = normalizeForCompare(userMessage);
        boolean looksLikeMusic = containsAny(userMessage, MUSIC_TRIGGER_WORDS);
        boolean wantsPlay = containsAny(userMessage, PLAY_TRIGGER_WORDS);
        boolean wantsLogout = containsAny(userMessage, LOGOUT_TRIGGER_WORDS);
        SongPlaylistActionParts specificSongPlaylistAction = extractSpecificSongPlaylistAction(userMessage);
        if (!specificSongPlaylistAction.getPlaylistTitle().isEmpty()) {
            plan.setIntent("add_song_to_playlist");
            plan.setSearchKeyword(specificSongPlaylistAction.getSongKeyword());
            plan.setActionTarget(specificSongPlaylistAction.getPlaylistTitle());
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String removeCurrentSongPlaylistTarget = extractRemoveCurrentSongFromPlaylistTarget(userMessage);
        if (!removeCurrentSongPlaylistTarget.isEmpty()) {
            plan.setIntent("remove_current_song_from_playlist");
            plan.setActionTarget(removeCurrentSongPlaylistTarget);
            plan.setSearchKeyword("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String addToPlaylistTarget = extractAddToPlaylistTarget(userMessage);
        if (!addToPlaylistTarget.isEmpty()) {
            plan.setIntent("add_current_song_to_playlist");
            plan.setActionTarget(addToPlaylistTarget);
            plan.setSearchKeyword("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String playlistDetailTarget = extractPlaylistDetailTarget(userMessage);
        if (!playlistDetailTarget.isEmpty()) {
            plan.setIntent("open_playlist_detail");
            plan.setActionTarget(playlistDetailTarget);
            plan.setSearchKeyword("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String favoritePlaylistTarget = extractFavoritePlaylistTarget(userMessage);
        if (!favoritePlaylistTarget.isEmpty()) {
            plan.setIntent("favorite_playlist");
            plan.setActionTarget(favoritePlaylistTarget);
            plan.setSearchKeyword("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String unfavoritePlaylistTarget = extractUnfavoritePlaylistTarget(userMessage);
        if (!unfavoritePlaylistTarget.isEmpty()) {
            plan.setIntent("unfavorite_playlist");
            plan.setActionTarget(unfavoritePlaylistTarget);
            plan.setSearchKeyword("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String playPlaylistTarget = extractPlayPlaylistTarget(userMessage);
        if (!playPlaylistTarget.isEmpty()) {
            plan.setIntent("play_playlist");
            plan.setActionTarget(playPlaylistTarget);
            plan.setSearchKeyword("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String recommendedPlaylistTheme = extractCreateRecommendedPlaylistTheme(userMessage);
        if (isCreateRecommendedPlaylistRequest(userMessage, normalizedMessage)) {
            plan.setIntent("create_recommended_playlist");
            plan.setActionTarget(recommendedPlaylistTheme);
            plan.setSearchKeyword("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String createPlaylistTarget = extractCreatePlaylistTarget(userMessage);
        if (!createPlaylistTarget.isEmpty()) {
            plan.setIntent("create_playlist");
            plan.setActionTarget(createPlaylistTarget);
            plan.setSearchKeyword("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String deletePlaylistTarget = extractDeleteMyPlaylistTarget(userMessage);
        if (!deletePlaylistTarget.isEmpty()) {
            plan.setIntent("delete_my_playlist");
            plan.setActionTarget(deletePlaylistTarget);
            plan.setSearchKeyword("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String singerDetailTarget = extractSingerDetailTarget(userMessage);
        if (!singerDetailTarget.isEmpty()) {
            plan.setIntent("open_singer_detail");
            plan.setActionTarget(singerDetailTarget);
            plan.setSearchKeyword("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String songCommentTarget = extractSongCommentTarget(userMessage);
        if (!songCommentTarget.isEmpty()) {
            plan.setIntent("open_song_comment");
            plan.setSearchKeyword(songCommentTarget);
            plan.setActionTarget("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String directActionIntent = resolveDirectActionIntent(normalizedMessage);

        if (directActionIntent != null) {
            plan.setIntent(directActionIntent);
            plan.setSearchKeyword("");
            plan.setActionTarget("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        if (wantsLogout) {
            plan.setIntent("logout");
            plan.setSearchKeyword("");
            plan.setActionTarget("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        String keyword = normalizeSearchKeyword(userMessage, userMessage);
        boolean broadSongBrowse = isBroadSongBrowseRequest(userMessage, keyword);
        if (!keyword.isEmpty() && (looksLikeMusic || wantsPlay || keyword.length() <= 16)) {
            plan.setIntent(broadSongBrowse ? "search_song" : (wantsPlay ? "play_song" : "search_song"));
            plan.setSearchKeyword(keyword);
            plan.setShouldAutoPlay(!broadSongBrowse && wantsPlay);
            plan.setAllowImportIfMissing(!broadSongBrowse && wantsPlay);
            return plan;
        }

        if (normalizedMessage.contains("歌") || normalizedMessage.contains("音乐")) {
            plan.setIntent("search_song");
            plan.setSearchKeyword(keyword);
            plan.setActionTarget("");
            plan.setShouldAutoPlay(false);
            plan.setAllowImportIfMissing(false);
            return plan;
        }

        plan.setIntent("chat");
        plan.setSearchKeyword("");
        plan.setActionTarget("");
        plan.setShouldAutoPlay(false);
        plan.setAllowImportIfMissing(false);
        return plan;
    }

    private String resolveDirectActionIntent(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isEmpty()) {
            return null;
        }

        if (containsAnyNormalized(normalizedMessage, "取消收藏这首歌", "取消喜欢这首歌", "取消收藏当前歌曲", "把这首歌取消收藏", "不喜欢这首歌")) {
            return "unfavorite_current_song";
        }
        if (containsAnyNormalized(normalizedMessage, "收藏这首歌", "收藏当前歌曲", "喜欢这首歌", "把这首歌加入收藏")) {
            return "favorite_current_song";
        }
        if (containsAnyNormalized(normalizedMessage, "清空搜索历史", "清除搜索历史", "删除搜索历史", "清空搜索记录", "清除搜索记录", "删除搜索记录", "清空最近搜索", "清除最近搜索")) {
            return "clear_search_history";
        }
        if (containsAnyNormalized(normalizedMessage, "打开我收藏的歌单", "查看我收藏的歌单", "打开收藏的歌单", "查看收藏的歌单", "打开收藏歌单", "查看收藏歌单", "进入收藏歌单", "去收藏歌单")) {
            return "navigate_collection_playlists";
        }
        if (containsAnyNormalized(normalizedMessage, "把这首歌从这个歌单移除", "把当前歌曲从这个歌单移除", "把这首歌从当前歌单移除", "把当前歌曲从当前歌单移除", "把这首歌移出这个歌单", "把当前歌曲移出这个歌单", "把这首歌移出当前歌单", "把当前歌曲移出当前歌单")) {
            return "remove_current_song_from_playlist";
        }
        if (containsAnyNormalized(normalizedMessage, "生成一个推荐歌单", "创建一个推荐歌单", "做一个推荐歌单", "推荐并生成一个歌单", "推荐并创建一个歌单", "帮我生成一个推荐歌单", "帮我做一个推荐歌单", "直接生成一个推荐歌单")) {
            return "create_recommended_playlist";
        }
        if (containsAnyNormalized(normalizedMessage, "上一首", "上一曲", "上一歌")) {
            return "player_previous";
        }
        if (containsAnyNormalized(normalizedMessage, "下一首", "下一曲", "切歌", "换一首", "切到下一首")) {
            return "player_next";
        }
        if (containsAnyNormalized(normalizedMessage, "暂停播放", "暂停一下", "先暂停", "停止播放")) {
            return "player_pause";
        }
        if (containsAnyNormalized(normalizedMessage, "继续播放", "恢复播放", "接着播放", "接着听", "继续听")) {
            return "player_resume";
        }
        if (containsAnyNormalized(normalizedMessage, "打开歌词", "查看歌词", "打开歌词页", "去歌词页")) {
            return "open_lyric";
        }
        if (containsAnyNormalized(normalizedMessage, "切换暗黑模式", "切换深色模式", "切换夜间模式", "打开暗黑模式", "打开深色模式", "关闭暗黑模式", "切换亮色模式")) {
            return "toggle_dark_mode";
        }
        if (containsAnyNormalized(normalizedMessage, "取消收藏这个歌单", "取消收藏当前歌单", "不收藏这个歌单", "不喜欢这个歌单")) {
            return "unfavorite_playlist";
        }
        if (containsAnyNormalized(normalizedMessage, "收藏这个歌单", "收藏当前歌单", "喜欢这个歌单", "把这个歌单加入收藏")) {
            return "favorite_playlist";
        }
        if (containsAnyNormalized(normalizedMessage, "播放这个歌单", "播放当前歌单", "播放这张歌单", "听这个歌单")) {
            return "play_playlist";
        }
        if (containsAnyNormalized(normalizedMessage, "打开设置", "去设置", "进入设置") || "设置".equals(normalizedMessage)) {
            return "navigate_setting";
        }
        if (containsAnyNormalized(normalizedMessage, "打开歌手详情", "查看歌手详情", "打开歌手主页", "查看歌手资料", "打开歌手页")) {
            return "open_singer_detail";
        }
        if (containsAnyNormalized(normalizedMessage, "打开评论区", "查看评论", "进入评论区", "去评论区")) {
            return "open_song_comment";
        }
        if ((containsAnyNormalized(normalizedMessage, "个人中心", "个人主页", "个人信息") && hasCommandCue(normalizedMessage))
                || "个人中心".equals(normalizedMessage)
                || "个人主页".equals(normalizedMessage)
                || "个人信息".equals(normalizedMessage)) {
            return "navigate_personal";
        }
        if ((containsAnyNormalized(normalizedMessage, "我的歌单") && hasCommandCue(normalizedMessage)) || "我的歌单".equals(normalizedMessage)) {
            return "navigate_my_song_sheet";
        }
        if ((containsAnyNormalized(normalizedMessage, "收藏") && hasCommandCue(normalizedMessage)) || "我的收藏".equals(normalizedMessage) || "收藏".equals(normalizedMessage)) {
            return "navigate_collection";
        }
        if ((containsAnyNormalized(normalizedMessage, "排行榜") && hasCommandCue(normalizedMessage)) || "排行榜".equals(normalizedMessage)) {
            return "navigate_rank";
        }
        if ((containsAnyNormalized(normalizedMessage, "歌手") && hasCommandCue(normalizedMessage)) || "歌手".equals(normalizedMessage)) {
            return "navigate_singer";
        }
        if ((containsAnyNormalized(normalizedMessage, "歌单") && hasCommandCue(normalizedMessage)) || "歌单".equals(normalizedMessage)) {
            return "navigate_song_sheet";
        }
        if ((containsAnyNormalized(normalizedMessage, "首页", "主页") && hasCommandCue(normalizedMessage))
                || "首页".equals(normalizedMessage)
                || "主页".equals(normalizedMessage)) {
            return "navigate_home";
        }
        return null;
    }

    private boolean hasCommandCue(String normalizedMessage) {
        return containsAnyNormalized(normalizedMessage, "打开", "去", "进入", "查看", "看看", "切到", "跳到", "回到", "返回");
    }

    private boolean requiresActionTarget(String intent) {
        return "open_playlist_detail".equals(intent)
                || "add_current_song_to_playlist".equals(intent)
                || "create_playlist".equals(intent)
                || "add_song_to_playlist".equals(intent)
                || "delete_my_playlist".equals(intent);
    }

    private boolean isCreateRecommendedPlaylistRequest(String userMessage, String normalizedMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return false;
        }
        if (!containsAnyText(message, "推荐") || !containsAnyText(message, "歌单", "列表")) {
            return false;
        }
        return containsAnyText(message, "生成", "创建", "做", "做个", "做一张", "整一个", "帮我做", "帮我生成", "帮我创建")
                || containsAnyNormalized(normalizedMessage, "推荐并生成", "推荐并创建", "直接生成推荐歌单", "直接创建推荐歌单");
    }

    private boolean isExplicitRecommendedPlaylistCreationRequest(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return false;
        }
        String normalizedMessage = normalizeForCompare(message);
        return containsAnyText(message, "推荐")
                && containsAnyText(message, "歌单", "列表")
                && (containsAnyText(message, "生成", "创建", "做", "做个", "做一张", "整一个", "新建", "帮我做", "帮我生成", "帮我创建")
                || containsAnyNormalized(normalizedMessage, "推荐并生成", "推荐并创建", "直接生成推荐歌单", "直接创建推荐歌单"));
    }

    private boolean isSongRecommendationRequest(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return false;
        }
        String normalizedMessage = normalizeForCompare(message);
        if (!containsAnyText(message, "推荐")) {
            return false;
        }
        if (isExplicitRecommendedPlaylistCreationRequest(message)) {
            return false;
        }
        if (containsAnyText(message, "歌单", "列表") && !containsAnyText(message, "歌", "歌曲", "听")) {
            return false;
        }
        return containsAnyText(message, "歌", "歌曲", "几首", "几首歌", "听")
                || containsAnyNormalized(normalizedMessage, "适合夜晚听", "适合晚上听", "适合通勤听", "适合学习听", "适合睡前听", "适合开车听");
    }

    private boolean containsAnyNormalized(String normalizedMessage, String... keywords) {
        if (normalizedMessage == null || normalizedMessage.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (normalizedMessage.contains(normalizeForCompare(keyword))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyText(String message, String... keywords) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isEmpty() && message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String message, List<String> keywords) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeSearchKeyword(String candidate, String fallbackMessage) {
        String working = sanitizeMessage(candidate);
        if (working.isEmpty()) {
            working = sanitizeMessage(fallbackMessage);
        }
        if (working.isEmpty()) {
            return "";
        }

        String quoted = extractQuotedKeyword(working);
        if (!quoted.isEmpty()) {
            working = quoted;
        }

        for (String stopWord : STOP_WORDS) {
            working = working.replace(stopWord, " ");
        }
        working = working.replace("的", " ");
        working = working.replace("吧", " ");
        working = working.replace("吗", " ");
        working = working.replace("呀", " ");
        working = working.replace("呢", " ");
        working = working.replace("给我放", " ");
        working = working.replace("播放", " ");
        working = working.replace("想听", " ");
        working = working.replace("搜一下", " ");
        working = working.replace("找一下", " ");
        working = NON_WORD_SPLIT_PATTERN.matcher(working).replaceAll(" ");
        working = MULTI_SPACE_PATTERN.matcher(working).replaceAll(" ").trim();

        if (working.length() > 32) {
            List<String> tokens = tokenizeKeyword(working);
            if (!tokens.isEmpty()) {
                working = String.join(" ", tokens.subList(0, Math.min(tokens.size(), 3)));
            }
        }
        return working;
    }

    private String extractPlaylistDetailTarget(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "歌单", "列表") || !containsAnyText(message, "打开", "查看", "进入", "去", "跳到", "切到", "看看")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:打开|查看|进入|去|跳到|切到|看看)(?:一下|下)?(?:我的|我想看的|我想听的|名为)?(.+?)(?:歌单详情|歌单|列表)(?:页)?(?:吧)?$",
                ".*?(?:打开|查看|进入|去|跳到|切到|看看)(?:一下|下)?《(.+?)》(?!.*《)"
        );
        return normalizeActionTarget(extracted);
    }

    private String extractCreatePlaylistTarget(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "创建", "新建", "建一个", "建个", "帮我建", "帮我创建") || !containsAnyText(message, "歌单", "列表")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:创建|新建|建一个|建个|帮我建|帮我创建)(?:一个|个|张)?(.+?)(?:歌单|列表)(?:吧)?$",
                ".*?(?:创建|新建|建一个|建个|帮我建|帮我创建)(?:一个|个|张)?《(.+?)》(?!.*《)"
        );
        return normalizeActionTarget(extracted);
    }

    private String extractCreateRecommendedPlaylistTheme(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "推荐") || !containsAnyText(message, "歌单", "列表")) {
            return "";
        }
        if (!containsAnyText(message, "生成", "创建", "做", "做个", "做一张", "整一个", "帮我做", "帮我生成", "帮我创建")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:推荐并(?:生成|创建)|生成|创建|做|做个|做一张|整一个|帮我做|帮我生成|帮我创建)(?:一个|个|张)?(.+?)推荐(?:歌单|列表)(?:吧)?$",
                ".*?(?:推荐并(?:生成|创建)|生成|创建|做|做个|做一张|整一个|帮我做|帮我生成|帮我创建)(?:一个|个|张)?(.+?)(?:歌单|列表)(?:吧)?$"
        );
        return normalizePlaylistTheme(extracted);
    }

    private String extractFavoritePlaylistTarget(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "收藏", "喜欢", "加入收藏") || !containsAnyText(message, "歌单", "列表")) {
            return "";
        }
        if (containsAnyText(message, "取消收藏", "不收藏", "不喜欢")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:收藏|喜欢|加入收藏)(?:一下|下)?(?:我的|我)?(.+?)(?:歌单|列表)(?:吧)?$",
                ".*?(?:把|将)?《(.+?)》(?:歌单|列表)?(?:加入收藏|收藏)(?:吧)?$"
        );
        return normalizeActionTarget(extracted);
    }

    private String extractUnfavoritePlaylistTarget(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "取消收藏", "不收藏", "不喜欢") || !containsAnyText(message, "歌单", "列表")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:取消收藏|不收藏|不喜欢)(?:一下|下)?(?:我的|我)?(.+?)(?:歌单|列表)(?:吧)?$",
                ".*?(?:把|将)?《(.+?)》(?:歌单|列表)?(?:取消收藏|不收藏)(?:吧)?$"
        );
        return normalizeActionTarget(extracted);
    }

    private String extractPlayPlaylistTarget(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "播放", "放", "听") || !containsAnyText(message, "歌单", "列表")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:播放|放|听)(?:一下|下)?(?:我的|我)?(.+?)(?:歌单|列表)(?:吧)?$",
                ".*?(?:播放|放|听)(?:一下|下)?《(.+?)》(?!.*《)"
        );
        return normalizeActionTarget(extracted);
    }

    private String extractSingerDetailTarget(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "歌手", "详情", "主页", "资料") || !containsAnyText(message, "打开", "查看", "进入", "去", "跳到", "看看")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:打开|查看|进入|去|跳到|看看)(?:一下|下)?(.+?)(?:歌手详情|歌手页面|歌手页|歌手主页|歌手资料|歌手)(?:吧)?$",
                ".*?(?:打开|查看|进入|去|跳到|看看)(?:一下|下)?《(.+?)》(?:的)?(?:歌手详情|歌手页面|歌手页|歌手主页|歌手资料)?(?:吧)?$"
        );
        return normalizeSingerTarget(extracted);
    }

    private String extractAddToPlaylistTarget(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "歌单", "列表") || !containsAnyText(message, "加入", "加到", "加进", "添加到", "添加进", "放到", "放进", "存到", "收藏到")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:加入|加到|加进|添加到|添加进|放到|放进|存到|收藏到)(?:我的|我)?(.+?)(?:歌单|列表)(?:里|里面|中)?(?:吧)?$",
                ".*?(?:把|将)?.*?(?:加入|加到|加进|添加到|添加进|放到|放进|存到|收藏到)(?:我的|我)?《(.+?)》(?!.*《)"
        );
        return normalizeActionTarget(extracted);
    }

    private SongPlaylistActionParts extractSpecificSongPlaylistAction(String userMessage) {
        SongPlaylistActionParts parts = new SongPlaylistActionParts();
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return parts;
        }
        if (!containsAnyText(message, "歌单", "列表") || !containsAnyText(message, "加入", "加到", "加进", "添加到", "添加进", "放到", "放进", "存到", "收藏到")) {
            return parts;
        }
        if (containsAnyText(message, "这首歌", "当前歌曲", "当前这首歌", "正在播放")) {
            return parts;
        }

        String[] extracted = extractSongPlaylistPartsByPatterns(message,
                ".*?(?:把|将)?(.+?)(?:加入|加到|加进|添加到|添加进|放到|放进|存到|收藏到)(?:我的|我)?(.+?)(?:歌单|列表)(?:里|里面|中)?(?:吧)?$",
                ".*?(?:把|将)?《(.+?)》(?:加入|加到|加进|添加到|添加进|放到|放进|存到|收藏到)(?:我的|我)?《(.+?)》(?:里|里面|中)?(?:吧)?$"
        );
        if (extracted == null) {
            return parts;
        }

        String songKeyword = normalizeSearchKeyword(extracted[0], extracted[0]);
        String playlistTitle = normalizeActionTarget(extracted[1]);
        if (songKeyword.isEmpty() || playlistTitle.isEmpty()) {
            return parts;
        }
        parts.setSongKeyword(songKeyword);
        parts.setPlaylistTitle(playlistTitle);
        return parts;
    }

    private String extractRemoveCurrentSongFromPlaylistTarget(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "移除", "删掉", "删除", "移出") || !containsAnyText(message, "歌单", "列表")) {
            return "";
        }
        if (!containsAnyText(message, "这首歌", "当前歌曲", "当前这首歌", "正在播放")) {
            return "";
        }
        if (containsAnyText(message, "这个歌单", "当前歌单", "本歌单")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:把|将)?(?:这首歌|当前歌曲|当前这首歌|正在播放的歌)(?:从|在)?(?:我的|我)?(.+?)(?:歌单|列表)(?:里|里面|中)?(?:移除|删掉|删除|移出)(?:吧)?$",
                ".*?(?:从|在)(?:我的|我)?(.+?)(?:歌单|列表)(?:里|里面|中)?(?:把|将)?(?:这首歌|当前歌曲|当前这首歌|正在播放的歌)(?:移除|删掉|删除|移出)(?:吧)?$"
        );
        return normalizeActionTarget(extracted);
    }

    private String extractSongCommentTarget(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "评论区", "评论") || !containsAnyText(message, "打开", "查看", "进入", "去", "看看")) {
            return "";
        }
        if (containsAnyText(message, "这首歌", "当前歌曲", "当前这首歌", "正在播放")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:打开|查看|进入|去|看看)(?:一下|下)?(.+?)(?:的)?评论区(?:吧)?$",
                ".*?(?:打开|查看|进入|去|看看)(?:一下|下)?《(.+?)》(?:的)?评论区(?:吧)?$",
                ".*?(?:打开|查看|进入|去|看看)(?:一下|下)?(.+?)(?:的)?评论(?:吧)?$"
        );
        return normalizeSearchKeyword(extracted, extracted);
    }

    private String extractDeleteMyPlaylistTarget(String userMessage) {
        String message = sanitizeMessage(userMessage);
        if (message.isEmpty()) {
            return "";
        }
        if (!containsAnyText(message, "删除", "删掉", "移除") || !containsAnyText(message, "歌单", "列表")) {
            return "";
        }
        String extracted = extractByPatterns(message,
                ".*?(?:删除|删掉|移除)(?:我的|我)?(.+?)(?:歌单|列表)(?:吧)?$",
                ".*?(?:删除|删掉|移除)(?:我的|我)?《(.+?)》(?!.*《)"
        );
        return normalizeActionTarget(extracted);
    }

    private String extractByPatterns(String message, String... regexes) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        for (String regex : regexes) {
            Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(message);
            if (matcher.matches()) {
                for (int index = 1; index <= matcher.groupCount(); index++) {
                    String matched = sanitizeMessage(matcher.group(index));
                    if (!matched.isEmpty()) {
                        return matched;
                    }
                }
            }
        }
        return "";
    }

    private String[] extractSongPlaylistPartsByPatterns(String message, String... regexes) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        for (String regex : regexes) {
            Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(message);
            if (matcher.matches() && matcher.groupCount() >= 2) {
                String left = sanitizeMessage(matcher.group(1));
                String right = sanitizeMessage(matcher.group(2));
                if (!left.isEmpty() && !right.isEmpty()) {
                    return new String[]{left, right};
                }
            }
        }
        return null;
    }

    private String normalizeActionTarget(String candidate) {
        String working = sanitizeMessage(candidate);
        if (working.isEmpty()) {
            return "";
        }

        String quoted = extractQuotedKeyword(working);
        if (!quoted.isEmpty()) {
            working = quoted;
        }

        working = working.replace("歌单详情", " ");
        working = working.replace("详情页", " ");
        working = working.replace("歌单", " ");
        working = working.replace("列表", " ");
        working = working.replace("页面", " ");
        working = working.replace("页", " ");
        working = working.replace("打开", " ");
        working = working.replace("查看", " ");
        working = working.replace("进入", " ");
        working = working.replace("跳到", " ");
        working = working.replace("切到", " ");
        working = working.replace("看看", " ");
        working = working.replace("加入", " ");
        working = working.replace("加到", " ");
        working = working.replace("加进", " ");
        working = working.replace("添加到", " ");
        working = working.replace("添加进", " ");
        working = working.replace("放到", " ");
        working = working.replace("放进", " ");
        working = working.replace("存到", " ");
        working = working.replace("收藏到", " ");
        working = working.replace("当前歌曲", " ");
        working = working.replace("当前这首歌", " ");
        working = working.replace("这首歌", " ");
        working = working.replace("这首", " ");
        working = working.replace("歌曲", " ");
        working = MULTI_SPACE_PATTERN.matcher(working).replaceAll(" ").trim();

        String normalized = normalizeForCompare(working);
        if (normalized.isEmpty()
                || "歌单".equals(normalized)
                || "列表".equals(normalized)
                || "我的".equals(normalized)
                || "全部".equals(normalized)) {
            return "";
        }
        return working;
    }

    private String normalizePlaylistTheme(String candidate) {
        String working = sanitizeMessage(candidate);
        if (working.isEmpty()) {
            return "";
        }

        String quoted = extractQuotedKeyword(working);
        if (!quoted.isEmpty()) {
            working = quoted;
        }

        working = working.replace("推荐歌单", " ");
        working = working.replace("推荐列表", " ");
        working = working.replace("歌单", " ");
        working = working.replace("列表", " ");
        working = working.replace("推荐", " ");
        working = working.replace("生成", " ");
        working = working.replace("创建", " ");
        working = working.replace("做一个", " ");
        working = working.replace("做个", " ");
        working = working.replace("做一张", " ");
        working = working.replace("整一个", " ");
        working = working.replace("帮我做", " ");
        working = working.replace("帮我生成", " ");
        working = working.replace("帮我创建", " ");
        working = working.replace("直接", " ");
        working = working.replace("一个", " ");
        working = working.replace("一张", " ");
        working = working.replace("个", " ");
        working = working.replace("张", " ");
        working = working.replace("新的", " ");
        working = working.replace("新", " ");
        working = working.replace("吧", " ");
        working = MULTI_SPACE_PATTERN.matcher(working).replaceAll(" ").trim();

        String normalized = normalizeForCompare(working);
        if (normalized.isEmpty()
                || "推荐".equals(normalized)
                || "歌单".equals(normalized)
                || "列表".equals(normalized)
                || "新".equals(normalized)
                || "新的".equals(normalized)) {
            return "";
        }
        return working;
    }

    private String normalizeSingerTarget(String candidate) {
        String working = sanitizeMessage(candidate);
        if (working.isEmpty()) {
            return "";
        }

        String quoted = extractQuotedKeyword(working);
        if (!quoted.isEmpty()) {
            working = quoted;
        }

        working = working.replace("歌手详情", " ");
        working = working.replace("歌手页面", " ");
        working = working.replace("歌手主页", " ");
        working = working.replace("歌手资料", " ");
        working = working.replace("歌手页", " ");
        working = working.replace("歌手", " ");
        working = working.replace("详情", " ");
        working = working.replace("主页", " ");
        working = working.replace("资料", " ");
        working = working.replace("打开", " ");
        working = working.replace("查看", " ");
        working = working.replace("进入", " ");
        working = working.replace("跳到", " ");
        working = working.replace("看看", " ");
        working = MULTI_SPACE_PATTERN.matcher(working).replaceAll(" ").trim();

        String normalized = normalizeForCompare(working);
        if (normalized.isEmpty()) {
            return "";
        }
        return working;
    }

    private List<String> tokenizeKeyword(String keyword) {
        String normalized = normalizeSearchKeyword(keyword, keyword);
        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }
        String[] rawTokens = normalized.split("\\s+");
        Map<String, Integer> distinctTokens = new LinkedHashMap<>();
        for (String rawToken : rawTokens) {
            String token = normalizeForCompare(rawToken);
            if (token.isEmpty()) {
                continue;
            }
            if (!distinctTokens.containsKey(token)) {
                distinctTokens.put(token, token.length());
            }
        }
        return new ArrayList<>(distinctTokens.keySet());
    }

    private String extractQuotedKeyword(String message) {
        Matcher matcher = Pattern.compile("[“\"'《](.+?)[”\"'》]").matcher(message);
        if (matcher.find()) {
            return sanitizeMessage(matcher.group(1));
        }
        return "";
    }

    private String normalizeForCompare(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace("的", "")
                .replace(" ", "")
                .replace("　", "")
                .replaceAll("[\\p{Punct}，。！？；：、“”‘’（）《》【】]", "")
                .trim();
    }

    private AssistantSongPayload toSongPayload(Song song) {
        if (song == null) {
            return null;
        }
        AssistantSongPayload payload = new AssistantSongPayload();
        payload.setId(song.getId());
        payload.setName(song.getName());
        payload.setSingerName(extractSingerName(song.getName()));
        payload.setSongTitle(extractSongTitle(song.getName()));
        payload.setIntroduction(song.getIntroduction());
        payload.setPic(song.getPic());
        payload.setLyric(song.getLyric());
        payload.setUrl(song.getUrl());
        payload.setType(song.getType());
        return payload;
    }

    private String extractSingerName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "未知歌手";
        }
        int index = name.indexOf("-");
        if (index <= 0) {
            return name.trim();
        }
        return name.substring(0, index).trim();
    }

    private String extractSongTitle(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "未知歌曲";
        }
        int index = name.indexOf("-");
        if (index < 0 || index == name.length() - 1) {
            return name.trim();
        }
        return name.substring(index + 1).trim();
    }

    private String sanitizeMessage(String message) {
        if (message == null) {
            return "";
        }
        String sanitized = message.replace('\u0000', ' ')
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
        sanitized = MULTI_SPACE_PATTERN.matcher(sanitized).replaceAll(" ");
        if (sanitized.length() > 600) {
            return sanitized.substring(0, 600).trim();
        }
        return sanitized;
    }

    private List<AssistantHistoryMessage> sanitizeHistory(List<AssistantHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        List<AssistantHistoryMessage> sanitized = new ArrayList<>();
        for (AssistantHistoryMessage item : history) {
            if (item == null) {
                continue;
            }
            AssistantHistoryMessage copy = new AssistantHistoryMessage();
            copy.setRole(Objects.toString(item.getRole(), "").trim().toLowerCase(Locale.ROOT));
            copy.setContent(sanitizeMessage(item.getContent()));
            if (copy.getContent().isEmpty()) {
                continue;
            }
            sanitized.add(copy);
        }
        if (sanitized.size() <= 12) {
            return sanitized;
        }
        return new ArrayList<>(sanitized.subList(sanitized.size() - 12, sanitized.size()));
    }

    private String sanitizeAssistantReply(String reply) {
        String sanitized = sanitizeMessage(reply);
        if (sanitized.isEmpty()) {
            return "我在这儿，你可以继续和我聊天，或者直接告诉我歌手名和歌名。";
        }
        return sanitized;
    }

    private JSONObject parseLooseJsonObject(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return null;
        }
        String candidate = CODE_BLOCK_PATTERN.matcher(rawText.trim()).replaceAll("").trim();
        try {
            return JSON.parseObject(candidate);
        } catch (Exception ignored) {
        }
        return parseLastJsonObject(candidate);
    }

    private JSONObject parseLastJsonObject(String output) {
        if (output == null || output.trim().isEmpty()) {
            return null;
        }
        for (int index = output.lastIndexOf('{'); index >= 0; index = output.lastIndexOf('{', index - 1)) {
            String candidate = output.substring(index).trim();
            try {
                return JSON.parseObject(candidate);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private JSONArray parseLastJsonArray(String output) {
        if (output == null || output.trim().isEmpty()) {
            return null;
        }
        for (int index = output.lastIndexOf('['); index >= 0; index = output.lastIndexOf('[', index - 1)) {
            String candidate = output.substring(index).trim();
            try {
                return JSON.parseArray(candidate);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Data
    private static class IntentPlan {
        private String intent = "chat";
        private String searchKeyword = "";
        private String actionTarget = "";
        private boolean shouldAutoPlay;
        private boolean allowImportIfMissing;
        private String originalMessage = "";
    }

    @Data
    private static class AssistantExecution {
        private String action;
        private String searchKeyword;
        private String actionTarget;
        private Boolean usedImport;
        private Song song;
        private List<Song> candidates = new ArrayList<>();
        private String errorMessage;
        private String replyMessage;
    }

    @Data
    private static class RankedSong {
        private final Song song;
        private final int score;
    }

    @Data
    private static class ImportBridgeResult {
        private Song song;
        private String errorMessage;
    }

    @Data
    private static class RemoteSongSuggestion {
        private String title;
        private String artist;
        private String source;
    }

    @Data
    private static class RemoteSearchBridgeResult {
        private List<RemoteSongSuggestion> suggestions = new ArrayList<>();
        private String errorMessage;
    }

    @Data
    private static class RecommendationCandidatePool {
        private List<Song> songs = new ArrayList<>();
        private boolean personalized;
    }

    @Data
    private static class SongRecommendationResult {
        private List<Song> songs = new ArrayList<>();
        private boolean personalized;
        private boolean modelSelected;
    }

    @Data
    private static class ConfirmationProposal {
        private String intent = "";
        private String searchKeyword = "";
        private String actionTarget = "";
        private boolean shouldAutoPlay;
        private boolean allowImportIfMissing;
        private String originalMessage = "";
        private String prompt = "";
        private double confidence;
    }

    @Data
    private static class PendingConfirmationResolution {
        private boolean confirmed;
        private boolean cancelled;
        private IntentPlan plan;
    }

    @Data
    private static class AssistantSessionState implements Serializable {
        private String lastRecommendationRequest = "";
        private long lastRecommendationAt;
        private PendingConfirmation pendingConfirmation;
    }

    @Data
    private static class PendingConfirmation implements Serializable {
        private String intent = "";
        private String searchKeyword = "";
        private String actionTarget = "";
        private boolean shouldAutoPlay;
        private boolean allowImportIfMissing;
        private String originalMessage = "";
        private String prompt = "";
        private double confidence;
        private long createdAt;
    }

    @Data
    private static class SongPlaylistActionParts {
        private String songKeyword = "";
        private String playlistTitle = "";
    }

    private static class StreamReader implements Callable<String> {
        private final InputStream inputStream;

        private StreamReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public String call() throws Exception {
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            }
            return builder.toString();
        }
    }
}
