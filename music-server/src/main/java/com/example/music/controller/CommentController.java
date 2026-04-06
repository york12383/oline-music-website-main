package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.request.CommentRequest;
import com.example.music.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
public class CommentController {
    @Autowired
    private CommentService commentService;


    //提交评论
    @PostMapping("/comment/add")
    public R addComment(@RequestBody CommentRequest addCommentRequest) {
        return commentService.addComment(addCommentRequest);
    }

    // 删除评论
    @GetMapping("/comment/delete")
    public R deleteComment(@RequestParam Integer id, HttpSession session) {
        return commentService.deleteComment(id, session);
    }

    // 管理员删除评论
    @DeleteMapping("/comment/delete")
    public R deleteCommentd(@RequestParam Integer id , HttpSession session) {
        if (session.getAttribute("name") == null)
            return R.error("请先登录");
        return commentService.deleteComment(id,session);
    }



    // 获得指定歌曲 ID 的评论列表
    @GetMapping("/comment/song/detail")
    public R commentOfSongId(@RequestParam Integer songId) {
        return commentService.commentOfSongId(songId);
    }

    // 获得指定歌单 ID 的评论列表
    @GetMapping("/comment/songList/detail")
    public R commentOfSongListId(@RequestParam Integer songListId) {
        return commentService.commentOfSongListId(songListId);
    }

    // 点赞
    @PostMapping("/comment/like")
    public R commentOfLike(@RequestBody CommentRequest upCommentRequest) {
        return commentService.updateCommentMsg(upCommentRequest);
    }


}
