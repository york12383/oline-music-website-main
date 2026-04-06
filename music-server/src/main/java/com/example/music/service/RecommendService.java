package com.example.music.service;

import com.example.music.model.domain.Singer;
import com.example.music.model.domain.Song;
import com.example.music.model.domain.SongList;
import com.example.music.model.request.RecommendBootstrapRequest;

import java.util.List;
import java.util.Map;

public interface RecommendService {
    // 协同过滤推荐歌曲
    List<Song> selectListByRecommend(Integer userId, Integer limit);

    // 协同过滤推荐歌单
    List<SongList> selectSongListsByRecommend(Integer userId, Integer limit);

    // 协同过滤推荐歌手
    List<Singer> selectSingersByRecommend(Integer userId, Integer limit);

    Map<String, Object> getBootstrapOptions(Integer userId);

    void saveBootstrapPreferences(Integer userId, RecommendBootstrapRequest request);

    Map<String, Object> getRecommendDebugProfile(Integer userId);
}
