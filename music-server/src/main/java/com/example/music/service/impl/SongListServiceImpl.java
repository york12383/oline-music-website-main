package com.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.controller.FileUploadController;
import com.example.music.mapper.CollectMapper;
import com.example.music.mapper.CommentMapper;
import com.example.music.mapper.ConsumerMapper;
import com.example.music.mapper.ListSongMapper;
import com.example.music.mapper.PlayHistoryMapper;
import com.example.music.mapper.RankListMapper;
import com.example.music.mapper.SongListMapper;
import com.example.music.model.domain.Collect;
import com.example.music.model.domain.Comment;
import com.example.music.model.domain.ListSong;
import com.example.music.model.domain.PlayHistory;
import com.example.music.model.domain.RankList;
import com.example.music.model.domain.SongList;
import com.example.music.model.request.SongListRequest;
import com.example.music.service.SongListService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SongListServiceImpl extends ServiceImpl<SongListMapper, SongList> implements SongListService {

    private static final byte SONG_LIST_COLLECT_TYPE = 1;
    private static final double HOT_PLAY_WEIGHT = 1.0;
    private static final double HOT_COLLECT_WEIGHT = 3.0;
    private static final double HOT_COMMENT_WEIGHT = 2.0;
    private static final double HOT_RATING_WEIGHT = 1.5;
    private static final double HOT_RATING_COUNT_WEIGHT = 1.0;

    @Autowired
    private SongListMapper songListMapper;
    @Value("${local.file.storage-path}")
    private String storagePath;
    @Autowired
    private ConsumerMapper consumerMapper;
    @Autowired
    private FileUploadController fileUploadController;
    @Autowired
    private CollectMapper collectMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ListSongMapper listSongMapper;
    @Autowired
    private PlayHistoryMapper playHistoryMapper;
    @Autowired
    private RankListMapper rankListMapper;

    @Override
    public R updateSongListMsg(SongListRequest updateSongListRequest) {
        SongList songList = new SongList();
        BeanUtils.copyProperties(updateSongListRequest, songList);
        if (songListMapper.updateById(songList) > 0) {
            return R.success("修改成功");
        } else {
            return R.error("修改失败");
        }
    }

    @Override
    public R updateUserSongListMsg(SongListRequest updateSongListRequest, HttpSession session) {
        String consumerName = (String) session.getAttribute("username");
        SongList songList = new SongList();
        BeanUtils.copyProperties(updateSongListRequest, songList);
        songList.setConsumer(consumerMapper.selectIdByconsumerName(consumerName));
        songList.setType(2);
        if (songListMapper.updateById(songList) > 0) {
            return R.success("修改成功");
        } else {
            return R.error("修改失败");
        }
    }

    @Override
    public R deleteSongList(Integer id) {
        if (songListMapper.deleteById(id) > 0) {
            return R.success("删除成功");
        } else {
            return R.error("删除失败");
        }
    }

    @Override
    public R allSongList() {
        return R.success(null, songListMapper.selectList(null));
    }

    @Override
    public List<SongList> findAllSong() {
        return songListMapper.selectList(null);
    }

    @Override
    public R likeTitle(String title) {
        QueryWrapper<SongList> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("title", title);
        queryWrapper.eq("type", 1);
        return R.success(null, songListMapper.selectList(queryWrapper));
    }

    @Override
    public R likeStyle(String style) {
        QueryWrapper<SongList> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("style", style);
        queryWrapper.eq("type", 1);
        return R.success(null, songListMapper.selectList(queryWrapper));
    }

    @Override
    public R songListOfId(Integer id) {
        QueryWrapper<SongList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        return R.success(null, songListMapper.selectList(queryWrapper));
    }

    @Override
    public R songListByPage(Integer currentPage, Integer pageSize) {
        try {
            Page<SongList> page = new Page<>(currentPage, pageSize);
            LambdaQueryWrapper<SongList> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SongList::getType, 1);
            queryWrapper.orderByDesc(SongList::getId);
            IPage<SongList> songListPage = songListMapper.selectPage(page, queryWrapper);
            Map<String, Object> result = new HashMap<>();
            result.put("records", songListPage.getRecords());
            result.put("total", songListPage.getTotal());
            result.put("size", songListPage.getSize());
            result.put("current", songListPage.getCurrent());
            result.put("pages", songListPage.getPages());
            return R.success("查询成功", result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("查询失败: " + e.getMessage());
        }
    }

    @Override
    public R getSongListByConsumerId(HttpSession session) {
        try {
            int id = (int) session.getAttribute("userId");
            QueryWrapper<SongList> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("consumer", id);
            List<SongList> songList = songListMapper.selectList(queryWrapper);
            return R.success("查询成功", songList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public R addSongList(SongListRequest addSongListRequest) {
        SongList songList = new SongList();
        BeanUtils.copyProperties(addSongListRequest, songList);
        songList.setPic("/img/songListPic/123.jpg");
        songList.setType(3);
        songList.setConsumer(1);
        if (songListMapper.insert(songList) > 0) {
            return R.success("添加成功");
        } else {
            return R.error("添加失败");
        }
    }

    @Override
    public R addUserSongList(SongListRequest addSongListRequest, HttpSession session) {
        String consumerName = (String) session.getAttribute("username");
        if (consumerName == null) {
            return R.error("请先登录");
        }
        SongList songList = new SongList();
        BeanUtils.copyProperties(addSongListRequest, songList);
        songList.setPic("/img/songListPic/123.jpg");
        songList.setConsumer(consumerMapper.selectIdByconsumerName(consumerName));
        songList.setType(2);
        if (songListMapper.insert(songList) > 0) {
            return R.success("添加成功");
        } else {
            return R.error("添加失败");
        }
    }

    @Override
    public R updateSongListImg(MultipartFile avatorFile, @RequestParam("id") int id) {
        String fileName = fileUploadController.uploadSonglistImgFile(avatorFile);
        String imgPath = "/img/songListPic/" + fileName;
        SongList songList = new SongList();
        songList.setId(id);
        songList.setPic(imgPath);
        SongList existingSongList = songListMapper.selectById(id);
        if (existingSongList != null) {
            songList.setConsumer(existingSongList.getConsumer());
        } else {
            return R.error("歌单不存在");
        }
        if (songListMapper.updateById(songList) > 0) {
            return R.success("上传成功", imgPath);
        } else {
            return R.error("上传失败");
        }
    }

    @Override
    public R getHotSongListsByRating(Integer limit) {
        try {
            if (limit == null || limit <= 0) {
                limit = 10;
            }

            QueryWrapper<SongList> activeSongListWrapper = new QueryWrapper<>();
            activeSongListWrapper.eq("type", 1);
            List<SongList> activeSongLists = new ArrayList<>(songListMapper.selectList(activeSongListWrapper));
            Map<Integer, Double> hotScoreMap = new HashMap<>();

            for (SongList songList : activeSongLists) {
                Integer songListId = songList.getId();
                List<Integer> songIds = getSongIdsBySongListId(songListId);
                int playCount = countSongListPlay(songIds);
                int collectCount = countSongListCollect(songListId);
                int commentCount = countSongListComment(songListId);
                int ratingCount = countSongListRating(songListId);
                double averageRating = calculateSongListAverageRating(songListId);

                double hotScore = playCount * HOT_PLAY_WEIGHT
                        + collectCount * HOT_COLLECT_WEIGHT
                        + commentCount * HOT_COMMENT_WEIGHT
                        + averageRating * HOT_RATING_WEIGHT
                        + ratingCount * HOT_RATING_COUNT_WEIGHT;

                hotScoreMap.put(songListId, hotScore);
            }

            activeSongLists.sort((left, right) -> {
                double rightScore = hotScoreMap.getOrDefault(right.getId(), 0.0);
                double leftScore = hotScoreMap.getOrDefault(left.getId(), 0.0);
                int compare = Double.compare(rightScore, leftScore);
                if (compare != 0) {
                    return compare;
                }
                return Integer.compare(right.getId(), left.getId());
            });

            int endIndex = Math.min(limit, activeSongLists.size());
            return R.success("获取热门歌单成功", new ArrayList<>(activeSongLists.subList(0, endIndex)));
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("获取热门歌单失败: " + e.getMessage());
        }
    }

    private List<Integer> getSongIdsBySongListId(Integer songListId) {
        QueryWrapper<ListSong> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id", songListId);
        List<ListSong> listSongs = listSongMapper.selectList(queryWrapper);
        List<Integer> songIds = new ArrayList<>();
        for (ListSong listSong : listSongs) {
            if (listSong.getSongId() != null) {
                songIds.add(listSong.getSongId());
            }
        }
        return songIds;
    }

    private int countSongListPlay(List<Integer> songIds) {
        if (songIds.isEmpty()) {
            return 0;
        }
        QueryWrapper<PlayHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("song_id", songIds);
        return Math.toIntExact(playHistoryMapper.selectCount(queryWrapper));
    }

    private int countSongListCollect(Integer songListId) {
        QueryWrapper<Collect> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", SONG_LIST_COLLECT_TYPE);
        queryWrapper.eq("song_list_id", songListId);
        return Math.toIntExact(collectMapper.selectCount(queryWrapper));
    }

    private int countSongListComment(Integer songListId) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id", songListId);
        return Math.toIntExact(commentMapper.selectCount(queryWrapper));
    }

    private int countSongListRating(Integer songListId) {
        QueryWrapper<RankList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id", songListId);
        return Math.toIntExact(rankListMapper.selectCount(queryWrapper));
    }

    private double calculateSongListAverageRating(Integer songListId) {
        QueryWrapper<RankList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id", songListId);
        List<RankList> rankLists = rankListMapper.selectList(queryWrapper);
        if (rankLists.isEmpty()) {
            return 0;
        }

        double scoreSum = 0;
        for (RankList rankList : rankLists) {
            scoreSum += rankList.getScore();
        }
        return scoreSum / rankLists.size();
    }
}