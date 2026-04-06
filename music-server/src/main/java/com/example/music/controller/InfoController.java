package com.example.music.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.music.common.R;
import com.example.music.mapper.ConsumerMapper;
import com.example.music.mapper.FeedbackMapper;
import com.example.music.mapper.PlayHistoryMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.mapper.SongListMapper;
import com.example.music.model.domain.Singer;
import com.example.music.model.domain.Song;
import com.example.music.model.domain.SongList;
import com.example.music.service.ConsumerService;
import com.example.music.service.SingerService;
import com.example.music.service.SongService;
import com.example.music.service.SongListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RestController
@RequestMapping("/info")
public class InfoController {

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    private SongService songService;

    @Autowired
    private SingerService singerService;

    @Autowired
    private SongListService songListService;
    
    @Autowired
    private PlayHistoryMapper playHistoryMapper;
    
    @Autowired
    private ConsumerMapper consumerMapper;
    
    @Autowired
    private SongMapper songMapper;

    @Autowired
    private SongListMapper songListMapper;

    @Autowired
    private FeedbackMapper feedbackMapper;

    /**
     * 获取主页统计信息
     *
     * @return 包含用户数、歌曲数、歌手数、歌单数的统计信息
     */
    @GetMapping("/home/count")
    public R getHomeCount() {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("userCount", Optional.ofNullable(consumerMapper.countActiveUsers()).orElse(0L));
        resultMap.put("songCount", songService.count());
        resultMap.put("singerCount", singerService.count());
        resultMap.put("songListCount", songListService.count());
        resultMap.put("pendingFeedbackCount", Optional.ofNullable(feedbackMapper.countPendingFeedback()).orElse(0L));
        return R.success(null, resultMap);
    }

    @GetMapping("/audit/overview")
    public R getAuditOverview() {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("pendingSongCount", songService.count(new QueryWrapper<Song>().eq("type", 3)));
        resultMap.put("pendingSongListCount", songListService.count(new QueryWrapper<SongList>().eq("type", 2)));
        resultMap.put("pendingFeedbackCount", Optional.ofNullable(feedbackMapper.countPendingFeedback()).orElse(0L));
        return R.success(null, resultMap);
    }

    @GetMapping("/content/health")
    public R getContentHealth() {
        Map<String, Object> resultMap = new HashMap<>();

        QueryWrapper<Song> missingLyricQuery = new QueryWrapper<>();
        missingLyricQuery.and(wrapper -> wrapper
                .apply("TRIM(COALESCE(lyric, '')) = ''")
                .or()
                .like("lyric", "暂无歌词"));

        QueryWrapper<Song> missingAudioQuery = new QueryWrapper<>();
        missingAudioQuery.apply("TRIM(COALESCE(url, '')) = ''");

        QueryWrapper<Singer> missingSingerIntroQuery = new QueryWrapper<>();
        missingSingerIntroQuery.apply("TRIM(COALESCE(introduction, '')) = ''");

        QueryWrapper<SongList> defaultSongListCoverQuery = new QueryWrapper<>();
        defaultSongListCoverQuery.and(wrapper -> wrapper
                .apply("TRIM(COALESCE(pic, '')) = ''")
                .or()
                .eq("pic", "/img/songListPic/123.jpg"));

        resultMap.put("missingLyricsCount", songService.count(missingLyricQuery));
        resultMap.put("missingAudioCount", songService.count(missingAudioQuery));
        resultMap.put("missingSingerIntroCount", singerService.count(missingSingerIntroQuery));
        resultMap.put("defaultSongListCoverCount", songListService.count(defaultSongListCoverQuery));
        return R.success(null, resultMap);
    }

    @GetMapping("/songList/top10")
    public R getTop10SongLists() {
        return songListService.getHotSongListsByRating(10);
    }

