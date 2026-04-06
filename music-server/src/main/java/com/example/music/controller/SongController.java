package com.example.music.controller;

import com.example.music.common.R;
import com.example.music.model.request.SongRequest;
import com.example.music.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpSession;

@RestController
public class SongController {

    @Autowired
    private SongService songService;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.of(20, DataUnit.MEGABYTES));
        factory.setMaxRequestSize(DataSize.of(20, DataUnit.MEGABYTES));
        return factory.createMultipartConfig();
    }

    // 添加歌曲
    @PostMapping("/song/add")
    public R addSong(SongRequest addSongRequest,
                     @RequestParam("lrcfile") MultipartFile lrcfile,
                     @RequestParam("file") MultipartFile mpfile,
                     HttpSession session) {
        return songService.addSong(addSongRequest, lrcfile, mpfile, session);
    }

    // 删除歌曲
    @DeleteMapping("/song/delete")
    public R deleteSong(@RequestParam int id) {
        return songService.deleteSong(id);
    }

    // 返回所有歌曲
    @GetMapping("/song")
    public R allSong() {
        return songService.allSong();
    }

    // 返回热门歌曲
    @GetMapping("/song/hot")
    public R hotSongs(@RequestParam(defaultValue = "10") Integer limit) {
        return songService.hotSongs(limit);
    }

    // 返回指定歌曲ID的歌曲
    @GetMapping("/song/detail")
    public R songOfId(@RequestParam int id) {
        return songService.songOfId(id);
    }

    // 返回指定歌手ID的歌曲
    @GetMapping("/song/singer/detail")
    public R songOfSingerId(@RequestParam int singerId, HttpSession session) {
        return songService.songOfSingerId(singerId, session);
    }

    // 分页返回指定歌手ID的歌曲
    @GetMapping("/song/singer/page")
    public R songPageOfSingerId(@RequestParam int singerId,
                                @RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                HttpSession session) {
        return songService.songPageOfSingerId(singerId, pageNum, pageSize, session);
    }

    // 用户获取自己上传的歌曲
    @GetMapping("/song/singer/user/detail")
    public R songOfuserSingerId(HttpSession session) {
        return songService.songOfuserSingerId(session);
    }

    // 返回指定歌手名的歌曲
    @GetMapping("/song/singerName/detail")
    public R songOfSingerName(@RequestParam String name) {
        return songService.songOfSingerName('%' + name + '%');
    }

    // 更新歌曲信息
    @PostMapping("/song/update")
    public R updateSongMsg(HttpSession session, @RequestBody SongRequest updateSongRequest) {
        return songService.updateSongMsg(updateSongRequest, session);
    }

    // 更新歌曲图片
    @PostMapping("/song/img/update")
    public R updateSongPic(@RequestParam("file") MultipartFile urlFile, @RequestParam("id") int id) {
        return songService.updateSongPic(urlFile, id);
    }

    // 更新歌曲文件
    @PostMapping("/song/url/update")
    public R updateSongUrl(@RequestParam("file") MultipartFile urlFile, @RequestParam("id") int id) {
        return songService.updateSongUrl(urlFile, id);
    }

    // 更新歌词
    @PostMapping("/song/lrc/update")
    public R updateSongLrc(@RequestParam("file") MultipartFile lrcFile, @RequestParam("id") int id) {
        return songService.updateSongLrc(lrcFile, id);
    }

    // 批量更新歌词
    @PostMapping("/song/lrc/batch/update")
    public R batchUpdateSongLrc(@RequestParam("files") MultipartFile[] lrcFiles,
                                @RequestParam(value = "singerId", required = false) Integer singerId,
                                HttpSession session) {
        return songService.batchUpdateSongLrc(lrcFiles, singerId, session);
    }

    // 用户更新歌曲状态
    @PostMapping("/song/update/status")
    public R updateSongStatus(@RequestParam("songId") int songId,
                              @RequestParam("singerId") int singerId,
                              HttpSession session) {
        return songService.updateSongStatus(songId, singerId, session);
    }

    @PostMapping("/song/admin/update")
    public R adminUpdateSongStatus(@RequestParam("songId") int songId,
                                   @RequestParam("type") int type,
                                   HttpSession session) {
        if (session.getAttribute("name") == null) {
            return R.error("权限不足，只有管理员可以执行此操作");
        }

        return songService.adminUpdateSongStatus(songId, type);
    }

    // 歌曲管理分页查询
    @GetMapping("/song/page")
    public R getPageSongs(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String lyricStatus,
                          @RequestParam(required = false) String resourceStatus) {
        return songService.getPageSongs(pageNum, pageSize, name, lyricStatus, resourceStatus);
    }
}
