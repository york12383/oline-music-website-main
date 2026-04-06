package com.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.mapper.CommentMapper;

import com.example.music.model.domain.Comment;

import com.example.music.model.request.CommentRequest;
import com.example.music.service.CommentService;
import com.example.music.service.RecommendService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Date;


@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
    @Autowired
    private CommentMapper commentMapper;


    // 添加评论，添加用户评论行为记录
    @Override
    public R addComment(CommentRequest addCommentRequest) {
        Comment comment = new Comment();
        BeanUtils.copyProperties(addCommentRequest, comment);
        //提交评论（复用：支持歌曲/歌单评论，通过type区分）
        comment.setType(addCommentRequest.getNowType());
        if (commentMapper.insert(comment) > 0) {
            return R.success("评论成功");
        } else {
            return R.error("评论失败");
        }
    }

    @Override
    public R updateCommentMsg(CommentRequest addCommentRequest) {
        Comment comment = new Comment();
        BeanUtils.copyProperties(addCommentRequest, comment);
        if (commentMapper.updateById(comment) > 0) {
            return R.success("点赞成功");
        } else {
            return R.error("点赞失败");
        }
    }

    //删除评论
    @Override
    public R deleteComment(Integer id, HttpSession session) {

        if (session.getAttribute("name") != null) {//管理员
            commentMapper.deleteById(id);
            return R.success("删除成功");
        }
        Comment comment = commentMapper.selectById(id);
        if (session.getAttribute("userId") != null) {
            Integer currentUserId = (Integer) session.getAttribute("userId");
            // 如果当前用户是评论发布者，则允许删除
            if (currentUserId.equals(comment.getUserId())) {
                if (commentMapper.deleteById(id) > 0) {
                    return R.success("删除成功");
                } else {
                    return R.error("删除失败");
                }
            }
        }
        return R.error("删除失败");
    }

    @Override//查看指定歌曲评论
    public R commentOfSongId(Integer songId) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_id",songId);
        return R.success(null, commentMapper.selectList(queryWrapper));
    }
    @Override//查看指定歌单评论
    public R commentOfSongListId(Integer songListId) {
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id",songListId);
        return R.success(null, commentMapper.selectList(queryWrapper));
    }
}
