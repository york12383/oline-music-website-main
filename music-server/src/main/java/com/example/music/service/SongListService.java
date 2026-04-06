package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.common.R;
import com.example.music.model.domain.SongList;
import com.example.music.model.request.SongListRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.util.List;

public interface SongListService extends IService<SongList> {

    R addSongList(SongListRequest addSongListRequest);

    R updateSongListMsg(SongListRequest updateSongListRequest);

    R updateSongListImg(MultipartFile avatorFile, int id);

    R deleteSongList(Integer id);

    R allSongList();

    List<SongList> findAllSong();

    R likeTitle(String title);

    R likeStyle(String style);

    R songListOfId(Integer id);

    R songListByPage(Integer currentPage, Integer pageSize);

    R getSongListByConsumerId(HttpSession session);

    R updateUserSongListMsg(SongListRequest updateSongListRequest, HttpSession session);

    R addUserSongList(SongListRequest addSongListRequest, HttpSession session);

    R getHotSongListsByRating(Integer limit);
}