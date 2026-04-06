package com.example.music.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.example.music.model.domain.RankSong;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 歌曲评分表的 Mapper 接口
 */
@Mapper // 标识这是一个 MyBatis Mapper
public interface RankSongMapper extends BaseMapper<RankSong> {
    /**
     * 查总分
     */
    int selectScoreSum(int songId);

    Integer selectUserRankSong(@Param("consumer_id") Integer consumerId, @Param("song_id")  Integer songId);

}