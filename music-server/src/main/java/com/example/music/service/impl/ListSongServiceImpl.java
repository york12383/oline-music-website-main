package com.example.music.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.music.common.R;
import com.example.music.mapper.ListSongMapper;
import com.example.music.mapper.SongListMapper;
import com.example.music.model.domain.ListSong;
import com.example.music.model.domain.SongList;
import com.example.music.model.request.ListSongRequest;
import com.example.music.service.ListSongService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.List;

@Service
public class ListSongServiceImpl extends ServiceImpl<ListSongMapper, ListSong> implements ListSongService {

    @Autowired
    private ListSongMapper listSongMapper;

    @Override
    public List<ListSong> allListSong() {
        return listSongMapper.selectList(null);
    }

    @Override
    public R updateListSongMsg(ListSongRequest updateListSongRequest, HttpSession session) {


        if (!checkSongListPermission(updateListSongRequest, null)) {
            return R.error("无权限");
        }

        ListSong listSong = new ListSong();
        BeanUtils.copyProperties(updateListSongRequest, listSong);
        if (listSongMapper.updateById(listSong) > 0) {
            return R.success("修改成功");
        } else {
            return R.error("修改失败");
        }
    }

    @Override
    public R deleteListSong(Integer songId) {
        QueryWrapper<ListSong> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_id",songId);
        if (listSongMapper.delete(queryWrapper) > 0) {
            return R.success("删除成功");
        } else {
            return R.error("删除失败");
        }
    }

    @Override
    public R deleteListSongUerr(Integer songId,Integer listSongId, HttpSession session) {

        //songListMapper.getSongListByConsumer( (int)session.getAttribute("userId"));

        QueryWrapper<ListSong> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_id",songId);
        queryWrapper.eq("song_list_id",listSongId);

        if (listSongMapper.delete(queryWrapper) > 0) {
            return R.success("删除成功");
        } else {
            return R.error("删除失败");
        }
    }


    @Override
    public R addListSong(ListSongRequest addListSongRequest) {
        // 检查歌曲是否已存在于该歌单中
        QueryWrapper<ListSong> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id", addListSongRequest.getSongListId());
        queryWrapper.eq("song_id", addListSongRequest.getSongId());

        if (listSongMapper.selectCount(queryWrapper) > 0) {
            return R.error("该歌曲已存在于歌单中，无法重复添加");
        }
        ListSong listSong = new ListSong();
        BeanUtils.copyProperties(addListSongRequest, listSong);
        if (listSongMapper.insert(listSong) > 0) {
            return R.success("添加成功");
        } else {
            return R.error("添加失败");
        }
    }



    @Override
    public R listSongOfSongId(Integer songListId) {
        QueryWrapper<ListSong> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("song_list_id",songListId);
        return R.success("查询成功", listSongMapper.selectList(queryWrapper));
    }



    @Autowired
    private SongListMapper songListMapper;

    //用户ID与歌单创建者ID匹配
    //用户具有管理员权限
    private boolean checkSongListPermission(ListSongRequest addListSongRequest, HttpSession session){
        try {
            // 检查是否为管理员
            Integer adminId = (Integer) session.getAttribute("adminID");
            if (adminId != null) {
                return true;
            }

            // 获取当前用户ID
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
                return false;
            }

            // 查询歌单信息
            SongList songList = songListMapper.selectById(addListSongRequest.getSongListId());
            if (songList == null) {
                return false;
            }

            // 检查用户ID是否与歌单创建者ID匹配
            if (userId.equals(songList.getConsumer())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }



}
