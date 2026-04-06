package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.common.R;
import com.example.music.model.domain.RankList;
import com.example.music.model.request.RankListRequest;
import com.example.music.model.request.RankSongRequest;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

public interface RankListService extends IService<RankList> {

    R addRank(RankListRequest rankListAddRequest);

    R rankOfSongListId(Long songListId);

    R getUserRank(Long consumerId, Long songListId);

    R rankOfSongId(Integer songId);

    R addSongRanK(RankSongRequest rankSongRequest);

    R getUserRankSong(Integer consumerId, Integer songId);

    R deleteRankOfId(Integer type, Integer id);

    R deleteRankSong(Integer songId, Integer consumerId, HttpSession session);

    R deleteRankList(Integer songListId, Integer consumerId, HttpSession session);

    // 在 RankListService.java 中添加以下方法声明
    R getAllRankByType(Integer type, Integer currentPage, Integer pageSize, String name,String consumerName);


}