    /**
     * 获取用户活跃趋势数据
     *
     * @param range 取值支持 7d / 30d / 6m
     * @return 用户活跃趋势
     */
    @GetMapping("/user/activity")
    public R getRecentUserActivity(@RequestParam(defaultValue = "30d") String range) {
        List<Map<String, Object>> activityData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime startDate;
        LocalDateTime endDate = today.plusDays(1).atStartOfDay();

        String normalizedRange = Optional.ofNullable(range).orElse("30d").toLowerCase(Locale.ROOT);
        if ("7d".equals(normalizedRange)) {
            startDate = today.minusDays(6).atStartOfDay();
        } else if ("6m".equals(normalizedRange)) {
            startDate = today.withDayOfMonth(1).minusMonths(5).atStartOfDay();
        } else {
            normalizedRange = "30d";
            startDate = today.minusDays(29).atStartOfDay();
        }

        List<Map<String, Object>> dbResults = playHistoryMapper.getDailyActiveUsers(startDate, endDate);
        Map<String, Integer> dailyResultMap = new HashMap<>();
        for (Map<String, Object> row : dbResults) {
            String dateStr = row.get("date").toString();
            Integer count = ((Number) row.get("count")).intValue();
            dailyResultMap.put(dateStr, count);
        }

        if ("6m".equals(normalizedRange)) {
            YearMonth currentMonth = YearMonth.from(today);
            Map<YearMonth, Integer> monthlyResultMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : dailyResultMap.entrySet()) {
                LocalDate date = LocalDate.parse(entry.getKey());
                YearMonth yearMonth = YearMonth.from(date);
                monthlyResultMap.merge(yearMonth, entry.getValue(), Integer::sum);
            }

            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            for (int i = 5; i >= 0; i--) {
                YearMonth yearMonth = currentMonth.minusMonths(i);

                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", yearMonth.format(monthFormatter));
                dataPoint.put("count", monthlyResultMap.getOrDefault(yearMonth, 0));
                activityData.add(dataPoint);
            }
        } else {
            int days = "7d".equals(normalizedRange) ? 7 : 30;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.format(DateTimeFormatter.ISO_DATE);

                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", date.format(formatter));
                dataPoint.put("count", dailyResultMap.getOrDefault(dateStr, 0));
                activityData.add(dataPoint);
            }
        }

        return R.success(null, activityData);
    }

    /**
     * 获取播放量前10的歌曲
     *
     * @return 播放量前10的歌曲及其播放次数
     */
    @GetMapping("/song/top10")
    public R getTop10Songs() {
        // 从数据库查询播放量前10的歌曲
        List<Map<String, Object>> topSongs = playHistoryMapper.getTopSongsByPlayCount(10);
        return R.success(null, topSongs);
    }

    /**
     * 获取新增内容趋势数据
     *
     * @param range 取值支持 7d / 30d / 6m
     * @return 最近 7 天、30 天或半年内容趋势
     */
    @GetMapping("/content/trend")
    public R getNewContentTrend(@RequestParam(defaultValue = "7d") String range) {
        Map<String, Object> trendData = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDateTime startDate;
        LocalDateTime endDate = today.plusDays(1).atStartOfDay();

        String normalizedRange = Optional.ofNullable(range).orElse("7d").toLowerCase(Locale.ROOT);
        if ("30d".equals(normalizedRange)) {
            startDate = today.minusDays(29).atStartOfDay();
        } else if ("6m".equals(normalizedRange)) {
            startDate = today.withDayOfMonth(1).minusMonths(5).atStartOfDay();
        } else {
            normalizedRange = "7d";
            startDate = today.minusDays(6).atStartOfDay();
        }

        List<Map<String, Object>> weeklyNewUsers = consumerMapper.getWeeklyNewUsers(startDate, endDate);
        Map<String, Integer> userResultMap = new HashMap<>();
        for (Map<String, Object> row : weeklyNewUsers) {
            String dateStr = row.get("date").toString();
            Integer count = ((Number) row.get("count")).intValue();
            userResultMap.put(dateStr, count);
        }

        List<Map<String, Object>> weeklyNewSongs = songMapper.getWeeklyNewSongs(startDate, endDate);
        Map<String, Integer> songResultMap = new HashMap<>();
        for (Map<String, Object> row : weeklyNewSongs) {
            String dateStr = row.get("date").toString();
            Integer count = ((Number) row.get("count")).intValue();
            songResultMap.put(dateStr, count);
        }

        List<Map<String, Object>> recentSongListTrend = songListMapper.getRecentSongListTrend(startDate, endDate);
        Map<String, Integer> songListTrendMap = new HashMap<>();
        for (Map<String, Object> row : recentSongListTrend) {
            String dateStr = row.get("date").toString();
            Integer count = ((Number) row.get("count")).intValue();
            songListTrendMap.put(dateStr, count);
        }

        List<String> dates = new ArrayList<>();
        List<Integer> newUser = new ArrayList<>();
        List<Integer> newSong = new ArrayList<>();
        List<Integer> newSinger = new ArrayList<>();
        List<Integer> newSongList = new ArrayList<>();

        if ("6m".equals(normalizedRange)) {
            YearMonth currentMonth = YearMonth.from(today);
            Map<YearMonth, Integer> monthlyUserMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : userResultMap.entrySet()) {
                LocalDate date = LocalDate.parse(entry.getKey());
                monthlyUserMap.merge(YearMonth.from(date), entry.getValue(), Integer::sum);
            }

            Map<YearMonth, Integer> monthlySongMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : songResultMap.entrySet()) {
                LocalDate date = LocalDate.parse(entry.getKey());
                monthlySongMap.merge(YearMonth.from(date), entry.getValue(), Integer::sum);
            }

            Map<YearMonth, Integer> monthlySongListMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : songListTrendMap.entrySet()) {
                LocalDate date = LocalDate.parse(entry.getKey());
                monthlySongListMap.merge(YearMonth.from(date), entry.getValue(), Integer::sum);
            }

            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            for (int i = 5; i >= 0; i--) {
                YearMonth yearMonth = currentMonth.minusMonths(i);
                dates.add(yearMonth.format(monthFormatter));
                newUser.add(monthlyUserMap.getOrDefault(yearMonth, 0));
                newSong.add(monthlySongMap.getOrDefault(yearMonth, 0));
                newSinger.add(0);
                newSongList.add(monthlySongListMap.getOrDefault(yearMonth, 0));
            }
        } else {
            int days = "30d".equals(normalizedRange) ? 30 : 7;
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.format(DateTimeFormatter.ISO_DATE);
                dates.add(date.format(dayFormatter));
                newUser.add(userResultMap.getOrDefault(dateStr, 0));
                newSong.add(songResultMap.getOrDefault(dateStr, 0));
                newSinger.add(0);
                newSongList.add(songListTrendMap.getOrDefault(dateStr, 0));
            }
        }

        trendData.put("dates", dates);
        trendData.put("newUser", newUser);
        trendData.put("newSong", newSong);
        trendData.put("newSinger", newSinger);
        trendData.put("newSongList", newSongList);
        
        return R.success(null, trendData);
    }

    /**
     * 获取用户年龄分布数据
     *
     * @return 各年龄段用户数量分布
     */
    @GetMapping("/user/age/distribution")
    public R getUserAgeDistribution() {
        List<String> ageRanges = Arrays.asList(
            "18岁以下",
            "18-30岁",
            "30-40岁",
            "40-50岁",
            "50-60岁",
            "60岁以上",
            "未知"
        );
        List<Map<String, Object>> dbResults = consumerMapper.getUserAgeDistribution();
        Map<String, Integer> ageCountMap = new LinkedHashMap<>();
        for (String ageRange : ageRanges) {
            ageCountMap.put(ageRange, 0);
        }

        for (Map<String, Object> row : dbResults) {
            String ageRange = Objects.toString(row.get("ageRange"), "未知");
            int count = ((Number) row.get("count")).intValue();
            if (!ageCountMap.containsKey(ageRange)) {
                ageCountMap.put(ageRange, 0);
            }
            ageCountMap.put(ageRange, count);
        }

        List<Map<String, Object>> ageData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ageCountMap.entrySet()) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("name", entry.getKey());
            dataPoint.put("value", entry.getValue());
            ageData.add(dataPoint);
        }

        return R.success(null, ageData);
    }

    /**
     * 获取用户性别分布数据
     *
     * @return 各性别用户数量分布
     */
    @GetMapping("/user/sex/distribution")
    public R getUserSexDistribution() {
        List<Map<String, Object>> dbResults = consumerMapper.getUserSexDistribution();

        List<Map<String, Object>> sexData = new ArrayList<>();
        for (Map<String, Object> row : dbResults) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("name", row.get("sexLabel"));
            dataPoint.put("value", ((Number) row.get("count")).intValue());
            sexData.add(dataPoint);
        }

        return R.success(null, sexData);
    }

    /**
     * 获取反馈趋势数据
     *
     * @param range 取值支持 7d / 30d / 6m
     * @return 最近 7 天、30 天或半年反馈趋势
     */
    @GetMapping("/feedback/trend")
    public R getRecentFeedbackTrend(@RequestParam(defaultValue = "7d") String range) {
        List<Map<String, Object>> trendData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDateTime startDate;
        LocalDateTime endDate = today.plusDays(1).atStartOfDay();

        String normalizedRange = Optional.ofNullable(range).orElse("7d").toLowerCase(Locale.ROOT);
        if ("30d".equals(normalizedRange)) {
            startDate = today.minusDays(29).atStartOfDay();
        } else if ("6m".equals(normalizedRange)) {
            startDate = today.withDayOfMonth(1).minusMonths(5).atStartOfDay();
        } else {
            normalizedRange = "7d";
            startDate = today.minusDays(6).atStartOfDay();
        }

        List<Map<String, Object>> dbResults = feedbackMapper.getDailyFeedbackTrend(startDate, endDate);
        Map<String, Integer> dailyResultMap = new HashMap<>();
        for (Map<String, Object> row : dbResults) {
            String dateStr = row.get("date").toString();
            Integer count = ((Number) row.get("count")).intValue();
            dailyResultMap.put(dateStr, count);
        }

        if ("6m".equals(normalizedRange)) {
            YearMonth currentMonth = YearMonth.from(today);
            Map<YearMonth, Integer> monthlyResultMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : dailyResultMap.entrySet()) {
                LocalDate date = LocalDate.parse(entry.getKey());
                YearMonth yearMonth = YearMonth.from(date);
                monthlyResultMap.merge(yearMonth, entry.getValue(), Integer::sum);
            }

            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            for (int i = 5; i >= 0; i--) {
                YearMonth yearMonth = currentMonth.minusMonths(i);

                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", yearMonth.format(monthFormatter));
                dataPoint.put("count", monthlyResultMap.getOrDefault(yearMonth, 0));
                trendData.add(dataPoint);
            }
        } else {
            int days = "30d".equals(normalizedRange) ? 30 : 7;
            DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.format(DateTimeFormatter.ISO_DATE);

                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", date.format(displayFormatter));
                dataPoint.put("count", dailyResultMap.getOrDefault(dateStr, 0));
                trendData.add(dataPoint);
            }
        }

        return R.success(null, trendData);
    }
}
