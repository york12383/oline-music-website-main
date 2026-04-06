package com.example.music.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.music.common.R;
import com.example.music.model.domain.Song;
import com.example.music.model.request.SongRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;

public interface SongService extends IService<Song> {

    R addSong (SongRequest addSongRequest, MultipartFile lrcfile, MultipartFile mpfile, HttpSession session);

    R updateSongMsg(SongRequest updateSongRequest, HttpSession session);

    R updateSongUrl(MultipartFile urlFile, int id);

    R updateSongPic(MultipartFile urlFile, int id);

    R deleteSong(Integer id);

    R allSong();

    R hotSongs(Integer limit);

    R songOfSingerId(Integer singerId, HttpSession session);

    R songPageOfSingerId(Integer singerId, Integer pageNum, Integer pageSize, HttpSession session);

    R songOfuserSingerId(HttpSession session);

    R songOfId(Integer id);

    R songOfSingerName(String name);

    R updateSongLrc(MultipartFile lrcFile, int id);

    R batchUpdateSongLrc(MultipartFile[] lrcFiles, Integer singerId, HttpSession session);

    R updateSongStatus(int songId, int singerId, HttpSession session);

    R adminUpdateSongStatus(int songId, int type);

    R getPageSongs(Integer pageNum, Integer pageSize, String name, String lyricStatus, String resourceStatus);
}
