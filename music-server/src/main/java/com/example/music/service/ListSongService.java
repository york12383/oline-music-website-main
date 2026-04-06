package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.common.R;
import com.example.music.model.domain.ListSong;
import com.example.music.model.request.ListSongRequest;

import javax.servlet.http.HttpSession;
import java.util.List;

public interface ListSongService extends IService<ListSong> {

    R addListSong(ListSongRequest addListSongRequest);

    R updateListSongMsg(ListSongRequest updateListSongRequest, HttpSession session);

    R deleteListSong(Integer songId);

    R deleteListSongUerr(Integer songId,Integer listSongId, HttpSession session);

    //看看这啥
    List<ListSong> allListSong();

    R listSongOfSongId(Integer songListId);
}
