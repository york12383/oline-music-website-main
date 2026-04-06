package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.request.SongListRequest;
import com.example.music.service.SongListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;

@RestController
public class SongListController {

    @Autowired
    private SongListService songListService;

    @PostMapping("/songList/add")
    public R addSongList(@RequestBody SongListRequest addSongListRequest) {
        return songListService.addSongList(addSongListRequest);
    }

    @PostMapping("/songList/user/add")
    public R addUserSongList(@RequestBody SongListRequest addSongListRequest, HttpSession session) {
        return songListService.addUserSongList(addSongListRequest, session);
    }

    @GetMapping("/songList/delete")
    public R deleteSongList(@RequestParam int id) {
        return songListService.deleteSongList(id);
    }

    @GetMapping("/songList")
    public R allSongList() {
        return songListService.allSongList();
    }

    @GetMapping("/songList/detail")
    public R songListOfId(@RequestParam Integer id) {
        return songListService.songListOfId(id);
    }

    @GetMapping("/songList/likeTitle/detail")
    public R songListOfLikeTitle(@RequestParam String title) {
        return songListService.likeTitle('%' + title + '%');
    }

    @GetMapping("/songList/style/detail")
    public R songListOfStyle(@RequestParam String style) {
        return songListService.likeStyle('%' + style + '%');
    }

    @PostMapping("/songList/update")
    public R updateSongListMsg(@RequestBody SongListRequest updateSongListRequest) {
        return songListService.updateSongListMsg(updateSongListRequest);
    }

    @PostMapping("/songList/user/update")
    public R updateUserSongListMsg(@RequestBody SongListRequest updateSongListRequest, HttpSession session) {
        return songListService.updateUserSongListMsg(updateSongListRequest, session);
    }

    @PostMapping("/songList/img/update")
    public R updateSongListPic(@RequestParam("file") MultipartFile avatorFile, @RequestParam("id") int id) {
        return songListService.updateSongListImg(avatorFile, id);
    }

    @GetMapping("/songList/page")
    public R songListByPage(@RequestParam(defaultValue = "1") Integer currentPage,
                            @RequestParam(defaultValue = "15") Integer pageSize) {
        return songListService.songListByPage(currentPage, pageSize);
    }

    @GetMapping("/songList/user/detail")
    public R getSongListByConsumerId(@RequestParam int id, HttpSession session) {
        return songListService.getSongListByConsumerId(session);
    }

    @GetMapping("/songList/user/all")
    public R getHotSongListsByRating() {
        return songListService.getHotSongListsByRating(3);
    }

    @GetMapping("/songList/hot")
    public R getHotSongLists(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return songListService.getHotSongListsByRating(limit);
    }
}