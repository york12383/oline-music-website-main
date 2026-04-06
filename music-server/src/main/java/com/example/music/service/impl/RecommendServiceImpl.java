package com.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.music.mapper.CollectMapper;
import com.example.music.mapper.CommentMapper;
import com.example.music.mapper.ConsumerRecommendSeedMapper;
import com.example.music.mapper.ListSongMapper;
import com.example.music.mapper.PlayHistoryMapper;
import com.example.music.mapper.RankListMapper;
import com.example.music.mapper.RankSongMapper;
import com.example.music.mapper.SingerMapper;
import com.example.music.mapper.SongListMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.model.domain.Collect;
import com.example.music.model.domain.Comment;
import com.example.music.model.domain.ConsumerRecommendSeed;
import com.example.music.model.domain.ListSong;
import com.example.music.model.domain.PlayHistory;
import com.example.music.model.domain.RankList;
import com.example.music.model.domain.RankSong;
import com.example.music.model.domain.Singer;
import com.example.music.model.domain.Song;
import com.example.music.model.domain.SongList;
import com.example.music.model.request.RecommendBootstrapRequest;
import com.example.music.service.RecommendService;
import com.example.music.utils.RecommendUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int TOP_NEIGHBOR_COUNT = 20;
    private static final int ITEM_CF_LIMIT = 30;
    private static final int MIN_OVERLAP = 2;
    private static final double SHRINKAGE = 8.0;

    private static final double PLAY_WEIGHT = 1.0;
    private static final double COLLECT_WEIGHT = 3.0;
    private static final double COMMENT_WEIGHT = 2.0;
    private static final double RATING_WEIGHT = 0.7;

    private static final RecommendWeights STABLE_WEIGHTS = new RecommendWeights(0.40, 0.25, 0.20, 0.15, "stable");
    private static final RecommendWeights SPARSE_WEIGHTS = new RecommendWeights(0.20, 0.25, 0.35, 0.20, "sparse");
    private static final RecommendWeights COLD_WEIGHTS = new RecommendWeights(0.00, 0.20, 0.45, 0.35, "cold");

    private static final int STABLE_SIGNAL_THRESHOLD = 6;
    private static final int BOOTSTRAP_TRIGGER_THRESHOLD = 3;

    private static final Pattern CHINESE_SEGMENT = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");
    private static final Pattern LATIN_SEGMENT = Pattern.compile("[a-zA-Z0-9]{3,}");

    @Resource
    private SongMapper songMapper;
    @Resource
    private SongListMapper songListMapper;
    @Resource
    private SingerMapper singerMapper;
    @Resource
    private ListSongMapper listSongMapper;
    @Resource
    private CollectMapper collectMapper;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private RankSongMapper rankSongMapper;
    @Resource
    private RankListMapper rankListMapper;
    @Resource
    private PlayHistoryMapper playHistoryMapper;
    @Resource
    private ConsumerRecommendSeedMapper consumerRecommendSeedMapper;

    @Override
    public List<Song> selectListByRecommend(Integer userId, Integer limit) {
        int size = normalizeLimit(limit);
        SongRecommendContext context = buildSongRecommendContext();
        RecommendProfile profile = buildSongProfile(userId, context);
        Map<Integer, Double> userCfScores = buildUserCfScores(userId, context.userVectors, profile.interactedItemIds);
        Map<Integer, Double> itemCfScores = buildSongItemCfScores(context, profile);
        Map<Integer, Double> contentScores = buildSongContentScores(context, profile);
        Map<Integer, Double> fallbackScores = buildSongFallbackScores(context, profile);
        List<RecommendCandidate> ranked = buildSongCandidates(context, profile, userCfScores, itemCfScores, contentScores, fallbackScores);
        ranked = rerankSongCandidates(ranked, size);
        if (ranked.isEmpty()) {
            ranked = fallbackSongCandidates(context, size);
        }

        List<Song> result = new ArrayList<>();
        for (RecommendCandidate candidate : ranked.subList(0, Math.min(size, ranked.size()))) {
            Song song = copySong(context.songById.get(candidate.id));
            applyReason(song, candidate);
            result.add(song);
        }
        return result;
    }

    @Override
    public List<SongList> selectSongListsByRecommend(Integer userId, Integer limit) {
        int size = normalizeLimit(limit);
        SongListRecommendContext context = buildSongListRecommendContext();
        RecommendProfile profile = buildSongListProfile(userId, context);
        Map<Integer, Double> userCfScores = buildUserCfScores(userId, context.userVectors, profile.interactedItemIds);
        Map<Integer, Double> itemCfScores = buildSongListItemCfScores(context, profile);
        Map<Integer, Double> contentScores = buildSongListContentScores(context, profile);
        Map<Integer, Double> fallbackScores = buildSongListFallbackScores(context, profile);
        List<RecommendCandidate> ranked = buildSongListCandidates(context, profile, userCfScores, itemCfScores, contentScores, fallbackScores);
        ranked = rerankSongListCandidates(ranked, size);
        if (ranked.isEmpty()) {
            ranked = fallbackSongListCandidates(context, size);
        }

        List<SongList> result = new ArrayList<>();
        for (RecommendCandidate candidate : ranked.subList(0, Math.min(size, ranked.size()))) {
            SongList songList = copySongList(context.songListById.get(candidate.id));
            applyReason(songList, candidate);
            result.add(songList);
        }
        return result;
    }

    @Override
    public List<Singer> selectSingersByRecommend(Integer userId, Integer limit) {
        int size = normalizeLimit(limit);
        SingerRecommendContext context = buildSingerRecommendContext();
        RecommendProfile profile = buildSingerProfile(userId, context);
        Map<Integer, Double> userCfScores = buildUserCfScores(userId, context.userVectors, profile.interactedItemIds);
        Map<Integer, Double> itemCfScores = buildSingerItemCfScores(context, profile);
        Map<Integer, Double> contentScores = buildSingerContentScores(context, profile);
        Map<Integer, Double> fallbackScores = buildSingerFallbackScores(context, profile);
        List<RecommendCandidate> ranked = buildSingerCandidates(context, profile, userCfScores, itemCfScores, contentScores, fallbackScores);
        if (ranked.isEmpty()) {
            ranked = fallbackSingerCandidates(context, size);
        }

        List<Singer> result = new ArrayList<>();
        for (RecommendCandidate candidate : ranked.subList(0, Math.min(size, ranked.size()))) {
            Singer singer = copySinger(context.singerById.get(candidate.id));
            applyReason(singer, candidate);
            result.add(singer);
        }
        return result;
    }

    @Override
    public Map<String, Object> getBootstrapOptions(Integer userId) {
        SongRecommendContext songContext = buildSongRecommendContext();
        SongListRecommendContext songListContext = buildSongListRecommendContext();
        SingerRecommendContext singerContext = buildSingerRecommendContext();
        RecommendProfile profile = buildSongProfile(userId, songContext);
        List<ConsumerRecommendSeed> seeds = getSeedsByUserId(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("needsBootstrap", profile.needsBootstrap);
        result.put("stage", profile.stage);
        result.put("weights", profile.weights.toMap());
        result.put("singers", fallbackSingerEntities(singerContext, 12));
        result.put("songs", fallbackSongEntities(songContext, 12));
        result.put("styles", buildTopStyles(songListContext, 12));
        result.put("selectedSingerIds", seeds.stream()
                .filter(seed -> "SINGER".equals(seed.getSeedType()) && seed.getRefId() != null)
                .map(ConsumerRecommendSeed::getRefId)
                .distinct()
                .collect(Collectors.toList()));
        result.put("selectedSongIds", seeds.stream()
                .filter(seed -> "SONG".equals(seed.getSeedType()) && seed.getRefId() != null)
                .map(ConsumerRecommendSeed::getRefId)
                .distinct()
                .collect(Collectors.toList()));
        result.put("selectedStyles", seeds.stream()
                .filter(seed -> "SONG_LIST_STYLE".equals(seed.getSeedType()) && StringUtils.isNotBlank(seed.getRefValue()))
                .map(ConsumerRecommendSeed::getRefValue)
                .distinct()
                .collect(Collectors.toList()));
        return result;
    }

    @Override
    public void saveBootstrapPreferences(Integer userId, RecommendBootstrapRequest request) {
        if (userId == null || userId <= 0) {
            return;
        }
        consumerRecommendSeedMapper.delete(new QueryWrapper<ConsumerRecommendSeed>().eq("user_id", userId));
        if (request == null) {
            return;
        }
        List<ConsumerRecommendSeed> seeds = new ArrayList<>();
        seeds.addAll(buildSingerSeeds(userId, request.getLikedSingerIds()));
        seeds.addAll(buildSongSeeds(userId, request.getLikedSongIds()));
        seeds.addAll(buildStyleSeeds(userId, request.getLikedStyles()));
        for (ConsumerRecommendSeed seed : seeds) {
            consumerRecommendSeedMapper.insert(seed);
        }
    }

    @Override
    public Map<String, Object> getRecommendDebugProfile(Integer userId) {
        SongRecommendContext songContext = buildSongRecommendContext();
        SongListRecommendContext songListContext = buildSongListRecommendContext();
        SingerRecommendContext singerContext = buildSingerRecommendContext();

        RecommendProfile songProfile = buildSongProfile(userId, songContext);
        RecommendProfile songListProfile = buildSongListProfile(userId, songListContext);
        RecommendProfile singerProfile = buildSingerProfile(userId, singerContext);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("songProfile", buildProfileDebug(songProfile, songContext.songById, songContext.singerById));
        result.put("songListProfile", buildProfileDebug(songListProfile, songListContext.songListById, singerContext.singerById));
        result.put("singerProfile", buildProfileDebug(singerProfile, singerContext.singerById, singerContext.singerById));
        return result;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, 100);
    }

    private Map<String, Object> buildProfileDebug(RecommendProfile profile,
                                                  Map<Integer, ?> primaryMap,
                                                  Map<Integer, Singer> singerMap) {
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("stage", profile.stage);
        debug.put("needsBootstrap", profile.needsBootstrap);
        debug.put("weights", profile.weights.toMap());
        debug.put("interactionCount", profile.interactionCount);
        debug.put("seedCount", profile.seedCount);
        debug.put("topItems", describeTopIntegerWeights(profile.itemWeights, primaryMap, 6));
        debug.put("topSingers", describeTopIntegerWeights(profile.singerWeights, singerMap, 6));
        debug.put("topStyles", topStringWeights(profile.styleWeights, 6));
        debug.put("topKeywords", topStringWeights(profile.keywordWeights, 8));
        return debug;
    }

    private SongRecommendContext buildSongRecommendContext() {
        SongRecommendContext context = new SongRecommendContext();
        List<Song> activeSongs = songMapper.selectList(new QueryWrapper<Song>().eq("type", 1));
        context.activeSongs = activeSongs;
        context.songById = activeSongs.stream().collect(Collectors.toMap(Song::getId, song -> song));

        List<Singer> singers = singerMapper.selectList(null);
        context.singerById = singers.stream().collect(Collectors.toMap(Singer::getId, singer -> singer));

        List<SongList> activeSongLists = songListMapper.selectList(new QueryWrapper<SongList>().eq("type", 1));
        context.songListById = activeSongLists.stream().collect(Collectors.toMap(SongList::getId, songList -> songList));

        List<ListSong> listSongs = listSongMapper.selectList(null);
        for (ListSong listSong : listSongs) {
            if (listSong.getSongId() == null || listSong.getSongListId() == null) {
                continue;
            }
            SongList songList = context.songListById.get(listSong.getSongListId());
            Song song = context.songById.get(listSong.getSongId());
            if (songList == null || song == null) {
                continue;
            }
            context.listIdsBySongId.computeIfAbsent(song.getId(), key -> new LinkedHashSet<>()).add(songList.getId());
            context.songIdsByListId.computeIfAbsent(songList.getId(), key -> new LinkedHashSet<>()).add(song.getId());
        }

        for (Song song : activeSongs) {
            SongFeature feature = new SongFeature();
            feature.songId = song.getId();
            feature.singerId = song.getSingerId();
            feature.createTime = song.getCreateTime();
            feature.keywords.addAll(extractKeywords(song.getName()));
            feature.keywords.addAll(extractKeywords(song.getIntroduction()));
            Set<Integer> listIds = context.listIdsBySongId.getOrDefault(song.getId(), Collections.emptySet());
            for (Integer listId : listIds) {
                SongList songList = context.songListById.get(listId);
                if (songList != null) {
                    feature.styles.addAll(splitStyles(songList.getStyle()));
                }
            }
            if (feature.styles.isEmpty()) {
                feature.styles.add("未分类");
            }
            context.featureBySongId.put(song.getId(), feature);
        }

        List<PlayHistory> playHistories = playHistoryMapper.selectList(null);
        for (PlayHistory playHistory : playHistories) {
            Song song = context.songById.get(playHistory.getSongId());
            if (song == null) {
                continue;
            }
            double value = Math.log1p(1) * PLAY_WEIGHT * calculateTimeDecay(playHistory.getPlayTime());
            context.userVectors.computeIfAbsent(playHistory.getUserId(), key -> new HashMap<>())
                    .merge(song.getId(), value, Double::sum);
            context.userPlayCountBySong.computeIfAbsent(playHistory.getUserId(), key -> new HashMap<>())
                    .merge(song.getId(), 1, Integer::sum);
            context.globalPlayCountBySong.merge(song.getId(), 1, Integer::sum);
        }

        List<Collect> collects = collectMapper.selectList(null);
        for (Collect collect : collects) {
            if (collect.getType() == null || collect.getType() != 0 || collect.getSongId() == null) {
                continue;
            }
            Song song = context.songById.get(collect.getSongId());
            if (song == null) {
                continue;
            }
            double value = COLLECT_WEIGHT * calculateTimeDecay(collect.getCreateTime());
            context.userVectors.computeIfAbsent(collect.getUserId(), key -> new HashMap<>())
                    .merge(song.getId(), value, Double::sum);
            context.globalCollectCountBySong.merge(song.getId(), 1, Integer::sum);
            context.collectUserSongPairs.add(buildPairKey(collect.getUserId(), song.getId()));
        }

        List<Comment> comments = commentMapper.selectList(null);
        for (Comment comment : comments) {
            if (comment.getSongId() == null) {
                continue;
            }
            Song song = context.songById.get(comment.getSongId());
            if (song == null) {
                continue;
            }
            double value = COMMENT_WEIGHT * calculateTimeDecay(comment.getCreateTime());
            context.userVectors.computeIfAbsent(comment.getUserId(), key -> new HashMap<>())
                    .merge(song.getId(), value, Double::sum);
            context.globalCommentCountBySong.merge(song.getId(), 1, Integer::sum);
        }

        List<RankSong> rankSongs = rankSongMapper.selectList(null);
        for (RankSong rankSong : rankSongs) {
            if (rankSong.getSongId() == null || rankSong.getConsumerId() == null) {
                continue;
            }
            Song song = context.songById.get(rankSong.getSongId());
            if (song == null) {
                continue;
            }
            double value = rankSong.getScore() * RATING_WEIGHT;
            context.userVectors.computeIfAbsent(rankSong.getConsumerId(), key -> new HashMap<>())
                    .merge(song.getId(), value, Double::sum);
            context.globalRatingSumBySong.merge(song.getId(), rankSong.getScore().doubleValue(), Double::sum);
            context.globalRatingCountBySong.merge(song.getId(), 1, Integer::sum);
            context.ratedUserSongPairs.add(buildPairKey(rankSong.getConsumerId(), song.getId()));
        }

        context.userVectors = pruneEmptyVectors(context.userVectors);
        context.itemVectors = RecommendUtils.transposeUserVectors(context.userVectors);
        context.hotScoreBySong = RecommendUtils.normalizeScores(calculateSongHotScores(context));
        return context;
    }

    private SongListRecommendContext buildSongListRecommendContext() {
        SongRecommendContext songContext = buildSongRecommendContext();
        SongListRecommendContext context = new SongListRecommendContext();
        context.songById = songContext.songById;
        context.singerById = songContext.singerById;
        context.songListById = songContext.songListById;
        context.songIdsByListId = songContext.songIdsByListId;

        for (SongList songList : context.songListById.values()) {
            SongListFeature feature = new SongListFeature();
            feature.songListId = songList.getId();
            feature.primaryStyle = normalizePrimaryStyle(songList.getStyle());
            feature.styles.addAll(splitStyles(songList.getStyle()));
            feature.keywords.addAll(extractKeywords(songList.getTitle()));
            feature.keywords.addAll(extractKeywords(songList.getIntroduction()));
            feature.songIds.addAll(context.songIdsByListId.getOrDefault(songList.getId(), Collections.emptySet()));
            for (Integer songId : feature.songIds) {
                Song song = context.songById.get(songId);
                if (song != null && song.getSingerId() != null) {
                    feature.singerIds.add(song.getSingerId());
                }
            }
            if (feature.primaryStyle == null) {
                feature.primaryStyle = "未分类";
            }
            context.featureBySongListId.put(songList.getId(), feature);
        }

        List<Collect> collects = collectMapper.selectList(null);
        for (Collect collect : collects) {
            if (collect.getType() == null || collect.getType() != 1 || collect.getSongListId() == null) {
                continue;
            }
            SongList songList = context.songListById.get(collect.getSongListId());
            if (songList == null) {
                continue;
            }
            double value = COLLECT_WEIGHT * calculateTimeDecay(collect.getCreateTime());
            context.userVectors.computeIfAbsent(collect.getUserId(), key -> new HashMap<>())
                    .merge(songList.getId(), value, Double::sum);
            context.globalCollectCountByList.merge(songList.getId(), 1, Integer::sum);
            context.collectUserListPairs.add(buildPairKey(collect.getUserId(), songList.getId()));
        }

        List<Comment> comments = commentMapper.selectList(null);
        for (Comment comment : comments) {
            if (comment.getSongListId() == null) {
                continue;
            }
            Integer listId = comment.getSongListId();
            SongList songList = context.songListById.get(listId);
            if (songList == null) {
                continue;
            }
            double value = COMMENT_WEIGHT * calculateTimeDecay(comment.getCreateTime());
            context.userVectors.computeIfAbsent(comment.getUserId(), key -> new HashMap<>())
                    .merge(listId, value, Double::sum);
            context.globalCommentCountByList.merge(listId, 1, Integer::sum);
        }

        List<RankList> rankLists = rankListMapper.selectList(null);
        for (RankList rankList : rankLists) {
            if (rankList.getSongListId() == null || rankList.getConsumerId() == null) {
                continue;
            }
            Integer listId = rankList.getSongListId().intValue();
            SongList songList = context.songListById.get(listId);
            if (songList == null) {
                continue;
            }
            double value = rankList.getScore() * RATING_WEIGHT;
            Integer userId = rankList.getConsumerId().intValue();
            context.userVectors.computeIfAbsent(userId, key -> new HashMap<>())
                    .merge(listId, value, Double::sum);
            context.globalRatingSumByList.merge(listId, rankList.getScore().doubleValue(), Double::sum);
            context.globalRatingCountByList.merge(listId, 1, Integer::sum);
            context.ratedUserListPairs.add(buildPairKey(userId, listId));
        }

        for (Map.Entry<Integer, Map<Integer, Double>> userSongEntry : songContext.userVectors.entrySet()) {
            Integer userId = userSongEntry.getKey();
            for (Map.Entry<Integer, Double> songEntry : userSongEntry.getValue().entrySet()) {
                Set<Integer> listIds = songContext.listIdsBySongId.get(songEntry.getKey());
                if (listIds == null || listIds.isEmpty()) {
                    continue;
                }
                double distributed = songEntry.getValue() * 0.35;
                for (Integer listId : listIds) {
                    if (context.songListById.containsKey(listId)) {
                        context.userVectors.computeIfAbsent(userId, key -> new HashMap<>())
                                .merge(listId, distributed, Double::sum);
                    }
                }
            }
        }

        List<PlayHistory> playHistories = playHistoryMapper.selectList(null);
        for (PlayHistory playHistory : playHistories) {
            Set<Integer> listIds = songContext.listIdsBySongId.get(playHistory.getSongId());
            if (listIds == null || listIds.isEmpty()) {
                continue;
            }
            for (Integer listId : listIds) {
                if (context.songListById.containsKey(listId)) {
                    context.globalPlayCountByList.merge(listId, 1, Integer::sum);
                }
            }
        }

        context.userVectors = pruneEmptyVectors(context.userVectors);
        context.itemVectors = RecommendUtils.transposeUserVectors(context.userVectors);
        context.hotScoreByList = RecommendUtils.normalizeScores(calculateSongListHotScores(context));
        return context;
    }

    private SingerRecommendContext buildSingerRecommendContext() {
        SongRecommendContext songContext = buildSongRecommendContext();
        SingerRecommendContext context = new SingerRecommendContext();
        context.songById = songContext.songById;
        context.singerById = songContext.singerById;
        context.listIdsBySongId = songContext.listIdsBySongId;
        context.songListById = songContext.songListById;

        for (Singer singer : context.singerById.values()) {
            SingerFeature feature = new SingerFeature();
            feature.singerId = singer.getId();
            feature.keywords.addAll(extractKeywords(singer.getName()));
            feature.keywords.addAll(extractKeywords(singer.getIntroduction()));
            context.featureBySingerId.put(singer.getId(), feature);
        }

        for (Song song : context.songById.values()) {
            if (song.getSingerId() == null || !context.featureBySingerId.containsKey(song.getSingerId())) {
                continue;
            }
            SingerFeature feature = context.featureBySingerId.get(song.getSingerId());
            feature.songIds.add(song.getId());
            feature.keywords.addAll(extractKeywords(song.getName()));
            feature.keywords.addAll(extractKeywords(song.getIntroduction()));
            if (song.getCreateTime() != null && (feature.latestSongTime == null || song.getCreateTime().after(feature.latestSongTime))) {
                feature.latestSongTime = song.getCreateTime();
            }
            for (Integer listId : context.listIdsBySongId.getOrDefault(song.getId(), Collections.emptySet())) {
                SongList songList = context.songListById.get(listId);
                if (songList != null) {
                    feature.styles.addAll(splitStyles(songList.getStyle()));
                }
            }
        }

        for (Map.Entry<Integer, Map<Integer, Double>> userSongEntry : songContext.userVectors.entrySet()) {
            Integer userId = userSongEntry.getKey();
            for (Map.Entry<Integer, Double> songEntry : userSongEntry.getValue().entrySet()) {
                Song song = context.songById.get(songEntry.getKey());
                if (song == null || song.getSingerId() == null || !context.singerById.containsKey(song.getSingerId())) {
                    continue;
                }
                context.userVectors.computeIfAbsent(userId, key -> new HashMap<>())
                        .merge(song.getSingerId(), songEntry.getValue(), Double::sum);
                context.globalPlayContributionBySinger.merge(song.getSingerId(), songEntry.getValue(), Double::sum);
            }
        }

        List<Collect> collects = collectMapper.selectList(null);
        for (Collect collect : collects) {
            if (collect.getType() == null || collect.getType() != 0 || collect.getSongId() == null) {
                continue;
            }
            Song song = context.songById.get(collect.getSongId());
            if (song == null || song.getSingerId() == null) {
                continue;
            }
            context.globalCollectCountBySinger.merge(song.getSingerId(), 1, Integer::sum);
            context.collectUserSingerPairs.add(buildPairKey(collect.getUserId(), song.getSingerId()));
        }

        List<Comment> comments = commentMapper.selectList(null);
        for (Comment comment : comments) {
            if (comment.getSongId() == null) {
                continue;
            }
            Song song = context.songById.get(comment.getSongId());
            if (song == null || song.getSingerId() == null) {
                continue;
            }
            context.globalCommentCountBySinger.merge(song.getSingerId(), 1, Integer::sum);
        }

        List<RankSong> rankSongs = rankSongMapper.selectList(null);
        for (RankSong rankSong : rankSongs) {
            if (rankSong.getSongId() == null) {
                continue;
            }
            Song song = context.songById.get(rankSong.getSongId());
            if (song == null || song.getSingerId() == null) {
                continue;
            }
            context.globalRatingSumBySinger.merge(song.getSingerId(), rankSong.getScore().doubleValue(), Double::sum);
            context.globalRatingCountBySinger.merge(song.getSingerId(), 1, Integer::sum);
            if (rankSong.getConsumerId() != null) {
                context.ratedUserSingerPairs.add(buildPairKey(rankSong.getConsumerId(), song.getSingerId()));
            }
        }

        context.userVectors = pruneEmptyVectors(context.userVectors);
        context.itemVectors = RecommendUtils.transposeUserVectors(context.userVectors);
        context.hotScoreBySinger = RecommendUtils.normalizeScores(calculateSingerHotScores(context));
        return context;
    }

    private RecommendProfile buildSongProfile(Integer userId, SongRecommendContext context) {
        List<ConsumerRecommendSeed> seeds = getSeedsByUserId(userId);
        RecommendProfile profile = new RecommendProfile();
        profile.userId = userId;
        Map<Integer, Double> rawSongWeights = new HashMap<>(context.userVectors.getOrDefault(userId, Collections.emptyMap()));
        Map<Integer, Integer> playCountBySong = context.userPlayCountBySong.getOrDefault(userId, Collections.emptyMap());
        applySongSeeds(profile, rawSongWeights, seeds);

        profile.itemWeights = RecommendUtils.normalizeScores(rawSongWeights);
        profile.interactedItemIds.addAll(profile.itemWeights.keySet());
        profile.playCountByItem.putAll(playCountBySong);

        for (Map.Entry<Integer, Double> entry : rawSongWeights.entrySet()) {
            SongFeature feature = context.featureBySongId.get(entry.getKey());
            if (feature == null) {
                continue;
            }
            if (feature.singerId != null) {
                profile.singerWeights.merge(feature.singerId, entry.getValue(), Double::sum);
            }
            for (String style : feature.styles) {
                profile.styleWeights.merge(style, entry.getValue(), Double::sum);
            }
            for (String keyword : feature.keywords) {
                profile.keywordWeights.merge(keyword, entry.getValue() * 0.45, Double::sum);
            }
        }

        normalizeProfile(profile);
        finalizeProfile(profile, seeds);
        return profile;
    }

    private RecommendProfile buildSongListProfile(Integer userId, SongListRecommendContext context) {
        List<ConsumerRecommendSeed> seeds = getSeedsByUserId(userId);
        RecommendProfile profile = new RecommendProfile();
        profile.userId = userId;
        Map<Integer, Double> rawListWeights = new HashMap<>(context.userVectors.getOrDefault(userId, Collections.emptyMap()));
        applyListStyleSeeds(profile, seeds);

        profile.itemWeights = RecommendUtils.normalizeScores(rawListWeights);
        profile.interactedItemIds.addAll(profile.itemWeights.keySet());

        for (Map.Entry<Integer, Double> entry : rawListWeights.entrySet()) {
            SongListFeature feature = context.featureBySongListId.get(entry.getKey());
            if (feature == null) {
                continue;
            }
            for (Integer singerId : feature.singerIds) {
                profile.singerWeights.merge(singerId, entry.getValue() * 0.7, Double::sum);
            }
            for (String style : feature.styles) {
                profile.styleWeights.merge(style, entry.getValue(), Double::sum);
            }
            for (String keyword : feature.keywords) {
                profile.keywordWeights.merge(keyword, entry.getValue() * 0.45, Double::sum);
            }
        }

        normalizeProfile(profile);
        finalizeProfile(profile, seeds);
        return profile;
    }

    private RecommendProfile buildSingerProfile(Integer userId, SingerRecommendContext context) {
        List<ConsumerRecommendSeed> seeds = getSeedsByUserId(userId);
        RecommendProfile profile = new RecommendProfile();
        profile.userId = userId;
        Map<Integer, Double> rawSingerWeights = new HashMap<>(context.userVectors.getOrDefault(userId, Collections.emptyMap()));
        applySingerSeeds(profile, rawSingerWeights, seeds);

        profile.itemWeights = RecommendUtils.normalizeScores(rawSingerWeights);
        profile.interactedItemIds.addAll(profile.itemWeights.keySet());

        for (Map.Entry<Integer, Double> entry : rawSingerWeights.entrySet()) {
            SingerFeature feature = context.featureBySingerId.get(entry.getKey());
            if (feature == null) {
                continue;
            }
            for (String style : feature.styles) {
                profile.styleWeights.merge(style, entry.getValue(), Double::sum);
            }
            for (String keyword : feature.keywords) {
                profile.keywordWeights.merge(keyword, entry.getValue() * 0.45, Double::sum);
            }
        }

        normalizeProfile(profile);
        finalizeProfile(profile, seeds);
        return profile;
    }

    private void normalizeProfile(RecommendProfile profile) {
        profile.singerWeights = RecommendUtils.normalizeScores(profile.singerWeights);
        profile.styleWeights = RecommendUtils.normalizeStringScores(profile.styleWeights);
        profile.keywordWeights = RecommendUtils.normalizeStringScores(profile.keywordWeights);
    }

    private void finalizeProfile(RecommendProfile profile, List<ConsumerRecommendSeed> seeds) {
        profile.seedCount = seeds.size();
        profile.interactionCount = profile.itemWeights.size();
        if (profile.interactionCount >= STABLE_SIGNAL_THRESHOLD) {
            profile.weights = STABLE_WEIGHTS;
        } else if (profile.interactionCount > 0 || profile.seedCount > 0) {
            profile.weights = SPARSE_WEIGHTS;
        } else {
            profile.weights = COLD_WEIGHTS;
        }
        profile.stage = profile.weights.stage;
        profile.needsBootstrap = profile.seedCount == 0 && profile.interactionCount < BOOTSTRAP_TRIGGER_THRESHOLD;
    }

    private void applySongSeeds(RecommendProfile profile, Map<Integer, Double> rawSongWeights, List<ConsumerRecommendSeed> seeds) {
        for (ConsumerRecommendSeed seed : seeds) {
            if ("SONG".equals(seed.getSeedType()) && seed.getRefId() != null) {
                rawSongWeights.merge(seed.getRefId(), defaultSeedWeight(seed, 1.4), Double::sum);
            } else if ("SINGER".equals(seed.getSeedType()) && seed.getRefId() != null) {
                profile.singerWeights.merge(seed.getRefId(), defaultSeedWeight(seed, 1.2), Double::sum);
            } else if ("SONG_LIST_STYLE".equals(seed.getSeedType()) && StringUtils.isNotBlank(seed.getRefValue())) {
                profile.styleWeights.merge(seed.getRefValue(), defaultSeedWeight(seed, 1.1), Double::sum);
            }
        }
    }

    private void applyListStyleSeeds(RecommendProfile profile, List<ConsumerRecommendSeed> seeds) {
        for (ConsumerRecommendSeed seed : seeds) {
            if ("SINGER".equals(seed.getSeedType()) && seed.getRefId() != null) {
                profile.singerWeights.merge(seed.getRefId(), defaultSeedWeight(seed, 1.2), Double::sum);
            } else if ("SONG_LIST_STYLE".equals(seed.getSeedType()) && StringUtils.isNotBlank(seed.getRefValue())) {
                profile.styleWeights.merge(seed.getRefValue(), defaultSeedWeight(seed, 1.4), Double::sum);
            }
        }
    }

    private void applySingerSeeds(RecommendProfile profile, Map<Integer, Double> rawSingerWeights, List<ConsumerRecommendSeed> seeds) {
        for (ConsumerRecommendSeed seed : seeds) {
            if ("SINGER".equals(seed.getSeedType()) && seed.getRefId() != null) {
                rawSingerWeights.merge(seed.getRefId(), defaultSeedWeight(seed, 1.4), Double::sum);
            } else if ("SONG_LIST_STYLE".equals(seed.getSeedType()) && StringUtils.isNotBlank(seed.getRefValue())) {
                profile.styleWeights.merge(seed.getRefValue(), defaultSeedWeight(seed, 1.0), Double::sum);
            }
        }
    }

    private Map<Integer, Double> buildUserCfScores(Integer userId,
                                                   Map<Integer, Map<Integer, Double>> userVectors,
                                                   Set<Integer> interactedItemIds) {
        Map<Integer, Double> rawScores = new HashMap<>();
        if (userId == null || !userVectors.containsKey(userId)) {
            return rawScores;
        }
        List<RecommendUtils.Neighbor> neighbors = RecommendUtils.getTopSimilarUsers(
                userId,
                userVectors,
                TOP_NEIGHBOR_COUNT,
                MIN_OVERLAP,
                SHRINKAGE
        );
        for (RecommendUtils.Neighbor neighbor : neighbors) {
            Map<Integer, Double> vector = userVectors.getOrDefault(neighbor.getUserId(), Collections.emptyMap());
            for (Map.Entry<Integer, Double> entry : vector.entrySet()) {
                if (interactedItemIds.contains(entry.getKey())) {
                    continue;
                }
                rawScores.merge(entry.getKey(), neighbor.getSimilarity() * entry.getValue(), Double::sum);
            }
        }
        return RecommendUtils.normalizeScores(rawScores);
    }

    private Map<Integer, Double> buildSongItemCfScores(SongRecommendContext context, RecommendProfile profile) {
        Map<Integer, Double> rawScores = new HashMap<>();
        if (profile.itemWeights.isEmpty()) {
            return rawScores;
        }
        List<Map.Entry<Integer, Double>> anchors = topIntegerEntries(profile.itemWeights, ITEM_CF_LIMIT);
        for (Song song : context.activeSongs) {
            if (profile.interactedItemIds.contains(song.getId())) {
                continue;
            }
            Map<Integer, Double> targetVector = context.itemVectors.get(song.getId());
            if (targetVector == null || targetVector.isEmpty()) {
                continue;
            }
            double score = 0;
            for (Map.Entry<Integer, Double> anchor : anchors) {
                Map<Integer, Double> anchorVector = context.itemVectors.get(anchor.getKey());
                if (anchorVector == null || anchorVector.isEmpty()) {
                    continue;
                }
                double similarity = RecommendUtils.sparseCosineSimilarity(targetVector, anchorVector, MIN_OVERLAP, SHRINKAGE);
                if (similarity > 0) {
                    score += similarity * anchor.getValue();
                }
            }
            if (score > 0) {
                rawScores.put(song.getId(), score);
            }
        }
        return RecommendUtils.normalizeScores(rawScores);
    }

    private Map<Integer, Double> buildSongListItemCfScores(SongListRecommendContext context, RecommendProfile profile) {
        Map<Integer, Double> rawScores = new HashMap<>();
        if (profile.itemWeights.isEmpty()) {
            return rawScores;
        }
        List<Map.Entry<Integer, Double>> anchors = topIntegerEntries(profile.itemWeights, ITEM_CF_LIMIT);
        for (SongList songList : context.songListById.values()) {
            if (profile.interactedItemIds.contains(songList.getId())) {
                continue;
            }
            Map<Integer, Double> targetVector = context.itemVectors.get(songList.getId());
            if (targetVector == null || targetVector.isEmpty()) {
                continue;
            }
            double score = 0;
            for (Map.Entry<Integer, Double> anchor : anchors) {
                Map<Integer, Double> anchorVector = context.itemVectors.get(anchor.getKey());
                if (anchorVector == null || anchorVector.isEmpty()) {
                    continue;
                }
                double similarity = RecommendUtils.sparseCosineSimilarity(targetVector, anchorVector, MIN_OVERLAP, SHRINKAGE);
                if (similarity > 0) {
                    score += similarity * anchor.getValue();
                }
            }
            if (score > 0) {
                rawScores.put(songList.getId(), score);
            }
        }
        return RecommendUtils.normalizeScores(rawScores);
    }

    private Map<Integer, Double> buildSingerItemCfScores(SingerRecommendContext context, RecommendProfile profile) {
        Map<Integer, Double> rawScores = new HashMap<>();
        if (profile.itemWeights.isEmpty()) {
            return rawScores;
        }
        List<Map.Entry<Integer, Double>> anchors = topIntegerEntries(profile.itemWeights, ITEM_CF_LIMIT);
        for (Singer singer : context.singerById.values()) {
            if (profile.interactedItemIds.contains(singer.getId())) {
                continue;
            }
            Map<Integer, Double> targetVector = context.itemVectors.get(singer.getId());
            if (targetVector == null || targetVector.isEmpty()) {
                continue;
            }
            double score = 0;
            for (Map.Entry<Integer, Double> anchor : anchors) {
                Map<Integer, Double> anchorVector = context.itemVectors.get(anchor.getKey());
                if (anchorVector == null || anchorVector.isEmpty()) {
                    continue;
                }
                double similarity = RecommendUtils.sparseCosineSimilarity(targetVector, anchorVector, MIN_OVERLAP, SHRINKAGE);
                if (similarity > 0) {
                    score += similarity * anchor.getValue();
                }
            }
            if (score > 0) {
                rawScores.put(singer.getId(), score);
            }
        }
        return RecommendUtils.normalizeScores(rawScores);
    }

    private Map<Integer, Double> buildSongContentScores(SongRecommendContext context, RecommendProfile profile) {
        Map<Integer, Double> rawScores = new HashMap<>();
        for (Song song : context.activeSongs) {
            SongFeature feature = context.featureBySongId.get(song.getId());
            if (feature == null) {
                continue;
            }
            double singerScore = feature.singerId == null ? 0 : profile.singerWeights.getOrDefault(feature.singerId, 0.0);
            double styleScore = averageStringScore(feature.styles, profile.styleWeights);
            double keywordScore = averageStringScore(feature.keywords, profile.keywordWeights);
            double freshness = calculateFreshnessBoost(feature.createTime);
            double total = singerScore * 0.45 + styleScore * 0.35 + keywordScore * 0.20 + freshness * 0.15;
            if (profile.interactedItemIds.contains(song.getId())) {
                total *= 0.35;
            }
            if (total > 0) {
                rawScores.put(song.getId(), total);
            }
        }
        return RecommendUtils.normalizeScores(rawScores);
    }

    private Map<Integer, Double> buildSongListContentScores(SongListRecommendContext context, RecommendProfile profile) {
        Map<Integer, Double> rawScores = new HashMap<>();
        for (SongList songList : context.songListById.values()) {
            SongListFeature feature = context.featureBySongListId.get(songList.getId());
            if (feature == null) {
                continue;
            }
            double styleScore = averageStringScore(feature.styles, profile.styleWeights);
            double singerScore = averageIntegerScore(feature.singerIds, profile.singerWeights);
            double keywordScore = averageStringScore(feature.keywords, profile.keywordWeights);
            double total = styleScore * 0.45 + singerScore * 0.35 + keywordScore * 0.20;
            if (profile.interactedItemIds.contains(songList.getId())) {
                total *= 0.4;
            }
            if (total > 0) {
                rawScores.put(songList.getId(), total);
            }
        }
        return RecommendUtils.normalizeScores(rawScores);
    }

    private Map<Integer, Double> buildSingerContentScores(SingerRecommendContext context, RecommendProfile profile) {
        Map<Integer, Double> rawScores = new HashMap<>();
        for (Singer singer : context.singerById.values()) {
            SingerFeature feature = context.featureBySingerId.get(singer.getId());
            if (feature == null) {
                continue;
            }
            double styleScore = averageStringScore(feature.styles, profile.styleWeights);
            double keywordScore = averageStringScore(feature.keywords, profile.keywordWeights);
            double freshness = calculateFreshnessBoost(feature.latestSongTime);
            double total = styleScore * 0.55 + keywordScore * 0.25 + freshness * 0.20;
            if (profile.interactedItemIds.contains(singer.getId())) {
                total *= 0.4;
            }
            if (total > 0) {
                rawScores.put(singer.getId(), total);
            }
        }
        return RecommendUtils.normalizeScores(rawScores);
    }

    private Map<Integer, Double> buildSongFallbackScores(SongRecommendContext context, RecommendProfile profile) {
        Map<Integer, Double> scores = new HashMap<>();
        for (Song song : context.activeSongs) {
            double hotScore = context.hotScoreBySong.getOrDefault(song.getId(), 0.0);
            double freshness = calculateFreshnessBoost(song.getCreateTime());
            double novelty = 1.0 - Math.min(profile.playCountByItem.getOrDefault(song.getId(), 0) / 6.0, 1.0);
            double total = hotScore * 0.65 + freshness * 0.20 + novelty * 0.15;
            if (profile.playCountByItem.getOrDefault(song.getId(), 0) >= 4
                    && !context.collectUserSongPairs.contains(buildPairKey(profile.userId, song.getId()))
                    && !context.ratedUserSongPairs.contains(buildPairKey(profile.userId, song.getId()))) {
                total *= 0.75;
            }
            scores.put(song.getId(), total);
        }
        return RecommendUtils.normalizeScores(scores);
    }

    private Map<Integer, Double> buildSongListFallbackScores(SongListRecommendContext context, RecommendProfile profile) {
        Map<Integer, Double> scores = new HashMap<>();
        for (SongList songList : context.songListById.values()) {
            double hotScore = context.hotScoreByList.getOrDefault(songList.getId(), 0.0);
            double styleAffinity = profile.styleWeights.getOrDefault(normalizePrimaryStyle(songList.getStyle()), 0.0);
            double total = hotScore * 0.75 + styleAffinity * 0.25;
            if (context.collectUserListPairs.contains(buildPairKey(profile.userId, songList.getId()))
                    || context.ratedUserListPairs.contains(buildPairKey(profile.userId, songList.getId()))) {
                total *= 0.45;
            }
            scores.put(songList.getId(), total);
        }
        return RecommendUtils.normalizeScores(scores);
    }

    private Map<Integer, Double> buildSingerFallbackScores(SingerRecommendContext context, RecommendProfile profile) {
        Map<Integer, Double> scores = new HashMap<>();
        for (Singer singer : context.singerById.values()) {
            SingerFeature feature = context.featureBySingerId.get(singer.getId());
            double hotScore = context.hotScoreBySinger.getOrDefault(singer.getId(), 0.0);
            double styleAffinity = averageStringScore(feature == null ? Collections.emptySet() : feature.styles, profile.styleWeights);
            double total = hotScore * 0.8 + styleAffinity * 0.2;
            if (context.collectUserSingerPairs.contains(buildPairKey(profile.userId, singer.getId()))
                    || context.ratedUserSingerPairs.contains(buildPairKey(profile.userId, singer.getId()))) {
                total *= 0.45;
            }
            scores.put(singer.getId(), total);
        }
        return RecommendUtils.normalizeScores(scores);
    }

    private List<RecommendCandidate> buildSongCandidates(SongRecommendContext context,
                                                         RecommendProfile profile,
                                                         Map<Integer, Double> userCfScores,
                                                         Map<Integer, Double> itemCfScores,
                                                         Map<Integer, Double> contentScores,
                                                         Map<Integer, Double> fallbackScores) {
        List<RecommendCandidate> candidates = new ArrayList<>();
        for (Song song : context.activeSongs) {
            double finalScore = combineScore(profile.weights, userCfScores.get(song.getId()), itemCfScores.get(song.getId()), contentScores.get(song.getId()), fallbackScores.get(song.getId()));
            if (finalScore <= 0) {
                continue;
            }
            RecommendCandidate candidate = new RecommendCandidate(song.getId(), finalScore);
            candidate.userCfScore = userCfScores.getOrDefault(song.getId(), 0.0);
            candidate.itemCfScore = itemCfScores.getOrDefault(song.getId(), 0.0);
            candidate.contentScore = contentScores.getOrDefault(song.getId(), 0.0);
            candidate.fallbackScore = fallbackScores.getOrDefault(song.getId(), 0.0);
            SongFeature feature = context.featureBySongId.get(song.getId());
            candidate.diversityKey = feature != null && feature.singerId != null ? String.valueOf(feature.singerId) : "unknown";
            applySongReason(candidate, context, feature);
            candidates.add(candidate);
        }
        candidates.sort(Comparator.comparingDouble(RecommendCandidate::getFinalScore).reversed());
        return candidates;
    }

    private List<RecommendCandidate> buildSongListCandidates(SongListRecommendContext context,
                                                             RecommendProfile profile,
                                                             Map<Integer, Double> userCfScores,
                                                             Map<Integer, Double> itemCfScores,
                                                             Map<Integer, Double> contentScores,
                                                             Map<Integer, Double> fallbackScores) {
        List<RecommendCandidate> candidates = new ArrayList<>();
        for (SongList songList : context.songListById.values()) {
            double finalScore = combineScore(profile.weights, userCfScores.get(songList.getId()), itemCfScores.get(songList.getId()), contentScores.get(songList.getId()), fallbackScores.get(songList.getId()));
            if (finalScore <= 0) {
                continue;
            }
            RecommendCandidate candidate = new RecommendCandidate(songList.getId(), finalScore);
            candidate.userCfScore = userCfScores.getOrDefault(songList.getId(), 0.0);
            candidate.itemCfScore = itemCfScores.getOrDefault(songList.getId(), 0.0);
            candidate.contentScore = contentScores.getOrDefault(songList.getId(), 0.0);
            candidate.fallbackScore = fallbackScores.getOrDefault(songList.getId(), 0.0);
            SongListFeature feature = context.featureBySongListId.get(songList.getId());
            candidate.diversityKey = feature != null ? feature.primaryStyle : "未分类";
            applySongListReason(candidate, feature);
            candidates.add(candidate);
        }
        candidates.sort(Comparator.comparingDouble(RecommendCandidate::getFinalScore).reversed());
        return candidates;
    }

    private List<RecommendCandidate> buildSingerCandidates(SingerRecommendContext context,
                                                           RecommendProfile profile,
                                                           Map<Integer, Double> userCfScores,
                                                           Map<Integer, Double> itemCfScores,
                                                           Map<Integer, Double> contentScores,
                                                           Map<Integer, Double> fallbackScores) {
        List<RecommendCandidate> candidates = new ArrayList<>();
        for (Singer singer : context.singerById.values()) {
            double finalScore = combineScore(profile.weights, userCfScores.get(singer.getId()), itemCfScores.get(singer.getId()), contentScores.get(singer.getId()), fallbackScores.get(singer.getId()));
            if (finalScore <= 0) {
                continue;
            }
            RecommendCandidate candidate = new RecommendCandidate(singer.getId(), finalScore);
            candidate.userCfScore = userCfScores.getOrDefault(singer.getId(), 0.0);
            candidate.itemCfScore = itemCfScores.getOrDefault(singer.getId(), 0.0);
            candidate.contentScore = contentScores.getOrDefault(singer.getId(), 0.0);
            candidate.fallbackScore = fallbackScores.getOrDefault(singer.getId(), 0.0);
            candidate.diversityKey = String.valueOf(singer.getId());
            applySingerReason(candidate, context.featureBySingerId.get(singer.getId()));
            candidates.add(candidate);
        }
        candidates.sort(Comparator.comparingDouble(RecommendCandidate::getFinalScore).reversed());
        return candidates;
    }

    private List<RecommendCandidate> rerankSongCandidates(List<RecommendCandidate> ranked, int limit) {
        List<RecommendCandidate> selected = new ArrayList<>();
        Map<String, Integer> singerCount = new HashMap<>();
        for (RecommendCandidate candidate : ranked) {
            String key = StringUtils.defaultIfBlank(candidate.diversityKey, "unknown");
            if (singerCount.getOrDefault(key, 0) >= 2) {
                continue;
            }
            selected.add(candidate);
            singerCount.merge(key, 1, Integer::sum);
            if (selected.size() >= limit) {
                return selected;
            }
        }
        return fillRemaining(selected, ranked, limit);
    }

    private List<RecommendCandidate> rerankSongListCandidates(List<RecommendCandidate> ranked, int limit) {
        List<RecommendCandidate> selected = new ArrayList<>();
        Map<String, Integer> styleCount = new HashMap<>();
        for (RecommendCandidate candidate : ranked) {
            String key = StringUtils.defaultIfBlank(candidate.diversityKey, "未分类");
            if (styleCount.getOrDefault(key, 0) >= 3) {
                continue;
            }
            selected.add(candidate);
            styleCount.merge(key, 1, Integer::sum);
            if (selected.size() >= limit) {
                return selected;
            }
        }
        return fillRemaining(selected, ranked, limit);
    }

    private List<RecommendCandidate> fillRemaining(List<RecommendCandidate> selected, List<RecommendCandidate> ranked, int limit) {
        if (selected.size() >= limit) {
            return selected;
        }
        for (RecommendCandidate candidate : ranked) {
            if (selected.contains(candidate)) {
                continue;
            }
            selected.add(candidate);
            if (selected.size() >= limit) {
                break;
            }
        }
        return selected;
    }

    private List<RecommendCandidate> fallbackSongCandidates(SongRecommendContext context, int limit) {
        return fallbackSongEntities(context, limit).stream()
                .map(song -> {
                    RecommendCandidate candidate = new RecommendCandidate(song.getId(), context.hotScoreBySong.getOrDefault(song.getId(), 0.0));
                    candidate.reasonCode = "HOT_FALLBACK";
                    candidate.reasonText = "最近热度上升，先为你保留在这里。";
                    return candidate;
                })
                .collect(Collectors.toList());
    }

    private List<RecommendCandidate> fallbackSongListCandidates(SongListRecommendContext context, int limit) {
        return context.songListById.values().stream()
                .sorted((left, right) -> Double.compare(context.hotScoreByList.getOrDefault(right.getId(), 0.0), context.hotScoreByList.getOrDefault(left.getId(), 0.0)))
                .limit(limit)
                .map(songList -> {
                    RecommendCandidate candidate = new RecommendCandidate(songList.getId(), context.hotScoreByList.getOrDefault(songList.getId(), 0.0));
                    candidate.reasonCode = "HOT_STYLE_FALLBACK";
                    candidate.reasonText = "这张歌单最近被很多人轻轻收藏。";
                    return candidate;
                })
                .collect(Collectors.toList());
    }

    private List<RecommendCandidate> fallbackSingerCandidates(SingerRecommendContext context, int limit) {
        return fallbackSingerEntities(context, limit).stream()
                .map(singer -> {
                    RecommendCandidate candidate = new RecommendCandidate(singer.getId(), context.hotScoreBySinger.getOrDefault(singer.getId(), 0.0));
                    candidate.reasonCode = "HOT_SINGER_FALLBACK";
                    candidate.reasonText = "最近有更多人把这位歌手放进耳机里。";
                    return candidate;
                })
                .collect(Collectors.toList());
    }

    private List<Song> fallbackSongEntities(SongRecommendContext context, int limit) {
        return context.activeSongs.stream()
                .sorted((left, right) -> Double.compare(context.hotScoreBySong.getOrDefault(right.getId(), 0.0), context.hotScoreBySong.getOrDefault(left.getId(), 0.0)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<Singer> fallbackSingerEntities(SingerRecommendContext context, int limit) {
        return context.singerById.values().stream()
                .sorted((left, right) -> Double.compare(context.hotScoreBySinger.getOrDefault(right.getId(), 0.0), context.hotScoreBySinger.getOrDefault(left.getId(), 0.0)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void applySongReason(RecommendCandidate candidate, SongRecommendContext context, SongFeature feature) {
        double maxScore = Math.max(Math.max(candidate.userCfScore, candidate.itemCfScore), Math.max(candidate.contentScore, candidate.fallbackScore));
        if (maxScore == candidate.userCfScore && feature != null && feature.singerId != null) {
            Singer singer = context.singerById.get(feature.singerId);
            candidate.reasonCode = "USER_CF";
            candidate.reasonText = singer == null ? "因为你与相似听众有接近的偏爱，推荐这首歌。" : "因为你常听 " + singer.getName() + " 附近的旋律，推荐这首歌。";
            return;
        }
        if (maxScore == candidate.itemCfScore) {
            candidate.reasonCode = "ITEM_CF";
            candidate.reasonText = "因为它与你最近播放过的歌曲气质相近。";
            return;
        }
        if (maxScore == candidate.contentScore && feature != null && !feature.styles.isEmpty()) {
            candidate.reasonCode = "CONTENT";
            candidate.reasonText = "因为你偏爱的 " + feature.styles.iterator().next() + " 氛围，还想把这首歌留给你。";
            return;
        }
        candidate.reasonCode = "FALLBACK";
        candidate.reasonText = "最近热度上升，也许会刚好适合此刻。";
    }

    private void applySongListReason(RecommendCandidate candidate, SongListFeature feature) {
        double maxScore = Math.max(Math.max(candidate.userCfScore, candidate.itemCfScore), Math.max(candidate.contentScore, candidate.fallbackScore));
        if (maxScore == candidate.userCfScore) {
            candidate.reasonCode = "USER_CF";
            candidate.reasonText = "与你口味相近的听众，也把这张歌单轻轻收藏。";
            return;
        }
        if (maxScore == candidate.itemCfScore) {
            candidate.reasonCode = "ITEM_CF";
            candidate.reasonText = "它和你最近停留过的歌单有相近的旋律走向。";
            return;
        }
        if (maxScore == candidate.contentScore && feature != null) {
            candidate.reasonCode = "CONTENT";
            candidate.reasonText = "因为你偏爱 " + feature.primaryStyle + " 的歌单气氛。";
            return;
        }
        candidate.reasonCode = "FALLBACK";
        candidate.reasonText = "最近有很多人把这张歌单留在收藏里。";
    }

    private void applySingerReason(RecommendCandidate candidate, SingerFeature feature) {
        double maxScore = Math.max(Math.max(candidate.userCfScore, candidate.itemCfScore), Math.max(candidate.contentScore, candidate.fallbackScore));
        if (maxScore == candidate.userCfScore) {
            candidate.reasonCode = "USER_CF";
            candidate.reasonText = "与你口味相近的听众，也常把这位歌手加入播放。";
            return;
        }
        if (maxScore == candidate.itemCfScore) {
            candidate.reasonCode = "ITEM_CF";
            candidate.reasonText = "这位歌手与你最近常听的声音气质很接近。";
            return;
        }
        if (maxScore == candidate.contentScore && feature != null && !feature.styles.isEmpty()) {
            candidate.reasonCode = "CONTENT";
            candidate.reasonText = "因为你最近偏爱 " + feature.styles.iterator().next() + " 这一类声音。";
            return;
        }
        candidate.reasonCode = "FALLBACK";
        candidate.reasonText = "最近有更多人把这位歌手放进了耳机里。";
    }

    private void applyReason(Song song, RecommendCandidate candidate) {
        if (song == null || candidate == null) {
            return;
        }
        song.setReasonCode(candidate.reasonCode);
        song.setReasonText(candidate.reasonText);
        song.setRecommendScore(candidate.finalScore);
    }

    private void applyReason(SongList songList, RecommendCandidate candidate) {
        if (songList == null || candidate == null) {
            return;
        }
        songList.setReasonCode(candidate.reasonCode);
        songList.setReasonText(candidate.reasonText);
        songList.setRecommendScore(candidate.finalScore);
    }

    private void applyReason(Singer singer, RecommendCandidate candidate) {
        if (singer == null || candidate == null) {
            return;
        }
        singer.setReasonCode(candidate.reasonCode);
        singer.setReasonText(candidate.reasonText);
        singer.setRecommendScore(candidate.finalScore);
    }

    private Map<Integer, Double> calculateSongHotScores(SongRecommendContext context) {
        Map<Integer, Double> scores = new HashMap<>();
        for (Song song : context.activeSongs) {
            double playScore = Math.log1p(context.globalPlayCountBySong.getOrDefault(song.getId(), 0));
            double collectScore = context.globalCollectCountBySong.getOrDefault(song.getId(), 0) * 1.6;
            double commentScore = context.globalCommentCountBySong.getOrDefault(song.getId(), 0) * 1.2;
            double ratingScore = 0.0;
            int ratingCount = context.globalRatingCountBySong.getOrDefault(song.getId(), 0);
            if (ratingCount > 0) {
                ratingScore = (context.globalRatingSumBySong.getOrDefault(song.getId(), 0.0) / ratingCount) * 0.45;
            }
            double freshness = calculateFreshnessBoost(song.getCreateTime()) * 0.8;
            scores.put(song.getId(), playScore + collectScore + commentScore + ratingScore + freshness);
        }
        return scores;
    }

    private Map<Integer, Double> calculateSongListHotScores(SongListRecommendContext context) {
        Map<Integer, Double> scores = new HashMap<>();
        for (SongList songList : context.songListById.values()) {
            double playScore = Math.log1p(context.globalPlayCountByList.getOrDefault(songList.getId(), 0));
            double collectScore = context.globalCollectCountByList.getOrDefault(songList.getId(), 0) * 1.8;
            double commentScore = context.globalCommentCountByList.getOrDefault(songList.getId(), 0) * 1.2;
            double ratingScore = 0.0;
            int ratingCount = context.globalRatingCountByList.getOrDefault(songList.getId(), 0);
            if (ratingCount > 0) {
                ratingScore = (context.globalRatingSumByList.getOrDefault(songList.getId(), 0.0) / ratingCount) * 0.5;
            }
            SongListFeature feature = context.featureBySongListId.get(songList.getId());
            double richness = feature == null ? 0.0 : Math.log1p(feature.songIds.size()) * 0.4;
            scores.put(songList.getId(), playScore + collectScore + commentScore + ratingScore + richness);
        }
        return scores;
    }

    private Map<Integer, Double> calculateSingerHotScores(SingerRecommendContext context) {
        Map<Integer, Double> scores = new HashMap<>();
        for (Singer singer : context.singerById.values()) {
            double playScore = Math.log1p(Math.round(context.globalPlayContributionBySinger.getOrDefault(singer.getId(), 0.0)));
            double collectScore = context.globalCollectCountBySinger.getOrDefault(singer.getId(), 0) * 1.5;
            double commentScore = context.globalCommentCountBySinger.getOrDefault(singer.getId(), 0) * 1.0;
            double ratingScore = 0.0;
            int ratingCount = context.globalRatingCountBySinger.getOrDefault(singer.getId(), 0);
            if (ratingCount > 0) {
                ratingScore = (context.globalRatingSumBySinger.getOrDefault(singer.getId(), 0.0) / ratingCount) * 0.45;
            }
            SingerFeature feature = context.featureBySingerId.get(singer.getId());
            double freshness = calculateFreshnessBoost(feature == null ? null : feature.latestSongTime) * 0.6;
            scores.put(singer.getId(), playScore + collectScore + commentScore + ratingScore + freshness);
        }
        return scores;
    }

    private Song copySong(Song source) {
        if (source == null) {
            return null;
        }
        Song target = new Song();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private SongList copySongList(SongList source) {
        if (source == null) {
            return null;
        }
        SongList target = new SongList();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private Singer copySinger(Singer source) {
        if (source == null) {
            return null;
        }
        Singer target = new Singer();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private List<String> buildTopStyles(SongListRecommendContext context, int limit) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (SongListFeature feature : context.featureBySongListId.values()) {
            if (feature == null || feature.styles.isEmpty()) {
                continue;
            }
            double base = context.hotScoreByList.getOrDefault(feature.songListId, 0.0) + 0.2;
            for (String style : feature.styles) {
                scores.merge(style, base, Double::sum);
            }
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<ConsumerRecommendSeed> getSeedsByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            return Collections.emptyList();
        }
        return consumerRecommendSeedMapper.selectList(new QueryWrapper<ConsumerRecommendSeed>()
                .eq("user_id", userId)
                .orderByDesc("create_time"));
    }

    private List<ConsumerRecommendSeed> buildSingerSeeds(Integer userId, List<Integer> singerIds) {
        if (userId == null || singerIds == null) {
            return Collections.emptyList();
        }
        List<ConsumerRecommendSeed> seeds = new ArrayList<>();
        for (Integer singerId : singerIds.stream().filter(id -> id != null && id > 0).distinct().collect(Collectors.toList())) {
            ConsumerRecommendSeed seed = new ConsumerRecommendSeed();
            seed.setUserId(userId);
            seed.setSeedType("SINGER");
            seed.setRefId(singerId);
            seed.setWeight(1.25);
            seeds.add(seed);
        }
        return seeds;
    }

    private List<ConsumerRecommendSeed> buildSongSeeds(Integer userId, List<Integer> songIds) {
        if (userId == null || songIds == null) {
            return Collections.emptyList();
        }
        List<ConsumerRecommendSeed> seeds = new ArrayList<>();
        for (Integer songId : songIds.stream().filter(id -> id != null && id > 0).distinct().collect(Collectors.toList())) {
            ConsumerRecommendSeed seed = new ConsumerRecommendSeed();
            seed.setUserId(userId);
            seed.setSeedType("SONG");
            seed.setRefId(songId);
            seed.setWeight(1.15);
            seeds.add(seed);
        }
        return seeds;
    }

    private List<ConsumerRecommendSeed> buildStyleSeeds(Integer userId, List<String> styles) {
        if (userId == null || styles == null) {
            return Collections.emptyList();
        }
        List<ConsumerRecommendSeed> seeds = new ArrayList<>();
        for (String style : styles.stream().filter(StringUtils::isNotBlank).map(this::normalizePrimaryStyle).distinct().collect(Collectors.toList())) {
            ConsumerRecommendSeed seed = new ConsumerRecommendSeed();
            seed.setUserId(userId);
            seed.setSeedType("SONG_LIST_STYLE");
            seed.setRefValue(style);
            seed.setWeight(1.05);
            seeds.add(seed);
        }
        return seeds;
    }

    private double defaultSeedWeight(ConsumerRecommendSeed seed, double fallback) {
        if (seed == null || seed.getWeight() == null || seed.getWeight() <= 0) {
            return fallback;
        }
        return seed.getWeight();
    }

    private Map<Integer, Map<Integer, Double>> pruneEmptyVectors(Map<Integer, Map<Integer, Double>> vectors) {
        Map<Integer, Map<Integer, Double>> pruned = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<Integer, Double>> entry : vectors.entrySet()) {
            Map<Integer, Double> filtered = entry.getValue().entrySet().stream()
                    .filter(vectorEntry -> safeDouble(vectorEntry.getValue()) > 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> right, LinkedHashMap::new));
            if (!filtered.isEmpty()) {
                pruned.put(entry.getKey(), filtered);
            }
        }
        return pruned;
    }

    private double combineScore(RecommendWeights weights, Double userCf, Double itemCf, Double content, Double fallback) {
        if (weights == null) {
            return 0.0;
        }
        return safeDouble(userCf) * weights.userCfWeight
                + safeDouble(itemCf) * weights.itemCfWeight
                + safeDouble(content) * weights.contentWeight
                + safeDouble(fallback) * weights.fallbackWeight;
    }

    private String buildPairKey(Integer userId, Integer itemId) {
        return String.valueOf(userId) + ":" + itemId;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double calculateTimeDecay(Date time) {
        if (time == null) {
            return 0.2;
        }
        long days = Math.max(0L, (System.currentTimeMillis() - time.getTime()) / (1000L * 60 * 60 * 24));
        if (days <= 30) {
            return 1.0;
        }
        if (days <= 90) {
            return 0.7;
        }
        if (days <= 180) {
            return 0.4;
        }
        return 0.2;
    }

    private double calculateFreshnessBoost(Date time) {
        if (time == null) {
            return 0.0;
        }
        long days = Math.max(0L, (System.currentTimeMillis() - time.getTime()) / (1000L * 60 * 60 * 24));
        if (days <= 30) {
            return 1.0;
        }
        if (days <= 90) {
            return 0.65;
        }
        if (days <= 180) {
            return 0.35;
        }
        return 0.12;
    }

    private Set<String> splitStyles(String styleText) {
        if (StringUtils.isBlank(styleText)) {
            return Collections.emptySet();
        }
        return java.util.Arrays.stream(styleText.split("[/|,，、\\s]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizePrimaryStyle(String styleText) {
        return splitStyles(styleText).stream().findFirst().orElse("未分类");
    }

    private Set<String> extractKeywords(String text) {
        if (StringUtils.isBlank(text)) {
            return Collections.emptySet();
        }
        Set<String> keywords = new LinkedHashSet<>();
        Matcher chinese = CHINESE_SEGMENT.matcher(text);
        while (chinese.find()) {
            keywords.add(chinese.group().trim());
        }
        Matcher latin = LATIN_SEGMENT.matcher(text.toLowerCase());
        while (latin.find()) {
            keywords.add(latin.group().trim());
        }
        return keywords;
    }

    private double averageStringScore(Collection<String> values, Map<String, Double> weights) {
        if (values == null || values.isEmpty() || weights == null || weights.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        int count = 0;
        for (String value : values) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            total += weights.getOrDefault(value, 0.0);
            count++;
        }
        return count == 0 ? 0.0 : total / count;
    }

    private double averageIntegerScore(Collection<Integer> values, Map<Integer, Double> weights) {
        if (values == null || values.isEmpty() || weights == null || weights.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        int count = 0;
        for (Integer value : values) {
            if (value == null) {
                continue;
            }
            total += weights.getOrDefault(value, 0.0);
            count++;
        }
        return count == 0 ? 0.0 : total / count;
    }

    private List<Map.Entry<Integer, Double>> topIntegerEntries(Map<Integer, Double> weights, int limit) {
        if (weights == null || weights.isEmpty()) {
            return Collections.emptyList();
        }
        return weights.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> describeTopIntegerWeights(Map<Integer, Double> weights, Map<Integer, ?> referenceMap, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : topIntegerEntries(weights, limit)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", entry.getKey());
            item.put("weight", entry.getValue());
            Object reference = referenceMap == null ? null : referenceMap.get(entry.getKey());
            if (reference instanceof Song) {
                Song song = (Song) reference;
                item.put("name", song.getName());
                item.put("rawName", song.getName());
            } else if (reference instanceof Singer) {
                item.put("name", ((Singer) reference).getName());
            } else if (reference instanceof SongList) {
                item.put("name", ((SongList) reference).getTitle());
            } else {
                item.put("name", String.valueOf(entry.getKey()));
            }
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> topStringWeights(Map<String, Double> weights, int limit) {
        if (weights == null || weights.isEmpty()) {
            return Collections.emptyList();
        }
        return weights.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("weight", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private static final class SongRecommendContext {
        private List<Song> activeSongs = new ArrayList<>();
        private Map<Integer, Song> songById = new HashMap<>();
        private Map<Integer, Singer> singerById = new HashMap<>();
        private Map<Integer, SongList> songListById = new HashMap<>();
        private Map<Integer, SongFeature> featureBySongId = new HashMap<>();
        private Map<Integer, Set<Integer>> listIdsBySongId = new HashMap<>();
        private Map<Integer, Set<Integer>> songIdsByListId = new HashMap<>();
        private Map<Integer, Map<Integer, Double>> userVectors = new HashMap<>();
        private Map<Integer, Map<Integer, Double>> itemVectors = new HashMap<>();
        private Map<Integer, Map<Integer, Integer>> userPlayCountBySong = new HashMap<>();
        private Map<Integer, Integer> globalPlayCountBySong = new HashMap<>();
        private Map<Integer, Integer> globalCollectCountBySong = new HashMap<>();
        private Map<Integer, Integer> globalCommentCountBySong = new HashMap<>();
        private Map<Integer, Double> globalRatingSumBySong = new HashMap<>();
        private Map<Integer, Integer> globalRatingCountBySong = new HashMap<>();
        private Map<Integer, Double> hotScoreBySong = new HashMap<>();
        private Set<String> collectUserSongPairs = new HashSet<>();
        private Set<String> ratedUserSongPairs = new HashSet<>();
    }

    private static final class SongListRecommendContext {
        private Map<Integer, Song> songById = new HashMap<>();
        private Map<Integer, Singer> singerById = new HashMap<>();
        private Map<Integer, SongList> songListById = new HashMap<>();
        private Map<Integer, SongListFeature> featureBySongListId = new HashMap<>();
        private Map<Integer, Set<Integer>> songIdsByListId = new HashMap<>();
        private Map<Integer, Map<Integer, Double>> userVectors = new HashMap<>();
        private Map<Integer, Map<Integer, Double>> itemVectors = new HashMap<>();
        private Map<Integer, Integer> globalPlayCountByList = new HashMap<>();
        private Map<Integer, Integer> globalCollectCountByList = new HashMap<>();
        private Map<Integer, Integer> globalCommentCountByList = new HashMap<>();
        private Map<Integer, Double> globalRatingSumByList = new HashMap<>();
        private Map<Integer, Integer> globalRatingCountByList = new HashMap<>();
        private Map<Integer, Double> hotScoreByList = new HashMap<>();
        private Set<String> collectUserListPairs = new HashSet<>();
        private Set<String> ratedUserListPairs = new HashSet<>();
    }

    private static final class SingerRecommendContext {
        private Map<Integer, Song> songById = new HashMap<>();
        private Map<Integer, Singer> singerById = new HashMap<>();
        private Map<Integer, SongList> songListById = new HashMap<>();
        private Map<Integer, Set<Integer>> listIdsBySongId = new HashMap<>();
        private Map<Integer, SingerFeature> featureBySingerId = new HashMap<>();
        private Map<Integer, Map<Integer, Double>> userVectors = new HashMap<>();
        private Map<Integer, Map<Integer, Double>> itemVectors = new HashMap<>();
        private Map<Integer, Double> globalPlayContributionBySinger = new HashMap<>();
        private Map<Integer, Integer> globalCollectCountBySinger = new HashMap<>();
        private Map<Integer, Integer> globalCommentCountBySinger = new HashMap<>();
        private Map<Integer, Double> globalRatingSumBySinger = new HashMap<>();
        private Map<Integer, Integer> globalRatingCountBySinger = new HashMap<>();
        private Map<Integer, Double> hotScoreBySinger = new HashMap<>();
        private Set<String> collectUserSingerPairs = new HashSet<>();
        private Set<String> ratedUserSingerPairs = new HashSet<>();
    }

    private static final class SongFeature {
        private Integer songId;
        private Integer singerId;
        private Date createTime;
        private Set<String> styles = new LinkedHashSet<>();
        private Set<String> keywords = new LinkedHashSet<>();
    }

    private static final class SongListFeature {
        private Integer songListId;
        private String primaryStyle;
        private Set<String> styles = new LinkedHashSet<>();
        private Set<String> keywords = new LinkedHashSet<>();
        private Set<Integer> singerIds = new LinkedHashSet<>();
        private Set<Integer> songIds = new LinkedHashSet<>();
    }

    private static final class SingerFeature {
        private Integer singerId;
        private Date latestSongTime;
        private Set<String> styles = new LinkedHashSet<>();
        private Set<String> keywords = new LinkedHashSet<>();
        private Set<Integer> songIds = new LinkedHashSet<>();
    }

    private static final class RecommendProfile {
        private Integer userId;
        private String stage;
        private RecommendWeights weights;
        private boolean needsBootstrap;
        private int interactionCount;
        private int seedCount;
        private Map<Integer, Double> itemWeights = new LinkedHashMap<>();
        private Map<Integer, Double> singerWeights = new LinkedHashMap<>();
        private Map<String, Double> styleWeights = new LinkedHashMap<>();
        private Map<String, Double> keywordWeights = new LinkedHashMap<>();
        private Set<Integer> interactedItemIds = new HashSet<>();
        private Map<Integer, Integer> playCountByItem = new HashMap<>();
    }

    private static final class RecommendWeights {
        private final double userCfWeight;
        private final double itemCfWeight;
        private final double contentWeight;
        private final double fallbackWeight;
        private final String stage;

        private RecommendWeights(double userCfWeight, double itemCfWeight, double contentWeight, double fallbackWeight, String stage) {
            this.userCfWeight = userCfWeight;
            this.itemCfWeight = itemCfWeight;
            this.contentWeight = contentWeight;
            this.fallbackWeight = fallbackWeight;
            this.stage = stage;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("stage", stage);
            map.put("userCf", userCfWeight);
            map.put("itemCf", itemCfWeight);
            map.put("content", contentWeight);
            map.put("fallback", fallbackWeight);
            return map;
        }
    }

    private static final class RecommendCandidate {
        private final Integer id;
        private final double finalScore;
        private double userCfScore;
        private double itemCfScore;
        private double contentScore;
        private double fallbackScore;
        private String diversityKey;
        private String reasonCode;
        private String reasonText;

        private RecommendCandidate(Integer id, double finalScore) {
            this.id = id;
            this.finalScore = finalScore;
        }

        private double getFinalScore() {
            return finalScore;
        }
    }
}
