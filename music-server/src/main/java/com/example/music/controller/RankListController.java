package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.request.RankListRequest;
import com.example.music.model.request.RankSongRequest;
import com.example.music.service.RankListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

/**
 * 评分功能
 */
@RestController
public class RankListController {

    @Autowired
    private RankListService rankListService;

    // 提交评分
    @PostMapping("/rankList/add")
    public R addRank(@RequestBody RankListRequest rankListAddRequest) {
        return rankListService.addRank(rankListAddRequest);
    }

    // 获取指定歌单的评分
    @GetMapping("/rankList")
    public R rankOfSongListId(@RequestParam Long songListId) {
        return rankListService.rankOfSongListId(songListId);
    }

    // 获取指定用户的歌单评分
    @GetMapping("/rankList/user")
    public R getUserRank(@RequestParam(required = false) Long consumerId, @RequestParam Long songListId) {
        return rankListService.getUserRank(consumerId, songListId);
    }

    // 获取指定歌单的评分
    @GetMapping("rankList/songRank")
    public R rankOfSongId(@RequestParam(required = false) int songId ) {
        return rankListService.rankOfSongId(songId);
    }

    // 添加歌曲评分
    @PostMapping("/rankList/addsong")
    public R addSongRanK(@RequestBody RankSongRequest rankSongRequest,HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return R.error("用户未登录或没有权力");
        }
        rankSongRequest.setConsumerId((int) session.getAttribute("userId"));
        return rankListService.addSongRanK(rankSongRequest);
    }

    // 获取指定用户的歌单评分
    @GetMapping("rankList/userRankSong")
    public R getUserRankSong(@RequestParam(required = false) int consumerId, @RequestParam int songId) {
        return rankListService.getUserRankSong(consumerId, songId);
    }

    //获取所有评分
    @GetMapping("/rankList/allByType")
    public R getAllRankByType(@RequestParam Integer type,
                              @RequestParam(defaultValue = "1") Integer currentPage,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(required = false) String name,
                              @RequestParam(required = false) String consumerName) {
        return rankListService.getAllRankByType(type, currentPage, pageSize, name, consumerName);
    }

    //我也不知道这是删什么的了1，删了就报错了
    @DeleteMapping("/rankList/delete")
    public R deleteRankOfId(@RequestParam("type") int type, @RequestParam("id") int id, HttpSession session) {
        session.getAttribute("consumerId");
        return rankListService.deleteRankOfId(type,id);
    }

    //我也不知道这是删什么的了2，删了就报错了
    @DeleteMapping("rankList/deleted")
    public R deleteRankOfId(@RequestParam("type") int type, @RequestParam("id") int id) {
        return rankListService.deleteRankOfId(type,id);
    }

    //我也不知道这是删什么的了3，删了就报错了
    @DeleteMapping("rankSong/deleted")
    public R deleteRankSong(@RequestParam Integer songId,@RequestParam Integer consumerId,HttpSession session) {
        return rankListService.deleteRankSong(songId, consumerId, session);
    }


    // 删除歌单评分，删除用户对指定歌单的评分
    @DeleteMapping("rankList/deletedd")
    public R deleteRankList(@RequestParam Integer songListId,@RequestParam Integer consumerId,HttpSession session) {
        return rankListService.deleteRankList(songListId, consumerId, session);
    }


}
