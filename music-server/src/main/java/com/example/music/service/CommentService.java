package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.common.R;
import com.example.music.model.domain.Comment;
import com.example.music.model.request.CommentRequest;

import javax.servlet.http.HttpSession;

public interface CommentService extends IService<Comment> {

    R addComment(CommentRequest addCommentRequest);

    R updateCommentMsg(CommentRequest upCommentRequest);

    R deleteComment(Integer id, HttpSession session);

    R commentOfSongId(Integer songId);

    R commentOfSongListId(Integer songListId);

}
