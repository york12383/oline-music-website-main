package com.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.controller.FileUploadController;
import com.example.music.mapper.AdminMapper;
import com.example.music.mapper.CollectMapper;
import com.example.music.mapper.CommentMapper;
import com.example.music.mapper.ConsumerMapper;
import com.example.music.mapper.PlayHistoryMapper;
import com.example.music.mapper.RankSongMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.model.domain.Collect;
import com.example.music.model.domain.Comment;
import com.example.music.model.domain.PlayHistory;
import com.example.music.model.domain.RankSong;
import com.example.music.model.domain.Song;
import com.example.music.model.request.SongRequest;
import com.example.music.service.SongService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SongServiceImpl extends ServiceImpl<SongMapper, Song> implements SongService {

    private static final double HOT_PLAY_WEIGHT = 1.0;
    private static final double HOT_COLLECT_WEIGHT = 3.0;
    private static final double HOT_COMMENT_WEIGHT = 2.0;
    private static final double HOT_RATING_WEIGHT = 1.5;
    private static final double HOT_RATING_COUNT_WEIGHT = 1.0;

    @Autowired
    private SongMapper songMapper;

    @Value("${local.file.storage-path}")
    private String storagePath;

    @Autowired
    private FileUploadController fileUploadController;

    @Autowired
    private ConsumerMapper consumerMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private CollectMapper collectMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RankSongMapper rankSongMapper;

    @Autowired
    private PlayHistoryMapper playHistoryMapper;

    @Override
    public R allSong() {
        return R.success(null, songMapper.selectList(null));
    }

    @Override
    public R hotSongs(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<Song> activeSongs = new ArrayList<>(songMapper.selectList(new QueryWrapper<Song>().eq("type", 1)));
        if (activeSongs.isEmpty()) {
            return R.success("获取热门歌曲成功", new ArrayList<>());
        }

        Map<Integer, Integer> playCountBySong = new HashMap<>();
        for (PlayHistory playHistory : playHistoryMapper.selectList(new QueryWrapper<>())) {
            playCountBySong.merge(playHistory.getSongId(), 1, Integer::sum);
        }

        Map<Integer, Integer> collectCountBySong = new HashMap<>();
        for (Collect collect : collectMapper.selectList(new QueryWrapper<>())) {
            if (collect.getSongId() != null && collect.getType() != null && collect.getType() == 0) {
                collectCountBySong.merge(collect.getSongId(), 1, Integer::sum);
            }
        }

        Map<Integer, Integer> commentCountBySong = new HashMap<>();
        for (Comment comment : commentMapper.selectList(new QueryWrapper<>())) {
            if (comment.getSongId() != null) {
                commentCountBySong.merge(comment.getSongId(), 1, Integer::sum);
            }
        }

        Map<Integer, Double> ratingSumBySong = new HashMap<>();
        Map<Integer, Integer> ratingCountBySong = new HashMap<>();
        for (RankSong rankSong : rankSongMapper.selectList(new QueryWrapper<>())) {
            if (rankSong.getSongId() != null) {
                ratingSumBySong.merge(rankSong.getSongId(), rankSong.getScore().doubleValue(), Double::sum);
                ratingCountBySong.merge(rankSong.getSongId(), 1, Integer::sum);
            }
        }

        Map<Integer, Double> hotScoreBySong = new HashMap<>();
        for (Song song : activeSongs) {
            int playCount = playCountBySong.getOrDefault(song.getId(), 0);
            int collectCount = collectCountBySong.getOrDefault(song.getId(), 0);
            int commentCount = commentCountBySong.getOrDefault(song.getId(), 0);
            int ratingCount = ratingCountBySong.getOrDefault(song.getId(), 0);
            double avgRating = ratingCount == 0 ? 0 : ratingSumBySong.getOrDefault(song.getId(), 0.0) / ratingCount;

            double hotScore = playCount * HOT_PLAY_WEIGHT
                    + collectCount * HOT_COLLECT_WEIGHT
                    + commentCount * HOT_COMMENT_WEIGHT
                    + avgRating * HOT_RATING_WEIGHT
                    + ratingCount * HOT_RATING_COUNT_WEIGHT;

            hotScoreBySong.put(song.getId(), hotScore);
        }

        activeSongs.sort((left, right) -> {
            int compare = Double.compare(
                    hotScoreBySong.getOrDefault(right.getId(), 0.0),
                    hotScoreBySong.getOrDefault(left.getId(), 0.0)
            );
            if (compare != 0) {
                return compare;
            }
            return Comparator.nullsLast(Date::compareTo).compare(right.getCreateTime(), left.getCreateTime());
        });

        return R.success("获取热门歌曲成功", new ArrayList<>(activeSongs.subList(0, Math.min(limit, activeSongs.size()))));
    }

    // 上传歌曲
    @Override
    public R addSong(SongRequest addSongRequest, MultipartFile lrcfile, MultipartFile mpfile, HttpSession session) {
        Song song = new Song();
        BeanUtils.copyProperties(addSongRequest, song);
        String pic = "/img/songPic/tubiao.jpg";

        String fileName = mpfile.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            return R.error("文件名不能为空");
        }
        if (mpfile.getSize() > 10 * 1024 * 1024) {
            return R.error("文件大小不能超过10MB");
        }
        if (!fileName.contains(".")) {
            return R.error("文件必须包含扩展名");
        }

        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        String[] allowedExtensions = {"mp3", "wav", "flac", "aac", "ogg", "m4a"};
        if (!Arrays.asList(allowedExtensions).contains(fileExtension)) {
            return R.error("不支持的文件格式");
        }

        String storedFileName = fileUploadController.uploadFile(mpfile);
        String storeUrlPath = "/song/" + storedFileName;
        song.setCreateTime(new Date());
        song.setUpdateTime(new Date());
        song.setPic(pic);
        song.setUrl(storeUrlPath);
        song.setType(3);

        if (lrcfile != null && !lrcfile.isEmpty()) {
            try {
                song.setLyric(new String(lrcfile.getBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (song.getLyric() == null || song.getLyric().trim().isEmpty()) {
            song.setLyric("[00:00.00]纯音乐，请欣赏");
        }

        if (songMapper.insert(song) > 0) {
            return R.success("上传成功");
        }
        return R.error("上传失败");
    }

    @Override
    public R updateSongMsg(SongRequest updateSongRequest, HttpSession session) {
        Song song = new Song();
        BeanUtils.copyProperties(updateSongRequest, song);
        song.setType(3);
        if (songMapper.updateById(song) > 0) {
            return R.success("修改成功");
        } else {
            return R.error("修改失败");
        }
    }

    @Override
    public R updateSongUrl(MultipartFile urlFile, int id) {
        Song song = songMapper.selectById(id);
        String path = song.getUrl();
        String[] parts = path.split("/");
        String fileName = parts[parts.length - 1];

        String filePath = storagePath + File.separator + "song" + File.separator + fileName;
        File fileToDelete = new File(filePath);
        if (fileToDelete.exists()) {
            fileToDelete.delete();
        }
        String s = fileUploadController.uploadFile(urlFile);
        String storeUrlPath = "/song/" + s;
        song.setId(id);
        song.setUrl(storeUrlPath);
        song.setType(3);
        if (songMapper.updateById(song) > 0) {
            return R.success("更新成功", storeUrlPath);
        } else {
            return R.error("更新失败");
        }
    }

    @Override
    public R updateSongPic(MultipartFile urlFile, int id) {
        String s = fileUploadController.uploadSongImgFile(urlFile);
        String storeUrlPath = "/img/songPic/" + s;
        Song song = new Song();
        song.setId(id);
        song.setPic(storeUrlPath);
        song.setType(3);
        if (songMapper.updateById(song) > 0) {
            return R.success("上传成功", storeUrlPath);
        }
        return R.error("上传失败");
    }

    @Override
    public R deleteSong(Integer id) {
        if (songMapper.deleteById(id) > 0) {
            return R.success("删除成功");
        } else {
            return R.error("删除失败");
        }
    }

    @Override
    public R songOfSingerId(Integer singerId, HttpSession session) {
        if (session.getAttribute("name") == null) {
            QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("singer_id", singerId);
            queryWrapper.eq("type", 1);
            return R.success(null, songMapper.selectList(queryWrapper));
        }

        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("singer_id", singerId);
        return R.success(null, songMapper.selectList(queryWrapper));
    }

    @Override
    public R songPageOfSingerId(Integer singerId, Integer pageNum, Integer pageSize, HttpSession session) {
        int resolvedPageNum = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        int resolvedPageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;

        Page<Song> page = new Page<>(resolvedPageNum, resolvedPageSize);
        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("singer_id", singerId);
        if (session.getAttribute("name") == null) {
            queryWrapper.eq("type", 1);
        }
        queryWrapper.orderByAsc("id");

        Page<Song> songPage = songMapper.selectPage(page, queryWrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", songPage.getRecords());
        result.put("total", songPage.getTotal());
        result.put("size", songPage.getSize());
        result.put("current", songPage.getCurrent());
        return R.success(null, result);
    }

    @Override
    public R songOfuserSingerId(HttpSession session) {
        try {
            if (session.getAttribute("userId") == null) {
                return R.error("请重新登录");
            }
            int userId = (int) session.getAttribute("userId");
            int singerId = consumerMapper.selectById(userId).getSingerId();
            QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("singer_id", singerId);
            return R.success(null, songMapper.selectList(queryWrapper));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public R songOfId(Integer id) {
        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        return R.success(null, songMapper.selectList(queryWrapper));
    }

    @Override
    public R songOfSingerName(String name) {
        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", name);
        List<Song> songs = songMapper.selectList(queryWrapper);
        if (songs.isEmpty()) {
            return R.error("添加失败，没有找到该歌,无法加入该歌单");
        }

        return R.success(null, songs);
    }

    @Override
    public R updateSongLrc(MultipartFile lrcFile, int id) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            return R.error("歌曲不存在");
        }
        if (lrcFile != null && !lrcFile.isEmpty()) {
            try {
                song.setLyric(new String(lrcFile.getBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        song.setType(3);
        if (songMapper.updateById(song) > 0) {
            return R.success("更新成功");
        }
        return R.error("更新失败");
    }

    @Override
    public R batchUpdateSongLrc(MultipartFile[] lrcFiles, Integer singerId, HttpSession session) {
        if (session.getAttribute("name") == null) {
            return R.error("权限不足，只有管理员可以执行此操作");
        }
        if (lrcFiles == null || lrcFiles.length == 0) {
            return R.error("请先选择至少一个LRC文件");
        }

        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        if (singerId != null) {
            queryWrapper.eq("singer_id", singerId);
        }
        List<Song> candidates = songMapper.selectList(queryWrapper);
        if (candidates.isEmpty()) {
            return R.error("当前范围内没有可匹配的歌曲");
        }

        Map<Integer, Song> songById = candidates.stream()
                .collect(Collectors.toMap(Song::getId, song -> song, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<Song>> songByFullName = candidates.stream()
                .collect(Collectors.groupingBy(song -> normalizeSongName(song.getName()), LinkedHashMap::new, Collectors.toList()));
        Map<String, List<Song>> songByTitle = candidates.stream()
                .collect(Collectors.groupingBy(song -> normalizeSongName(extractSongTitle(song.getName())), LinkedHashMap::new, Collectors.toList()));

        int successCount = 0;
        List<String> unmatchedFiles = new ArrayList<>();
        List<String> invalidFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (MultipartFile file : lrcFiles) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".lrc")) {
                invalidFiles.add(originalFilename == null ? "未命名文件" : originalFilename);
                continue;
            }

            Song matchedSong = matchSongByLyricFile(originalFilename, singerId, songById, songByFullName, songByTitle);
            if (matchedSong == null) {
                unmatchedFiles.add(originalFilename);
                continue;
            }

            try {
                matchedSong.setLyric(new String(file.getBytes(), StandardCharsets.UTF_8));
                matchedSong.setType(3);
                matchedSong.setUpdateTime(new Date());
                if (songMapper.updateById(matchedSong) > 0) {
                    successCount++;
                } else {
                    failedFiles.add(originalFilename);
                }
            } catch (IOException e) {
                failedFiles.add(originalFilename);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("unmatchedFiles", unmatchedFiles);
        result.put("invalidFiles", invalidFiles);
        result.put("failedFiles", failedFiles);
        result.put("totalFiles", lrcFiles.length);

        String message = String.format("批量导入完成：成功 %d 个，未匹配 %d 个，失败 %d 个",
                successCount, unmatchedFiles.size(), failedFiles.size() + invalidFiles.size());
        return R.success(message, result);
    }

    @Override
    public R updateSongStatus(int songId, int singerId, HttpSession session) {
        int userId = (int) session.getAttribute("userId");
        if (singerId != consumerMapper.selectById(userId).getSingerId()) {
            return R.error("请勿越权操作");
        }

        Song song = songMapper.selectById(songId);
        if (song == null) {
            return R.error("歌曲不存在");
        }
        if (song.getSingerId() != singerId) {
            return R.error("歌手信息不匹配");
        }
        song.setType(3);

        if (songMapper.updateById(song) > 0) {
            return R.success("歌曲状态更新成功");
        } else {
            return R.error("歌曲状态更新失败");
        }
    }

    @Override
    public R adminUpdateSongStatus(int songId, int type) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            return R.error("歌曲不存在");
        }

        if (type < 1 || type > 4) {
            return R.error("无效的歌曲状态值");
        }

        song.setType(type);
        song.setUpdateTime(new Date());

        if (songMapper.updateById(song) > 0) {
            return R.success("歌曲状态更新成功");
        } else {
            return R.error("歌曲状态更新失败");
        }
    }

    /**
     * 分页获取歌曲列表
     */
    @Override
    public R getPageSongs(Integer pageNum, Integer pageSize, String name, String lyricStatus, String resourceStatus) {
        Page<Song> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Song> queryWrapper = new QueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }
        applyLyricStatusFilter(queryWrapper, lyricStatus);
        applyResourceStatusFilter(queryWrapper, resourceStatus);
        queryWrapper.orderByDesc("update_time");
        Page<Song> songPage = songMapper.selectPage(page, queryWrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", songPage.getRecords());
        result.put("total", songPage.getTotal());
        result.put("size", songPage.getSize());
        result.put("current", songPage.getCurrent());
        return R.success(null, result);
    }

    private void applyLyricStatusFilter(QueryWrapper<Song> queryWrapper, String lyricStatus) {
        if (lyricStatus == null || lyricStatus.trim().isEmpty() || "all".equalsIgnoreCase(lyricStatus)) {
            return;
        }

        switch (lyricStatus) {
            case "missing":
                queryWrapper.apply("TRIM(COALESCE(lyric, '')) = ''");
                break;
            case "health_missing":
                queryWrapper.and(wrapper -> wrapper
                        .apply("TRIM(COALESCE(lyric, '')) = ''")
                        .or()
                        .and(inner -> inner.like("lyric", "暂无歌词").notLike("lyric", "纯音乐")));
                break;
            case "placeholder":
                queryWrapper.like("lyric", "暂无歌词").notLike("lyric", "纯音乐");
                break;
            case "abnormal":
                queryWrapper.apply("TRIM(COALESCE(lyric, '')) <> ''")
                        .notLike("lyric", "暂无歌词")
                        .notLike("lyric", "纯音乐")
                        .apply("lyric NOT REGEXP '\\\\[[0-9]{2}:[0-9]{2}(\\\\.[0-9]{2,3})?\\\\]'");
                break;
            case "ready":
                queryWrapper.and(wrapper -> wrapper
                        .and(inner -> inner.apply("TRIM(COALESCE(lyric, '')) <> ''")
                                .notLike("lyric", "暂无歌词")
                                .apply("lyric REGEXP '\\\\[[0-9]{2}:[0-9]{2}(\\\\.[0-9]{2,3})?\\\\]'"))
                        .or()
                        .like("lyric", "纯音乐"));
                break;
            default:
                break;
        }
    }

    private void applyResourceStatusFilter(QueryWrapper<Song> queryWrapper, String resourceStatus) {
        if (resourceStatus == null || resourceStatus.trim().isEmpty() || "all".equalsIgnoreCase(resourceStatus)) {
            return;
        }

        if ("missing_audio".equalsIgnoreCase(resourceStatus)) {
            queryWrapper.apply("TRIM(COALESCE(url, '')) = ''");
        }
    }

    private Song matchSongByLyricFile(String originalFilename,
                                      Integer singerId,
                                      Map<Integer, Song> songById,
                                      Map<String, List<Song>> songByFullName,
                                      Map<String, List<Song>> songByTitle) {
        String baseName = extractBaseName(originalFilename);
        if (baseName.isEmpty()) {
            return null;
        }

        if (baseName.matches("\\d+")) {
            Song byId = songById.get(Integer.parseInt(baseName));
            if (byId != null) {
                return byId;
            }
        }

        List<Song> exactSongs = songByFullName.get(normalizeSongName(baseName));
        if (exactSongs != null && exactSongs.size() == 1) {
            return exactSongs.get(0);
        }

        if (singerId != null) {
            List<Song> titleSongs = songByTitle.get(normalizeSongName(baseName));
            if (titleSongs != null && titleSongs.size() == 1) {
                return titleSongs.get(0);
            }
        }

        return null;
    }

    private String extractBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName.trim();
        }
        return fileName.substring(0, dotIndex).trim();
    }

    private String extractSongTitle(String fullName) {
        if (fullName == null) {
            return "";
        }
        int splitIndex = fullName.indexOf('-');
        if (splitIndex < 0 || splitIndex == fullName.length() - 1) {
            return fullName;
        }
        return fullName.substring(splitIndex + 1);
    }

    private String normalizeSongName(String name) {
        return Objects.toString(name, "")
                .trim()
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }
}
