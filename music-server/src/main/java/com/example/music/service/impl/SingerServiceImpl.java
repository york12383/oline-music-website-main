package com.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.controller.FileUploadController;
import com.example.music.mapper.CollectMapper;
import com.example.music.mapper.CommentMapper;
import com.example.music.mapper.ConsumerMapper;
import com.example.music.mapper.PlayHistoryMapper;
import com.example.music.mapper.RankSongMapper;
import com.example.music.mapper.SingerMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.model.domain.Collect;
import com.example.music.model.domain.Comment;
import com.example.music.model.domain.Consumer;
import com.example.music.model.domain.PlayHistory;
import com.example.music.model.domain.RankSong;
import com.example.music.model.domain.Singer;
import com.example.music.model.domain.Song;
import com.example.music.model.request.SingerRequest;
import com.example.music.service.SingerService;
import com.example.music.utils.DefaultAvatarUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SingerServiceImpl extends ServiceImpl<SingerMapper, Singer> implements SingerService {

    private static final double HOT_PLAY_WEIGHT = 1.0;
    private static final double HOT_COLLECT_WEIGHT = 3.0;
    private static final double HOT_COMMENT_WEIGHT = 2.0;
    private static final double HOT_RATING_WEIGHT = 1.5;
    private static final double HOT_RATING_COUNT_WEIGHT = 1.0;

    @Autowired
    private SingerMapper singerMapper;
    @Autowired
    private ConsumerMapper consumerMapper;
    @Autowired
    private SongMapper songMapper;
    @Autowired
    private CollectMapper collectMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private RankSongMapper rankSongMapper;
    @Autowired
    private PlayHistoryMapper playHistoryMapper;

    @Override
    public R updateSingerMsg(SingerRequest updateSingerRequest) {
        Singer singer = new Singer();
        BeanUtils.copyProperties(updateSingerRequest, singer);
        if (singerMapper.updateById(singer) > 0) {
            return R.success("修改成功");
        } else {
            return R.error("修改失败");
        }
    }

    @Autowired
    private FileUploadController fileUploadController;

    @Override
    public R updateSingerPic(MultipartFile avatorFile, int id) {
        String fileName = avatorFile.getOriginalFilename();
        fileUploadController.uploadImgFile(avatorFile);
        String imgPath = "/img/singerPic/" + fileName;
        Singer singer = new Singer();
        singer.setId(id);
        singer.setPic(imgPath);
        if (singerMapper.updateById(singer) > 0) {
            return R.success("上传成功", imgPath);
        } else {
            return R.error("上传失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deleteSinger(Integer id) {
        consumerMapper.update(null, new UpdateWrapper<Consumer>()
                .eq("singer_id", id)
                .set("singer_id", null));

        if (singerMapper.deleteById(id) > 0) {
            return R.success("删除成功");
        } else {
            return R.error("删除失败");
        }
    }

    @Override
    public R allSinger() {
        return R.success(null, singerMapper.selectList(null));
    }

    @Override
    public R addSinger(SingerRequest addSingerRequest) {
        Singer singer = new Singer();
        BeanUtils.copyProperties(addSingerRequest, singer);
        singer.setPic(DefaultAvatarUtils.resolvePreparedDefaultAvatar(null, singer.getName()));
        if (singerMapper.insert(singer) > 0) {
            return R.success("添加成功");
        } else {
            return R.error("添加失败");
        }
    }

    @Override
    public R singerOfName(String name) {
        QueryWrapper<Singer> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", name);
        return R.success(null, singerMapper.selectList(queryWrapper));
    }

    @Override
    public R singerOfSex(Integer sex) {
        QueryWrapper<Singer> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("sex", sex);
        return R.success(null, singerMapper.selectList(queryWrapper));
    }

    @Override
    public R hotSingers(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<Song> activeSongs = songMapper.selectList(new QueryWrapper<Song>().eq("type", 1));
        if (activeSongs.isEmpty()) {
            return R.success(null, Collections.emptyList());
        }

        Map<Integer, Singer> singerById = new HashMap<>();
        for (Singer singer : singerMapper.selectList(null)) {
            singerById.put(singer.getId(), singer);
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

        Map<Integer, Double> singerHotScore = new HashMap<>();
        Map<Integer, Date> singerLatestSongTime = new HashMap<>();
        for (Song song : activeSongs) {
            Integer singerId = song.getSingerId();
            if (singerId == null || !singerById.containsKey(singerId)) {
                continue;
            }

            int playCount = playCountBySong.getOrDefault(song.getId(), 0);
            int collectCount = collectCountBySong.getOrDefault(song.getId(), 0);
            int commentCount = commentCountBySong.getOrDefault(song.getId(), 0);
            int ratingCount = ratingCountBySong.getOrDefault(song.getId(), 0);
            double avgRating = ratingCount == 0 ? 0 : ratingSumBySong.getOrDefault(song.getId(), 0.0) / ratingCount;

            double songHotScore = playCount * HOT_PLAY_WEIGHT
                    + collectCount * HOT_COLLECT_WEIGHT
                    + commentCount * HOT_COMMENT_WEIGHT
                    + avgRating * HOT_RATING_WEIGHT
                    + ratingCount * HOT_RATING_COUNT_WEIGHT;

            singerHotScore.merge(singerId, songHotScore, Double::sum);

            Date latestSongTime = singerLatestSongTime.get(singerId);
            if (latestSongTime == null || (song.getCreateTime() != null && song.getCreateTime().after(latestSongTime))) {
                singerLatestSongTime.put(singerId, song.getCreateTime());
            }
        }

        List<Map.Entry<Integer, Double>> rankedSingerIds = new ArrayList<>(singerHotScore.entrySet());
        rankedSingerIds.sort((left, right) -> {
            int heatCompare = Double.compare(right.getValue(), left.getValue());
            if (heatCompare != 0) {
                return heatCompare;
            }
            Date rightDate = singerLatestSongTime.get(right.getKey());
            Date leftDate = singerLatestSongTime.get(left.getKey());
            return Comparator.nullsLast(Date::compareTo).compare(rightDate, leftDate);
        });

        List<Singer> hotSingers = new ArrayList<>();
        for (Map.Entry<Integer, Double> rankedSinger : rankedSingerIds) {
            Singer singer = singerById.get(rankedSinger.getKey());
            if (singer != null) {
                hotSingers.add(singer);
            }
            if (hotSingers.size() >= limit) {
                break;
            }
        }

        return R.success(null, hotSingers);
    }
}
